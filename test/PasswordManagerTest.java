import interfaces.ICryptoService;
import interfaces.IPasswordRepository;
import manager.PasswordManager;
import model.PasswordEntry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PasswordManagerTest {

    private ICryptoService crypto;
    private IPasswordRepository repository;
    private PasswordManager manager;

    private SecretKey secretKey;

    private PasswordEntry entry(String account, String username, String password) {
        return new PasswordEntry.Builder(account)
                .username(username)
                .password(password)
                .build();
    }

    @Before
    public void setUp() {
        crypto = mock(ICryptoService.class);
        repository = mock(IPasswordRepository.class);
        manager = new PasswordManager(crypto, repository);
        secretKey = new SecretKeySpec(new byte[16], "AES");
    }

    @Test
    public void initialize_noMasterPassword_shouldSetMasterPassword() {
        when(repository.getEncryptedMasterPassword()).thenReturn(null);
        when(crypto.generateSalt()).thenReturn(new byte[]{1,2,3});
        when(crypto.deriveKey(anyString(), any())).thenReturn(secretKey);
        when(crypto.encrypt(anyString(), any())).thenReturn("encrypted");

        System.setIn(new ByteArrayInputStream("master123\nmaster123\n".getBytes()));

        boolean result = manager.initialize();

        assertTrue(result);
        verify(repository).setSalt(any());
        verify(repository).setEncryptedMasterPassword("encrypted");
        verify(repository).save();
    }

    @Test
    public void authenticate_correctPassword_shouldReturnTrue() {
        when(repository.getSalt()).thenReturn(new byte[]{1});
        when(repository.getEncryptedMasterPassword()).thenReturn("encrypted");
        when(crypto.deriveKey(anyString(), any())).thenReturn(secretKey);
        when(crypto.encrypt(anyString(), any())).thenReturn("encrypted");

        System.setIn(new ByteArrayInputStream("master123\n".getBytes()));

        boolean result = manager.authenticate();

        assertTrue(result);
    }

    @Test
    public void start_shouldNotCallRun_whenInitializeReturnsFalse_usingSpy() {
        // create a spy of the manager so we can stub initialize() without invoking real logic
        PasswordManager spyManager = spy(new PasswordManager(crypto, repository));

        // stub initialize to return false
        doReturn(false).when(spyManager).initialize();

        // call start
        spyManager.start();

        // run() should never be called
        verify(spyManager, never()).run();
    }

    @Test
    public void start_shouldCallRun_whenInitializeReturnsTrue_usingSubclass() {
        // create a small subclass to capture run() invocation without side effects
        class TestablePasswordManager extends PasswordManager {
            boolean runCalled = false;
            TestablePasswordManager(ICryptoService c, IPasswordRepository r) { super(c, r); }
            @Override public void run() { runCalled = true; }
        }

        TestablePasswordManager testMgr = spy(new TestablePasswordManager(crypto, repository));
        // stub initialize to return true
        doReturn(true).when(testMgr).initialize();

        testMgr.start();

        // ensure our overridden run() was called
        assertTrue(testMgr.runCalled);
    }

    @Test
    public void authenticate_wrongPasswordThreeTimes_shouldReturnFalse() {
        when(repository.getSalt()).thenReturn(new byte[]{1});
        when(repository.getEncryptedMasterPassword()).thenReturn("correct");
        when(crypto.deriveKey(anyString(), any())).thenReturn(secretKey);
        when(crypto.encrypt(anyString(), any())).thenReturn("wrong");

        System.setIn(new ByteArrayInputStream(
                "a\nb\nc\n".getBytes()
        ));

        boolean result = manager.authenticate();

        assertFalse(result);
    }

    @Test
    public void addPassword_newAccount_shouldEncryptAndSave() {
        // repository.find returns a Map; return empty map to indicate no existing account
        when(repository.find("gmail")).thenReturn(new HashMap<>());
        when(crypto.encryptEntry(any(PasswordEntry.class), any())).thenReturn("encrypted");
        when(repository.addEncryptedEntry(eq("encrypted"), eq("gmail"))).thenReturn(true);

        System.setIn(new ByteArrayInputStream(
                "gmail\nuser\npass\nnotes\nurl\n".getBytes()
        ));

        manager.secretKey = secretKey;
        manager.addPassword(new Scanner(System.in));

        ArgumentCaptor<PasswordEntry> captor =
                ArgumentCaptor.forClass(PasswordEntry.class);

        verify(crypto).encryptEntry(captor.capture(), any());
        verify(repository).addEncryptedEntry("encrypted", "gmail");
        verify(repository).save();

        PasswordEntry entry = captor.getValue();
        assertEquals("gmail", entry.getAccountName());
        assertEquals("user", entry.getUsername());
        assertEquals("pass", entry.getPassword());
    }

    @Test
    public void addPassword_shouldNotAdd_whenInputInvalid() {
        when(repository.find("gmail")).thenReturn(new HashMap<>());

        System.setIn(new ByteArrayInputStream(
                "gmail\n\npass\nnotes\nurl\n".getBytes() // empty username
        ));

        manager.addPassword(new Scanner(System.in));

        verify(crypto, never()).encryptEntry(any(PasswordEntry.class), any());
        verify(repository, never()).addEncryptedEntry(anyString(), anyString());
        verify(repository, never()).save();
    }

    @Test
    public void removePassword_existingAccount_shouldRemoveAndSave() {
        when(repository.remove("gmail")).thenReturn(true);

        System.setIn(new ByteArrayInputStream("gmail\n".getBytes()));

        manager.removePassword(new Scanner(System.in));

        verify(repository).remove("gmail");
        verify(repository).save();
    }

    @Test
    public void viewPasswords_shouldDecryptAndPrint() {
        HashMap<String, String> map = new HashMap<>();
        map.put("gmail", "encrypted");

        when(repository.getEntries()).thenReturn(map);
        when(crypto.decryptEntry("encrypted", secretKey)).thenReturn(entry("gmail", "user", "pass"));

        manager.secretKey = secretKey;

        manager.viewPasswords();

        verify(crypto).decryptEntry("encrypted", secretKey);
    }

    @Test
    public void authenticate_succeeds_onSecondAttempt() {
        // first attempt wrong, second attempt correct
        when(repository.getSalt()).thenReturn(new byte[]{1});
        when(repository.getEncryptedMasterPassword()).thenReturn("encrypted");
        when(crypto.deriveKey(anyString(), any())).thenReturn(secretKey);
        // first call to encrypt returns "wrong", second returns "encrypted"
        when(crypto.encrypt(anyString(), any())).thenReturn("wrong", "encrypted");

        System.setIn(new ByteArrayInputStream("firstTry\nmaster123\n".getBytes()));

        boolean result = manager.authenticate();

        assertTrue(result);
        // deriveKey should have been called at least twice
        verify(crypto, atLeast(2)).deriveKey(anyString(), any());
    }

    @Test
    public void addPassword_existingAccount_shouldNotAddAndNotSave() {
        // repository.find returns a Map; return a non-empty map to indicate existing account
        HashMap<String, String> existing = new HashMap<>();
        existing.put("gmail", "enc");
        when(repository.find("gmail")).thenReturn(existing);

        System.setIn(new ByteArrayInputStream(
                "gmail\nuser\npass\nnotes\nurl\n".getBytes()
        ));

        manager.secretKey = secretKey;
        manager.addPassword(new Scanner(System.in));

        // should not add when account exists
        verify(crypto, never()).encryptEntry(any(PasswordEntry.class), any());
        verify(repository, never()).addEncryptedEntry(anyString(), anyString());
        verify(repository, never()).save();
    }

    @Test
    public void updateEntry_existingAccount_shouldEncryptAndImport() {
        HashMap<String, String> entries = new HashMap<>();
        entries.put("gmail", "enc");

        HashMap<String, String> found = new HashMap<>();
        found.put("gmail", "enc");

        when(repository.find("gmail")).thenReturn(found);
        when(repository.getEntries()).thenReturn(entries);
        when(crypto.decryptEntry("enc", secretKey)).thenReturn(entry("gmail", "existing", "oldpass"));
        when(crypto.encryptEntry(any(PasswordEntry.class), any())).thenReturn("newenc");

        System.setIn(new ByteArrayInputStream("gmail\nnewuser\nnewpass\nnewnotes\nnewurl\n".getBytes()));

        manager.secretKey = secretKey;
        manager.updateEntry(new Scanner(System.in));

        verify(crypto).encryptEntry(any(PasswordEntry.class), eq(secretKey));
        verify(repository).importEntries(any());
        verify(repository).save();
    }

    @Test
    public void searchPassword_notFound_shouldPrintMessage() {
        // repository.find returns a Map; return empty map to indicate not found
        when(repository.find("unknown")).thenReturn(new HashMap<>());

        System.setIn(new ByteArrayInputStream("unknown\n".getBytes()));

        manager.searchPassword(new Scanner(System.in));

        verify(repository).find("unknown");
    }

    @Test
    public void updateMasterPassword_shouldReimportEntriesAndSave() {
        HashMap<String, String> entries = new HashMap<>();
        entries.put("a", "encA");
        entries.put("b", "encB");

        when(repository.getEntries()).thenReturn(entries);
        when(crypto.decryptEntry("encA", secretKey)).thenReturn(entry("a", "u", "pA"));
        when(crypto.decryptEntry("encB", secretKey)).thenReturn(entry("b", "v", "pB"));
        when(crypto.generateSalt()).thenReturn(new byte[]{9});
        when(crypto.deriveKey(anyString(), any())).thenReturn(secretKey);
        when(crypto.encrypt(anyString(), any())).thenReturn("newMasterEnc");
        when(crypto.encryptEntry(any(PasswordEntry.class), any())).thenReturn("reEncrypted");
        when(repository.addEncryptedEntry(anyString(), anyString())).thenReturn(true);

        // setMasterPassword input: master, master
        System.setIn(new ByteArrayInputStream("newmaster\nnewmaster\n".getBytes()));

        manager.secretKey = secretKey;

        manager.updateMasterPassword();

        // dump should be called to clear repository before re-import
        verify(repository).dump();
        // repository.addEncryptedEntry should be called for each former entry
        verify(repository, atLeast(2)).addEncryptedEntry(anyString(), anyString());
        verify(repository, times(2)).save();
    }

    @Test
    public void addPassword_emptyAccountOrPass_shouldNotAdd() {
        when(repository.find("")).thenReturn(new HashMap<>());
        when(repository.find("acct")).thenReturn(new HashMap<>());

        System.setIn(new ByteArrayInputStream("\nuser\npass\nnotes\nurl\n".getBytes())); // empty account
        manager.addPassword(new Scanner(System.in));
        verify(repository, never()).addEncryptedEntry(anyString(), anyString());

        System.setIn(new ByteArrayInputStream("acct\nuser\n\nnotes\nurl\n".getBytes())); // empty pass
        manager.addPassword(new Scanner(System.in));
        verify(repository, never()).addEncryptedEntry(anyString(), anyString());
    }

    @Test
    public void viewPasswords_emptyRepository_shouldNotCallDecrypt() {
        when(repository.getEntries()).thenReturn(new HashMap<>());

        manager.secretKey = secretKey;
        manager.viewPasswords();

        verify(crypto, never()).decryptEntry(anyString(), any());
    }

    @Test
    public void initialize_noMasterPassword_callsSetMasterAndReturnsTrue() {
        // repository has no master password -> setMasterPassword path
        when(repository.getEncryptedMasterPassword()).thenReturn(null);
        // mocks needed by setMasterPassword
        when(crypto.generateSalt()).thenReturn(new byte[]{1});
        when(crypto.deriveKey(anyString(), any())).thenReturn(secretKey);
        when(crypto.encrypt(anyString(), any())).thenReturn("encryptedMaster");

        // provide matching master password + confirmation
        System.setIn(new ByteArrayInputStream("newmaster\nnewmaster\n".getBytes()));

        boolean result = manager.initialize();

        // initialize should succeed (set master password)
        assertTrue(result);
        verify(repository).setEncryptedMasterPassword("encryptedMaster");
        verify(repository).save();
    }

    @Test
    public void removePassword_nonExistingAccount_callsRemoveAndSave() {
        when(repository.remove("nope")).thenReturn(false);

        System.setIn(new ByteArrayInputStream("nope\n".getBytes()));

        manager.removePassword(new Scanner(System.in));

        verify(repository).remove("nope");
        // implementation always calls save() after remove(...)
        verify(repository).save();
    }

    @Test
    public void addPassword_nullSecretKey_shouldCallEncryptAndAttemptAdd() {
        when(repository.find("site")).thenReturn(new HashMap<>());
        when(crypto.encryptEntry(any(PasswordEntry.class), any())).thenReturn("encrypted");
        when(repository.addEncryptedEntry(eq("encrypted"), eq("site"))).thenReturn(true);

        System.setIn(new ByteArrayInputStream("site\nuser\npass\nnotes\nurl\n".getBytes()));

        manager.secretKey = null; // not authenticated (current implementation does not guard)
        manager.addPassword(new Scanner(System.in));

        // current implementation will call encryptEntry even if secretKey is null
        verify(crypto).encryptEntry(any(PasswordEntry.class), isNull());
        // repository.addEncryptedEntry is attempted and save is called
        verify(repository).addEncryptedEntry("encrypted", "site");
        verify(repository).save();
    }

    @Test
    public void updateEntry_nonExistingAccount_shouldNotEncryptOrImport() {
        when(repository.find("missing")).thenReturn(new HashMap<>());

        System.setIn(new ByteArrayInputStream("missing\nnewuser\nnewpass\nnewnotes\nnewurl\n".getBytes()));

        manager.secretKey = secretKey;
        manager.updateEntry(new Scanner(System.in));

        verify(crypto, never()).encryptEntry(any(PasswordEntry.class), any());
        verify(repository, never()).importEntries(any());
    }

    @Test
    public void authenticate_withNoSalt_shouldFollowCryptoBehavior() {
        when(repository.getSalt()).thenReturn(null);
        when(repository.getEncryptedMasterPassword()).thenReturn("encrypted");
        when(crypto.deriveKey(anyString(), any())).thenReturn(secretKey);
        // make crypto.encrypt produce the same encrypted value so authenticate returns true
        when(crypto.encrypt(anyString(), any())).thenReturn("encrypted");

        System.setIn(new ByteArrayInputStream("master123\n".getBytes()));

        boolean result = manager.authenticate();

        // authentication succeeds because crypto.encrypt produced "encrypted"
        assertTrue(result);
        verify(crypto).deriveKey(anyString(), isNull());
        verify(crypto).encrypt(anyString(), any());
    }

    @Test
    public void searchPassword_existingAccount_shouldReturnEntry() {
        HashMap<String, String> results = new HashMap<>();
        results.put("acct", "enc");
        when(repository.find("acct")).thenReturn(results);
        when(crypto.decryptEntry("enc", secretKey)).thenReturn(entry("acct", "u", "p"));

        System.setIn(new ByteArrayInputStream("acct\n".getBytes()));

        manager.secretKey = secretKey;
        manager.searchPassword(new Scanner(System.in));

        verify(repository).find("acct");
        verify(crypto).decryptEntry("enc", secretKey);
    }

    @Test
    public void removePassword_emptyInput_shouldCallRemoveAndSaveWithEmptyString() {
        System.setIn(new ByteArrayInputStream("\n".getBytes()));

        manager.removePassword(new Scanner(System.in));

        // current implementation will call remove with empty string and then save
        verify(repository).remove("");
        verify(repository).save();
    }

    @Test
    public void updateMasterPassword_noEntries_shouldSetMasterAndSaveTwice() {
        when(repository.getEntries()).thenReturn(new HashMap<>());
        when(crypto.generateSalt()).thenReturn(new byte[]{9});
        when(crypto.deriveKey(anyString(), any())).thenReturn(secretKey);
        when(crypto.encrypt(anyString(), any())).thenReturn("newMasterEnc");

        System.setIn(new ByteArrayInputStream("newmaster\nnewmaster\n".getBytes()));

        manager.secretKey = secretKey;

        manager.updateMasterPassword();

        // setMasterPassword calls repository.save() once; updateMasterPassword calls save() again at end
        verify(repository, times(2)).save();
        verify(repository).dump();
    }

}

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
        when(repository.find("gmail")).thenReturn(null);
        when(crypto.encrypt(eq("pass"), any())).thenReturn("encrypted");

        System.setIn(new ByteArrayInputStream(
                "gmail\nuser\npass\n".getBytes()
        ));

        manager.secretKey = secretKey;
        manager.addPassword(new java.util.Scanner(System.in));

        ArgumentCaptor<PasswordEntry> captor =
                ArgumentCaptor.forClass(PasswordEntry.class);

        verify(repository).add(captor.capture());
        verify(repository).save();

        PasswordEntry entry = captor.getValue();
        assertEquals("gmail", entry.getAccountName());
        assertEquals("user", entry.getUsername());
        assertEquals("encrypted", entry.getEncryptedPassword());
    }

    @Test
    public void addPassword_shouldNotAdd_whenInputInvalid() {
        when(repository.find("gmail")).thenReturn(null);

        System.setIn(new ByteArrayInputStream(
                "gmail\n\npass\n".getBytes() // empty username
        ));

        manager.addPassword(new Scanner(System.in));

        verify(repository, never()).add(any());
        verify(repository, never()).save();
    }

    @Test
    public void removePassword_existingAccount_shouldRemoveAndSave() {
        when(repository.remove("gmail")).thenReturn(true);

        System.setIn(new ByteArrayInputStream("gmail\n".getBytes()));

        manager.removePassword(new java.util.Scanner(System.in));

        verify(repository).remove("gmail");
        verify(repository).save();
    }

    @Test
    public void viewPasswords_shouldDecryptAndPrint() {
        HashMap<String, PasswordEntry> map = new HashMap<>();
        map.put("gmail",
                new PasswordEntry("gmail", "user", "encrypted"));

        when(repository.getEntries()).thenReturn(map);
        when(crypto.decrypt("encrypted", secretKey)).thenReturn("plain");

        manager.secretKey = secretKey;

        manager.viewPasswords();

        verify(crypto).decrypt("encrypted", secretKey);
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
        when(repository.find("gmail")).thenReturn(new PasswordEntry("gmail", "existing", "enc"));

        System.setIn(new ByteArrayInputStream(
                "gmail\nuser\npass\n".getBytes()
        ));

        manager.secretKey = secretKey;
        manager.addPassword(new Scanner(System.in));

        // should not add when account exists
        verify(repository, never()).add(any());
        verify(repository, never()).save();
    }

    @Test
    public void updateEntry_existingAccount_shouldEncryptAndUpdate() {
        when(repository.find("gmail")).thenReturn(new PasswordEntry("gmail", "old", "oldenc"));
        when(crypto.encrypt(eq("newpass"), any())).thenReturn("newenc");
        when(repository.update(eq("gmail"), eq("newuser"), eq("newenc"))).thenReturn(true);

        System.setIn(new ByteArrayInputStream("gmail\nnewuser\nnewpass\n".getBytes()));

        manager.secretKey = secretKey;
        manager.updateEntry(new Scanner(System.in));

        verify(crypto).encrypt("newpass", secretKey);
        verify(repository).update("gmail", "newuser", "newenc");
    }

    @Test
    public void searchPassword_notFound_shouldPrintMessage() {
        when(repository.find("unknown")).thenReturn(null);

        System.setIn(new ByteArrayInputStream("unknown\n".getBytes()));

        manager.searchPassword(new Scanner(System.in));

        verify(repository).find("unknown");
    }

    @Test
    public void updateMasterPassword_shouldReimportEntriesAndSave() {
        Map<String, PasswordEntry> entries = new HashMap<>();
        entries.put("a", new PasswordEntry("a", "u", "encA"));
        entries.put("b", new PasswordEntry("b", "v", "encB"));

        when(repository.getEntries()).thenReturn(entries);
        when(crypto.generateSalt()).thenReturn(new byte[]{9});
        when(crypto.deriveKey(anyString(), any())).thenReturn(secretKey);
        when(crypto.encrypt(anyString(), any())).thenReturn("newMasterEnc");

        // setMasterPassword input: master, master
        System.setIn(new ByteArrayInputStream("newmaster\nnewmaster\n".getBytes()));

        manager.secretKey = secretKey;

        manager.updateMasterPassword();

        // dump should be called to clear repository before re-import
        verify(repository).dump();
        // repository.add should be called for each former entry with new master password stored
        verify(repository, atLeast(2)).add(any(PasswordEntry.class));
        verify(repository, times(2)).save();
    }

    @Test
    public void addPassword_emptyAccountOrPass_shouldNotAdd() {
        when(repository.find("")).thenReturn(null);

        System.setIn(new ByteArrayInputStream("\nuser\npass\n".getBytes())); // empty account
        manager.addPassword(new Scanner(System.in));
        verify(repository, never()).add(any());

        System.setIn(new ByteArrayInputStream("acct\nuser\n\n".getBytes())); // empty pass
        manager.addPassword(new Scanner(System.in));
        verify(repository, never()).add(any());
    }

    @Test
    public void viewPasswords_emptyRepository_shouldNotCallDecrypt() {
        when(repository.getEntries()).thenReturn(new HashMap<>());

        manager.secretKey = secretKey;
        manager.viewPasswords();

        verify(crypto, never()).decrypt(anyString(), any());
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
        when(repository.find("site")).thenReturn(null);
        when(crypto.encrypt(eq("pass"), any())).thenReturn("encrypted");
        when(repository.add(any(PasswordEntry.class))).thenReturn(true);

        System.setIn(new ByteArrayInputStream("site\nuser\npass\n".getBytes()));

        manager.secretKey = null; // not authenticated (current implementation does not guard)
        manager.addPassword(new Scanner(System.in));

        // current implementation will call encrypt even if secretKey is null
        verify(crypto).encrypt(eq("pass"), isNull());
        // repository.add is attempted and save is called
        verify(repository).add(any(PasswordEntry.class));
        verify(repository).save();
    }

    @Test
    public void updateEntry_nonExistingAccount_shouldNotEncryptOrUpdate() {
        when(repository.find("missing")).thenReturn(null);

        System.setIn(new ByteArrayInputStream("missing\nnewuser\nnewpass\n".getBytes()));

        manager.secretKey = secretKey;
        manager.updateEntry(new Scanner(System.in));

        verify(crypto, never()).encrypt(anyString(), any());
        verify(repository, never()).update(anyString(), anyString(), anyString());
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
        PasswordEntry entry = new PasswordEntry("acct", "u", "enc");
        when(repository.find("acct")).thenReturn(entry);

        System.setIn(new ByteArrayInputStream("acct\n".getBytes()));

        manager.secretKey = secretKey;
        manager.searchPassword(new Scanner(System.in));

        verify(repository).find("acct");
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

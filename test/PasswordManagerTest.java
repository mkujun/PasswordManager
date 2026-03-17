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

}

import interfaces.ICryptoService;
import interfaces.IPasswordRepository;
import model.PasswordEntry;
import org.junit.Before;
import org.junit.Test;
import service.PasswordsService;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PasswordsServiceTest {

    private ICryptoService crypto;
    private IPasswordRepository repository;
    private PasswordsService service;

    private SecretKey secretKey;
    private SecretKey otherKey;

    private PasswordEntry entry(String account, String username, String password) {
        return new PasswordEntry.Builder(account)
                .username(username)
                .password(password)
                .build();
    }

    /** Logs in successfully so the service holds {@link #secretKey}. */
    private void loginWithSecretKey() {
        byte[] salt = {1};
        when(repository.getSalt()).thenReturn(salt);
        when(repository.getEncryptedMasterPassword()).thenReturn("encryptedMaster");
        when(crypto.deriveKey("master123", salt)).thenReturn(secretKey);
        when(crypto.encrypt("master123", secretKey)).thenReturn("encryptedMaster");
        assertTrue(service.login("master123"));
    }

    @Before
    public void setUp() {
        crypto = mock(ICryptoService.class);
        repository = mock(IPasswordRepository.class);
        service = new PasswordsService(crypto, repository);
        secretKey = new SecretKeySpec(new byte[16], "AES");
        otherKey = new SecretKeySpec(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, "AES");
    }

    // ---------------------------------------------------------------
    //  Master password
    // ---------------------------------------------------------------

    @Test
    public void masterPasswordExists_noStoredPassword_returnsFalse() {
        when(repository.getEncryptedMasterPassword()).thenReturn(null);

        assertFalse(service.masterPasswordExists());
    }

    @Test
    public void masterPasswordExists_storedPassword_returnsTrue() {
        when(repository.getEncryptedMasterPassword()).thenReturn("encryptedMaster");

        assertTrue(service.masterPasswordExists());
    }

    @Test
    public void setMasterPassword_shouldStoreSaltAndEncryptedPasswordAndSave() {
        byte[] salt = {1, 2, 3};
        when(crypto.generateSalt()).thenReturn(salt);
        when(crypto.deriveKey("master123", salt)).thenReturn(secretKey);
        when(crypto.encrypt("master123", secretKey)).thenReturn("encryptedMaster");

        service.setMasterPassword("master123");

        verify(repository).setSalt(salt);
        verify(repository).setEncryptedMasterPassword("encryptedMaster");
        verify(repository).save();
    }

    @Test
    public void setMasterPassword_shouldUseDerivedKeyForFollowingOperations() {
        byte[] salt = {1, 2, 3};
        when(crypto.generateSalt()).thenReturn(salt);
        when(crypto.deriveKey("master123", salt)).thenReturn(secretKey);
        when(crypto.encrypt("master123", secretKey)).thenReturn("encryptedMaster");

        service.setMasterPassword("master123");
        service.addPasswordEntry(entry("gmail", "user", "pass"));

        verify(crypto).encryptEntry(any(PasswordEntry.class), eq(secretKey));
    }

    // ---------------------------------------------------------------
    //  Login
    // ---------------------------------------------------------------

    @Test
    public void login_correctPassword_returnsTrue() {
        when(repository.getSalt()).thenReturn(new byte[]{1});
        when(repository.getEncryptedMasterPassword()).thenReturn("encryptedMaster");
        when(crypto.deriveKey(anyString(), any())).thenReturn(secretKey);
        when(crypto.encrypt(anyString(), any())).thenReturn("encryptedMaster");

        assertTrue(service.login("master123"));
    }

    @Test
    public void login_wrongPassword_returnsFalse() {
        when(repository.getSalt()).thenReturn(new byte[]{1});
        when(repository.getEncryptedMasterPassword()).thenReturn("encryptedMaster");
        when(crypto.deriveKey(anyString(), any())).thenReturn(secretKey);
        when(crypto.encrypt(anyString(), any())).thenReturn("somethingElse");

        assertFalse(service.login("wrong"));
    }

    @Test
    public void login_shouldDeriveKeyFromStoredSalt() {
        byte[] salt = {7, 7, 7};
        when(repository.getSalt()).thenReturn(salt);
        when(repository.getEncryptedMasterPassword()).thenReturn("encryptedMaster");
        when(crypto.deriveKey(anyString(), any())).thenReturn(secretKey);
        when(crypto.encrypt(anyString(), any())).thenReturn("encryptedMaster");

        service.login("master123");

        verify(crypto).deriveKey("master123", salt);
    }

    @Test
    public void login_correctPassword_shouldUseKeyForFollowingOperations() {
        loginWithSecretKey();

        service.addPasswordEntry(entry("gmail", "user", "pass"));

        verify(crypto).encryptEntry(any(PasswordEntry.class), eq(secretKey));
    }

    @Test
    public void login_wrongPassword_shouldNotSetKey() {
        when(repository.getSalt()).thenReturn(new byte[]{1});
        when(repository.getEncryptedMasterPassword()).thenReturn("encryptedMaster");
        when(crypto.deriveKey(anyString(), any())).thenReturn(otherKey);
        when(crypto.encrypt(anyString(), any())).thenReturn("somethingElse");

        service.login("wrong");
        service.addPasswordEntry(entry("gmail", "user", "pass"));

        verify(crypto).encryptEntry(any(PasswordEntry.class), isNull());
    }

    @Test
    public void login_wrongPasswordAfterSuccessfulLogin_shouldKeepPreviousKey() {
        loginWithSecretKey();

        when(crypto.deriveKey(eq("wrong"), any())).thenReturn(otherKey);
        when(crypto.encrypt("wrong", otherKey)).thenReturn("somethingElse");
        assertFalse(service.login("wrong"));

        service.addPasswordEntry(entry("gmail", "user", "pass"));

        verify(crypto).encryptEntry(any(PasswordEntry.class), eq(secretKey));
        verify(crypto, never()).encryptEntry(any(PasswordEntry.class), eq(otherKey));
    }

    // ---------------------------------------------------------------
    //  View / get
    // ---------------------------------------------------------------

    @Test
    public void viewPasswords_emptyFilter_returnsAllDecryptedEntries() {
        loginWithSecretKey();
        Map<String, String> stored = new HashMap<>();
        stored.put("gmail", "encGmail");
        stored.put("github", "encGithub");
        when(repository.getEntries()).thenReturn(stored);
        when(crypto.decryptEntry("encGmail", secretKey)).thenReturn(entry("gmail", "u1", "p1"));
        when(crypto.decryptEntry("encGithub", secretKey)).thenReturn(entry("github", "u2", "p2"));

        Map<String, PasswordEntry> result = service.viewPasswords("");

        assertEquals(2, result.size());
        assertEquals("u1", result.get("gmail").getUsername());
        assertEquals("u2", result.get("github").getUsername());
    }

    @Test
    public void viewPasswords_filter_returnsOnlyMatchingAccounts() {
        loginWithSecretKey();
        Map<String, String> stored = new HashMap<>();
        stored.put("gmail", "encGmail");
        stored.put("github", "encGithub");
        when(repository.getEntries()).thenReturn(stored);
        when(crypto.decryptEntry("encGmail", secretKey)).thenReturn(entry("gmail", "u1", "p1"));
        when(crypto.decryptEntry("encGithub", secretKey)).thenReturn(entry("github", "u2", "p2"));

        Map<String, PasswordEntry> result = service.viewPasswords("hub");

        assertEquals(1, result.size());
        assertTrue(result.containsKey("github"));
    }

    @Test
    public void viewPasswords_emptyRepository_shouldNotDecrypt() {
        when(repository.getEntries()).thenReturn(new HashMap<>());

        Map<String, PasswordEntry> result = service.viewPasswords("");

        assertTrue(result.isEmpty());
        verify(crypto, never()).decryptEntry(anyString(), any());
    }

    @Test
    public void getPasswordEntry_shouldDecryptStoredEntry() {
        loginWithSecretKey();
        when(repository.getEntry("gmail")).thenReturn("encGmail");
        when(crypto.decryptEntry("encGmail", secretKey)).thenReturn(entry("gmail", "user", "pass"));

        PasswordEntry result = service.getPasswordEntry("gmail");

        assertEquals("gmail", result.getAccountName());
        assertEquals("user", result.getUsername());
        assertEquals("pass", result.getPassword());
    }

    @Test
    public void accountExists_shouldDelegateToRepository() {
        when(repository.accountExists("gmail")).thenReturn(true);
        when(repository.accountExists("missing")).thenReturn(false);

        assertTrue(service.accountExists("gmail"));
        assertFalse(service.accountExists("missing"));
    }

    // ---------------------------------------------------------------
    //  Add / update / delete
    // ---------------------------------------------------------------

    @Test
    public void addPasswordEntry_shouldEncryptAddAndSave() {
        loginWithSecretKey();
        PasswordEntry newEntry = entry("gmail", "user", "pass");
        when(crypto.encryptEntry(newEntry, secretKey)).thenReturn("encGmail");

        service.addPasswordEntry(newEntry);

        verify(repository).addEncryptedEntry("encGmail", "gmail");
        verify(repository).save();
    }

    @Test
    public void updatePasswordEntry_shouldEncryptUpdateAndSave() {
        loginWithSecretKey();
        PasswordEntry updated = entry("gmail", "newUser", "newPass");
        when(crypto.encryptEntry(updated, secretKey)).thenReturn("newEnc");

        service.updatePasswordEntry("gmail", updated);

        verify(repository).updateEntry("gmail", "newEnc");
        verify(repository).save();
    }

    @Test
    public void deletePasswordEntry_shouldRemoveAndSave() {
        service.deletePasswordEntry("gmail");

        verify(repository).remove("gmail");
        verify(repository).save();
    }

    @Test
    public void deleteAllEntries_shouldDumpRepository() {
        service.deleteAllEntries();

        verify(repository).dump();
    }
}

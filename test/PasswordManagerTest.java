import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

public class PasswordManagerTest {

    private PasswordManager manager;
    private SecretKey key;

    @Before
    public void setUp() throws Exception {
        manager = new PasswordManager();

        // Create a mock key for encryption/decryption testing
        byte[] salt = getSalt(manager);
        key = invokeDeriveKey(manager, "test123", salt);

        // Inject the static secretKey field (needed for encrypt/decrypt)
        Field f = PasswordManager.class.getDeclaredField("secretKey");
        f.setAccessible(true);
        f.set(null, key);
    }

    @Test
    public void testGenerateSaltLength() throws Exception {
        byte[] salt = invokeGenerateSalt(manager);
        assertNotNull(salt);
        assertEquals(16, salt.length);
    }

    @Test
    public void testDeriveKeyConsistency() throws Exception {
        byte[] salt = invokeGenerateSalt(manager);
        SecretKey key1 = invokeDeriveKey(manager, "password", salt);
        SecretKey key2 = invokeDeriveKey(manager, "password", salt);

        // Derived keys should be identical for same password and salt
        assertArrayEquals(key1.getEncoded(), key2.getEncoded());
    }

    @Test
    public void testEncryptDecrypt() throws Exception {
        String text = "helloWorld123";
        String encrypted = invokeEncrypt(manager, text);
        assertNotEquals(text, encrypted);

        String decrypted = invokeDecrypt(manager, encrypted);
        assertEquals(text, decrypted);
    }

    @Test
    public void testEntryExists() throws Exception {
        List<PasswordEntry> entries = new ArrayList<>();
        entries.add(new PasswordEntry("gmail", "user", "abc"));

        boolean exists = invokeEntryExists(manager, entries, "gmail");
        boolean notExists = invokeEntryExists(manager, entries, "facebook");

        assertTrue(exists);
        assertFalse(notExists);
    }

    @Test
    public void testFindEntryByAccountName() throws Exception {
        List<PasswordEntry> entries = new ArrayList<>();
        entries.add(new PasswordEntry("GitHub", "coder", "enc1"));
        entries.add(new PasswordEntry("Gmail", "mailUser", "enc2"));

        Optional<PasswordEntry> found = invokeFindEntry(manager, entries, "github");
        Optional<PasswordEntry> notFound = invokeFindEntry(manager, entries, "unknown");

        assertTrue(found.isPresent());
        assertEquals("GitHub", found.get().getAccountName());
        assertFalse(notFound.isPresent());
    }

    // ---------- Helper methods using reflection (for private methods) ----------

    private byte[] getSalt(PasswordManager manager) throws Exception {
        Field saltField = PasswordManager.class.getDeclaredField("salt");
        saltField.setAccessible(true);
        byte[] salt = new byte[16];
        new Random().nextBytes(salt);
        saltField.set(manager, salt);
        return salt;
    }

    private SecretKey invokeDeriveKey(PasswordManager manager, String password, byte[] salt) throws Exception {
        Method method = PasswordManager.class.getDeclaredMethod("deriveKey", String.class, byte[].class);
        method.setAccessible(true);
        return (SecretKey) method.invoke(manager, password, salt);
    }

    private byte[] invokeGenerateSalt(PasswordManager manager) throws Exception {
        Method method = PasswordManager.class.getDeclaredMethod("generateSalt");
        method.setAccessible(true); // allows reflection access
        return (byte[]) method.invoke(manager);
    }

    private String invokeEncrypt(PasswordManager manager, String plain) throws Exception {
        Method method = PasswordManager.class.getDeclaredMethod("encrypt", String.class);
        method.setAccessible(true);
        return (String) method.invoke(manager, plain);
    }

    private String invokeDecrypt(PasswordManager manager, String plain) throws Exception {
        Method method = PasswordManager.class.getDeclaredMethod("decrypt", String.class);
        method.setAccessible(true);
        return (String) method.invoke(manager, plain);
    }

    private boolean invokeEntryExists(PasswordManager manager, List<PasswordEntry> list, String name) throws Exception {
        Method method = PasswordManager.class.getDeclaredMethod("entryExists", List.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(manager, list, name);
    }

    @SuppressWarnings("unchecked")
    private Optional<PasswordEntry> invokeFindEntry(PasswordManager manager, List<PasswordEntry> list, String name) throws Exception {
        Method method = PasswordManager.class.getDeclaredMethod("findEntryByAccountName", List.class, String.class);
        method.setAccessible(true);
        return (Optional<PasswordEntry>) method.invoke(manager, list, name);
    }
}

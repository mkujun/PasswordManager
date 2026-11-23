import crypto.CryptoService;
import interfaces.ICryptoService;
import model.PasswordEntry;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.Assert.*;

public class CryptoServiceTest {

    private ICryptoService cryptoService;

    @Before
    public void setUp() {
        cryptoService = new CryptoService();
    }

    @Test
    public void generateSalt_shouldReturnNonNullSaltWithCorrectLength() {
        byte[] salt = cryptoService.generateSalt();

        assertNotNull(salt);
        assertEquals(16, salt.length);
    }

    @Test
    public void deriveKey_shouldReturnNonNullSecretKey() {
        byte[] salt = cryptoService.generateSalt();
        SecretKey key = cryptoService.deriveKey("masterPassword", salt);

        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
    }

    @Test
    public void encrypt_sameInputSameKey_shouldProduceSameOutput() {
        byte[] salt = cryptoService.generateSalt();
        SecretKey key = cryptoService.deriveKey("password", salt);

        String text = "test";
        String encrypted1 = cryptoService.encrypt(text, key);
        String encrypted2 = cryptoService.encrypt(text, key);

        assertEquals(encrypted1, encrypted2);
    }

    @Test
    public void generateSalt_shouldReturnDifferentValuesOnEachCall() {
        assertFalse(Arrays.equals(cryptoService.generateSalt(), cryptoService.generateSalt()));
    }

    @Test
    public void deriveKey_samePasswordAndSalt_shouldProduceSameKey() {
        byte[] salt = cryptoService.generateSalt();

        SecretKey key1 = cryptoService.deriveKey("password", salt);
        SecretKey key2 = cryptoService.deriveKey("password", salt);

        assertArrayEquals(key1.getEncoded(), key2.getEncoded());
        assertEquals(16, key1.getEncoded().length);
    }

    @Test
    public void deriveKey_differentSalt_shouldProduceDifferentKey() {
        SecretKey key1 = cryptoService.deriveKey("password", new byte[]{1, 2, 3});
        SecretKey key2 = cryptoService.deriveKey("password", new byte[]{4, 5, 6});

        assertFalse(Arrays.equals(key1.getEncoded(), key2.getEncoded()));
    }

    @Test
    public void deriveKey_differentPassword_shouldProduceDifferentKey() {
        byte[] salt = cryptoService.generateSalt();

        SecretKey key1 = cryptoService.deriveKey("password1", salt);
        SecretKey key2 = cryptoService.deriveKey("password2", salt);

        assertFalse(Arrays.equals(key1.getEncoded(), key2.getEncoded()));
    }

    @Test(expected = RuntimeException.class)
    public void deriveKey_nullPassword_shouldThrow() {
        cryptoService.deriveKey(null, cryptoService.generateSalt());
    }

    @Test(expected = RuntimeException.class)
    public void deriveKey_nullSalt_shouldThrow() {
        cryptoService.deriveKey("password", null);
    }

    @Test
    public void encrypt_shouldNotReturnPlainText() {
        SecretKey key = cryptoService.deriveKey("password", cryptoService.generateSalt());

        String encrypted = cryptoService.encrypt("secret", key);

        assertNotNull(encrypted);
        assertNotEquals("secret", encrypted);
    }

    @Test
    public void encrypt_differentKeys_shouldProduceDifferentOutput() {
        SecretKey key1 = cryptoService.deriveKey("password", new byte[]{1});
        SecretKey key2 = cryptoService.deriveKey("password", new byte[]{2});

        assertNotEquals(cryptoService.encrypt("text", key1), cryptoService.encrypt("text", key2));
    }

    @Test(expected = RuntimeException.class)
    public void encrypt_nullKey_shouldThrow() {
        cryptoService.encrypt("text", null);
    }

    @Test
    public void encryptEntry_decryptEntry_shouldRoundTripAllFields() {
        SecretKey key = cryptoService.deriveKey("password", cryptoService.generateSalt());
        LocalDateTime created = LocalDateTime.of(2026, 1, 2, 3, 4);
        LocalDateTime updated = LocalDateTime.of(2026, 5, 6, 7, 8);
        PasswordEntry entry = new PasswordEntry.Builder("gmail")
                .username("user")
                .password("pass")
                .notes("notes")
                .url("https://mail.google.com")
                .createdAt(created)
                .updatedAt(updated)
                .accountActive(false)
                .build();

        String encrypted = cryptoService.encryptEntry(entry, key);
        PasswordEntry decrypted = cryptoService.decryptEntry(encrypted, key);

        assertNotNull(encrypted);
        assertEquals("gmail", decrypted.getAccountName());
        assertEquals("user", decrypted.getUsername());
        assertEquals("pass", decrypted.getPassword());
        assertEquals("notes", decrypted.getNotes());
        assertEquals("https://mail.google.com", decrypted.getUrl());
        assertEquals(created, decrypted.getCreatedAt());
        assertEquals(updated, decrypted.getUpdatedAt());
        assertFalse(decrypted.isAccountActive());
    }

    @Test
    public void encryptEntry_shouldNotContainPlainPassword() {
        SecretKey key = cryptoService.deriveKey("password", cryptoService.generateSalt());
        PasswordEntry entry = new PasswordEntry.Builder("gmail").password("verySecret").build();

        String encrypted = cryptoService.encryptEntry(entry, key);
        String decoded = new String(Base64.getDecoder().decode(encrypted), StandardCharsets.ISO_8859_1);

        assertFalse(decoded.contains("verySecret"));
    }

    @Test(expected = RuntimeException.class)
    public void encryptEntry_nullKey_shouldThrow() {
        cryptoService.encryptEntry(new PasswordEntry.Builder("gmail").build(), null);
    }

    @Test(expected = RuntimeException.class)
    public void decryptEntry_wrongKey_shouldThrow() {
        SecretKey key = cryptoService.deriveKey("password", new byte[]{1});
        SecretKey wrongKey = cryptoService.deriveKey("other", new byte[]{1});
        String encrypted = cryptoService.encryptEntry(new PasswordEntry.Builder("gmail").build(), key);

        cryptoService.decryptEntry(encrypted, wrongKey);
    }

    @Test(expected = RuntimeException.class)
    public void decryptEntry_invalidBase64_shouldThrow() {
        SecretKey key = cryptoService.deriveKey("password", new byte[]{1});

        cryptoService.decryptEntry("not base64 !!!", key);
    }

    @Test(expected = RuntimeException.class)
    public void decryptEntry_encryptedNonEntry_shouldThrow() {
        SecretKey key = cryptoService.deriveKey("password", new byte[]{1});
        // Valid ciphertext, but the plain text is not a serialized PasswordEntry.
        String encrypted = cryptoService.encrypt("just text", key);

        cryptoService.decryptEntry(encrypted, key);
    }
}

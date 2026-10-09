import crypto.CryptoService;
import interfaces.ICryptoService;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;

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
}

import org.junit.*;
import javax.crypto.SecretKey;
import java.util.*;
import static org.junit.Assert.*;

public class CryptoServiceTest {

    private PasswordRepository repo;
    private CryptoService crypto;

    @Before
    public void setUp() {
        repo = new PasswordRepository(new PersistenceService("test_crypto.dat"));
        crypto = new CryptoService(repo);
        repo.setSalt(new byte[]{1, 2, 3, 4});
    }

    @After
    public void tearDown() {
        new java.io.File("test_crypto.dat").delete();
    }

    @Test
    public void testEncryptionAndDecryption() {
        crypto.setMasterPassword("mySecret");
        String original = "password123";
        String enc = crypto.encrypt(original);
        assertNotEquals(original, enc);
        String dec = crypto.decrypt(enc);
        assertEquals(original, dec);
    }

    @Test
    public void testAddPasswordEncryptsCorrectly() {
        crypto.setMasterPassword("test");
        crypto.addPassword("Gmail", "john", "abc123");

        PasswordEntry entry = repo.findByAccountName("Gmail").get();
        assertNotEquals("abc123", entry.getEncryptedPassword());
        assertEquals("abc123", crypto.decrypt(entry.getEncryptedPassword()));
    }

    @Test
    public void testUpdateAndRemove() {
        crypto.setMasterPassword("test");
        crypto.addPassword("LinkedIn", "jane", "pwd1");

        crypto.updateEntry("LinkedIn", "newJane", "pwd2");
        PasswordEntry entry = repo.findByAccountName("LinkedIn").get();
        assertEquals("newJane", entry.getUsername());
        assertEquals("pwd2", crypto.decrypt(entry.getEncryptedPassword()));

        assertTrue(crypto.removePassword("LinkedIn"));
        assertFalse(repo.findByAccountName("LinkedIn").isPresent());
    }
}

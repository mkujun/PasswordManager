import org.junit.*;
import java.io.File;
import java.util.*;
import static org.junit.Assert.*;

public class PersistenceServiceTest {

    private static final String TEST_FILE = "test_passwords.dat";
    private PersistenceService persistence;

    @Before
    public void setUp() {
        persistence = new PersistenceService(TEST_FILE);
    }

    @After
    public void tearDown() {
        new File(TEST_FILE).delete();
    }

    @Test
    public void testSaveAndLoad() {
        byte[] salt = new byte[]{1, 2, 3, 4};
        String encryptedMasterPassword = "abc123";
        HashMap<String, PasswordEntry> entries = new HashMap<>();
        entries.put("Gmail", new PasswordEntry("Gmail", "user", "enc123"));

        persistence.save(salt, encryptedMasterPassword, entries);

        PersistenceService.LoadedData data = persistence.load();
        assertNotNull(data);
        assertArrayEquals(salt, data.salt);
        assertEquals(encryptedMasterPassword, data.encryptedMasterPassword);
        assertEquals(1, data.entries.size());
        assertEquals("Gmail", data.entries.get(0).getAccountName());
    }

    @Test
    public void testLoadFromMissingFileReturnsNull() {
        new File(TEST_FILE).delete();
        PersistenceService.LoadedData data = persistence.load();
        assertNull(data);
    }
}

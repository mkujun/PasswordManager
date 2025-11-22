import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;

import model.PasswordEntry;
import persistence.PersistenceService;
import interfaces.IPersistenceService;

import static org.junit.Assert.*;

public class PersistenceServiceTest {

    private File tempFile;
    private PersistenceService persistenceService;

    @Before
    public void setUp() throws Exception {
        tempFile = File.createTempFile("passwords", ".dat");
        tempFile.deleteOnExit();
        persistenceService = new PersistenceService(tempFile.getAbsolutePath());
    }

    @After
    public void tearDown() {
        tempFile.delete();
    }

    @Test
    public void testSaveAndLoad() {
        byte[] salt = new byte[]{1, 2, 3};
        String encryptedMaster = "encrypted123";

        HashMap<String, PasswordEntry> entries = new HashMap<>();
        entries.put("gmail", new PasswordEntry("gmail", "user1", "enc-pass-1"));
        entries.put("facebook", new PasswordEntry("facebook", "user2", "enc-pass-2"));

        // Save
        persistenceService.save(salt, encryptedMaster, entries);

        // Load
        IPersistenceService.LoadedData data = persistenceService.load();

        assertNotNull(data);
        assertArrayEquals(salt, data.salt);
        assertEquals(encryptedMaster, data.encryptedMasterPassword);

        assertEquals(2, data.entries.size());
        assertTrue(data.entries.containsKey("gmail"));
        assertTrue(data.entries.containsKey("facebook"));
    }

    @Test
    public void testLoadReturnsNullWhenFileMissing() {
        // delete file manually
        tempFile.delete();

        IPersistenceService.LoadedData data = persistenceService.load();

        assertNull(data);
    }
}

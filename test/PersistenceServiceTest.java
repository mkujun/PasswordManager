import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

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

        HashMap<String, String> entries = new HashMap<>();
        entries.put("gmail", "enc-pass-1");
        entries.put("facebook", "enc-pass-2");

        // Save
        persistenceService.save(salt, encryptedMaster, entries);

        // Load
        IPersistenceService.LoadedData data = persistenceService.load();

        assertNotNull(data);
        assertArrayEquals(salt, data.salt);
        assertEquals(encryptedMaster, data.encryptedMasterPassword);

        assertEquals(2, data.encryptedEntries.size());
        assertTrue(data.encryptedEntries.containsKey("gmail"));
        assertTrue(data.encryptedEntries.containsKey("facebook"));
    }

    @Test
    public void testLoadReturnsNullWhenFileMissing() {
        // delete file manually
        tempFile.delete();

        IPersistenceService.LoadedData data = persistenceService.load();

        assertNull(data);
    }

    @Test
    public void testSaveOverwritesPreviousData() {
        HashMap<String, String> first = new HashMap<>();
        first.put("gmail", "enc1");
        persistenceService.save(new byte[]{1}, "master1", first);

        HashMap<String, String> second = new HashMap<>();
        second.put("github", "enc2");
        persistenceService.save(new byte[]{2}, "master2", second);

        IPersistenceService.LoadedData data = persistenceService.load();

        assertArrayEquals(new byte[]{2}, data.salt);
        assertEquals("master2", data.encryptedMasterPassword);
        assertEquals(1, data.encryptedEntries.size());
        assertEquals("enc2", data.encryptedEntries.get("github"));
    }

    @Test
    public void testSaveAndLoadNullValuesAndEmptyMap() {
        persistenceService.save(null, null, new HashMap<>());

        IPersistenceService.LoadedData data = persistenceService.load();

        assertNotNull(data);
        assertNull(data.salt);
        assertNull(data.encryptedMasterPassword);
        assertTrue(data.encryptedEntries.isEmpty());
    }

    @Test
    public void testLoadReturnsNullForCorruptFile() throws Exception {
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write("this is not serialized data".getBytes());
        }

        assertNull(persistenceService.load());
    }

    @Test
    public void testLoadReturnsNullForEmptyFile() {
        // File.createTempFile creates an empty file.
        assertNull(persistenceService.load());
    }

    @Test
    public void testSaveToInvalidPathDoesNotThrow() {
        File dir = tempFile.getParentFile();
        // A directory cannot be opened as a file: save must log, not throw.
        PersistenceService invalid = new PersistenceService(dir.getAbsolutePath());

        invalid.save(new byte[]{1}, "master", new HashMap<>());

        assertNull(invalid.load());
    }
}

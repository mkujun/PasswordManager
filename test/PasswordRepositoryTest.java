import interfaces.IPersistenceService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import persistence.PersistenceService;
import repository.PasswordRepository;

import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PasswordRepositoryTest {

    private IPersistenceService persistence;
    private PasswordRepository repository;

    @Before
    public void setUp() {
        persistence = mock(IPersistenceService.class);
    }

    @Test
    public void constructor_shouldLoadData_whenPersistenceReturnsData() {
        byte[] salt = new byte[]{1, 2, 3};
        String encryptedMaster = "encryptedMaster";

        HashMap<String, String> entries = new HashMap<>();
        entries.put("gmail", "encPass");

        PersistenceService.LoadedData data =
                new PersistenceService.LoadedData(salt, encryptedMaster, entries);

        when(persistence.load()).thenReturn(data);

        repository = new PasswordRepository(persistence);

        assertArrayEquals(salt, repository.getSalt());
        assertEquals(encryptedMaster, repository.getEncryptedMasterPassword());
        assertEquals(1, repository.getEntries().size());
    }

    @Test
    public void constructor_shouldStartEmpty_whenNoData() {
        when(persistence.load()).thenReturn(null);

        repository = new PasswordRepository(persistence);

        assertNotNull(repository.getEntries());
        assertTrue(repository.getEntries().isEmpty());
        assertNull(repository.getSalt());
        assertNull(repository.getEncryptedMasterPassword());
    }

    @Test
    public void addEncryptedEntry_shouldInsertNewEntry() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        boolean result = repository.addEncryptedEntry("enc", "gmail");

        assertTrue(result);
        assertEquals("enc", repository.find("gmail").get("gmail"));
    }

    @Test
    public void addEncryptedEntry_shouldFailForDuplicateAccount() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        assertTrue(repository.addEncryptedEntry("enc1", "gmail"));
        assertFalse(repository.addEncryptedEntry("enc2", "gmail"));
    }

    @Test
    public void remove_shouldRemoveExistingEntry() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        repository.addEncryptedEntry("enc", "gmail");

        boolean removed = repository.remove("gmail");

        assertTrue(removed);
        // find returns a Map; after removal it should be empty
        assertTrue(repository.find("gmail").isEmpty());
    }

    @Test
    public void remove_shouldReturnFalse_whenEntryDoesNotExist() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        assertFalse(repository.remove("unknown"));
    }

    @Test
    public void importEntries_shouldReplaceExistingEntry() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        repository.addEncryptedEntry("oldEnc", "gmail");

        HashMap<String, String> replacement = new HashMap<>();
        replacement.put("gmail", "newEnc");
        repository.importEntries(replacement);

        assertEquals("newEnc", repository.find("gmail").get("gmail"));
    }

    @Test
    public void find_shouldReturnEmpty_whenEntryDoesNotExist() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        assertTrue(repository.find("missing").isEmpty());
    }

    @Test
    public void save_shouldDelegateToPersistenceService() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        byte[] salt = new byte[]{9, 9, 9};
        String encryptedMaster = "masterEnc";

        repository.setSalt(salt);
        repository.setEncryptedMasterPassword(encryptedMaster);

        repository.addEncryptedEntry("enc", "gmail");

        repository.save();

        ArgumentCaptor<HashMap> entriesCaptor = ArgumentCaptor.forClass(HashMap.class);

        verify(persistence).save(
                eq(salt),
                eq(encryptedMaster),
                entriesCaptor.capture()
        );

        assertEquals(1, entriesCaptor.getValue().size());
    }
}

import interfaces.IPersistenceService;
import model.PasswordEntry;
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

        HashMap<String, PasswordEntry> entries = new HashMap<>();
        entries.put("gmail", new PasswordEntry("gmail", "user", "encPass"));

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
    public void add_shouldInsertNewEntry() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        PasswordEntry entry = new PasswordEntry("gmail", "user", "enc");

        boolean result = repository.add(entry);

        assertTrue(result);
        assertEquals(entry, repository.find("gmail"));
    }

    @Test
    public void add_shouldFailForDuplicateAccount() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        PasswordEntry entry1 = new PasswordEntry("gmail", "user1", "enc1");
        PasswordEntry entry2 = new PasswordEntry("gmail", "user2", "enc2");

        assertTrue(repository.add(entry1));
        assertFalse(repository.add(entry2));
    }

    @Test
    public void remove_shouldRemoveExistingEntry() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        repository.add(new PasswordEntry("gmail", "user", "enc"));

        boolean removed = repository.remove("gmail");

        assertTrue(removed);
        assertNull(repository.find("gmail"));
    }

    @Test
    public void remove_shouldReturnFalse_whenEntryDoesNotExist() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        assertFalse(repository.remove("unknown"));
    }

    @Test
    public void update_shouldReplaceExistingEntry() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        repository.add(new PasswordEntry("gmail", "oldUser", "oldEnc"));

        boolean updated = repository.update("gmail", "newUser", "newEnc");

        assertTrue(updated);

        PasswordEntry updatedEntry = repository.find("gmail");
        assertEquals("newUser", updatedEntry.getUsername());
        assertEquals("newEnc", updatedEntry.getEncryptedPassword());
    }

    @Test
    public void update_shouldFail_whenEntryDoesNotExist() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        assertFalse(repository.update("missing", "user", "enc"));
    }

    @Test
    public void save_shouldDelegateToPersistenceService() {
        when(persistence.load()).thenReturn(null);
        repository = new PasswordRepository(persistence);

        byte[] salt = new byte[]{9, 9, 9};
        String encryptedMaster = "masterEnc";

        repository.setSalt(salt);
        repository.setEncryptedMasterPassword(encryptedMaster);

        repository.add(new PasswordEntry("gmail", "user", "enc"));

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

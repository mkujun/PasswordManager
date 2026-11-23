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

    private PasswordRepository emptyRepository() {
        when(persistence.load()).thenReturn(null);
        return new PasswordRepository(persistence);
    }

    @Test
    public void constructor_shouldCallLoadOnce() {
        emptyRepository();

        verify(persistence, times(1)).load();
    }

    @Test
    public void find_shouldReturnAllAccountsContainingText() {
        repository = emptyRepository();
        repository.addEncryptedEntry("e1", "gmail");
        repository.addEncryptedEntry("e2", "gmail-work");
        repository.addEncryptedEntry("e3", "github");

        assertEquals(2, repository.find("gmail").size());
        assertEquals(3, repository.find("").size());
        assertEquals(1, repository.find("hub").size());
    }

    @Test
    public void find_isCaseSensitive() {
        repository = emptyRepository();
        repository.addEncryptedEntry("e1", "Gmail");

        assertTrue(repository.find("gmail").isEmpty());
        assertEquals(1, repository.find("Gm").size());
    }

    @Test
    public void accountExists_shouldMatchExactName() {
        repository = emptyRepository();
        repository.addEncryptedEntry("enc", "gmail");

        assertTrue(repository.accountExists("gmail"));
        assertFalse(repository.accountExists("gmai"));
        assertFalse(repository.accountExists("unknown"));
    }

    @Test
    public void getEntry_shouldReturnStoredValueOrNull() {
        repository = emptyRepository();
        repository.addEncryptedEntry("enc", "gmail");

        assertEquals("enc", repository.getEntry("gmail"));
        assertNull(repository.getEntry("unknown"));
    }

    @Test
    public void updateEntry_existingAccount_shouldReplaceValue() {
        repository = emptyRepository();
        repository.addEncryptedEntry("old", "gmail");

        repository.updateEntry("gmail", "new");

        assertEquals("new", repository.getEntry("gmail"));
        assertEquals(1, repository.getEntries().size());
    }

    @Test
    public void updateEntry_missingAccount_shouldNotAddEntry() {
        repository = emptyRepository();

        repository.updateEntry("missing", "new");

        assertFalse(repository.accountExists("missing"));
        assertTrue(repository.getEntries().isEmpty());
    }

    @Test
    public void dump_shouldClearEntriesAndMasterPasswordData() {
        repository = emptyRepository();
        repository.setSalt(new byte[]{1});
        repository.setEncryptedMasterPassword("master");
        repository.addEncryptedEntry("enc", "gmail");

        repository.dump();

        assertTrue(repository.getEntries().isEmpty());
        assertEquals("", repository.getEncryptedMasterPassword());
        assertNull(repository.getSalt());
    }

    @Test
    public void importEntries_shouldReplaceWholeMap() {
        repository = emptyRepository();
        repository.addEncryptedEntry("enc", "old");

        HashMap<String, String> replacement = new HashMap<>();
        replacement.put("new1", "a");
        replacement.put("new2", "b");
        repository.importEntries(replacement);

        assertFalse(repository.accountExists("old"));
        assertEquals(2, repository.getEntries().size());
    }

    @Test
    public void setters_shouldUpdateSaltAndMasterPassword() {
        repository = emptyRepository();
        byte[] salt = {4, 5, 6};

        repository.setSalt(salt);
        repository.setEncryptedMasterPassword("master");

        assertArrayEquals(salt, repository.getSalt());
        assertEquals("master", repository.getEncryptedMasterPassword());
    }

    @Test
    public void save_emptyRepository_shouldPassNullsAndEmptyMap() {
        repository = emptyRepository();

        repository.save();

        verify(persistence).save(isNull(), isNull(), eq(new HashMap<String, String>()));
    }

    @Test
    public void addEncryptedEntry_duplicate_shouldKeepFirstValue() {
        repository = emptyRepository();

        repository.addEncryptedEntry("first", "gmail");
        repository.addEncryptedEntry("second", "gmail");

        assertEquals("first", repository.getEntry("gmail"));
    }
}

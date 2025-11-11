import org.junit.*;
import java.util.*;
import static org.junit.Assert.*;

public class PasswordRepositoryTest {

    private PasswordRepository repo;

    @Before
    public void setUp() {
        repo = new PasswordRepository(new PersistenceService("test_repo.dat"));
        repo.setSalt(new byte[]{1,2,3});
        repo.setEncryptedMasterPassword("master123");
    }

    @After
    public void tearDown() {
        new java.io.File("test_repo.dat").delete();
    }

    @Test
    public void testAddAndFindEntry() {
        PasswordEntry entry = new PasswordEntry("GitHub", "john", "encPass");
        assertTrue(repo.add(entry));

        Optional<PasswordEntry> found = repo.findByAccountName("GitHub");
        assertTrue(found.isPresent());
        assertEquals("john", found.get().getUsername());
    }

    @Test
    public void testAddDuplicateFails() {
        repo.add(new PasswordEntry("GitHub", "john", "encPass"));
        assertFalse(repo.add(new PasswordEntry("GitHub", "bob", "encPass2")));
    }

    @Test
    public void testUpdateEntry() {
        repo.add(new PasswordEntry("Slack", "john", "oldEnc"));
        assertTrue(repo.update("Slack", "newUser", "newEnc"));
        assertEquals("newUser", repo.findByAccountName("Slack").get().getUsername());
    }

    @Test
    public void testRemoveEntry() {
        repo.add(new PasswordEntry("Zoom", "user", "enc"));
        assertTrue(repo.remove("Zoom"));
        assertFalse(repo.findByAccountName("Zoom").isPresent());
    }
}

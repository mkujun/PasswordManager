import org.junit.*;
import java.io.File;

import static org.junit.Assert.*;

public class PasswordManagerTest {

    private PasswordManager manager;
    private File file = new File("passwords.dat");

    @Before
    public void setUp() {
        if (file.exists()) file.delete();
        manager = new PasswordManager();
    }

    @After
    public void tearDown() {
        file.delete();
    }

    @Test
    public void testInitializationAndMasterPassword() {
        PersistenceService persistence = new PersistenceService("passwords.dat");
        PasswordRepository repo = new PasswordRepository(persistence);
        CryptoService crypto = new CryptoService(repo);

        crypto.generateMasterPassword("root123");
        assertNotNull(repo.getSalt());
        assertNotNull(repo.getEncryptedMasterPassword());
    }

    /*
    @Test
    public void testFullFlowAddAndSearch() {
        PersistenceService persistence = new PersistenceService("passwords.dat");
        PasswordRepository repo = new PasswordRepository(persistence);
        CryptoService crypto = new CryptoService(repo);

        crypto.setMasterPassword("admin");
        crypto.addPassword("Discord", "neo", "matrix");

        Optional<PasswordEntry> found = crypto.findEntryByAccountName("Discord");
        assertTrue(found.isPresent());
        assertEquals("matrix", crypto.decrypt(found.get().getEncryptedPassword()));
    }

     */
}

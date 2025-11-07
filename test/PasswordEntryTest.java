import org.junit.Test;
import static org.junit.Assert.*;

public class PasswordEntryTest {

    @Test
    public void testConstructorAndGetters() {
        PasswordEntry entry = new PasswordEntry("Gmail", "user123", "encryptedPass");

        assertEquals("Gmail", entry.getAccountName());
        assertEquals("user123", entry.getUsername());
        assertEquals("encryptedPass", entry.getEncryptedPassword());
    }

    @Test
    public void testSetUsername() {
        PasswordEntry entry = new PasswordEntry("Gmail", "user123", "encryptedPass");
        entry.setUsername("newUser");

        assertEquals("newUser", entry.getUsername());
    }

    @Test
    public void testSetEncryptedPassword() {
        PasswordEntry entry = new PasswordEntry("Gmail", "user123", "encryptedPass");
        entry.setEncryptedPassword("newEncrypted");

        assertEquals("newEncrypted", entry.getEncryptedPassword());
    }

    @Test
    public void testAccountNameUnchanged() {
        PasswordEntry entry = new PasswordEntry("GitHub", "coder", "abc123");
        entry.setUsername("updatedUser");

        // account name should remain unchanged
        assertEquals("GitHub", entry.getAccountName());
    }
}

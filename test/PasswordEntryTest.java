import model.PasswordEntry;
import org.junit.Test;
import static org.junit.Assert.*;

public class PasswordEntryTest {

    private PasswordEntry buildEntry(String account, String username, String password) {
        return new PasswordEntry.Builder(account)
                .username(username)
                .password(password)
                .build();
    }

    @Test
    public void testConstructorAndGetters() {
        PasswordEntry entry = buildEntry("Gmail", "user123", "encryptedPass");

        assertEquals("Gmail", entry.getAccountName());
        assertEquals("user123", entry.getUsername());
        assertEquals("encryptedPass", entry.getPassword());
    }

    @Test
    public void testSetUsername() {
        PasswordEntry entry = buildEntry("Gmail", "user123", "encryptedPass");
        entry.setUsername("newUser");

        assertEquals("newUser", entry.getUsername());
    }

    @Test
    public void testSetEncryptedPassword() {
        PasswordEntry entry = buildEntry("Gmail", "user123", "encryptedPass");
        entry.setPassword("newEncrypted");

        assertEquals("newEncrypted", entry.getPassword());
    }

    @Test
    public void testAccountNameUnchanged() {
        PasswordEntry entry = buildEntry("GitHub", "coder", "abc123");
        entry.setUsername("updatedUser");

        // account name should remain unchanged
        assertEquals("GitHub", entry.getAccountName());
    }
}

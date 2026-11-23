import model.PasswordEntry;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;

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

    @Test
    public void testBuilderSetsAllFields() {
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime updated = LocalDateTime.of(2026, 2, 1, 10, 0);

        PasswordEntry entry = new PasswordEntry.Builder("Gmail")
                .username("user")
                .password("pass")
                .notes("my notes")
                .url("https://mail.google.com")
                .createdAt(created)
                .updatedAt(updated)
                .accountActive(false)
                .build();

        assertEquals("Gmail", entry.getAccountName());
        assertEquals("user", entry.getUsername());
        assertEquals("pass", entry.getPassword());
        assertEquals("my notes", entry.getNotes());
        assertEquals("https://mail.google.com", entry.getUrl());
        assertEquals(created, entry.getCreatedAt());
        assertEquals(updated, entry.getUpdatedAt());
        assertFalse(entry.isAccountActive());
    }

    @Test
    public void testBuilderDefaults() {
        LocalDateTime before = LocalDateTime.now();
        PasswordEntry entry = new PasswordEntry.Builder("Gmail").build();
        LocalDateTime after = LocalDateTime.now();

        assertNull(entry.getUsername());
        assertNull(entry.getPassword());
        assertNull(entry.getNotes());
        assertNull(entry.getUrl());
        assertTrue(entry.isAccountActive());
        assertFalse(entry.getCreatedAt().isBefore(before));
        assertFalse(entry.getCreatedAt().isAfter(after));
        assertFalse(entry.getUpdatedAt().isBefore(before));
        assertFalse(entry.getUpdatedAt().isAfter(after));
    }

    @Test
    public void testSetNotesUrlAndUpdatedAt() {
        PasswordEntry entry = buildEntry("Gmail", "user123", "pass");
        LocalDateTime updated = LocalDateTime.of(2030, 3, 3, 3, 3);

        entry.setNotes("new notes");
        entry.setUrl("https://example.com");
        entry.setUpdatedAt(updated);

        assertEquals("new notes", entry.getNotes());
        assertEquals("https://example.com", entry.getUrl());
        assertEquals(updated, entry.getUpdatedAt());
    }

    @Test
    public void testSetUpdatedAtDoesNotChangeCreatedAt() {
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 0, 0);
        PasswordEntry entry = new PasswordEntry.Builder("Gmail").createdAt(created).build();

        entry.setUpdatedAt(LocalDateTime.of(2027, 1, 1, 0, 0));

        assertEquals(created, entry.getCreatedAt());
    }

    @Test
    public void testSerializationRoundTrip() throws Exception {
        PasswordEntry entry = new PasswordEntry.Builder("Gmail")
                .username("user").password("pass").notes("n").url("u").build();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(entry);
        }
        PasswordEntry copy;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            copy = (PasswordEntry) ois.readObject();
        }

        assertEquals("Gmail", copy.getAccountName());
        assertEquals("user", copy.getUsername());
        assertEquals("pass", copy.getPassword());
        assertEquals(entry.getCreatedAt(), copy.getCreatedAt());
    }
}

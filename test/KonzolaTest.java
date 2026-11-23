import interfaces.IPasswordsService;
import model.PasswordEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the plain console UI. Each test feeds a scripted System.in, runs
 * {@link Konzola#auth()} and checks the calls on a mocked service and the
 * printed output. Every script ends with menu option 7 (Exit) so the loops
 * terminate.
 */
public class KonzolaTest {

    private static final String EXIT = "7\n";

    private IPasswordsService service;
    private Konzola konzola;

    private InputStream originalIn;
    private PrintStream originalOut;
    private ByteArrayOutputStream out;

    @Before
    public void setUp() {
        service = mock(IPasswordsService.class);
        konzola = new Konzola(service);

        originalIn = System.in;
        originalOut = System.out;
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true));
    }

    @After
    public void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    private void input(String script) {
        System.setIn(new ByteArrayInputStream(script.getBytes()));
    }

    private String output() {
        return out.toString();
    }

    /** Logged-in user (master password exists, login with "master" succeeds). */
    private void runLoggedIn(String menuScript) {
        when(service.masterPasswordExists()).thenReturn(true);
        when(service.login("master")).thenReturn(true);
        input("master\n" + menuScript);
        konzola.auth();
    }

    private static PasswordEntry entry(String account, String username, String password) {
        return new PasswordEntry.Builder(account).username(username).password(password).build();
    }

    // ---------------------------------------------------------------
    //  Signup
    // ---------------------------------------------------------------

    @Test
    public void auth_noMasterPassword_yes_shouldCreateMasterPasswordAndOpenMenu() {
        when(service.masterPasswordExists()).thenReturn(false);
        input("yes\nmaster\nmaster\n" + EXIT);

        konzola.auth();

        verify(service).setMasterPassword("master");
        assertTrue(output().contains("Access Granted."));
        assertTrue(output().contains("7. Exit"));
    }

    @Test
    public void auth_noMasterPassword_no_shouldExitWithoutCreating() {
        when(service.masterPasswordExists()).thenReturn(false);
        input("no\n");

        konzola.auth();

        verify(service, never()).setMasterPassword(anyString());
        assertFalse(output().contains("Access Granted."));
    }

    @Test
    public void auth_noMasterPassword_invalidAnswer_shouldAskAgain() {
        when(service.masterPasswordExists()).thenReturn(false);
        input("maybe\nno\n");

        konzola.auth();

        assertTrue(output().contains("Type yes or no"));
        verify(service, never()).setMasterPassword(anyString());
    }

    @Test
    public void signup_emptyPassword_shouldAskAgain() {
        when(service.masterPasswordExists()).thenReturn(false);
        input("yes\n\nmaster\nmaster\n" + EXIT);

        konzola.auth();

        assertTrue(output().contains("Password should not be empty!"));
        verify(service, times(1)).setMasterPassword("master");
    }

    @Test
    public void signup_passwordsDoNotMatch_shouldAskAgain() {
        when(service.masterPasswordExists()).thenReturn(false);
        input("yes\nmaster\nother\nmaster\nmaster\n" + EXIT);

        konzola.auth();

        assertTrue(output().contains("Passwords don't match!"));
        verify(service, times(1)).setMasterPassword(anyString());
        verify(service).setMasterPassword("master");
    }

    @Test
    public void signup_shouldTrimInput() {
        when(service.masterPasswordExists()).thenReturn(false);
        input("  yes  \n  master  \nmaster\n" + EXIT);

        konzola.auth();

        verify(service).setMasterPassword("master");
    }

    // ---------------------------------------------------------------
    //  Login
    // ---------------------------------------------------------------

    @Test
    public void login_correctPassword_shouldOpenMenu() {
        runLoggedIn(EXIT);

        verify(service).login("master");
        assertTrue(output().contains("Access Granted."));
    }

    @Test
    public void login_wrongThenCorrect_shouldRetry() {
        when(service.masterPasswordExists()).thenReturn(true);
        when(service.login("wrong")).thenReturn(false);
        when(service.login("master")).thenReturn(true);
        input("wrong\nmaster\n" + EXIT);

        konzola.auth();

        assertTrue(output().contains("Incorrect Password. Access Denied."));
        verify(service).login("wrong");
        verify(service).login("master");
    }

    // ---------------------------------------------------------------
    //  Menu
    // ---------------------------------------------------------------

    @Test
    public void menu_invalidNumber_shouldShowMessage() {
        runLoggedIn("9\n" + EXIT);

        assertTrue(output().contains("Invalid option. Try again."));
    }

    @Test
    public void menu_notANumber_shouldShowMessage() {
        runLoggedIn("abc\n" + EXIT);

        assertTrue(output().contains("You did not enter a number. Try again."));
    }

    // ---------------------------------------------------------------
    //  1. Add password
    // ---------------------------------------------------------------

    @Test
    public void addPassword_validInput_shouldAddEntry() {
        when(service.accountExists("gmail")).thenReturn(false, true);

        runLoggedIn("1\ngmail\nuser\npass\nnotes\nurl\n" + EXIT);

        ArgumentCaptor<PasswordEntry> captor = ArgumentCaptor.forClass(PasswordEntry.class);
        verify(service).addPasswordEntry(captor.capture());
        PasswordEntry added = captor.getValue();
        assertEquals("gmail", added.getAccountName());
        assertEquals("user", added.getUsername());
        assertEquals("pass", added.getPassword());
        assertEquals("notes", added.getNotes());
        assertEquals("url", added.getUrl());
        assertTrue(output().contains("Password added successfully"));
    }

    @Test
    public void addPassword_notStored_shouldNotPrintSuccess() {
        when(service.accountExists("gmail")).thenReturn(false, false);

        runLoggedIn("1\ngmail\nuser\npass\nnotes\nurl\n" + EXIT);

        verify(service).addPasswordEntry(any(PasswordEntry.class));
        assertFalse(output().contains("Password added successfully"));
    }

    @Test
    public void addPassword_emptyAccount_shouldNotAdd() {
        runLoggedIn("1\n\n" + EXIT);

        assertTrue(output().contains("Account name is empty. Try again."));
        verify(service, never()).addPasswordEntry(any());
    }

    @Test
    public void addPassword_existingAccount_shouldNotAdd() {
        when(service.accountExists("gmail")).thenReturn(true);

        runLoggedIn("1\ngmail\n" + EXIT);

        assertTrue(output().contains("Account with that name already exists!"));
        verify(service, never()).addPasswordEntry(any());
    }

    @Test
    public void addPassword_emptyUsername_shouldNotAdd() {
        runLoggedIn("1\ngmail\n\npass\nnotes\nurl\n" + EXIT);

        assertTrue(output().contains("Username and password fields are required!"));
        verify(service, never()).addPasswordEntry(any());
    }

    @Test
    public void addPassword_emptyPassword_shouldNotAdd() {
        runLoggedIn("1\ngmail\nuser\n\nnotes\nurl\n" + EXIT);

        assertTrue(output().contains("Username and password fields are required!"));
        verify(service, never()).addPasswordEntry(any());
    }

    // ---------------------------------------------------------------
    //  2. View passwords
    // ---------------------------------------------------------------

    @Test
    public void viewPasswords_shouldPrintAllEntries() {
        Map<String, PasswordEntry> entries = new HashMap<>();
        entries.put("gmail", entry("gmail", "user1", "pass1"));
        entries.put("github", entry("github", "user2", "pass2"));
        when(service.viewPasswords("")).thenReturn(entries);

        runLoggedIn("2\n" + EXIT);

        String text = output();
        assertTrue(text.contains("Account: gmail"));
        assertTrue(text.contains("Username: user1"));
        assertTrue(text.contains("Password: pass1"));
        assertTrue(text.contains("Account: github"));
        assertTrue(text.contains("Account active: true"));
    }

    @Test
    public void viewPasswords_empty_shouldPrintNoEntries() {
        when(service.viewPasswords("")).thenReturn(Collections.emptyMap());

        runLoggedIn("2\n" + EXIT);

        assertFalse(output().contains("Account: "));
    }

    // ---------------------------------------------------------------
    //  3. Remove password
    // ---------------------------------------------------------------

    @Test
    public void removePassword_existingAccount_shouldDelete() {
        when(service.accountExists("gmail")).thenReturn(true, false);

        runLoggedIn("3\ngmail\n" + EXIT);

        verify(service).deletePasswordEntry("gmail");
        assertTrue(output().contains("Password deleted successfully"));
    }

    @Test
    public void removePassword_stillExistsAfterDelete_shouldNotPrintSuccess() {
        when(service.accountExists("gmail")).thenReturn(true, true);

        runLoggedIn("3\ngmail\n" + EXIT);

        verify(service).deletePasswordEntry("gmail");
        assertFalse(output().contains("Password deleted successfully"));
    }

    @Test
    public void removePassword_missingAccount_shouldNotDelete() {
        when(service.accountExists("missing")).thenReturn(false);

        runLoggedIn("3\nmissing\n" + EXIT);

        assertTrue(output().contains("Account does not exist. Try again."));
        verify(service, never()).deletePasswordEntry(anyString());
    }

    // ---------------------------------------------------------------
    //  4. Search
    // ---------------------------------------------------------------

    @Test
    public void searchPassword_found_shouldPrintEntry() {
        Map<String, PasswordEntry> result = new HashMap<>();
        result.put("gmail", entry("gmail", "user", "pass"));
        when(service.viewPasswords("gm")).thenReturn(result);

        runLoggedIn("4\ngm\n" + EXIT);

        assertTrue(output().contains("Account: gmail"));
        assertFalse(output().contains("Entry not found."));
    }

    @Test
    public void searchPassword_notFound_shouldPrintMessage() {
        when(service.viewPasswords("xyz")).thenReturn(Collections.emptyMap());

        runLoggedIn("4\nxyz\n" + EXIT);

        assertTrue(output().contains("Entry not found."));
    }

    // ---------------------------------------------------------------
    //  5. Update entry
    // ---------------------------------------------------------------

    @Test
    public void updateEntry_validInput_shouldUpdate() {
        PasswordEntry existing = entry("gmail", "old", "oldPass");
        when(service.accountExists("gmail")).thenReturn(true);
        when(service.getPasswordEntry("gmail")).thenReturn(existing);

        runLoggedIn("5\ngmail\nnewUser\nnewPass\nnewNotes\nnewUrl\n" + EXIT);

        verify(service).updatePasswordEntry("gmail", existing);
        assertEquals("newUser", existing.getUsername());
        assertEquals("newPass", existing.getPassword());
        assertEquals("newNotes", existing.getNotes());
        assertEquals("newUrl", existing.getUrl());
        assertTrue(output().contains("Password updated successfully"));
    }

    @Test
    public void updateEntry_missingAccount_shouldNotUpdate() {
        when(service.accountExists("missing")).thenReturn(false);

        runLoggedIn("5\nmissing\n" + EXIT);

        assertTrue(output().contains("Account with that name does not exists!"));
        verify(service, never()).updatePasswordEntry(anyString(), any());
    }

    @Test
    public void updateEntry_emptyPassword_shouldNotUpdate() {
        PasswordEntry existing = entry("gmail", "old", "oldPass");
        when(service.accountExists("gmail")).thenReturn(true);
        when(service.getPasswordEntry("gmail")).thenReturn(existing);

        runLoggedIn("5\ngmail\nnewUser\n\nnotes\nurl\n" + EXIT);

        assertTrue(output().contains("Username and password fields are required!"));
        verify(service, never()).updatePasswordEntry(anyString(), any());
        assertEquals("old", existing.getUsername());
    }

    @Test
    public void updateEntry_accountGoneAfterUpdate_shouldNotPrintSuccess() {
        PasswordEntry existing = entry("gmail", "old", "oldPass");
        when(service.accountExists("gmail")).thenReturn(true, false);
        when(service.getPasswordEntry("gmail")).thenReturn(existing);

        runLoggedIn("5\ngmail\nu\np\nn\nurl\n" + EXIT);

        verify(service).updatePasswordEntry("gmail", existing);
        assertFalse(output().contains("Password updated successfully"));
    }

    // ---------------------------------------------------------------
    //  6. Update master password
    // ---------------------------------------------------------------

    @Test
    public void updateMasterPassword_shouldReAddAllEntriesWithNewMaster() {
        PasswordEntry gmail = entry("gmail", "u1", "p1");
        PasswordEntry github = entry("github", "u2", "p2");
        Map<String, PasswordEntry> entries = new HashMap<>();
        entries.put("gmail", gmail);
        entries.put("github", github);
        when(service.viewPasswords("")).thenReturn(entries);

        runLoggedIn("6\nnewMaster\nnewMaster\n" + EXIT);

        org.mockito.InOrder order = inOrder(service);
        order.verify(service).viewPasswords("");
        order.verify(service).deleteAllEntries();
        order.verify(service).setMasterPassword("newMaster");
        order.verify(service, times(2)).addPasswordEntry(any(PasswordEntry.class));
        verify(service).addPasswordEntry(gmail);
        verify(service).addPasswordEntry(github);
        assertTrue(output().contains("Master Password has been updated."));
    }
}

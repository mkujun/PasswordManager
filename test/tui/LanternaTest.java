package tui;

import interfaces.IPasswordsService;
import model.PasswordEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static tui.TuiTestSupport.*;

/**
 * Flow tests for the {@link Lanterna} controller. The flow methods are
 * private and block in {@code addWindowAndWait}, so each test queues the
 * user's key strokes on the virtual terminal up front, then calls the flow
 * method via reflection. Every script ends by closing the opened windows
 * (ESC), otherwise the call would block; the timeout rule guards against that.
 *
 * {@link Lanterna#run()} is not covered: it opens a real system terminal.
 */
public class LanternaTest {

    @Rule
    public Timeout timeout = Timeout.seconds(20);

    private TuiTestSupport tui;
    private IPasswordsService service;
    private Lanterna lanterna;

    private PasswordEntry gmail;

    @Before
    public void setUp() throws Exception {
        tui = new TuiTestSupport();
        service = mock(IPasswordsService.class);
        lanterna = new Lanterna(service);
        lanterna.gui = tui.gui;

        gmail = new PasswordEntry.Builder("gmail")
                .username("u").password("p").url("url").notes("notes")
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .accountActive(false)
                .build();
    }

    @After
    public void tearDown() throws Exception {
        tui.stop();
    }

    private Object call(String method, Object... args) throws Exception {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }
        Method m = Lanterna.class.getDeclaredMethod(method, types);
        m.setAccessible(true);
        try {
            return m.invoke(lanterna, args);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    private void tabs(int count) {
        for (int i = 0; i < count; i++) {
            tui.queue(TAB);
        }
    }

    private void withEntries(PasswordEntry... entries) {
        Map<String, PasswordEntry> map = new HashMap<>();
        for (PasswordEntry e : entries) {
            map.put(e.getAccountName(), e);
        }
        when(service.viewPasswords("")).thenReturn(map);
    }

    // ---------------------------------------------------------------
    //  Signup
    // ---------------------------------------------------------------

    @Test
    public void signup_validPassword_shouldSetMasterPasswordAndShowSuccess() throws Exception {
        tui.queueText("master");
        tui.queue(TAB);
        tui.queueText("master");
        tui.queue(TAB, TAB, ENTER); // -> checkbox -> Register, press it
        tui.queue(ENTER);           // OK in the success dialog
        tui.queue(ESC);             // close the signup screen

        call("showSignupScreen");

        verify(service).setMasterPassword("master");
    }

    @Test
    public void signup_escape_shouldNotSetMasterPassword() throws Exception {
        tui.queue(ESC);

        call("showSignupScreen");

        verify(service, never()).setMasterPassword(anyString());
    }

    @Test
    public void signup_mismatch_shouldNotSetMasterPassword() throws Exception {
        tui.queueText("master");
        tui.queue(TAB);
        tui.queueText("other");
        tui.queue(TAB, TAB, ENTER, ESC);

        call("showSignupScreen");

        verify(service, never()).setMasterPassword(anyString());
    }

    // ---------------------------------------------------------------
    //  Login
    // ---------------------------------------------------------------

    @Test
    public void login_wrongPassword_shouldStayOnLogin() throws Exception {
        when(service.login("bad")).thenReturn(false);
        tui.queueText("bad");
        tui.queue(TAB, ENTER); // Unlock
        tui.queue(ESC);        // quit login

        call("showLoginScreen");

        verify(service).login("bad");
        verify(service, never()).viewPasswords(anyString());
    }

    @Test
    public void login_wrongThenCorrect_shouldOpenMenuAndCloseLoginOnExit() throws Exception {
        when(service.login("bad")).thenReturn(false);
        when(service.login("master")).thenReturn(true);
        tui.queueText("bad");
        tui.queue(TAB, ENTER);   // wrong attempt, field is cleared, focus stays on Unlock
        tui.queue(TAB);          // back to the password field
        tui.queueText("master");
        tui.queue(TAB, ENTER);   // correct attempt -> main menu
        tui.queue(ESC);          // close menu -> login window closes too

        call("showLoginScreen");

        verify(service).login("bad");
        verify(service).login("master");
    }

    // ---------------------------------------------------------------
    //  Main menu -> password list (view / delete / edit)
    // ---------------------------------------------------------------

    @Test
    public void menu_viewPasswords_shouldLoadEntries() throws Exception {
        withEntries(gmail);
        tui.queue(ENTER); // 1. View passwords
        tui.queue(ESC);   // close list
        tui.queue(ESC);   // close menu

        call("showMainMenu");

        verify(service).viewPasswords("");
    }

    @Test
    public void passwordList_deleteConfirmed_shouldDeleteViaService() throws Exception {
        withEntries(gmail);
        when(service.accountExists("gmail")).thenReturn(false);
        tui.queue(ENTER);                      // open list (focus on search box)
        tui.queue(TAB, character('d'), ENTER); // focus list, delete, confirm "Yes"
        tui.queue(ESC, ESC);

        call("showMainMenu");

        verify(service).deletePasswordEntry("gmail");
        verify(service).accountExists("gmail");
    }

    @Test
    public void passwordList_editUnchanged_shouldNotUpdate() throws Exception {
        withEntries(gmail);
        tui.queue(ENTER, TAB, character('e')); // open list, focus it, edit
        tabs(7);
        tui.queue(ENTER);                      // Save without changes
        tui.queue(ESC, ESC);

        call("showMainMenu");

        verify(service, never()).updatePasswordEntry(anyString(), any());
    }

    // ---------------------------------------------------------------
    //  Edit entry
    // ---------------------------------------------------------------

    @Test
    public void editEntry_changedUsername_shouldUpdateAndReturnTrue() throws Exception {
        tui.queue(TAB);           // -> username
        tui.queueText("X");
        tabs(6);                  // -> Save
        tui.queue(ENTER);

        boolean changed = (Boolean) call("editEntry", gmail);

        assertTrue(changed);
        ArgumentCaptor<PasswordEntry> captor = ArgumentCaptor.forClass(PasswordEntry.class);
        verify(service).updatePasswordEntry(eq("gmail"), captor.capture());
        PasswordEntry updated = captor.getValue();
        assertTrue(updated.getUsername().contains("X"));
        assertEquals("p", updated.getPassword());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), updated.getCreatedAt());
        assertFalse(updated.isAccountActive());
        // The in-memory entry is updated too.
        assertEquals(updated.getUsername(), gmail.getUsername());
        assertEquals(updated.getUpdatedAt(), gmail.getUpdatedAt());
    }

    @Test
    public void editEntry_unchanged_shouldReturnFalse() throws Exception {
        tabs(7);
        tui.queue(ENTER);

        boolean changed = (Boolean) call("editEntry", gmail);

        assertFalse(changed);
        verify(service, never()).updatePasswordEntry(anyString(), any());
    }

    @Test
    public void editEntry_nullFields_unchanged_shouldReturnFalse() throws Exception {
        PasswordEntry sparse = new PasswordEntry.Builder("site").username("u").password("p").build();
        tabs(7);
        tui.queue(ENTER);

        assertFalse((Boolean) call("editEntry", sparse));
    }

    @Test
    public void editEntry_eachFieldChanged_shouldUpdate() throws Exception {
        // password + repeat changed together, then URL and notes.
        tabs(2);
        tui.queueText("1");
        tabs(2);
        tui.queueText("1");
        tabs(1);
        tui.queueText("U");
        tabs(1);
        tui.queueText("N");
        tabs(1);
        tui.queue(ENTER);

        assertTrue((Boolean) call("editEntry", gmail));
        verify(service).updatePasswordEntry(eq("gmail"), any(PasswordEntry.class));
        assertTrue(gmail.getPassword().contains("1"));
        assertTrue(gmail.getUrl().contains("U"));
        assertTrue(gmail.getNotes().contains("N"));
    }

    @Test
    public void editEntry_urlOnlyChanged_shouldUpdate() throws Exception {
        tabs(5);
        tui.queueText("U");
        tabs(2);
        tui.queue(ENTER);

        assertTrue((Boolean) call("editEntry", gmail));
    }

    @Test
    public void editEntry_notesOnlyChanged_shouldUpdate() throws Exception {
        tabs(6);
        tui.queueText("N");
        tabs(1);
        tui.queue(ENTER);

        assertTrue((Boolean) call("editEntry", gmail));
    }

    @Test
    public void editEntry_emptyUsername_shouldNotUpdate() throws Exception {
        // Username "u" is one character: BACKSPACE or DELETE removes it,
        // depending on where the caret is after prefill.
        tui.queue(TAB, key(com.googlecode.lanterna.input.KeyType.Backspace),
                key(com.googlecode.lanterna.input.KeyType.Delete));
        tabs(6);
        tui.queue(ENTER); // Save -> validation error
        tui.queue(ESC);   // cancel

        assertFalse((Boolean) call("editEntry", gmail));
        verify(service, never()).updatePasswordEntry(anyString(), any());
    }

    @Test
    public void editEntry_passwordMismatch_shouldNotUpdate() throws Exception {
        tabs(4);            // -> repeat password
        tui.queueText("Z");
        tabs(3);            // -> Save
        tui.queue(ENTER, ESC);

        assertFalse((Boolean) call("editEntry", gmail));
        verify(service, never()).updatePasswordEntry(anyString(), any());
    }

    // ---------------------------------------------------------------
    //  Add password
    // ---------------------------------------------------------------

    /** Fills the add form: account, username, password, repeat, then focuses Save. */
    private void queueAddForm(String account, String user, String pass, String repeat) {
        tui.queueText(account);
        tui.queue(TAB);
        tui.queueText(user);
        tui.queue(TAB);
        tui.queueText(pass);
        tui.queue(TAB, TAB);   // skip Show/Hide
        tui.queueText(repeat);
        tui.queue(TAB);
        tui.queueText("https://x");
        tui.queue(TAB);
        tui.queueText("n");
        tui.queue(TAB);        // -> Save
    }

    @Test
    public void addPassword_valid_shouldAddAndClose() throws Exception {
        when(service.accountExists("site")).thenReturn(false, true);
        queueAddForm("site", "user", "pass", "pass");
        tui.queue(ENTER);

        call("showAddPasswordScreen");

        ArgumentCaptor<PasswordEntry> captor = ArgumentCaptor.forClass(PasswordEntry.class);
        verify(service).addPasswordEntry(captor.capture());
        assertEquals("site", captor.getValue().getAccountName());
        assertEquals("user", captor.getValue().getUsername());
        assertEquals("pass", captor.getValue().getPassword());
        assertEquals("https://x", captor.getValue().getUrl());
        assertEquals("n", captor.getValue().getNotes());
    }

    @Test
    public void addPassword_notSaved_shouldStayOpen() throws Exception {
        when(service.accountExists("site")).thenReturn(false, false);
        queueAddForm("site", "user", "pass", "pass");
        tui.queue(ENTER, ESC);

        call("showAddPasswordScreen");

        verify(service).addPasswordEntry(any(PasswordEntry.class));
    }

    @Test
    public void addPassword_emptyFields_shouldNotAdd() throws Exception {
        tabs(7);
        tui.queue(ENTER, ESC);

        call("showAddPasswordScreen");

        verify(service, never()).addPasswordEntry(any());
    }

    @Test
    public void addPassword_missingUsername_shouldNotAdd() throws Exception {
        queueAddForm("site", "", "pass", "pass");
        tui.queue(ENTER, ESC);

        call("showAddPasswordScreen");

        verify(service, never()).addPasswordEntry(any());
    }

    @Test
    public void addPassword_missingPassword_shouldNotAdd() throws Exception {
        queueAddForm("site", "user", "", "");
        tui.queue(ENTER, ESC);

        call("showAddPasswordScreen");

        verify(service, never()).addPasswordEntry(any());
    }

    @Test
    public void addPassword_mismatch_shouldNotAdd() throws Exception {
        queueAddForm("site", "user", "pass", "other");
        tui.queue(ENTER, ESC);

        call("showAddPasswordScreen");

        verify(service, never()).addPasswordEntry(any());
    }

    @Test
    public void addPassword_duplicate_shouldNotAdd() throws Exception {
        when(service.accountExists("site")).thenReturn(true);
        queueAddForm("site", "user", "pass", "pass");
        tui.queue(ENTER, ESC);

        call("showAddPasswordScreen");

        verify(service, never()).addPasswordEntry(any());
    }

    @Test
    public void menu_addPassword_cancel_shouldReturnToMenu() throws Exception {
        tui.queue(DOWN, ENTER); // 2. Add new password
        tui.queue(ESC);         // cancel
        tui.queue(ESC);         // close menu

        call("showMainMenu");

        verify(service, never()).addPasswordEntry(any());
    }

    // ---------------------------------------------------------------
    //  Settings
    // ---------------------------------------------------------------

    @Test
    public void menu_settings_shouldShowInfoDialog() throws Exception {
        tui.queue(DOWN, DOWN, ENTER); // 3. Settings
        tui.queue(ENTER);             // OK
        tui.queue(ESC);

        call("showMainMenu");

        verifyNoInteractions(service);
    }

    @Test
    public void validateUser_shouldDelegateToService() throws Exception {
        when(service.login("master")).thenReturn(true);

        assertTrue((Boolean) call("validateUser", "master"));
        assertFalse((Boolean) call("validateUser", "other"));
    }

    @Test
    public void emptyPasswordList_shouldOpenAndClose() throws Exception {
        when(service.viewPasswords("")).thenReturn(Collections.emptyMap());
        tui.queue(ESC);

        call("showPasswordListScreen");

        verify(service).viewPasswords("");
    }
}

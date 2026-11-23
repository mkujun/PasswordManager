package tui;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.TextBox;
import model.PasswordEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static tui.TuiTestSupport.*;

public class AddPasswordTest {

    private TuiTestSupport tui;
    private AddPassword screen;
    private BasicWindow window;

    // Fields in form order.
    private TextBox account;
    private TextBox username;
    private TextBox password;
    private TextBox repeat;
    private TextBox url;
    private TextBox notes;

    private Button showHide;
    private Button save;
    private Button cancel;

    @Before
    public void setUp() throws Exception {
        tui = new TuiTestSupport();
        screen = new AddPassword(Theme.getTheme(), null, null);
        window = screen.getWindow();

        List<TextBox> boxes = find(window, TextBox.class);
        account = boxes.get(0);
        username = boxes.get(1);
        password = boxes.get(2);
        repeat = boxes.get(3);
        url = boxes.get(4);
        notes = boxes.get(5);

        List<Button> buttons = find(window, Button.class);
        showHide = buttons.get(0);
        save = buttons.get(1);
        cancel = buttons.get(2);

        tui.show(window);
    }

    @After
    public void tearDown() throws Exception {
        tui.stop();
    }

    private void click(Button button) {
        focus(window, button);
        press(window, ENTER);
    }

    @Test
    public void constructor_shouldBuildSixFieldsAndThreeButtons() {
        assertEquals(6, find(window, TextBox.class).size());
        assertEquals(3, find(window, Button.class).size());
        assertEquals(Character.valueOf('*'), password.getMask());
        assertEquals(Character.valueOf('*'), repeat.getMask());
        assertNull(account.getMask());
        assertNotNull(labelContaining(window, "Add new password"));
    }

    @Test
    public void getters_shouldTrimTextFieldsButNotPasswords() {
        account.setText("  gmail ");
        username.setText(" user ");
        password.setText(" pass ");
        repeat.setText(" pass ");
        url.setText(" https://x ");
        notes.setText(" n ");

        assertEquals("gmail", screen.getAccount());
        assertEquals("user", screen.getUsername());
        assertEquals(" pass ", screen.getPassword());
        assertEquals(" pass ", screen.getRepeatPassword());
        assertEquals("https://x", screen.getUrl());
        assertEquals("n", screen.getNotes());
    }

    @Test
    public void passwordsMatch_shouldCompareBothFields() {
        password.setText("a");
        repeat.setText("a");
        assertTrue(screen.passwordsMatch());

        repeat.setText("b");
        assertFalse(screen.passwordsMatch());
    }

    @Test
    public void showHide_shouldToggleMaskAndLabel() {
        click(showHide);

        assertNull(password.getMask());
        assertNull(repeat.getMask());
        assertTrue(showHide.getLabel().contains("Hide"));

        click(showHide);

        assertEquals(Character.valueOf('*'), password.getMask());
        assertEquals(Character.valueOf('*'), repeat.getMask());
        assertTrue(showHide.getLabel().contains("Show"));
    }

    @Test
    public void prefill_shouldFillFieldsAndLockAccount() {
        PasswordEntry entry = new PasswordEntry.Builder("gmail")
                .username("user").password("pass").url("https://x").notes("n").build();

        screen.prefill(entry);

        assertEquals("gmail", account.getText());
        assertTrue(account.isReadOnly());
        assertEquals("user", username.getText());
        assertEquals("pass", password.getText());
        assertEquals("pass", repeat.getText());
        assertEquals("https://x", url.getText());
        assertEquals("n", notes.getText());
        assertNotNull(labelContaining(window, "Edit password"));
    }

    @Test
    public void prefill_nullFields_shouldUseEmptyStrings() {
        screen.prefill(new PasswordEntry.Builder("gmail").build());

        assertEquals("", username.getText());
        assertEquals("", password.getText());
        assertEquals("", url.getText());
        assertEquals("", notes.getText());
    }

    @Test
    public void showError_shouldDisplayMessage() {
        screen.showError("Passwords do not match.");

        assertNotNull(labelContaining(window, "Passwords do not match."));
    }

    @Test
    public void save_withoutCallback_shouldDoNothing() {
        click(save);

        assertTrue(isOpen(window));
    }

    @Test
    public void save_shouldRunCallbackSetLater() {
        AtomicInteger saves = new AtomicInteger();
        screen.setOnSave(saves::incrementAndGet);

        click(save);

        assertEquals(1, saves.get());
    }

    @Test
    public void constructorCallbacks_shouldBeUsed() throws Exception {
        AtomicInteger saves = new AtomicInteger();
        AtomicInteger cancels = new AtomicInteger();
        AddPassword withCallbacks = new AddPassword(Theme.getTheme(),
                saves::incrementAndGet, cancels::incrementAndGet);
        BasicWindow w = withCallbacks.getWindow();
        List<Button> buttons = find(w, Button.class);
        tui.show(w);

        focus(w, buttons.get(1));
        press(w, ENTER);
        focus(w, buttons.get(2));
        press(w, ENTER);

        assertEquals(1, saves.get());
        assertEquals(1, cancels.get());
        assertTrue(isOpen(w));
    }

    @Test
    public void cancel_withoutCallback_shouldCloseWindow() {
        click(cancel);

        assertFalse(isOpen(window));
    }

    @Test
    public void cancel_withCallback_shouldRunCallbackInsteadOfClosing() {
        AtomicInteger cancels = new AtomicInteger();
        screen.setOnCancel(cancels::incrementAndGet);

        click(cancel);

        assertEquals(1, cancels.get());
        assertTrue(isOpen(window));
    }

    @Test
    public void escape_shouldActAsCancel() {
        AtomicInteger cancels = new AtomicInteger();
        screen.setOnCancel(cancels::incrementAndGet);

        press(window, ESC);

        assertEquals(1, cancels.get());
    }

    @Test
    public void escape_withoutCallback_shouldCloseWindow() {
        press(window, ESC);

        assertFalse(isOpen(window));
    }

    @Test
    public void qKey_shouldBeTypeableAndNotClose() {
        focus(window, account);
        press(window, character('q'));

        assertEquals("q", account.getText());
        assertTrue(isOpen(window));
    }

    @Test
    public void otherUnhandledKey_shouldNotClose() {
        focus(window, save);
        press(window, character('x'));

        assertTrue(isOpen(window));
    }
}

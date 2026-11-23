package tui;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.TextBox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static tui.TuiTestSupport.*;

public class LoginTest {

    private TuiTestSupport tui;
    private AtomicInteger loginCalls;
    private Login login;
    private BasicWindow window;
    private TextBox passwordBox;
    private Button unlock;

    @Before
    public void setUp() throws Exception {
        tui = new TuiTestSupport();
        loginCalls = new AtomicInteger();
        login = new Login(Theme.getTheme(), loginCalls::incrementAndGet);
        window = login.getWindow();
        passwordBox = find(window, TextBox.class).get(0);
        unlock = find(window, Button.class).get(0);
        tui.show(window);
    }

    @After
    public void tearDown() throws Exception {
        tui.stop();
    }

    @Test
    public void constructor_shouldBuildMaskedPasswordFieldAndUnlockButton() {
        assertEquals(1, find(window, TextBox.class).size());
        assertEquals(Character.valueOf('*'), passwordBox.getMask());
        assertTrue(unlock.getLabel().contains("Unlock"));
        assertNotNull(labelContaining(window, "PASSWORD  MANAGER"));
        assertNotNull(labelContaining(window, "Enter your master password"));
    }

    @Test
    public void typing_shouldFillPasswordField() {
        focus(window, passwordBox);
        press(window, character('p'), character('w'));

        assertEquals("pw", login.getEnteredPassword());
    }

    @Test
    public void unlockButton_shouldRunCallback() {
        focus(window, unlock);
        press(window, ENTER);

        assertEquals(1, loginCalls.get());
    }

    @Test
    public void showError_shouldDisplayMessage() {
        login.showError("Incorrect password. Try again.");

        assertNotNull(labelContaining(window, "Incorrect password. Try again."));
    }

    @Test
    public void clearPassword_shouldEmptyField() {
        passwordBox.setText("secret");

        login.clearPassword();

        assertEquals("", login.getEnteredPassword());
    }

    @Test
    public void escape_shouldCloseWindow() {
        press(window, ESC);

        assertFalse(isOpen(window));
    }

    @Test
    public void upperCaseQ_onButton_shouldCloseWindow() {
        focus(window, unlock);
        press(window, character('Q'));

        assertFalse(isOpen(window));
    }

    @Test
    public void otherKey_onButton_shouldNotCloseWindow() {
        focus(window, unlock);
        press(window, character('x'));

        assertTrue(isOpen(window));
    }

    @Test
    public void close_shouldRemoveWindowFromGui() {
        login.close();

        assertFalse(isOpen(window));
    }
}

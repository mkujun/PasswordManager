package tui;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.CheckBox;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.TextBox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static tui.TuiTestSupport.*;

public class SignupTest {

    private TuiTestSupport tui;
    private AtomicInteger signupCalls;
    private Signup signup;
    private BasicWindow window;
    private TextBox passwordBox;
    private TextBox confirmBox;
    private CheckBox showPassword;
    private Button register;

    @Before
    public void setUp() throws Exception {
        tui = new TuiTestSupport();
        signupCalls = new AtomicInteger();
        signup = new Signup(Theme.getTheme(), signupCalls::incrementAndGet);
        window = signup.getWindow();

        List<TextBox> boxes = find(window, TextBox.class);
        passwordBox = boxes.get(0);
        confirmBox = boxes.get(1);
        showPassword = find(window, CheckBox.class).get(0);
        register = find(window, Button.class).get(0);

        tui.show(window);
    }

    @After
    public void tearDown() throws Exception {
        tui.stop();
    }

    private void clickRegister() {
        focus(window, register);
        press(window, ENTER);
    }

    @Test
    public void constructor_shouldBuildFormWithMaskedFields() {
        assertEquals(2, find(window, TextBox.class).size());
        assertEquals(Character.valueOf('*'), passwordBox.getMask());
        assertEquals(Character.valueOf('*'), confirmBox.getMask());
        assertFalse(showPassword.isChecked());
        assertTrue(register.getLabel().contains("Register"));
        assertNotNull(labelContaining(window, "CREATE  MASTER  PASSWORD"));
    }

    @Test
    public void getEnteredPassword_shouldReturnFirstField() {
        passwordBox.setText("master");
        confirmBox.setText("other");

        assertEquals("master", signup.getEnteredPassword());
    }

    @Test
    public void typing_shouldFillFocusedField() {
        focus(window, passwordBox);
        press(window, character('a'), character('b'), character('c'));

        assertEquals("abc", signup.getEnteredPassword());
    }

    @Test
    public void register_emptyPassword_shouldShowErrorAndNotCallback() {
        clickRegister();

        assertEquals(0, signupCalls.get());
        assertTrue(labelTexts(window).contains("Master password must not be empty"));
    }

    @Test
    public void register_passwordsDoNotMatch_shouldShowErrorAndNotCallback() {
        passwordBox.setText("master");
        confirmBox.setText("other");

        clickRegister();

        assertEquals(0, signupCalls.get());
        assertTrue(labelTexts(window).contains("Passwords do not match"));
    }

    @Test
    public void register_validInput_shouldCallbackAndClearStatus() {
        passwordBox.setText("master");
        confirmBox.setText("master");
        signup.showError("old error");

        clickRegister();

        assertEquals(1, signupCalls.get());
        assertFalse(labelTexts(window).contains("old error"));
    }

    @Test
    public void register_nullCallback_shouldNotThrow() throws Exception {
        Signup noCallback = new Signup(Theme.getTheme(), null);
        BasicWindow w = noCallback.getWindow();
        List<TextBox> boxes = find(w, TextBox.class);
        boxes.get(0).setText("master");
        boxes.get(1).setText("master");
        tui.show(w);

        focus(w, find(w, Button.class).get(0));
        press(w, ENTER);

        assertTrue(isOpen(w));
    }

    @Test
    public void showPassword_toggle_shouldUnmaskAndMaskBothFields() throws Exception {
        showPassword.setChecked(true);
        tui.pump();

        assertNull(passwordBox.getMask());
        assertNull(confirmBox.getMask());

        showPassword.setChecked(false);
        tui.pump();

        assertEquals(Character.valueOf('*'), passwordBox.getMask());
        assertEquals(Character.valueOf('*'), confirmBox.getMask());
    }

    @Test
    public void showPassword_spaceKey_shouldToggleCheckbox() throws Exception {
        focus(window, showPassword);
        press(window, character(' '));
        tui.pump();

        assertTrue(showPassword.isChecked());
        assertNull(passwordBox.getMask());
    }

    @Test
    public void clearPasswords_shouldEmptyBothFields() {
        passwordBox.setText("a");
        confirmBox.setText("b");

        signup.clearPasswords();

        assertEquals("", passwordBox.getText());
        assertEquals("", confirmBox.getText());
    }

    @Test
    public void showError_shouldSetRedStatusText() {
        signup.showError("Something failed");

        Label status = labelContaining(window, "Something failed");
        assertNotNull(status);
        assertEquals(Theme.ERROR, status.getForegroundColor());
    }

    @Test
    public void showSuccess_shouldShowDialogAndLockForm() {
        passwordBox.setText("master");
        confirmBox.setText("master");
        // The success dialog blocks until it is closed: OK is pressed via queued ENTER.
        tui.queue(ENTER);

        signup.showSuccess("Master password created");

        Label status = labelContaining(window, "Master password created");
        assertNotNull(status);
        assertEquals(Theme.SUCCESS, status.getForegroundColor());
        assertEquals("", passwordBox.getText());
        assertEquals("", confirmBox.getText());
        assertFalse(passwordBox.isEnabled());
        assertFalse(confirmBox.isEnabled());
        assertFalse(showPassword.isEnabled());
        assertFalse(register.isEnabled());
        assertTrue(isOpen(window));
        assertEquals(window, tui.gui.getActiveWindow());
    }

    @Test
    public void showSuccess_windowNotShown_shouldOnlyUpdateStatus() {
        Signup hidden = new Signup(Theme.getTheme(), null);

        hidden.showSuccess("done");

        assertNotNull(labelContaining(hidden.getWindow(), "done"));
    }

    @Test
    public void showError_afterSuccess_shouldBeRedAgain() {
        tui.queue(ENTER);
        signup.showSuccess("ok");

        signup.showError("bad");

        assertEquals(Theme.ERROR, labelContaining(window, "bad").getForegroundColor());
    }

    @Test
    public void escape_shouldCloseWindow() {
        press(window, ESC);

        assertFalse(isOpen(window));
    }

    @Test
    public void qKey_shouldCloseWindowWhenNoFieldFocused() {
        focus(window, register);
        press(window, character('q'));

        assertFalse(isOpen(window));
    }
}

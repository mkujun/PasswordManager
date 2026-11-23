package tui;

import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static tui.TuiTestSupport.*;

public class MainMenuTest {

    private TuiTestSupport tui;
    private AtomicInteger viewCalls;
    private AtomicInteger addCalls;
    private AtomicInteger settingsCalls;
    private BasicWindow window;
    private ActionListBox menu;

    @Before
    public void setUp() throws Exception {
        tui = new TuiTestSupport();
        viewCalls = new AtomicInteger();
        addCalls = new AtomicInteger();
        settingsCalls = new AtomicInteger();
        MainMenu mainMenu = new MainMenu(Theme.getTheme(),
                viewCalls::incrementAndGet,
                addCalls::incrementAndGet,
                settingsCalls::incrementAndGet);
        window = mainMenu.getWindow();
        menu = find(window, ActionListBox.class).get(0);
        tui.show(window);
        focus(window, menu);
    }

    @After
    public void tearDown() throws Exception {
        tui.stop();
    }

    @Test
    public void constructor_shouldListFourMenuItems() {
        assertEquals(4, menu.getItemCount());
        assertTrue(menu.getItemAt(0).toString().contains("View passwords"));
        assertTrue(menu.getItemAt(1).toString().contains("Add new password"));
        assertTrue(menu.getItemAt(2).toString().contains("Settings"));
        assertTrue(menu.getItemAt(3).toString().contains("Exit"));
        assertNotNull(labelContaining(window, "v1.0"));
    }

    @Test
    public void firstItem_enter_shouldRunViewCallback() {
        press(window, ENTER);

        assertEquals(1, viewCalls.get());
        assertEquals(0, addCalls.get());
        assertTrue(isOpen(window));
    }

    @Test
    public void secondItem_enter_shouldRunAddCallback() {
        press(window, DOWN, ENTER);

        assertEquals(1, addCalls.get());
    }

    @Test
    public void thirdItem_enter_shouldRunSettingsCallback() {
        press(window, DOWN, DOWN, ENTER);

        assertEquals(1, settingsCalls.get());
    }

    @Test
    public void exitItem_enter_shouldCloseWindow() {
        press(window, DOWN, DOWN, DOWN, ENTER);

        assertFalse(isOpen(window));
    }

    @Test
    public void escape_shouldCloseWindow() {
        press(window, ESC);

        assertFalse(isOpen(window));
    }

    @Test
    public void nullCallbacks_shouldBeNoOps() throws Exception {
        MainMenu empty = new MainMenu(Theme.getTheme(), null, null, null);
        BasicWindow w = empty.getWindow();
        ActionListBox items = find(w, ActionListBox.class).get(0);
        tui.show(w);
        focus(w, items);

        press(w, ENTER, DOWN, ENTER, DOWN, ENTER);

        assertTrue(isOpen(w));
    }
}

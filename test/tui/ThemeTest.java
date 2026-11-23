package tui;

import com.googlecode.lanterna.gui2.BasicWindow;
import org.junit.Test;

import static org.junit.Assert.*;

public class ThemeTest {

    @Test
    public void getTheme_shouldReturnSameConfiguredTheme() {
        assertNotNull(Theme.getTheme());
        assertSame(Theme.getTheme(), Theme.getTheme());
        assertEquals(Theme.FG, Theme.getTheme().getDefaultDefinition().getNormal().getForeground());
        assertEquals(Theme.BG, Theme.getTheme().getDefaultDefinition().getNormal().getBackground());
    }

    @Test
    public void colors_shouldBeDistinct() {
        assertNotEquals(Theme.ERROR, Theme.SUCCESS);
        assertNotEquals(Theme.BG, Theme.CARD_BG);
        assertNotEquals(Theme.ACCENT, Theme.ACCENT2);
        assertNotNull(Theme.MUTED);
    }

    @Test
    public void appScreen_shouldCreateFullScreenUndecoratedWindowWithTheme() {
        AppScreen screen = new Login(Theme.getTheme(), null);
        BasicWindow window = screen.getWindow();

        assertSame(Theme.getTheme(), window.getTheme());
        assertTrue(window.getHints().contains(com.googlecode.lanterna.gui2.Window.Hint.FULL_SCREEN));
        assertTrue(window.getHints().contains(com.googlecode.lanterna.gui2.Window.Hint.NO_DECORATIONS));
    }
}

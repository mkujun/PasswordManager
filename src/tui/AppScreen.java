package tui;

import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for all full-screen TUI screens.
 *
 * Holds the shared window + theme and provides common setup that every
 * screen needs: a full-screen undecorated window and a Q/ESC exit handler.
 * Concrete screens build their own component tree in {@link #buildContent()}.
 */
public abstract class AppScreen {

    protected final SimpleTheme theme;
    protected final BasicWindow window;

    protected AppScreen(SimpleTheme theme) {
        this.theme = theme;
        this.window = createWindow(theme);
    }

    /** Builds a full-screen, undecorated window using the given theme. */
    private static BasicWindow createWindow(SimpleTheme theme) {
        BasicWindow window = new BasicWindow();
        window.setTheme(theme);
        window.setHints(Arrays.asList(
                Window.Hint.FULL_SCREEN,
                Window.Hint.NO_DECORATIONS
        ));
        return window;
    }

    /** Exposes the underlying window so the GUI can display it. */
    public BasicWindow getWindow() {
        return window;
    }

    /** Closes this screen's window. */
    public void close() {
        window.close();
    }

    /**
     * Registers a listener that closes the given window when the user
     * presses Q or ESC. Shared by all screens so the behaviour stays
     * consistent and is defined in a single place.
     */
    protected static void addExitOnQorEsc(final BasicWindow window) {
        window.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onUnhandledInput(Window basePane, KeyStroke keyStroke,
                                         AtomicBoolean hasBeenHandled) {
                if (isQuitKey(keyStroke)) {
                    window.close();
                    hasBeenHandled.set(true);
                }
            }
        });
    }

    /** Returns true if the key stroke is ESC or (upper/lower case) Q. */
    private static boolean isQuitKey(KeyStroke keyStroke) {
        if (keyStroke.getKeyType() == KeyType.Escape) {
            return true;
        }
        return keyStroke.getKeyType() == KeyType.Character
                && Character.toUpperCase(keyStroke.getCharacter()) == 'Q';
    }
}

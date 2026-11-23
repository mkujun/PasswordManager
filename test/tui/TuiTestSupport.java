package tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Composite;
import com.googlecode.lanterna.gui2.Container;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test helper for the Lanterna screens. Runs a real {@link MultiWindowTextGUI}
 * on top of an in-memory {@link DefaultVirtualTerminal}, so screens can be
 * shown, receive key strokes and open modal dialogs without a real terminal.
 *
 * Key strokes can be sent directly to a window ({@link #press}) or queued on
 * the virtual terminal ({@link #queue}); queued input is consumed by blocking
 * calls such as {@code addWindowAndWait} or {@code MessageDialog.showDialog}.
 */
final class TuiTestSupport {

    final DefaultVirtualTerminal terminal;
    final TerminalScreen screen;
    final MultiWindowTextGUI gui;

    TuiTestSupport() throws IOException {
        terminal = new DefaultVirtualTerminal(new TerminalSize(140, 45));
        screen = new TerminalScreen(terminal);
        screen.startScreen();
        gui = new MultiWindowTextGUI(screen);
    }

    void stop() throws IOException {
        screen.stopScreen();
    }

    /** Adds the window to the GUI without blocking and renders it once. */
    void show(Window window) throws IOException {
        gui.addWindow(window);
        gui.updateScreen();
    }

    /** True while the window is still part of the GUI (not closed). */
    static boolean isOpen(Window window) {
        return window.getTextGUI() != null;
    }

    /**
     * Runs tasks queued with invokeLater (e.g. CheckBox listeners, which
     * Lanterna dispatches on the GUI thread) and redraws the screen.
     */
    void pump() throws IOException {
        gui.getGUIThread().processEventsAndUpdate();
    }

    /** Sends key strokes straight to the window (listeners + focused widget). */
    static void press(Window window, KeyStroke... keys) {
        for (KeyStroke key : keys) {
            window.handleInput(key);
        }
    }

    /** Queues key strokes on the virtual terminal for blocking GUI loops. */
    void queue(KeyStroke... keys) {
        for (KeyStroke key : keys) {
            terminal.addInput(key);
        }
    }

    /** Queues one character key stroke per character of the text. */
    void queueText(String text) {
        for (char c : text.toCharArray()) {
            terminal.addInput(character(c));
        }
    }

    static KeyStroke character(char c) {
        return new KeyStroke(c, false, false);
    }

    static KeyStroke key(KeyType type) {
        return new KeyStroke(type);
    }

    static final KeyStroke ENTER = new KeyStroke(KeyType.Enter);
    static final KeyStroke ESC = new KeyStroke(KeyType.Escape);
    static final KeyStroke TAB = new KeyStroke(KeyType.Tab);
    static final KeyStroke DOWN = new KeyStroke(KeyType.ArrowDown);
    static final KeyStroke UP = new KeyStroke(KeyType.ArrowUp);
    static final KeyStroke RIGHT = new KeyStroke(KeyType.ArrowRight);

    // ---------------------------------------------------------------
    //  Component tree lookup
    // ---------------------------------------------------------------

    /** Returns all components of the given type in tree order. */
    static <T extends Component> List<T> find(BasicWindow window, Class<T> type) {
        List<T> result = new ArrayList<>();
        collect(window.getComponent(), type, result);
        return result;
    }

    private static <T extends Component> void collect(Component component, Class<T> type, List<T> out) {
        if (component == null) {
            return;
        }
        if (type.isInstance(component)) {
            out.add(type.cast(component));
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getChildren()) {
                collect(child, type, out);
            }
        } else if (component instanceof Composite) {
            collect(((Composite) component).getComponent(), type, out);
        }
    }

    /** Returns the text of every label in the window. */
    static List<String> labelTexts(BasicWindow window) {
        List<String> texts = new ArrayList<>();
        for (Label label : find(window, Label.class)) {
            texts.add(label.getText());
        }
        return texts;
    }

    /** Returns the first label whose text contains the given fragment, or null. */
    static Label labelContaining(BasicWindow window, String fragment) {
        for (Label label : find(window, Label.class)) {
            if (label.getText().contains(fragment)) {
                return label;
            }
        }
        return null;
    }

    static void focus(Window window, Interactable interactable) {
        window.setFocusedInteractable(interactable);
    }
}

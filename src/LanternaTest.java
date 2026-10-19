import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.util.Arrays;

public class LanternaTest {

    public static void main(String[] args) throws Exception {
        // Force a text (unix) terminal so the UI runs inside the WSL Ubuntu
        // terminal instead of opening a separate Swing/AWT window.
        DefaultTerminalFactory factory = new DefaultTerminalFactory()
                .setForceTextTerminal(true)
                .setTerminalEmulatorTitle("Password Manager")
                .setUnixTerminalCtrlCBehaviour(com.googlecode.lanterna.terminal.ansi.UnixLikeTerminal.CtrlCBehaviour.TRAP);

        Terminal terminal = factory.createTerminal();
        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();

        try {
            // Create gui
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);

            final BasicWindow window = new BasicWindow();
            window.setHints(Arrays.asList(
                    Window.Hint.FULL_SCREEN,
                    Window.Hint.NO_DECORATIONS
            ));

            // Root panel uses BorderLayout so we get TOP / CENTER / BOTTOM regions
            Panel root = new Panel(new BorderLayout());

            // ---- Header ----
            Panel header = new Panel(new LinearLayout(Direction.HORIZONTAL));
            Label headerLabel = new Label("[ Password Manager ]");
            headerLabel.setForegroundColor(TextColor.ANSI.BLACK);
            headerLabel.setBackgroundColor(TextColor.ANSI.CYAN);
            // Stretch the header label across the full terminal width.
            headerLabel.setLayoutData(
                    LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
            header.addComponent(headerLabel);
            header.setLayoutData(BorderLayout.Location.TOP);

            // ---- Content ----
            Panel content = new Panel(new LinearLayout(Direction.VERTICAL));
            content.addComponent(new Label("Welcome!"));
            content.addComponent(new EmptySpace());
            content.addComponent(new Label("This is the content area."));
            content.setLayoutData(BorderLayout.Location.CENTER);

            // ---- Footer ----
            Panel footer = new Panel(new LinearLayout(Direction.HORIZONTAL));
            Label footerLabel = new Label("Press Q or ESC to exit");
            footerLabel.setForegroundColor(TextColor.ANSI.BLACK);
            footerLabel.setBackgroundColor(TextColor.ANSI.WHITE);
            footerLabel.setLayoutData(
                    LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
            footer.addComponent(footerLabel);
            footer.setLayoutData(BorderLayout.Location.BOTTOM);

            // Assemble window
            root.addComponent(header);
            root.addComponent(content);
            root.addComponent(footer);

            window.setComponent(root);

            // Close the window on Q / q / ESC so the program exits cleanly
            // in a real terminal (no window decorations to click).
            window.addWindowListener(new WindowListenerAdapter() {
                @Override
                public void onUnhandledInput(Window basePane, KeyStroke keyStroke,
                                             java.util.concurrent.atomic.AtomicBoolean hasBeenHandled) {
                    if (keyStroke.getKeyType() == KeyType.Escape
                            || (keyStroke.getKeyType() == KeyType.Character
                                && Character.toUpperCase(keyStroke.getCharacter()) == 'Q')) {
                        window.close();
                        hasBeenHandled.set(true);
                    }
                }
            });

            gui.addWindowAndWait(window);
        } finally {
            // Restore the terminal so the WSL shell stays usable after exit.
            screen.stopScreen();
        }
    }

}

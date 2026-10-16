import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

public class LanternaTest {

    public static void main(String[] args) throws Exception {
        Screen screen = new DefaultTerminalFactory()
                .setInitialTerminalSize(new TerminalSize(80, 24))
                .createScreen();

        screen.startScreen();

        try {
            screen.newTextGraphics().putString(
                    2, 2, "Lanterna works!"
            );
            screen.refresh();

            screen.readInput();
        } finally {
            screen.stopScreen();
        }
    }
}
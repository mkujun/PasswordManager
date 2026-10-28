package tui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static tui.Theme.ACCENT;
import static tui.Theme.CARD_BG;
import static tui.Theme.FG;
import static tui.Theme.MUTED;
import static tui.Theme.SUCCESS;

/**
 * Main menu screen, shown right after a successful login (sketch screen 1).
 *
 * Offers the top-level actions: view passwords, add a new password, open
 * settings and exit. Each action is supplied by the caller as a Runnable so
 * this screen stays decoupled from navigation/business logic.
 */
public class MainMenu extends AppScreen {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final String APP_VERSION = "v1.0";

    private final Runnable onViewPasswords;
    private final Runnable onAddPassword;
    private final Runnable onSettings;

    public MainMenu(SimpleTheme theme,
                    Runnable onViewPasswords,
                    Runnable onAddPassword,
                    Runnable onSettings) {
        super(theme);
        this.onViewPasswords = onViewPasswords;
        this.onAddPassword = onAddPassword;
        this.onSettings = onSettings;
        initGuiElements();
        // ESC / Q closes the menu (and thus exits the app at the top level).
        addExitOnQorEsc(window);
    }

    private void initGuiElements() {
        Panel root = new Panel(new BorderLayout());
        root.addComponent(buildHeader());
        root.addComponent(buildBody());
        root.addComponent(buildFooter());
        window.setComponent(root);
    }

    // =====================================================
    //  Header: title (left) + version / date (right)
    // =====================================================
    private Panel buildHeader() {
        Panel header = new Panel(new BorderLayout());

        Label title = new Label("  Password Manager ");
        title.setForegroundColor(ACCENT);
        title.setBackgroundColor(CARD_BG);
        title.addStyle(SGR.BOLD);
        title.setLayoutData(BorderLayout.Location.LEFT);

        Label meta = new Label(APP_VERSION + "  |  "
                + LocalDateTime.now().format(DATE_FORMAT) + " ");
        meta.setForegroundColor(MUTED);
        meta.setBackgroundColor(CARD_BG);
        meta.setLayoutData(BorderLayout.Location.RIGHT);

        // Spacer in the middle so the background stretches across the bar.
        Label filler = new Label("");
        filler.setBackgroundColor(CARD_BG);
        filler.setLayoutData(BorderLayout.Location.CENTER);

        header.addComponent(title);
        header.addComponent(filler);
        header.addComponent(meta);
        header.setLayoutData(BorderLayout.Location.TOP);
        return header;
    }

    // =====================================================
    //  Body: the menu entries inside a bordered card
    // =====================================================
    private Panel buildBody() {
        ActionListBox menu = new ActionListBox(new TerminalSize(60, 4));
        menu.addItem(" 1. View passwords", runOrNoop(onViewPasswords));
        menu.addItem(" 2. Add new password", runOrNoop(onAddPassword));
        menu.addItem(" 3. Settings", runOrNoop(onSettings));
        menu.addItem(" 4. Exit", window::close);

        Panel card = new Panel(new LinearLayout(Direction.VERTICAL));
        card.addComponent(menu);

        Panel body = new Panel(new LinearLayout(Direction.VERTICAL));
        body.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        body.addComponent(card.withBorder(Borders.singleLine()));
        body.setLayoutData(BorderLayout.Location.CENTER);
        return body;
    }

    // =====================================================
    //  Footer: key hints
    // =====================================================
    private Panel buildFooter() {
        Panel footer = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Label footerLabel = new Label(" UP/DOWN navigate | ENTER select | Q / ESC exit ");
        footerLabel.setForegroundColor(SUCCESS);
        footerLabel.setBackgroundColor(CARD_BG);
        footerLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        footer.addComponent(footerLabel);
        footer.setLayoutData(BorderLayout.Location.BOTTOM);
        return footer;
    }

    /** Wraps a possibly-null callback so the menu item never NPEs. */
    private static Runnable runOrNoop(Runnable action) {
        return action == null ? () -> { } : action;
    }
}

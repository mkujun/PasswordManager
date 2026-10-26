package tui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.*;

import static tui.Theme.ACCENT;
import static tui.Theme.ACCENT2;
import static tui.Theme.CARD_BG;
import static tui.Theme.ERROR;
import static tui.Theme.FG;
import static tui.Theme.SUCCESS;

/**
 * Login screen: master password prompt inside a centered card.
 *
 * Exposes intent-based methods (getEnteredPassword, showError, clearPassword)
 * instead of leaking its internal widgets, so callers do not depend on the
 * screen's layout.
 */
public class Login extends AppScreen {

    private final Button loginButton;
    private TextBox passwordBox;
    private Label statusLabel;

    public Login(SimpleTheme theme, Runnable onLogin) {
        super(theme);
        this.loginButton = new Button("  Unlock  ", onLogin);
        initGuiElements();
        addExitOnQorEsc(window);
    }

    /** Returns the password currently typed by the user. */
    public String getEnteredPassword() {
        return passwordBox.getText();
    }

    /** Shows a status/error message to the user. */
    public void showError(String message) {
        statusLabel.setText(message);
    }

    /** Clears the password input field. */
    public void clearPassword() {
        passwordBox.setText("");
    }

    private void initGuiElements() {
        // Root panel fills the whole screen and centers the card.
        Panel root = new Panel(new BorderLayout());

        // =====================================================
        //  CENTERED CARD (double-line border)
        // =====================================================
        Panel card = new Panel(new LinearLayout(Direction.VERTICAL));

        // --- ASCII logo / brand ---
        Label line1 = new Label("    .--------.    ");
        Label line2 = new Label("    |  o__o  |    ");
        Label line3 = new Label("    |__[==]__|    ");

        for (Label l : new Label[]{line1, line2, line3}) {
            l.setForegroundColor(ACCENT);
            l.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
            card.addComponent(l);
        }

        Label title = new Label("PASSWORD  MANAGER");
        title.setForegroundColor(ACCENT);
        title.addStyle(SGR.BOLD);
        title.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        card.addComponent(title);

        Label subtitle = new Label("secure vault - master login");
        subtitle.setForegroundColor(ACCENT2);
        subtitle.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        card.addComponent(subtitle);

        card.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        card.addComponent(new Separator(Direction.HORIZONTAL)
                .setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill)));
        card.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        // --- Prompt + password input ---
        Label prompt = new Label("Enter your master password");
        prompt.setForegroundColor(FG);
        prompt.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        card.addComponent(prompt);

        card.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        // Mask the password with asterisks (inside its own border).
        passwordBox = new TextBox().setMask('*');
        passwordBox.setPreferredSize(new TerminalSize(32, 1));
        Panel passwordWrap = new Panel(new LinearLayout(Direction.HORIZONTAL));
        passwordWrap.addComponent(passwordBox.withBorder(Borders.singleLine()));
        passwordWrap.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        card.addComponent(passwordWrap);

        card.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        // Status / error message.
        statusLabel = new Label(" ");
        statusLabel.setForegroundColor(ERROR);
        statusLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        card.addComponent(statusLabel);

        card.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        // Unlock button, placed right after the status message.
        loginButton.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        card.addComponent(loginButton);

        // Card border + centering within the screen.
        Panel cardWrap = new Panel(new GridLayout(1));
        Border borderedCard = card.withBorder(Borders.doubleLine());
        borderedCard.setLayoutData(GridLayout.createLayoutData(
                GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, true, true));
        cardWrap.addComponent(borderedCard);
        cardWrap.setLayoutData(BorderLayout.Location.CENTER);

        // =====================================================
        //  Header (top bar)
        // =====================================================
        Panel header = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Label headerLabel = new Label("  Password Manager ");
        headerLabel.setForegroundColor(ACCENT);
        headerLabel.setBackgroundColor(CARD_BG);
        headerLabel.addStyle(SGR.BOLD);
        headerLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        header.addComponent(headerLabel);
        header.setLayoutData(BorderLayout.Location.TOP);

        // =====================================================
        //  Footer (bottom bar)
        // =====================================================
        Panel footer = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Label footerLabel = new Label(" TAB move | ENTER unlock | Q / ESC exit ");
        footerLabel.setForegroundColor(SUCCESS);
        footerLabel.setBackgroundColor(CARD_BG);
        footerLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        footer.addComponent(footerLabel);
        footer.setLayoutData(BorderLayout.Location.BOTTOM);

        root.addComponent(header);
        root.addComponent(cardWrap);
        root.addComponent(footer);

        window.setComponent(root);
    }
}

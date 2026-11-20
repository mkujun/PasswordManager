package tui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;

import static tui.Theme.ACCENT;
import static tui.Theme.ACCENT2;
import static tui.Theme.CARD_BG;
import static tui.Theme.ERROR;
import static tui.Theme.FG;
import static tui.Theme.SUCCESS;

/**
 * Signup screen: the user chooses a master password (entered twice) inside a
 * centered card. The password can be shown/hidden with a checkbox.
 *
 * The register action only runs the supplied callback when the input is valid
 * (non-empty and both entries match); otherwise an error is shown.
 */
public class Signup extends AppScreen {

    private static final char MASK = '*';

    private final Runnable onSignup;
    private TextBox passwordBox;
    private TextBox confirmBox;
    private CheckBox showPasswordBox;
    private Button registerButton;
    private Label statusLabel;

    protected Signup(SimpleTheme theme, Runnable onSignup) {
        super(theme);
        this.onSignup = onSignup;
        initGuiElements();
        addExitOnQorEsc(window);
    }

    /** Returns the master password typed in the first field. */
    public String getEnteredPassword() {
        return passwordBox.getText();
    }

    /** Shows a status/error message to the user. */
    public void showError(String message) {
        statusLabel.setForegroundColor(ERROR);
        statusLabel.setText(message);
    }

    /**
     * Notifies the user that the master password was created: shows a green
     * status message and a modal confirmation dialog, then clears and locks
     * the form so the password cannot be registered twice.
     */
    public void showSuccess(String message) {
        statusLabel.setForegroundColor(SUCCESS);
        statusLabel.setText(message);

        clearPasswords();
        passwordBox.setEnabled(false);
        confirmBox.setEnabled(false);
        showPasswordBox.setEnabled(false);
        registerButton.setEnabled(false);

        WindowBasedTextGUI gui = window.getTextGUI();
        if (gui != null) {
            MessageDialog dialog = new MessageDialogBuilder()
                    .setTitle("Signup successful")
                    .setText(message)
                    .addButton(MessageDialogButton.OK)
                    .build();
            dialog.setTheme(theme);
            dialog.showDialog(gui);
        }
    }

    /** Clears both password input fields. */
    public void clearPasswords() {
        passwordBox.setText("");
        confirmBox.setText("");
    }

    /** Validates the input and runs the signup callback if it is valid. */
    private void onRegister() {
        String password = passwordBox.getText();
        String confirm = confirmBox.getText();

        if (password.isEmpty()) {
            showError("Master password must not be empty");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match");
            return;
        }
        showError(" ");
        if (onSignup != null) {
            onSignup.run();
        }
    }

    /** Toggles between masked and plain-text display of both fields. */
    private void applyPasswordVisibility(boolean visible) {
        Character mask = visible ? null : MASK;
        passwordBox.setMask(mask);
        confirmBox.setMask(mask);
    }

    private Component centered(Component component) {
        component.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        return component;
    }

    private Panel inputWrap(TextBox box) {
        box.setPreferredSize(new TerminalSize(32, 1));
        Panel wrap = new Panel(new LinearLayout(Direction.HORIZONTAL));
        wrap.addComponent(box.withBorder(Borders.singleLine()));
        wrap.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        return wrap;
    }

    private void initGuiElements() {
        Panel root = new Panel(new BorderLayout());

        // =====================================================
        //  CENTERED CARD (double-line border)
        // =====================================================
        Panel card = new Panel(new LinearLayout(Direction.VERTICAL));

        Label title = new Label("CREATE  MASTER  PASSWORD");
        title.setForegroundColor(ACCENT);
        title.addStyle(SGR.BOLD);
        card.addComponent(centered(title));

        Label subtitle = new Label("sign up - first time setup");
        subtitle.setForegroundColor(ACCENT2);
        card.addComponent(centered(subtitle));

        card.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        card.addComponent(new Separator(Direction.HORIZONTAL)
                .setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill)));
        card.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        // --- Master password ---
        Label prompt = new Label("Enter master password");
        prompt.setForegroundColor(FG);
        card.addComponent(centered(prompt));

        passwordBox = new TextBox().setMask(MASK);
        card.addComponent(inputWrap(passwordBox));

        card.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        // --- Confirmation ---
        Label confirmPrompt = new Label("Repeat master password");
        confirmPrompt.setForegroundColor(FG);
        card.addComponent(centered(confirmPrompt));

        confirmBox = new TextBox().setMask(MASK);
        card.addComponent(inputWrap(confirmBox));

        card.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        // --- Show / hide password ---
        showPasswordBox = new CheckBox("Show password");
        showPasswordBox.addListener(this::applyPasswordVisibility);
        card.addComponent(centered(showPasswordBox));

        card.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        // --- Status / error message ---
        statusLabel = new Label(" ");
        statusLabel.setForegroundColor(ERROR);
        card.addComponent(centered(statusLabel));

        card.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        // --- Register button ---
        registerButton = new Button("  Register  ", this::onRegister);
        card.addComponent(centered(registerButton));

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
        Label footerLabel = new Label(" TAB move | SPACE toggle | ENTER select | ESC exit ");
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

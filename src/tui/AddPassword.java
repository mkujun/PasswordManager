package tui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Separator;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import java.util.concurrent.atomic.AtomicBoolean;

import static tui.Theme.ACCENT;
import static tui.Theme.CARD_BG;
import static tui.Theme.ERROR;
import static tui.Theme.MUTED;
import static tui.Theme.SUCCESS;

/**
 * Add new password screen (sketch screen 4).
 *
 * Collects the fields for a new entry and exposes intent-based accessors
 * (getAccount, getUsername, ...) plus showError(), instead of leaking its
 * widgets. The caller wires the Save/Cancel buttons to callbacks and performs
 * the actual validation, encryption and persistence.
 */
public class AddPassword extends AppScreen {

    private final Button saveButton;
    private final Button cancelButton;
    private Runnable onSave;
    private Runnable onCancel;

    private TextBox accountBox;
    private TextBox usernameBox;
    private TextBox passwordBox;
    private TextBox repeatPasswordBox;
    private TextBox urlBox;
    private TextBox notesBox;
    private Label statusLabel;
    private Button showHideButton;

    // Whether the password fields currently show plain text instead of '*'.
    private boolean passwordVisible = false;

    public AddPassword(SimpleTheme theme, Runnable onSave, Runnable onCancel) {
        super(theme);
        this.onSave = onSave;
        this.onCancel = onCancel;
        // Buttons call through to the (possibly later-assigned) callbacks.
        this.saveButton = new Button("  Save  ", this::fireSave);
        this.cancelButton = new Button(" Cancel ", this::fireCancel);
        initGuiElements();
        // ESC acts as Cancel at the window level. We do NOT use the shared
        // Q/ESC handler here because Q must be typeable in the text fields.
        addCancelOnEsc();
    }

    /** Assigns the Save callback (may be set after construction). */
    public void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    /** Assigns the Cancel callback (may be set after construction). */
    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    private void fireSave() {
        if (onSave != null) {
            onSave.run();
        }
    }

    private void fireCancel() {
        if (onCancel != null) {
            onCancel.run();
        } else {
            window.close();
        }
    }

    // =====================================================
    //  Intent-based accessors (used by the caller)
    // =====================================================
    public String getAccount() {
        return accountBox.getText().trim();
    }

    public String getUsername() {
        return usernameBox.getText().trim();
    }

    public String getPassword() {
        return passwordBox.getText();
    }

    public String getRepeatPassword() {
        return repeatPasswordBox.getText();
    }

    /** Returns true if the password and its repeat are identical. */
    public boolean passwordsMatch() {
        return getPassword().equals(getRepeatPassword());
    }

    public String getUrl() {
        return urlBox.getText().trim();
    }

    public String getNotes() {
        return notesBox.getText().trim();
    }

    /** Shows a validation/error message to the user. */
    public void showError(String message) {
        statusLabel.setForegroundColor(ERROR);
        statusLabel.setText(message);
    }

    private void initGuiElements() {
        Panel root = new Panel(new BorderLayout());
        root.addComponent(buildHeader());
        root.addComponent(buildBody());
        root.addComponent(buildFooter());
        window.setComponent(root);
    }

    // =====================================================
    //  Header
    // =====================================================
    private Panel buildHeader() {
        Panel header = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Label headerLabel = new Label("  Password Manager ");
        headerLabel.setForegroundColor(ACCENT);
        headerLabel.setBackgroundColor(CARD_BG);
        headerLabel.addStyle(SGR.BOLD);
        headerLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        header.addComponent(headerLabel);
        header.setLayoutData(BorderLayout.Location.TOP);
        return header;
    }

    // =====================================================
    //  Body: the input form inside a centered card
    // =====================================================
    private Panel buildBody() {
        Panel form = new Panel(new LinearLayout(Direction.VERTICAL));

        Label title = new Label("Add new password");
        title.setForegroundColor(ACCENT);
        title.addStyle(SGR.BOLD);
        form.addComponent(title);
        form.addComponent(new Separator(Direction.HORIZONTAL)
                .setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill)));
        form.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        // --- Website / Service ---
        accountBox = new TextBox(new TerminalSize(40, 1));
        addField(form, "Website / Service:", accountBox);

        // --- Username ---
        usernameBox = new TextBox(new TerminalSize(40, 1));
        addField(form, "Username:", usernameBox);

        // --- Password + Show/Hide button ---
        form.addComponent(caption("Password:"));
        passwordBox = new TextBox(new TerminalSize(40, 1)).setMask('*');
        showHideButton = new Button("  Show  ", this::togglePasswordVisibility);
        Panel passwordRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
        passwordRow.addComponent(passwordBox.withBorder(Borders.singleLine()));
        passwordRow.addComponent(showHideButton);
        form.addComponent(passwordRow);
        form.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        // --- Repeat password ---
        repeatPasswordBox = new TextBox(new TerminalSize(40, 1)).setMask('*');
        addField(form, "Repeat password:", repeatPasswordBox);

        // --- URL (optional) ---
        urlBox = new TextBox(new TerminalSize(40, 1));
        addField(form, "URL (optional):", urlBox);

        // --- Notes (optional) ---
        notesBox = new TextBox(new TerminalSize(40, 3));
        addField(form, "Notes (optional):", notesBox);

        // --- Status / error line ---
        statusLabel = new Label(" ");
        statusLabel.setForegroundColor(ERROR);
        form.addComponent(statusLabel);
        form.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        // --- Save / Cancel ---
        Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
        buttons.addComponent(saveButton);
        buttons.addComponent(cancelButton);
        form.addComponent(buttons);

        // Center the card on screen.
        Panel cardWrap = new Panel(new GridLayout(1));
        com.googlecode.lanterna.gui2.Border borderedCard = form.withBorder(Borders.singleLine());
        borderedCard.setLayoutData(GridLayout.createLayoutData(
                GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, true, true));
        cardWrap.addComponent(borderedCard);
        cardWrap.setLayoutData(BorderLayout.Location.CENTER);
        return cardWrap;
    }

    /** Adds a captioned, single-line-bordered input row to the form. */
    private void addField(Panel form, String caption, TextBox box) {
        form.addComponent(caption(caption));
        form.addComponent(box.withBorder(Borders.singleLine()));
        form.addComponent(new EmptySpace(new TerminalSize(1, 1)));
    }

    private Label caption(String text) {
        Label label = new Label(text);
        label.setForegroundColor(MUTED);
        return label;
    }

    // =====================================================
    //  Footer
    // =====================================================
    private Panel buildFooter() {
        Panel footer = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Label footerLabel = new Label(" TAB move | Show/Hide reveal password | ENTER save | ESC cancel ");
        footerLabel.setForegroundColor(SUCCESS);
        footerLabel.setBackgroundColor(CARD_BG);
        footerLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        footer.addComponent(footerLabel);
        footer.setLayoutData(BorderLayout.Location.BOTTOM);
        return footer;
    }

    // =====================================================
    //  Show / hide password
    // =====================================================
    /**
     * Toggles between masked ('*') and plain-text display for both the
     * password and repeat-password fields.
     */
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        Character mask = passwordVisible ? null : '*';
        passwordBox.setMask(mask);
        repeatPasswordBox.setMask(mask);
        showHideButton.setLabel(passwordVisible ? "  Hide  " : "  Show  ");
    }

    // =====================================================
    //  ESC-to-cancel handler
    // =====================================================
    /**
     * Registers a listener that triggers the Cancel action when the user
     * presses ESC. Only ESC is used (not Q) so letters remain typeable in the
     * text fields.
     */
    private void addCancelOnEsc() {
        window.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onUnhandledInput(Window basePane, KeyStroke keyStroke,
                                         AtomicBoolean hasBeenHandled) {
                if (keyStroke.getKeyType() == KeyType.Escape) {
                    fireCancel();
                    hasBeenHandled.set(true);
                }
            }
        });
    }
}

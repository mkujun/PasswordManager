package tui;

import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import interfaces.IPasswordsService;
import model.PasswordEntry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lanterna UI prototype.
 *
 * Flow:
 *   1. Login screen  -> user enters the master password.
 *   2. Validation uses the same logic as PasswordManager.authenticate():
 *        deriveKey(password, salt) -> encrypt(password, key) -> compare
 *        against the stored encryptedMasterPassword.
 *   3. On success -> the main menu, from which the user opens the password
 *      list, adds a password, opens settings or exits.
 */
public class Lanterna {
    private static final String FILE_NAME = "passwords.dat";

    private final IPasswordsService passwordsService;

    private final SimpleTheme theme = Theme.getTheme();

    private Login loginWindow;
    private Signup signupWindow;
    private MainMenu mainMenu;

    public MultiWindowTextGUI gui;

    public Lanterna(IPasswordsService passwordsService) {
        this.passwordsService = passwordsService;
    }

    /*
    public static void main(String[] args) throws Exception {
        new Lanterna().run();
    }

     */

    public void run() throws Exception {
        DefaultTerminalFactory factory = new DefaultTerminalFactory()
                .setForceTextTerminal(true)
                .setTerminalEmulatorTitle("Password Manager")
                .setUnixTerminalCtrlCBehaviour(
                        com.googlecode.lanterna.terminal.ansi.UnixLikeTerminal.CtrlCBehaviour.TRAP);

        Terminal terminal = factory.createTerminal();
        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();

        try {
            gui = new MultiWindowTextGUI(screen);
            if (passwordsService.masterPasswordExists()) {
                showLoginScreen();
            }
            else {
                showSignupScreen();
            }
        } finally {
            screen.stopScreen();
        }
    }

    /** Shows the login screen and waits until the user logs in or quits. */
    private void showLoginScreen() {
        loginWindow = new Login(theme, this::login);

        gui.addWindowAndWait(loginWindow.getWindow());
    }

    private void showSignupScreen() {
        signupWindow = new Signup(theme, this::signup);
        gui.addWindowAndWait(signupWindow.getWindow());
    }

    private void signup() {
        String enteredPassword = signupWindow.getEnteredPassword();
        passwordsService.setMasterPassword(enteredPassword);
        signupWindow.showSuccess("Master password created");
    }

    private void login() {
        String enteredPassword = loginWindow.getEnteredPassword();

        if (validateUser(enteredPassword)) {
            showMainMenu();
        }
        else {
            loginWindow.showError("Incorrect password. Try again.");
            loginWindow.clearPassword();
        }
    }

    /**
     * Validates the master password — same logic as
     * PasswordManager.authenticate().
     */
    private boolean validateUser(String enteredPassword) {
        return passwordsService.login(enteredPassword);
    }

    /**
     * Shows the main menu (sketch screen 1). Each menu action opens its own
     * sub-screen with addWindowAndWait, so closing a sub-screen returns here.
     * The menu itself blocks until the user chooses Exit (or Q/ESC).
     */
    private void showMainMenu() {
        mainMenu = new MainMenu(
                theme,
                this::showPasswordListScreen,
                this::showAddPasswordScreen,
                this::showSettingsScreen);
        gui.addWindowAndWait(mainMenu.getWindow());

        // Returning from the menu (Exit or Q/ESC) means the user is done, so
        // close the login window too and let the app terminate.
        if (loginWindow != null) {
            loginWindow.close();
        }
    }

    /** Shows the password list screen. */
    private void showPasswordListScreen() {
        Map<String, PasswordEntry> entries = passwordsService.viewPasswords("");
        List<PasswordEntry> passwordEntries = new ArrayList<PasswordEntry>(entries.values());

        PasswordList passwordList = new PasswordList(theme, passwordEntries);
        passwordList.setOnDelete(entry -> {
            passwordsService.deletePasswordEntry(entry.getAccountName());
            return !passwordsService.accountExists(entry.getAccountName());
        });
        passwordList.setOnEdit(this::editEntry);
        gui.addWindowAndWait(passwordList.getWindow());
    }

    /**
     * Shows the edit form pre-filled with the entry's values. On Save the input
     * is validated, encrypted and persisted, and the given entry is updated in
     * place. Returns true if the entry was changed.
     */
    private boolean editEntry(PasswordEntry entry) {
        AddPassword editScreen = new AddPassword(theme, null, null);
        editScreen.prefill(entry);
        boolean[] changed = {false};

        editScreen.setOnSave(() -> {
            String username = editScreen.getUsername();
            String password = editScreen.getPassword();
            String url = editScreen.getUrl();
            String notes = editScreen.getNotes();

            if (username.isEmpty() || password.isEmpty()) {
                editScreen.showError("Username and password are required.");
                return;
            }
            if (!editScreen.passwordsMatch()) {
                editScreen.showError("Passwords do not match.");
                return;
            }

            // Nothing changed -> keep the entry (and its updatedAt) untouched.
            if (username.equals(nullToEmpty(entry.getUsername()))
                    && password.equals(nullToEmpty(entry.getPassword()))
                    && url.equals(nullToEmpty(entry.getUrl()))
                    && notes.equals(nullToEmpty(entry.getNotes()))) {
                editScreen.close();
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            PasswordEntry updated = new PasswordEntry.Builder(entry.getAccountName())
                    .username(username)
                    .password(password)
                    .url(url)
                    .notes(notes)
                    .createdAt(entry.getCreatedAt())
                    .updatedAt(now)
                    .accountActive(entry.isAccountActive())
                    .build();

            passwordsService.updatePasswordEntry(entry.getAccountName(), updated);

            // Persisted -> reflect the change in the in-memory entry.
            entry.setUsername(username);
            entry.setPassword(password);
            entry.setUrl(url);
            entry.setNotes(notes);
            entry.setUpdatedAt(now);
            changed[0] = true;
            editScreen.close();
        });
        editScreen.setOnCancel(editScreen::close);

        gui.addWindowAndWait(editScreen.getWindow());
        return changed[0];
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Shows the "add new password" screen (sketch screen 4). On Save the input
     * is validated, encrypted and persisted; on success the screen closes and
     * control returns to the menu. Cancel / ESC just closes the screen.
     */
    private void showAddPasswordScreen() {
        AddPassword addScreen = new AddPassword(theme, null, null);

        Runnable onSave = () -> {
            String account = addScreen.getAccount();
            String username = addScreen.getUsername();
            String password = addScreen.getPassword();
            String url = addScreen.getUrl();
            String notes = addScreen.getNotes();

            // Required fields (same rule as PasswordManager.addPassword).
            if (account.isEmpty() || username.isEmpty() || password.isEmpty()) {
                addScreen.showError("Website, username and password are required.");
                return;
            }

            // Password and its repeat must match.
            if (!addScreen.passwordsMatch()) {
                addScreen.showError("Passwords do not match.");
                return;
            }

            // Reject duplicate account names.
            if (passwordsService.accountExists(account)) {
                addScreen.showError("An account with that name already exists.");
                return;
            }

            PasswordEntry entry = new PasswordEntry.Builder(account)
                    .username(username)
                    .password(password)
                    .notes(notes)
                    .url(url)
                    .build();

            passwordsService.addPasswordEntry(entry);
            if (passwordsService.accountExists(account)) {
                addScreen.close();
            } else {
                addScreen.showError("Could not save the password. Try again.");
            }
        };

        addScreen.setOnSave(onSave);
        addScreen.setOnCancel(addScreen::close);

        gui.addWindowAndWait(addScreen.getWindow());
    }

    /**
     * Placeholder for the "settings" screen (sketch screen 5), not implemented
     * yet. Shows an informational dialog and returns to the menu.
     */
    private void showSettingsScreen() {
        MessageDialog.showMessageDialog(
                gui,
                "Settings",
                "The settings screen is not implemented yet.",
                MessageDialogButton.OK);
    }
}

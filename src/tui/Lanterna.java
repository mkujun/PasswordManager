package tui;

import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import crypto.CryptoService;
import interfaces.ICryptoService;
import interfaces.IPasswordRepository;
import model.PasswordEntry;
import persistence.PersistenceService;
import repository.PasswordRepository;

import javax.crypto.SecretKey;
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

    private final ICryptoService crypto = new CryptoService();
    private final IPasswordRepository repository =
            new PasswordRepository(new PersistenceService(FILE_NAME));

    private final SimpleTheme theme = Theme.getTheme();

    private SecretKey secretKey;

    private Login loginWindow;
    private MainMenu mainMenu;

    // todo: remove this, user can guess any number of times
    // it is not that the app can lock itself from further usage
    private int loginAttempts = 3;

    public MultiWindowTextGUI gui;

    public static void main(String[] args) throws Exception {
        new Lanterna().run();
    }

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
            showLoginScreen();
        } finally {
            screen.stopScreen();
        }
    }

    /** Shows the login screen and waits until the user logs in or quits. */
    private void showLoginScreen() {
        loginWindow = new Login(theme, this::login);

        if (repository.getEncryptedMasterPassword() == null
                || repository.getEncryptedMasterPassword().isEmpty()) {
            loginWindow.showError("No master password set. Run the CLI app first.");
        }

        gui.addWindowAndWait(loginWindow.getWindow());
    }

    private void login() {
        String enteredPassword = loginWindow.getEnteredPassword();

        if (validateUser(enteredPassword)) {
            showMainMenu();
        } else {
            loginAttempts--;
            if (loginAttempts == 0) {
                loginWindow.showError("Too many failed attempts. Exiting.");
                loginWindow.close();
            } else {
                loginWindow.showError("Incorrect password. Attempts left: " + loginAttempts);
                loginWindow.clearPassword();
            }
        }
    }

    /**
     * Validates the master password — same logic as
     * PasswordManager.authenticate().
     */
    private boolean validateUser(String enteredPassword) {
        SecretKey key = crypto.deriveKey(enteredPassword, repository.getSalt());
        String encryptedEntered = crypto.encrypt(enteredPassword, key);

        if (encryptedEntered.equals(repository.getEncryptedMasterPassword())) {
            this.secretKey = key; // keep the key for later screens
            return true;
        }
        return false;
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

    // todo: extract this onDelete into separate function
    /** Shows the password list screen. */
    private void showPasswordListScreen() {
        Map<String, String> entries = repository.getEntries();
        List<PasswordEntry> passwordEntries = new ArrayList<>();
        entries.forEach((key, value) -> {
            passwordEntries.add(crypto.decryptEntry(value, secretKey));
        });

        PasswordList passwordList = new PasswordList(theme, passwordEntries);
        passwordList.setOnDelete(entry -> {
            boolean removed = repository.remove(entry.getAccountName());
            if (removed) {
                repository.save();
            }
            return removed;
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

            Map<String, String> encryptedEntries = repository.getEntries();
            encryptedEntries.replace(entry.getAccountName(),
                    crypto.encryptEntry(updated, secretKey));
            repository.importEntries(encryptedEntries);
            repository.save();

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
            if (!repository.find(account).isEmpty()) {
                addScreen.showError("An account with that name already exists.");
                return;
            }

            String encrypted = crypto.encryptEntry(
                    new PasswordEntry.Builder(account)
                            .username(username)
                            .password(password)
                            .notes(notes)
                            .url(url)
                            .build(),
                    secretKey);

            if (repository.addEncryptedEntry(encrypted, account)) {
                repository.save();
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

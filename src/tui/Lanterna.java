package tui;

import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import crypto.CryptoService;
import interfaces.ICryptoService;
import interfaces.IPasswordRepository;
import persistence.PersistenceService;
import repository.PasswordRepository;

import javax.crypto.SecretKey;

/**
 * Lanterna UI prototype.
 *
 * Flow:
 *   1. Login screen  -> user enters the master password.
 *   2. Validation uses the same logic as PasswordManager.authenticate():
 *        deriveKey(password, salt) -> encrypt(password, key) -> compare
 *        against the stored encryptedMasterPassword.
 *   3. On success -> the Password List screen.
 */
public class Lanterna {
    private static final String FILE_NAME = "passwords.dat";

    private final ICryptoService crypto = new CryptoService();
    private final IPasswordRepository repository =
            new PasswordRepository(new PersistenceService(FILE_NAME));

    private final SimpleTheme theme = Theme.getTheme();

    private SecretKey secretKey;

    private Login loginWindow;

    private int loginAttempts = 3;

    public MultiWindowTextGUI gui;

    public static void main(String[] args) throws Exception {
        new Lanterna().run();
    }

    private void run() throws Exception {
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
            showPasswordListScreen();
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

    /** Shows the password list screen. */
    private void showPasswordListScreen() {
        PasswordList passwordList = new PasswordList(theme);
        gui.addWindowAndWait(passwordList.getWindow());
    }
}

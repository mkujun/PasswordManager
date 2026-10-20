import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import crypto.CryptoService;
import interfaces.ICryptoService;
import interfaces.IPasswordRepository;
import interfaces.IPersistenceService;
import persistence.PersistenceService;
import repository.PasswordRepository;

import javax.crypto.SecretKey;
import java.util.Arrays;

/**
 * Lanterna UI prototype.
 *
 * Flow:
 *   1. Login screen  -> korisnik unosi master password.
 *   2. Validacija se radi istom logikom kao u PasswordManager.authenticate():
 *        deriveKey(password, salt) -> encrypt(password, key) -> usporedba
 *        s pohranjenim encryptedMasterPassword.
 *   3. Na uspjeh -> prazan "Password List" ekran (zasad samo title).
 */
public class LanternaTest {

    private static final String FILE_NAME = "passwords.dat";

    // Shared services (ista arhitektura kao u Main.java)
    private final ICryptoService crypto = new CryptoService();
    private final IPasswordRepository repository =
            new PasswordRepository(new PersistenceService(FILE_NAME));

    // Postavljeni ključ nakon uspješne prijave (za buduće ekrane).
    private SecretKey secretKey;

    public static void main(String[] args) throws Exception {
        new LanternaTest().run();
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
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);

            boolean loggedIn = showLoginScreen(gui);
            if (loggedIn) {
                showPasswordListScreen(gui);
            }
        } finally {
            screen.stopScreen();
        }
    }

    /**
     * Prikazuje login ekran. Vraća true ako je korisnik uspješno unio
     * ispravnu master lozinku, false ako je odustao (Q/ESC).
     */
    private boolean showLoginScreen(MultiWindowTextGUI gui) {
        final BasicWindow window = new BasicWindow();
        window.setHints(Arrays.asList(
                Window.Hint.FULL_SCREEN,
                Window.Hint.NO_DECORATIONS
        ));

        // =====================================================
        //  DARK THEME PALETA  (inspirirana Dracula / Tokyo Night)
        //  RGB radi jer koristimo terminal emulator (setForceTextTerminal).
        // =====================================================
        final TextColor BG        = new TextColor.RGB(0x1E, 0x1E, 0x2E); // base pozadina
        final TextColor CARD_BG   = new TextColor.RGB(0x28, 0x2A, 0x36); // kartica / paneli
        final TextColor ACCENT    = new TextColor.RGB(0xBD, 0x93, 0xF9); // ljubičasti akcent
        final TextColor ACCENT2   = new TextColor.RGB(0x8B, 0xE9, 0xFD); // cyan akcent
        final TextColor FG        = new TextColor.RGB(0xF8, 0xF8, 0xF2); // glavni tekst
        final TextColor MUTED     = new TextColor.RGB(0x62, 0x72, 0xA4); // prigušeni tekst
        final TextColor ERROR     = new TextColor.RGB(0xFF, 0x55, 0x55); // greška
        final TextColor SUCCESS   = new TextColor.RGB(0x50, 0xFA, 0x7B); // zelena (footer)

        // Globalna dark tema za prozor — boja svu pozadinu i default tekst.
        SimpleTheme theme = SimpleTheme.makeTheme(
                true,       // activeIsBold
                FG,         // base foreground
                BG,         // base background
                FG,         // editable foreground
                CARD_BG,    // editable background
                ACCENT,     // selected foreground
                CARD_BG,    // selected background
                BG          // gui background
        );
        window.setTheme(theme);

        // Korijenski panel ispunjava cijeli ekran i centrira karticu.
        Panel root = new Panel(new BorderLayout());

        // =====================================================
        //  CENTRIRANA KARTICA (okvir dvostrukom linijom)
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
        title.addStyle(com.googlecode.lanterna.SGR.BOLD);
        title.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        card.addComponent(title);

        Label subtitle = new Label("secure vault - master login");
        subtitle.setForegroundColor(ACCENT2);
        subtitle.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        card.addComponent(subtitle);

        card.addComponent(new EmptySpace(new com.googlecode.lanterna.TerminalSize(1, 1)));
        card.addComponent(new Separator(Direction.HORIZONTAL)
                .setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill)));
        card.addComponent(new EmptySpace(new com.googlecode.lanterna.TerminalSize(1, 1)));

        // --- Prompt + unos lozinke ---
        Label prompt = new Label("Enter your master password");
        prompt.setForegroundColor(FG);
        prompt.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        card.addComponent(prompt);

        card.addComponent(new EmptySpace(new com.googlecode.lanterna.TerminalSize(1, 1)));

        // Mask lozinke zvjezdicama (unutar vlastitog okvira).
        final TextBox passwordBox = new TextBox().setMask('*');
        passwordBox.setPreferredSize(new com.googlecode.lanterna.TerminalSize(32, 1));
        Panel passwordWrap = new Panel(new LinearLayout(Direction.HORIZONTAL));
        passwordWrap.addComponent(passwordBox.withBorder(Borders.singleLine()));
        passwordWrap.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        card.addComponent(passwordWrap);

        card.addComponent(new EmptySpace(new com.googlecode.lanterna.TerminalSize(1, 1)));

        // Status/poruka o grešci.
        final Label statusLabel = new Label(" ");
        statusLabel.setForegroundColor(ERROR);
        statusLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        card.addComponent(statusLabel);

        card.addComponent(new EmptySpace(new com.googlecode.lanterna.TerminalSize(1, 1)));

        // Login gumb.
        final boolean[] success = {false};
        final int[] attempts = {0};
        final int MAX_ATTEMPTS = 3;

        Button loginButton = new Button("  Unlock  ", () -> {
            String entered = passwordBox.getText();

            if (repository.getEncryptedMasterPassword() == null
                    || repository.getEncryptedMasterPassword().isEmpty()) {
                statusLabel.setText("No master password set. Run the CLI app first.");
                return;
            }

            if (validate(entered)) {
                success[0] = true;
                window.close();
            } else {
                attempts[0]++;
                int left = MAX_ATTEMPTS - attempts[0];
                if (left <= 0) {
                    statusLabel.setText("Too many failed attempts. Exiting.");
                    window.close();
                } else {
                    statusLabel.setText("Incorrect password. Attempts left: " + left);
                    passwordBox.setText("");
                }
            }
        });
        loginButton.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
        card.addComponent(loginButton);

        // Kartica s okvirom + centriranje unutar ekrana.
        Panel cardWrap = new Panel(new GridLayout(1));
        Border borderedCard = card.withBorder(Borders.doubleLine());
        borderedCard.setLayoutData(GridLayout.createLayoutData(
                GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, true, true));
        cardWrap.addComponent(borderedCard);
        cardWrap.setLayoutData(BorderLayout.Location.CENTER);

        // =====================================================
        //  Header (traka na vrhu)
        // =====================================================
        Panel header = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Label headerLabel = new Label("  Password Manager ");
        headerLabel.setForegroundColor(ACCENT);
        headerLabel.setBackgroundColor(CARD_BG);
        headerLabel.addStyle(com.googlecode.lanterna.SGR.BOLD);
        headerLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        header.addComponent(headerLabel);
        header.setLayoutData(BorderLayout.Location.TOP);

        // =====================================================
        //  Footer (traka na dnu)
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

        // ESC / Q za izlaz bez prijave.
        window.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onUnhandledInput(Window basePane, KeyStroke keyStroke,
                                         java.util.concurrent.atomic.AtomicBoolean hasBeenHandled) {
                if (keyStroke.getKeyType() == KeyType.Escape) {
                    window.close();
                    hasBeenHandled.set(true);
                }
            }
        });

        gui.addWindowAndWait(window);
        return success[0];
    }

    /**
     * Validacija master lozinke — ista logika kao PasswordManager.authenticate().
     */
    private boolean validate(String enteredPassword) {
        SecretKey key = crypto.deriveKey(enteredPassword, repository.getSalt());
        String encryptedEntered = crypto.encrypt(enteredPassword, key);

        if (encryptedEntered.equals(repository.getEncryptedMasterPassword())) {
            this.secretKey = key; // spremi ključ za buduće ekrane
            return true;
        }
        return false;
    }

    /**
     * Prazan ekran za popis lozinki — zasad samo title (proof of concept).
     */
    private void showPasswordListScreen(MultiWindowTextGUI gui) {
        final BasicWindow window = new BasicWindow();
        window.setHints(Arrays.asList(
                Window.Hint.FULL_SCREEN,
                Window.Hint.NO_DECORATIONS
        ));

        Panel root = new Panel(new BorderLayout());

        // ---- Header ----
        Panel header = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Label headerLabel = new Label("[ Password List ]");
        headerLabel.setForegroundColor(TextColor.ANSI.BLACK);
        headerLabel.setBackgroundColor(TextColor.ANSI.GREEN);
        headerLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        header.addComponent(headerLabel);
        header.setLayoutData(BorderLayout.Location.TOP);

        // ---- Content (prazan) ----
        Panel content = new Panel(new LinearLayout(Direction.VERTICAL));
        content.addComponent(new Label("(no passwords yet)"));
        content.setLayoutData(BorderLayout.Location.CENTER);

        // ---- Footer ----
        Panel footer = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Label footerLabel = new Label("Press Q or ESC to exit");
        footerLabel.setForegroundColor(TextColor.ANSI.BLACK);
        footerLabel.setBackgroundColor(TextColor.ANSI.WHITE);
        footerLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        footer.addComponent(footerLabel);
        footer.setLayoutData(BorderLayout.Location.BOTTOM);

        root.addComponent(header);
        root.addComponent(content);
        root.addComponent(footer);

        window.setComponent(root);

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
    }

}

package tui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Separator;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import model.PasswordEntry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static tui.Theme.ACCENT;
import static tui.Theme.ACCENT2;
import static tui.Theme.CARD_BG;
import static tui.Theme.ERROR;
import static tui.Theme.FG;
import static tui.Theme.MUTED;
import static tui.Theme.SUCCESS;

/**
 * Password list screen.
 *
 * Shows all decrypted {@link PasswordEntry} items in a selectable list on the
 * left and renders the details of the currently selected entry on the right.
 * Passwords are masked by default and can be revealed with the SPACE key.
 */
public class PasswordList extends AppScreen {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final List<PasswordEntry> entries;

    /**
     * Callback invoked when the user confirms deletion of an entry. It should
     * perform the actual removal (repository + persistence) and return true on
     * success so the screen can update its view.
     */
    public interface DeleteHandler {
        boolean delete(PasswordEntry entry);
    }

    private DeleteHandler onDelete;

    // List + filter widgets.
    private ActionListBox listBox;
    private TextBox searchBox;
    private Label listTitle;

    // Entries currently shown in the list (after filtering), index-aligned
    // with the list box items so selection changes map back to an entry.
    private final java.util.List<PasswordEntry> visibleEntries = new java.util.ArrayList<>();

    // Detail widgets (populated when the selection changes).
    private Label detailAccount;
    private Label detailUsername;
    private Label detailPassword;
    private Label detailUrl;
    private Label detailNotes;
    private Label detailStatus;
    private Label detailCreated;
    private Label detailUpdated;

    private boolean passwordRevealed = false;
    private PasswordEntry selectedEntry;

    public PasswordList(SimpleTheme theme, List<PasswordEntry> passwordEntries) {
        super(theme);
        // Defensive mutable copy so we can remove entries after deletion.
        this.entries = new java.util.ArrayList<>(
                passwordEntries == null ? java.util.Collections.emptyList() : passwordEntries);
        initGuiElements();
        addExitOnQorEsc(window);
        addToggleOnSpace();
        addDeleteOnD();
    }

    /** Assigns the delete callback (may be set after construction). */
    public void setOnDelete(DeleteHandler onDelete) {
        this.onDelete = onDelete;
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
        int count = entries == null ? 0 : entries.size();
        Label headerLabel = new Label("  Password List  (" + count + " entries) ");
        headerLabel.setForegroundColor(ACCENT);
        headerLabel.setBackgroundColor(CARD_BG);
        headerLabel.addStyle(SGR.BOLD);
        headerLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        header.addComponent(headerLabel);
        header.setLayoutData(BorderLayout.Location.TOP);
        return header;
    }

    // =====================================================
    //  Body: list (left) + details (right)
    // =====================================================
    private Panel buildBody() {
        Panel body = new Panel(new LinearLayout(Direction.HORIZONTAL));
        body.setLayoutData(BorderLayout.Location.CENTER);

        // Build the details panel first so the detail labels exist before the
        // list calls showDetails() during its initial population.
        Panel detailsPanel = buildDetails();
        Panel listPanel = buildList();

        body.addComponent(listPanel);
        body.addComponent(new EmptySpace(new TerminalSize(2, 1)));
        body.addComponent(detailsPanel);

        return body;
    }

    private Panel buildList() {
        // Subclass ActionListBox so we can react to *selection* changes
        // (arrow keys, Home/End, PgUp/PgDn, mouse) — not just ENTER. The
        // built-in action Runnable only fires on activation, which is why the
        // details used to stay stuck on the first entry while navigating.
        listBox = new ActionListBox(new TerminalSize(28, 18)) {
            @Override
            public synchronized com.googlecode.lanterna.gui2.Interactable.Result handleKeyStroke(
                    com.googlecode.lanterna.input.KeyStroke keyStroke) {
                com.googlecode.lanterna.gui2.Interactable.Result result = super.handleKeyStroke(keyStroke);
                syncDetailsToSelection();
                return result;
            }
        };

        // --- Filter / search field ---
        searchBox = new TextBox(new TerminalSize(28, 1));
        searchBox.setTextChangeListener((newText, changedByUser) -> populateList(newText));

        Panel searchWrap = new Panel(new LinearLayout(Direction.HORIZONTAL));
        searchWrap.addComponent(searchBox.withBorder(Borders.singleLine("Search account")));

        // --- List title (shows how many entries match) ---
        listTitle = new Label("");
        listTitle.setForegroundColor(MUTED);

        Panel listColumn = new Panel(new LinearLayout(Direction.VERTICAL));
        listColumn.addComponent(searchWrap);
        listColumn.addComponent(listBox.withBorder(Borders.singleLine("Accounts")));
        listColumn.addComponent(listTitle);

        // Initial population (no filter).
        populateList("");

        return listColumn;
    }

    /**
     * (Re)builds the account list, keeping only entries whose account name
     * contains the given filter text (case-insensitive). An empty or null
     * filter shows every entry.
     */
    private void populateList(String filter) {
        listBox.clearItems();
        visibleEntries.clear();

        if (entries == null || entries.isEmpty()) {
            listTitle.setText(" (no passwords yet)");
            selectedEntry = null;
            clearDetails();
            return;
        }

        String needle = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);

        // Remember which entry was selected so we can keep it selected after
        // the rebuild if it still matches the filter.
        PasswordEntry previouslySelected = selectedEntry;

        for (final PasswordEntry entry : entries) {
            String account = entry.getAccountName() == null ? "" : entry.getAccountName();
            if (!needle.isEmpty() && !account.toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }

            String label = account;
            if (!entry.isAccountActive()) {
                label = label + " (inactive)";
            }
            listBox.addItem(label, () -> showDetails(entry));
            visibleEntries.add(entry);
        }

        int matches = visibleEntries.size();
        if (matches == 0) {
            listTitle.setText(" no matches");
            selectedEntry = null;
            clearDetails();
            return;
        }

        listTitle.setText(" " + matches + " shown");

        // Restore the previous selection if it is still visible, else select
        // the first item. Then render the details for whatever is selected.
        int targetIndex = previouslySelected == null ? 0 : visibleEntries.indexOf(previouslySelected);
        if (targetIndex < 0) {
            targetIndex = 0;
        }
        listBox.setSelectedIndex(targetIndex);
        syncDetailsToSelection();
    }

    /**
     * Reads the list's currently selected index and renders the matching
     * entry's details. Called whenever the selection may have changed.
     */
    private void syncDetailsToSelection() {
        int index = listBox.getSelectedIndex();
        if (index < 0 || index >= visibleEntries.size()) {
            return;
        }
        PasswordEntry entry = visibleEntries.get(index);
        if (entry != selectedEntry) {
            showDetails(entry);
        }
    }

    private Panel buildDetails() {
        Panel details = new Panel(new LinearLayout(Direction.VERTICAL));

        detailAccount = addDetailRow(details, "Account", ACCENT, true);
        details.addComponent(new Separator(Direction.HORIZONTAL)
                .setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill)));
        details.addComponent(new EmptySpace(new TerminalSize(1, 1)));

        detailUsername = addDetailRow(details, "Username", FG, false);
        detailPassword = addDetailRow(details, "Password", ACCENT2, false);
        detailUrl = addDetailRow(details, "URL", FG, false);
        detailNotes = addDetailRow(details, "Notes", FG, false);

        details.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        detailStatus = addDetailRow(details, "Status", FG, false);
        detailCreated = addDetailRow(details, "Created", MUTED, false);
        detailUpdated = addDetailRow(details, "Updated", MUTED, false);

        Panel wrap = new Panel(new LinearLayout(Direction.VERTICAL));
        wrap.addComponent(details.withBorder(Borders.singleLine("Details")));

        clearDetails();
        return wrap;
    }

    /**
     * Adds a "Label: value" row to the given panel and returns the value label
     * so it can be updated later.
     */
    private Label addDetailRow(Panel parent, String caption, TextColor valueColor,
                               boolean bold) {
        Panel row = new Panel(new LinearLayout(Direction.HORIZONTAL));

        Label captionLabel = new Label(String.format("%-9s: ", caption));
        captionLabel.setForegroundColor(MUTED);

        Label valueLabel = new Label("");
        valueLabel.setForegroundColor(valueColor);
        if (bold) {
            valueLabel.addStyle(SGR.BOLD);
        }

        row.addComponent(captionLabel);
        row.addComponent(valueLabel);
        parent.addComponent(row);
        return valueLabel;
    }

    // =====================================================
    //  Footer
    // =====================================================
    private Panel buildFooter() {
        Panel footer = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Label footerLabel = new Label(" type to search | UP/DOWN select | SPACE show/hide | D delete | Q / ESC exit ");
        footerLabel.setForegroundColor(SUCCESS);
        footerLabel.setBackgroundColor(CARD_BG);
        footerLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        footer.addComponent(footerLabel);
        footer.setLayoutData(BorderLayout.Location.BOTTOM);
        return footer;
    }

    // =====================================================
    //  Selection handling
    // =====================================================
    private void showDetails(PasswordEntry entry) {
        this.selectedEntry = entry;
        this.passwordRevealed = false; // hide password again when switching entries
        renderSelectedEntry();
    }

    private void renderSelectedEntry() {
        if (selectedEntry == null) {
            clearDetails();
            return;
        }

        detailAccount.setText(safe(selectedEntry.getAccountName()));
        detailUsername.setText(safe(selectedEntry.getUsername()));
        detailPassword.setText(passwordDisplay(selectedEntry.getPassword()));
        detailUrl.setText(safe(selectedEntry.getUrl()));
        detailNotes.setText(safe(selectedEntry.getNotes()));

        if (selectedEntry.isAccountActive()) {
            detailStatus.setText("active");
            detailStatus.setForegroundColor(SUCCESS);
        } else {
            detailStatus.setText("inactive");
            detailStatus.setForegroundColor(ERROR);
        }

        detailCreated.setText(formatDate(selectedEntry.getCreatedAt()));
        detailUpdated.setText(formatDate(selectedEntry.getUpdatedAt()));
    }

    private void clearDetails() {
        detailAccount.setText("-");
        detailUsername.setText("-");
        detailPassword.setText("-");
        detailUrl.setText("-");
        detailNotes.setText("-");
        detailStatus.setText("-");
        detailStatus.setForegroundColor(FG);
        detailCreated.setText("-");
        detailUpdated.setText("-");
    }

    private String passwordDisplay(String password) {
        if (password == null || password.isEmpty()) {
            return "-";
        }
        if (passwordRevealed) {
            return password;
        }
        StringBuilder masked = new StringBuilder(password.length());
        for (int i = 0; i < password.length(); i++) {
            masked.append('*');
        }
        return masked.toString();
    }

    private static String safe(String value) {
        return (value == null || value.isEmpty()) ? "-" : value;
    }

    private static String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_FORMAT);
    }

    // =====================================================
    //  SPACE-to-reveal handler
    // =====================================================
    /**
     * Registers a listener that toggles password visibility when the user
     * presses SPACE. We use onInput (fired before the focused component sees
     * the key) because the focused ActionListBox otherwise consumes SPACE,
     * so onUnhandledInput would never be reached. When the search field is
     * focused, SPACE is left alone so the user can type spaces.
     */
    private void addToggleOnSpace() {
        window.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window basePane, KeyStroke keyStroke,
                                AtomicBoolean deliverEvent) {
                if (!isSpaceKey(keyStroke)) {
                    return;
                }
                // Let the search box receive the space as normal text.
                if (window.getFocusedInteractable() == searchBox) {
                    return;
                }
                passwordRevealed = !passwordRevealed;
                if (selectedEntry != null) {
                    detailPassword.setText(passwordDisplay(selectedEntry.getPassword()));
                }
                // Consume the key so the list does not also act on it.
                deliverEvent.set(false);
            }
        });
    }

    private static boolean isSpaceKey(KeyStroke keyStroke) {
        return keyStroke.getKeyType() == KeyType.Character
                && keyStroke.getCharacter() != null
                && keyStroke.getCharacter() == ' ';
    }

    // =====================================================
    //  Delete handler (D key)
    // =====================================================
    /**
     * Registers a listener that deletes the selected entry when the user
     * presses D (upper or lower case). The search box keeps the key when it is
     * focused so 'd' remains typeable while searching.
     */
    private void addDeleteOnD() {
        window.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window basePane, KeyStroke keyStroke,
                                AtomicBoolean deliverEvent) {
                if (!isDeleteKey(keyStroke)) {
                    return;
                }
                // While searching, let the field receive the 'd' character.
                if (window.getFocusedInteractable() == searchBox) {
                    return;
                }
                deliverEvent.set(false);
                deleteSelected();
            }
        });
    }

    private static boolean isDeleteKey(KeyStroke keyStroke) {
        return keyStroke.getKeyType() == KeyType.Character
                && keyStroke.getCharacter() != null
                && Character.toUpperCase(keyStroke.getCharacter()) == 'D';
    }

    /**
     * Asks for confirmation and, if accepted, deletes the currently selected
     * entry via the delete callback, then refreshes the list.
     */
    private void deleteSelected() {
        if (selectedEntry == null || onDelete == null) {
            return;
        }

        PasswordEntry target = selectedEntry;
        MessageDialogButton choice = MessageDialog.showMessageDialog(
                window.getTextGUI(),
                "Delete password",
                "Delete the entry for \"" + safe(target.getAccountName()) + "\"?",
                MessageDialogButton.Yes, MessageDialogButton.No);

        if (choice != MessageDialogButton.Yes) {
            return;
        }

        if (onDelete.delete(target)) {
            entries.remove(target);
            selectedEntry = null;
            // Rebuild the list honoring the current filter.
            populateList(searchBox == null ? "" : searchBox.getText());
            listTitle.setText(" deleted \"" + safe(target.getAccountName()) + "\"");
        }
    }
}

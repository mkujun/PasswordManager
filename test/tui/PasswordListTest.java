package tui;

import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.TextBox;
import model.PasswordEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static tui.TuiTestSupport.*;

public class PasswordListTest {

    private TuiTestSupport tui;
    private PasswordList screen;
    private BasicWindow window;
    private ActionListBox listBox;
    private TextBox searchBox;

    private PasswordEntry gmail;
    private PasswordEntry github;
    private PasswordEntry bank;

    @Before
    public void setUp() throws Exception {
        tui = new TuiTestSupport();
        gmail = new PasswordEntry.Builder("gmail")
                .username("gUser").password("gPass").url("https://mail.google.com").notes("mail")
                .createdAt(LocalDateTime.of(2026, 1, 2, 3, 4))
                .updatedAt(LocalDateTime.of(2026, 5, 6, 7, 8))
                .build();
        github = new PasswordEntry.Builder("github").username("hUser").password("hPass").build();
        bank = new PasswordEntry.Builder("Bank").username("bUser").password("bPass")
                .accountActive(false).build();
        open(Arrays.asList(gmail, github, bank));
    }

    @After
    public void tearDown() throws Exception {
        tui.stop();
    }

    private void open(List<PasswordEntry> entries) throws Exception {
        screen = new PasswordList(Theme.getTheme(), entries);
        window = screen.getWindow();
        listBox = find(window, ActionListBox.class).get(0);
        searchBox = find(window, TextBox.class).get(0);
        tui.show(window);
        focus(window, listBox);
    }

    /** Returns the value label next to the given detail caption (e.g. "Username"). */
    private Label detail(String caption) {
        List<Label> labels = find(window, Label.class);
        String captionText = String.format("%-9s: ", caption);
        for (int i = 0; i < labels.size() - 1; i++) {
            if (labels.get(i).getText().equals(captionText)) {
                return labels.get(i + 1);
            }
        }
        throw new AssertionError("No detail row " + caption);
    }

    private String item(int index) {
        return listBox.getItemAt(index).toString();
    }

    private void search(String text) throws Exception {
        searchBox.setText(text);
        tui.pump();
    }

    // ---------------------------------------------------------------
    //  Initial state
    // ---------------------------------------------------------------

    @Test
    public void constructor_shouldListAllEntriesAndShowFirstDetails() {
        assertEquals(3, listBox.getItemCount());
        assertEquals("gmail", item(0));
        assertEquals("github", item(1));
        assertEquals("Bank (inactive)", item(2));
        assertNotNull(labelContaining(window, "(3 entries)"));
        assertNotNull(labelContaining(window, "3 shown"));

        assertEquals("gmail", detail("Account").getText());
        assertEquals("gUser", detail("Username").getText());
        assertEquals("*****", detail("Password").getText());
        assertEquals("https://mail.google.com", detail("URL").getText());
        assertEquals("mail", detail("Notes").getText());
        assertEquals("active", detail("Status").getText());
        assertEquals(Theme.SUCCESS, detail("Status").getForegroundColor());
        assertEquals("2026-01-02 03:04", detail("Created").getText());
        assertEquals("2026-05-06 07:08", detail("Updated").getText());
    }

    @Test
    public void emptyList_shouldShowPlaceholder() throws Exception {
        open(Collections.emptyList());

        assertEquals(0, listBox.getItemCount());
        assertNotNull(labelContaining(window, "(0 entries)"));
        assertNotNull(labelContaining(window, "(no passwords yet)"));
        assertEquals("-", detail("Account").getText());
        assertEquals("-", detail("Password").getText());
    }

    @Test
    public void nullList_shouldBehaveLikeEmptyList() throws Exception {
        open(null);

        assertEquals(0, listBox.getItemCount());
        assertNotNull(labelContaining(window, "(no passwords yet)"));
    }

    @Test
    public void missingValues_shouldBeShownAsDash() throws Exception {
        PasswordEntry sparse = new PasswordEntry.Builder(null)
                .password("").createdAt(null).updatedAt(null).build();
        open(Collections.singletonList(sparse));

        assertEquals("", item(0));
        assertEquals("-", detail("Account").getText());
        assertEquals("-", detail("Username").getText());
        assertEquals("-", detail("Password").getText());
        assertEquals("-", detail("URL").getText());
        assertEquals("-", detail("Created").getText());
        assertEquals("-", detail("Updated").getText());
    }

    @Test
    public void nullPassword_shouldBeShownAsDash() throws Exception {
        open(Collections.singletonList(new PasswordEntry.Builder("x").build()));

        assertEquals("-", detail("Password").getText());
    }

    // ---------------------------------------------------------------
    //  Navigation
    // ---------------------------------------------------------------

    @Test
    public void arrowDown_shouldShowDetailsOfNextEntry() {
        press(window, DOWN);

        assertEquals(1, listBox.getSelectedIndex());
        assertEquals("github", detail("Account").getText());
        assertEquals("hUser", detail("Username").getText());
    }

    @Test
    public void inactiveEntry_shouldShowInactiveStatus() {
        press(window, DOWN, DOWN);

        assertEquals("Bank", detail("Account").getText());
        assertEquals("inactive", detail("Status").getText());
        assertEquals(Theme.ERROR, detail("Status").getForegroundColor());
    }

    @Test
    public void enterOnItem_shouldShowItsDetails() {
        press(window, DOWN, ENTER);

        assertEquals("github", detail("Account").getText());
    }

    @Test
    public void arrowUpAtTop_shouldKeepFirstEntry() {
        press(window, UP);

        assertEquals("gmail", detail("Account").getText());
    }

    @Test
    public void arrowKeys_onEmptyList_shouldNotFail() throws Exception {
        open(Collections.emptyList());

        press(window, DOWN, UP);

        assertEquals("-", detail("Account").getText());
    }

    // ---------------------------------------------------------------
    //  Show / hide password (SPACE)
    // ---------------------------------------------------------------

    @Test
    public void space_shouldRevealAndHidePassword() {
        press(window, character(' '));
        assertEquals("gPass", detail("Password").getText());

        press(window, character(' '));
        assertEquals("*****", detail("Password").getText());
    }

    @Test
    public void switchingEntry_shouldHidePasswordAgain() {
        press(window, character(' '));
        press(window, DOWN);

        assertEquals("*****", detail("Password").getText());
    }

    @Test
    public void space_inSearchBox_shouldBeTyped() {
        focus(window, searchBox);
        press(window, character('g'), character(' '));

        assertEquals("g ", searchBox.getText());
        assertEquals("*****", detail("Password").getText());
    }

    @Test
    public void space_onEmptyList_shouldNotFail() throws Exception {
        open(Collections.emptyList());

        press(window, character(' '));

        assertEquals("-", detail("Password").getText());
    }

    // ---------------------------------------------------------------
    //  Search / filter
    // ---------------------------------------------------------------

    @Test
    public void search_shouldFilterCaseInsensitive() throws Exception {
        search("GI");

        assertEquals(1, listBox.getItemCount());
        assertEquals("github", item(0));
        assertNotNull(labelContaining(window, "1 shown"));
        assertEquals("github", detail("Account").getText());
    }

    @Test
    public void search_noMatches_shouldClearDetails() throws Exception {
        search("zzz");

        assertEquals(0, listBox.getItemCount());
        assertNotNull(labelContaining(window, "no matches"));
        assertEquals("-", detail("Account").getText());
    }

    @Test
    public void search_shouldKeepSelectedEntryWhenStillVisible() throws Exception {
        press(window, DOWN); // github selected

        search("g");

        assertEquals("github", item(listBox.getSelectedIndex()));
        assertEquals("github", detail("Account").getText());
    }

    @Test
    public void search_selectedEntryFilteredOut_shouldSelectFirstMatch() throws Exception {
        press(window, DOWN, DOWN); // Bank selected

        search("git");

        assertEquals(0, listBox.getSelectedIndex());
        assertEquals("github", detail("Account").getText());
    }

    @Test
    public void search_clearFilter_shouldShowAllAgain() throws Exception {
        search("git");
        search("");

        assertEquals(3, listBox.getItemCount());
    }

    @Test
    public void typing_inSearchBox_shouldFilterList() throws Exception {
        focus(window, searchBox);
        press(window, character('b'), character('a'));
        tui.pump();

        assertEquals(1, listBox.getItemCount());
        assertEquals("Bank (inactive)", item(0));
    }

    // ---------------------------------------------------------------
    //  Edit (E)
    // ---------------------------------------------------------------

    @Test
    public void edit_changed_shouldRefreshListAndDetails() {
        List<PasswordEntry> edited = new ArrayList<>();
        screen.setOnEdit(entry -> {
            edited.add(entry);
            entry.setUsername("changedUser");
            return true;
        });

        press(window, character('e'));

        assertEquals(Collections.singletonList(gmail), edited);
        assertEquals("changedUser", detail("Username").getText());
        assertNotNull(labelContaining(window, "updated \"gmail\""));
    }

    @Test
    public void edit_upperCaseE_notChanged_shouldNotShowUpdated() {
        List<PasswordEntry> edited = new ArrayList<>();
        screen.setOnEdit(entry -> {
            edited.add(entry);
            return false;
        });

        press(window, character('E'));

        assertEquals(1, edited.size());
        assertNull(labelContaining(window, "updated"));
    }

    @Test
    public void edit_withoutHandler_shouldDoNothing() {
        press(window, character('e'));

        assertTrue(isOpen(window));
        assertEquals("gmail", detail("Account").getText());
    }

    @Test
    public void edit_onEmptyList_shouldNotCallHandler() throws Exception {
        open(Collections.emptyList());
        List<PasswordEntry> edited = new ArrayList<>();
        screen.setOnEdit(entry -> edited.add(entry));

        press(window, character('e'));

        assertTrue(edited.isEmpty());
    }

    @Test
    public void edit_inSearchBox_shouldTypeLetter() {
        List<PasswordEntry> edited = new ArrayList<>();
        screen.setOnEdit(entry -> edited.add(entry));
        focus(window, searchBox);

        press(window, character('e'));

        assertEquals("e", searchBox.getText());
        assertTrue(edited.isEmpty());
    }

    // ---------------------------------------------------------------
    //  Delete (D) with confirmation dialog
    // ---------------------------------------------------------------

    @Test
    public void delete_confirmed_shouldRemoveEntry() {
        List<PasswordEntry> deleted = new ArrayList<>();
        screen.setOnDelete(entry -> deleted.add(entry));
        tui.queue(ENTER); // "Yes" is the focused button in the dialog

        press(window, character('d'));

        assertEquals(Collections.singletonList(gmail), deleted);
        assertEquals(2, listBox.getItemCount());
        assertEquals("github", item(0));
        assertNotNull(labelContaining(window, "deleted \"gmail\""));
    }

    @Test
    public void delete_rejected_shouldKeepEntry() {
        List<PasswordEntry> deleted = new ArrayList<>();
        screen.setOnDelete(entry -> deleted.add(entry));
        tui.queue(TAB, ENTER); // move to "No" and press it

        press(window, character('D'));

        assertTrue(deleted.isEmpty());
        assertEquals(3, listBox.getItemCount());
    }

    @Test
    public void delete_handlerFails_shouldKeepEntry() {
        screen.setOnDelete(entry -> false);
        tui.queue(ENTER);

        press(window, character('d'));

        assertEquals(3, listBox.getItemCount());
        assertNull(labelContaining(window, "deleted"));
    }

    @Test
    public void delete_withSearchFilter_shouldKeepFilter() throws Exception {
        screen.setOnDelete(entry -> true);
        search("g");
        tui.queue(ENTER);

        focus(window, listBox);
        press(window, character('d'));

        assertEquals(1, listBox.getItemCount());
        assertEquals("github", item(0));
    }

    @Test
    public void delete_withoutHandler_shouldDoNothing() {
        press(window, character('d'));

        assertEquals(3, listBox.getItemCount());
    }

    @Test
    public void delete_onEmptyList_shouldNotCallHandler() throws Exception {
        open(Collections.emptyList());
        List<PasswordEntry> deleted = new ArrayList<>();
        screen.setOnDelete(entry -> deleted.add(entry));

        press(window, character('d'));

        assertTrue(deleted.isEmpty());
    }

    @Test
    public void delete_inSearchBox_shouldTypeLetter() {
        List<PasswordEntry> deleted = new ArrayList<>();
        screen.setOnDelete(entry -> deleted.add(entry));
        focus(window, searchBox);

        press(window, character('d'));

        assertEquals("d", searchBox.getText());
        assertTrue(deleted.isEmpty());
    }

    // ---------------------------------------------------------------
    //  Exit
    // ---------------------------------------------------------------

    @Test
    public void escape_shouldCloseWindow() {
        press(window, ESC);

        assertFalse(isOpen(window));
    }

    @Test
    public void nonCharacterKey_shouldBeIgnoredByShortcuts() {
        press(window, key(com.googlecode.lanterna.input.KeyType.F1));

        assertTrue(isOpen(window));
        assertEquals("gmail", detail("Account").getText());
    }
}

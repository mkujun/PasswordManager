package tui;

import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.graphics.SimpleTheme;

import static tui.Theme.ACCENT;
import static tui.Theme.CARD_BG;
import static tui.Theme.FG;
import static tui.Theme.SUCCESS;

/**
 * Password list screen.
 *
 * Currently a proof-of-concept placeholder: a themed header, an empty
 * content area and a footer. The entry list will be rendered here later.
 */
public class PasswordList extends AppScreen {

    public PasswordList(SimpleTheme theme) {
        super(theme);
        initGuiElements();
        addExitOnQorEsc(window);
    }

    private void initGuiElements() {
        Panel root = new Panel(new BorderLayout());

        // ---- Header ----
        Panel header = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Label headerLabel = new Label("  Password List ");
        headerLabel.setForegroundColor(ACCENT);
        headerLabel.setBackgroundColor(CARD_BG);
        headerLabel.addStyle(com.googlecode.lanterna.SGR.BOLD);
        headerLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        header.addComponent(headerLabel);
        header.setLayoutData(BorderLayout.Location.TOP);

        // ---- Content (empty for now) ----
        Panel content = new Panel(new LinearLayout(Direction.VERTICAL));
        Label placeholder = new Label("(no passwords yet)");
        placeholder.setForegroundColor(FG);
        content.addComponent(placeholder);
        content.setLayoutData(BorderLayout.Location.CENTER);

        // ---- Footer ----
        Panel footer = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Label footerLabel = new Label(" Q / ESC exit ");
        footerLabel.setForegroundColor(SUCCESS);
        footerLabel.setBackgroundColor(CARD_BG);
        footerLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        footer.addComponent(footerLabel);
        footer.setLayoutData(BorderLayout.Location.BOTTOM);

        root.addComponent(header);
        root.addComponent(content);
        root.addComponent(footer);

        window.setComponent(root);
    }
}

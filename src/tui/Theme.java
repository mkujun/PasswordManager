package tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;

public class Theme {
    // =====================================================
    //  DARK THEME PALETA  (inspired by Dracula / Tokyo Night)
    //  RGB for terminal emulator (setForceTextTerminal).
    // =====================================================
    static final TextColor BG        = new TextColor.RGB(0x1E, 0x1E, 0x2E); // base pozadina
    static final TextColor CARD_BG   = new TextColor.RGB(0x28, 0x2A, 0x36); // kartica / paneli
    static final TextColor ACCENT    = new TextColor.RGB(0xBD, 0x93, 0xF9); // ljubičasti akcent
    static final TextColor ACCENT2   = new TextColor.RGB(0x8B, 0xE9, 0xFD); // cyan akcent
    static final TextColor FG        = new TextColor.RGB(0xF8, 0xF8, 0xF2); // glavni tekst
    static final TextColor MUTED     = new TextColor.RGB(0x62, 0x72, 0xA4); // prigušeni tekst
    static final TextColor ERROR     = new TextColor.RGB(0xFF, 0x55, 0x55); // greška
    static final TextColor SUCCESS   = new TextColor.RGB(0x50, 0xFA, 0x7B); // zelena (footer)

    static SimpleTheme theme = SimpleTheme.makeTheme(
            true,       // activeIsBold
            FG,         // base foreground
            BG,         // base background
            FG,         // editable foreground
            CARD_BG,    // editable background
            ACCENT,     // selected foreground
            CARD_BG,    // selected background
            BG          // gui background
    );

    public static SimpleTheme getTheme() {
        return theme;
    }
}

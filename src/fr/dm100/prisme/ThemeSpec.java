package fr.dm100.prisme;

import android.graphics.Color;

/** Complete palettes and layout identities, rather than one grid in twelve colors. */
public final class ThemeSpec {
    public final int id;
    public final String name, subtitle, mode;
    public final int bg, bg2, surface, accent, text, muted;
    public final int recommendedColumns, recommendedIconSize, recommendedTextSize;

    private ThemeSpec(int id, String name, String subtitle, String mode,
                      String bg, String bg2, String surface, String accent, String text, String muted,
                      int columns, int iconSize, int textSize) {
        this.id = id; this.name = name; this.subtitle = subtitle; this.mode = mode;
        this.bg = Color.parseColor(bg); this.bg2 = Color.parseColor(bg2);
        this.surface = Color.parseColor(surface); this.accent = Color.parseColor(accent);
        this.text = Color.parseColor(text); this.muted = Color.parseColor(muted);
        this.recommendedColumns = columns; this.recommendedIconSize = iconSize; this.recommendedTextSize = textSize;
    }

    public static final ThemeSpec[] ALL = {
        new ThemeSpec(0, "Prisme Touch", "Téléphone · widgets et grille tactile", "grid",
                "#101A38", "#503375", "#344164", "#9CC9FF", "#FFFFFF", "#C1CEE8", 5, 72, 16),
        new ThemeSpec(1, "Tablette", "Agenda mensuel · applications en panneau latéral", "tablet",
                "#091D2C", "#17505D", "#193B4B", "#5CE4CE", "#F4FFFE", "#A5C8CF", 4, 80, 18),
        new ThemeSpec(2, "Bureau Windows", "Bureau · icônes libres et fenêtre explorateur", "desktop",
                "#071B37", "#0D5EAA", "#153959", "#45CCFF", "#F3FBFF", "#A7C5DE", 5, 64, 15),
        new ThemeSpec(3, "Dock macOS", "Dock · galerie de grandes applications", "dock",
                "#241C3F", "#A04D74", "#513C61", "#FFCBA4", "#FFF8F4", "#DEC4DB", 5, 72, 16),
        new ThemeSpec(4, "Kali Workbench", "Inspecteur · sélectionner, inspecter, ouvrir", "workbench",
                "#07101D", "#122941", "#132333", "#3DBDFF", "#E8F4FF", "#95B0C8", 4, 64, 15),
        new ThemeSpec(5, "Ghost Terminal", "Console · répertoire des applications installées", "terminal",
                "#030B08", "#08251A", "#092016", "#62FFA5", "#B6FFD2", "#65AF83", 3, 52, 16),
        new ThemeSpec(6, "Arcade 1986", "Arcade · une application et bouton START", "retro",
                "#190A32", "#36174A", "#281344", "#FFE867", "#FFF6D6", "#CBADDE", 4, 72, 18),
        new ThemeSpec(7, "Meca Cockpit", "Cockpit · trois instruments de lancement", "mechanics",
                "#15191E", "#303842", "#252D35", "#FFBD58", "#FFF2D9", "#ADB5BB", 4, 68, 16),
        new ThemeSpec(8, "Orbit", "Constellation · six applications en orbite", "orbit",
                "#050B24", "#151647", "#181C43", "#B9A2FF", "#F2EDFF", "#B6ABD4", 5, 64, 15),
        new ThemeSpec(9, "Papier", "Répertoire éditorial · index typographique", "paper",
                "#F2EEE5", "#DED8CB", "#FFFFFF", "#855D3A", "#24231F", "#746E62", 4, 60, 18),
        new ThemeSpec(10, "Aurora", "Mosaïque asymétrique · grandes et petites cartes", "aurora",
                "#071E28", "#302047", "#183846", "#83FFE2", "#F0FFFB", "#A3C6CA", 4, 76, 17),
        new ThemeSpec(11, "LCD Studio", "Clavier LCD · touches sans icônes", "lcd",
                "#CED8BD", "#AEBE99", "#DCE4CC", "#34462C", "#202D1D", "#607050", 4, 60, 18),
        new ThemeSpec(12, "Apple Ultra", "Action · grandes commandes verticales", "ultra",
                "#050608", "#1B2025", "#171C21", "#FF7B21", "#F5F5F0", "#A6AFB5", 4, 62, 15),
        new ThemeSpec(13, "Samsung Galaxy", "Nuage de bulles · nid d’abeille", "galaxy",
                "#070910", "#262239", "#202236", "#B4ABFF", "#FAF8FF", "#B6B2CB", 5, 64, 15),
        new ThemeSpec(14, "Garmin MARQ", "Instruments · quatre champs de données", "marq",
                "#0A1111", "#152727", "#1B2928", "#D9CC91", "#F0F2DE", "#A5B5AB", 4, 62, 15),
        new ThemeSpec(15, "TAG Racing", "Grille de départ · applications en diagonale", "racing",
                "#0B0C0F", "#282C32", "#1E2229", "#FF4D50", "#F5F5F4", "#A8AEB7", 4, 60, 15),
        new ThemeSpec(16, "Hublot Fusion", "Triade · trois cellules hexagonales", "fusion",
                "#090E13", "#222D36", "#1B252E", "#5BE0D0", "#E9F2F6", "#9BABB6", 4, 64, 15),
        new ThemeSpec(17, "Tambour Light", "Voyage · étiquettes et références", "tambour",
                "#241522", "#563246", "#493343", "#EAC68F", "#FFF4E4", "#D0B6BC", 4, 66, 16),
        new ThemeSpec(18, "Montblanc Summit", "Carnet personnel · note et marque-pages", "summit",
                "#F2F0E8", "#DADDD9", "#FFFFFF", "#314A65", "#182B3F", "#6D7982", 4, 60, 18),
        new ThemeSpec(19, "Ultimate Design", "Sélecteur vertical · glisser de haut en bas", "ultimate",
                "#061128", "#162E59", "#13264A", "#E7BF68", "#FBF5E3", "#A6B4CC", 4, 64, 16),
        new ThemeSpec(20, "Nova", "Quotidien · tâches, minuteur et carnet", "nova", "#07121C", "#143342", "#173340", "#85E9CF", "#F3FFF9", "#A4BFC6", 4, 72, 17),
        new ThemeSpec(21, "Silence", "Minimal · une application, sans icône", "quiet", "#000000", "#080A0D", "#18202A", "#BCD9FF", "#FFFFFF", "#9FAFBE", 4, 64, 19)

    };

    public static ThemeSpec get(int id) { return ALL[id >= 0 && id < ALL.length ? id : 0]; }
}

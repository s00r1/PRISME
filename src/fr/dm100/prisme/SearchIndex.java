package fr.dm100.prisme;

import java.text.Normalizer;
import java.util.Locale;

/** Shared accent-insensitive matching for applications, actions and settings. */
public final class SearchIndex {
    private SearchIndex() {}
    public static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }
    public static boolean matches(String haystack, String query) {
        String source = normalize(haystack), needle = normalize(query);
        if (needle.isEmpty()) return true;
        for (String word : needle.split("\\s+")) if (!source.contains(word)) return false;
        return true;
    }
}

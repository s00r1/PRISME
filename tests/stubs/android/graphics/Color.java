package android.graphics;
/** Only palette parsing is needed by the real ThemeSpec in these non-UI tests. */
public final class Color {
    public static int parseColor(String text) {
        if (!text.startsWith("#")) throw new IllegalArgumentException("Not a hex palette color");
        long value = Long.parseLong(text.substring(1), 16);
        if (text.length() == 7) value |= 0xff000000L;
        else if (text.length() != 9) throw new IllegalArgumentException("Invalid palette color");
        return (int)value;
    }
}

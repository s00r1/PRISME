package fr.dm100.prisme;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Exercises the actual preferences class with Android's JSON API and an isolated store. */
public final class DashboardPrefsTest {
    private static final String ID = "fr.prisme.demo/fr.prisme.demo.Main";
    private static int checks;

    public static void main(String[] args) throws Exception {
        migrationIsNarrowAndIdempotent();
        localBoundsAndDefaults();
        legacyProfilePreservesAllExistingFamilies();
        shortcutRoundTripAndReset();
        invalidImportsNeverWrite();
        setsAndNamesRemainDefensive();
        v6ModuleRoundtripAndSafety();
        System.out.println("PASS: " + checks + " assertions across 7 preference scenarios.");
    }

    private static void migrationIsNarrowAndIdempotent() throws Exception {
        MemoryContext context = new MemoryContext();
        MemoryPrefs old = context.store("prisme_dashboard");
        old.edit().putBoolean("volumeRail", true).putInt("theme", 17)
                .putString("notes", "Conserver mes notes").putStringSet("favorites", set(ID))
                .putString("customLabel:" + ID, "Ma caméra").apply();
        context.store("prisme_profiles").edit().putString("Confort XL", "profil personnel").apply();
        DashboardPrefs p = new DashboardPrefs(context);
        check(!p.getBoolean("volumeRail", true), "Legacy reserved strip is released once");
        check(p.getInt("theme", 0) == 17, "Selected style survives migration");
        check(p.getString("notes", "").equals("Conserver mes notes"), "Notes survive migration");
        check(p.isFavorite(ID) && p.getCustomLabel(ID).equals("Ma caméra"), "Personal apps survive migration");
        check(context.store("prisme_profiles").getString("Confort XL", "").equals("profil personnel"), "Named profiles remain untouched");
        p.putBoolean("volumeRail", true);
        DashboardPrefs reopened = new DashboardPrefs(context);
        check(reopened.getBoolean("volumeRail", false), "An explicit v2 rail choice survives recreation");
        reopened.reset();
        reopened.putBoolean("volumeRail", true);
        check(new DashboardPrefs(context).getBoolean("volumeRail", false), "A post-reset choice is not remigrated");
        check(!p.exportJSONObject().has("_formatVersion"), "Internal migration marker is not a backup option");
    }

    private static void localBoundsAndDefaults() throws Exception {
        MemoryContext context = new MemoryContext();
        DashboardPrefs p = new DashboardPrefs(context);
        check(!p.getBoolean("volumeRail", true), "Fresh install fills the display by default");
        check(p.exportJSONObject().getJSONObject("settings").getInt("styleScale") == 100, "Backup includes a sensible scale default");
        String[] keys = {"theme", "iconSize", "textSize", "columns", "gap", "workspace", "font", "accentIndex", "wallpaper", "iconShape", "order", "styleScale", "contentInset", "fitMode"};
        int[] minima = {0, 52, 12, 3, 4, 0, 0, 0, 0, 0, 0, 75, 0, 0};
        int[] maxima = {ThemeSpec.ALL.length - 1, 100, 24, 7, 22, 3, 3, 6, 5, 3, 3, 140, 24, 2};
        check(ThemeSpec.ALL.length == 22, "Twenty original styles and two new styles remain represented");
        for (int i = 0; i < keys.length; i++) {
            p.putInt(keys[i], Integer.MIN_VALUE);
            check(p.getInt(keys[i], -1) == minima[i], keys[i] + " clamps its lower bound");
            p.putInt(keys[i], Integer.MAX_VALUE);
            check(p.getInt(keys[i], -1) == maxima[i], keys[i] + " clamps its upper bound");
        }
        p.putInt("untrusted", 999);p.putBoolean("untrustedFlag", true);p.putString("untrustedText", "bad");
        JSONObject exported = p.exportJSONObject();
        check(!exported.getJSONObject("settings").has("untrusted"), "Unknown numeric preferences are ignored");
        check(p.getBoolean("untrustedFlag", false) == false, "Unknown Boolean preferences are ignored");
        context.store("prisme_dashboard").edit().putString("styleScale", "wrong type").apply();
        check(p.getInt("styleScale", 100) == 100, "A corrupted stored type falls back safely");
    }

    private static void legacyProfilePreservesAllExistingFamilies() throws Exception {
        MemoryContext context = new MemoryContext();DashboardPrefs p = new DashboardPrefs(context);
        JSONObject legacy = new JSONObject("{\"schemaVersion\":1,\"settings\":{\"theme\":12,\"iconSize\":100,\"textSize\":24,\"columns\":3,\"gap\":12,\"workspace\":2,\"font\":3,\"accentIndex\":6,\"labels\":false,\"volumeRail\":true,\"keepScreenOn\":true},\"notes\":\"Note héritée\"}");
        legacy.put("favorites", new JSONArray().put(ID));legacy.put("workspace2", new JSONArray().put(ID));
        legacy.put("customLabels", new JSONObject().put(ID, "Caméra"));
        legacy.put("launches", new JSONObject().put(ID, 1790000000));
        p.importJSONObject(legacy);
        check(p.getInt("theme", 0) == 12 && p.getInt("iconSize", 0) == 100, "Legacy geometry and style are respected");
        check(p.getInt("workspace", 0) == 2 && p.getSet("workspace2").contains(ID), "Legacy workspaces survive import");
        check(p.getCustomLabel(ID).equals("Caméra") && p.isFavorite(ID), "Legacy labels and favorites survive import");
        check(p.getInt("launch:" + ID, 0) == 1790000000 && p.getString("notes", "").equals("Note héritée"), "Legacy history and notes survive import");
        check(p.getBoolean("volumeRail", false), "Explicitly imported legacy rail remains exact");
        check(new DashboardPrefs(context).getBoolean("volumeRail", false), "Imported profile is not changed on recreation");
        check(p.getShortcut(0, "clock").equals("clock") && p.getShortcut(0, "styles").equals("styles"), "Schema1 has no forced replacement of style shortcuts");
        JSONObject saved = p.exportJSONObject();
        check(saved.getInt("schemaVersion") == 7 && saved.getJSONObject("shortcuts").length() == 0, "Legacy profile exports as schema4 without invented overrides");
        p.importJSONObject(new JSONObject("{\"schemaVersion\":1,\"settings\":{}}"));
        check(!p.getBoolean("volumeRail", true), "An omitted legacy rail uses the new full-screen default");
    }

    private static void shortcutRoundTripAndReset() throws Exception {
        DashboardPrefs p = new DashboardPrefs(new MemoryContext());
        String[] actions = {"clock", "app:com.android.settings/.Settings", "app:fr.prisme.demo/fr.prisme.demo.Main$Inner", "notes", "search"};
        for (int slot = 0; slot < DashboardPrefs.SLOT_COUNT; slot++) p.setShortcut(slot, actions[slot]);
        JSONObject backup = p.exportJSONObject();p.reset();p.importJSONObject(new JSONObject(backup.toString()));
        for (int slot = 0; slot < DashboardPrefs.SLOT_COUNT; slot++) check(p.getShortcut(slot, "apps").equals(actions[slot]), "Shortcut slot " + slot + " survives a JSON round trip");
        p.setShortcut(2, null);
        check(p.getShortcut(2, "styles").equals("styles"), "Removing a slot restores the current style's default");
        check(!p.exportJSONObject().getJSONObject("shortcuts").has("2"), "A removed shortcut is absent from backups");
        p.setShortcut(1, "app:broken");
        check(p.getShortcut(1, "apps").equals(actions[1]), "A malformed direct write does not destroy the prior shortcut");
        p.setShortcut(-1, "clock");p.setShortcut(5, "clock");
        check(p.exportJSONObject().getJSONObject("shortcuts").length() == 4, "Out-of-range slots do not create new settings");
        check(p.getShortcut(50, "invalid").equals("apps"), "Unsafe caller fallbacks cannot become launch actions");
        String[] valid = {"clock", "styles", "customize", "shortcuts", "apps", "search", "favorites", "tools", "notes", "timer", "focus", "stopwatch", "workspaces", "home", "controlCenter", "controlCenterSettings", "floatingVolumeSettings", "app:android/com.android.internal.app.ResolverActivity", "app:fr.demo/.Main"};
        for (String action : valid) check(DashboardPrefs.isValidShortcutAction(action), "Allowed action " + action);
    }

    private static void invalidImportsNeverWrite() throws Exception {
        MemoryContext context = new MemoryContext();DashboardPrefs p = new DashboardPrefs(context);
        p.putInt("theme", 6);p.putString("notes", "Ma note intacte");p.toggleSet("favorites", ID);p.setShortcut(4, "clock");
        JSONObject original = p.exportJSONObject();
        String[] fields = {"styleScale", "styleScale", "contentInset", "contentInset", "fitMode", "styleScale", "labels", "unknownOption"};
        Object[] values = {74, 141, -1, 25, 3, 100.5, "true", 1};
        for (int i = 0; i < fields.length; i++) {
            JSONObject bad = copy(original);bad.getJSONObject("settings").put(fields[i], values[i]);reject(p, context, bad);
        }
        String[] invalidActions = {"", "Clock", "clock ", "app:missingSlash", "app:/Class", "app:com.app/", "app:com.app/.", "app:com.app/.Class/Extra", "app:com.app/.Class;evil", "app:com..app/.Class", "app:com.app/.Cl ass", "app:com.app/.Cl\nass", "intent:#Intent;action=android.intent.action.VIEW;end", "https://example.com", "app:com.app/1NotJava", "app:com.app/.Class?x=y"};
        for (String action : invalidActions) {
            check(!DashboardPrefs.isValidShortcutAction(action), "Reject malformed action " + action.replace('\n', ' '));
            JSONObject bad = copy(original);bad.put("shortcuts", new JSONObject().put("0", action));reject(p, context, bad);
        }
        for (String slot : new String[]{"-1", "5", "01", "1.0", "one"}) {
            JSONObject bad = copy(original);bad.put("shortcuts", new JSONObject().put(slot, "clock"));reject(p, context, bad);
        }
        for (Object shortcuts : new Object[]{new JSONArray().put("clock"), true, JSONObject.NULL, new JSONObject().put("0", 7)}) {
            JSONObject bad = copy(original);bad.put("shortcuts", shortcuts);reject(p, context, bad);
        }
        JSONObject bad = copy(original);bad.put("schemaVersion", 8);reject(p, context, bad);
        bad = copy(original);bad.put("schemaVersion", true);reject(p, context, bad);
        bad = copy(original);bad.put("schemaVersion", 1);reject(p, context, bad); // version1 cannot smuggle version2 fields
        bad = copy(original);bad.put("unknownRoot", true);reject(p, context, bad);
        bad = copy(original);bad.put("hidden", false);reject(p, context, bad);
        bad = copy(original);bad.put("favorites", new JSONArray().put("bad-id"));reject(p, context, bad);
        bad = copy(original);bad.put("launches", new JSONObject().put(ID, -1));reject(p, context, bad);
        bad = copy(original);bad.put("notes", repeat(2001));reject(p, context, bad);
        bad = copy(original);bad.put("notes", repeat(65537));reject(p, context, bad);
    }


    private static void v6ModuleRoundtripAndSafety() throws Exception {
        MemoryContext c=new MemoryContext();DashboardPrefs p=new DashboardPrefs(c);
        c.store("prisme_battery_overlay").edit().putBoolean("adaptiveContrast",true).putInt("style",23).apply();
        c.store("prisme_music").edit().putInt("size",125).apply();
        c.store("prisme_sense").edit().putBoolean("enabled",true).putInt("nightTheme",21).apply();
        JSONObject all=p.exportJSONObject();
        check(all.getInt("schemaVersion")==7 && all.getJSONObject("v6").getJSONObject("battery").getBoolean("adaptiveContrast"),
              "New watch modules are portable with validation");
        c.store("prisme_battery_overlay").edit().putBoolean("adaptiveContrast",false).apply();
        p.importJSONObject(new JSONObject(all.toString()));
        check(c.store("prisme_battery_overlay").getBoolean("adaptiveContrast",false)
              && c.store("prisme_music").getInt("size",0)==125
              && c.store("prisme_sense").getBoolean("enabled",false),"V6 state round trips");
        JSONObject bad=new JSONObject(all.toString());
        bad.getJSONObject("v6").getJSONObject("battery").put("style",1000);
        reject(p,c,bad);
        bad=new JSONObject(all.toString());
        bad.getJSONObject("v6").getJSONObject("sense").put("secretUntrusted",true);
        reject(p,c,bad);
    }

    private static void setsAndNamesRemainDefensive() throws Exception {
        DashboardPrefs p = new DashboardPrefs(new MemoryContext());p.toggleSet("favorites", ID);
        Set<String> copy = p.getSet("favorites");copy.clear();copy.add("fr.fake/.Main");
        check(p.isFavorite(ID) && p.getSet("favorites").size() == 1, "Returned sets cannot mutate live preferences");
        p.setCustomLabel(ID, "\n Caméra\u0000 \t");
        check(p.getCustomLabel(ID).equals("Caméra"), "Custom labels remove control characters");
        p.putString("notes", repeat(2500));check(p.getString("notes", "").length() == 2000, "Direct note editing retains its safety limit");
    }

    private static void reject(DashboardPrefs p, MemoryContext context, JSONObject json) throws Exception {
        Map<String, ?> before = context.store("prisme_dashboard").getAll();
        try { p.importJSONObject(json);throw new AssertionError("Invalid import was accepted"); }
        catch (JSONException expected) { }
        check(before.equals(context.store("prisme_dashboard").getAll()), "Invalid import is entirely non-destructive");
    }
    private static JSONObject copy(JSONObject json) throws Exception { return new JSONObject(json.toString()); }
    private static Set<String> set(String value) { return new HashSet<String>(Arrays.asList(value)); }
    private static String repeat(int length) { char[] chars = new char[length];Arrays.fill(chars, 'x');return new String(chars); }
    private static void check(boolean condition, String message) { checks++;if (!condition) throw new AssertionError(message); }

    static final class MemoryContext extends Context {
        private final Map<String, MemoryPrefs> stores = new HashMap<String, MemoryPrefs>();
        MemoryPrefs store(String name) { if (!stores.containsKey(name)) stores.put(name, new MemoryPrefs());return stores.get(name); }
        @Override public SharedPreferences getSharedPreferences(String name, int mode) { return store(name); }
    }
    static final class MemoryPrefs implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<String, Object>();
        private final Set<OnSharedPreferenceChangeListener> listeners = new HashSet<OnSharedPreferenceChangeListener>();
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { listeners.add(listener); }
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { listeners.remove(listener); }
        @Override public Map<String, ?> getAll() {
            Map<String, Object> copy = new HashMap<String, Object>();
            for (Map.Entry<String, Object> entry : values.entrySet()) copy.put(entry.getKey(), cloneValue(entry.getValue()));
            return copy;
        }
        @Override public int getInt(String key, int fallback) { return values.containsKey(key) ? (Integer)values.get(key) : fallback; }
        @Override public boolean getBoolean(String key, boolean fallback) { return values.containsKey(key) ? (Boolean)values.get(key) : fallback; }
        @Override public String getString(String key, String fallback) { return values.containsKey(key) ? (String)values.get(key) : fallback; }
        @SuppressWarnings("unchecked") @Override public Set<String> getStringSet(String key, Set<String> fallback) { return values.containsKey(key) ? (Set<String>)cloneValue(values.get(key)) : fallback; }
        @Override public boolean contains(String key) { return values.containsKey(key); }
        @Override public Editor edit() {
            return new Editor() {
                private final Map<String, Object> pending = new HashMap<String, Object>();private boolean clear;
                @Override public Editor putString(String key, String value) { pending.put(key, value);return this; }
                @Override public Editor putStringSet(String key, Set<String> value) { pending.put(key, new HashSet<String>(value));return this; }
                @Override public Editor putInt(String key, int value) { pending.put(key, value);return this; }
                @Override public Editor putLong(String key,long value){pending.put(key,value);return this;}
                @Override public Editor putFloat(String key,float value){pending.put(key,value);return this;}
                @Override public Editor putBoolean(String key, boolean value) { pending.put(key, value);return this; }
                @Override public Editor remove(String key) { pending.put(key, this);return this; }
                @Override public Editor clear() { clear = true;return this; }
                @Override public void apply() {
                    Map<String, Object> before = new HashMap<String, Object>(values);
                    if (clear) values.clear();
                    for (Map.Entry<String, Object> entry : pending.entrySet()) {
                        if (entry.getValue() == this) values.remove(entry.getKey());else values.put(entry.getKey(), cloneValue(entry.getValue()));
                    }
                    Set<String> changed = new HashSet<String>(before.keySet());changed.addAll(values.keySet());
                    for (String key : changed) if (!java.util.Objects.equals(before.get(key), values.get(key))) {
                        for (OnSharedPreferenceChangeListener listener : new HashSet<OnSharedPreferenceChangeListener>(listeners)) listener.onSharedPreferenceChanged(MemoryPrefs.this, key);
                    }
                }
            };
        }
        @SuppressWarnings("unchecked") private static Object cloneValue(Object value) { return value instanceof Set ? new HashSet<String>((Set<String>)value) : value; }
    }
}

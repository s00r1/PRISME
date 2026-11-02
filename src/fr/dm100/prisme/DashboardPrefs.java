package fr.dm100.prisme;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Small, local settings. Imports are whitelisted and validated before any write. */
public final class DashboardPrefs {
    public static final String THEME = "theme", ICON_SIZE = "iconSize", TEXT_SIZE = "textSize",
            COLUMNS = "columns", GAP = "gap", LABELS = "labels", ANIMATIONS = "animations",
            FAVORITES_ONLY = "favoritesOnly", SHOW_CLOCK = "showClock", SHOW_BATTERY = "showBattery",
            SHOW_DOCK = "showDock", WALLPAPER = "wallpaper", ICON_SHAPE = "iconShape", ORDER = "order",
            FAVORITES = "favorites", HIDDEN = "hidden", WORKSPACE = "workspace", FONT = "font",
            ACCENT_INDEX = "accentIndex", HAPTIC = "haptic", KEEP_SCREEN_ON = "keepScreenOn", VOLUME_RAIL = "volumeRail",
            STYLE_SCALE = "styleScale", CONTENT_INSET = "contentInset", FIT_MODE = "fitMode";
    public static final int SLOT_COUNT = 5;
    private static final String LABEL_PREFIX = "customLabel:", LAUNCH_PREFIX = "launch:", SHORTCUT_PREFIX = "shortcut:";
    private static final String FORMAT_VERSION = "_formatVersion";
    private static final Set<String> SHORTCUT_ACTIONS = new HashSet<String>(Arrays.asList(
            "clock", "styles", "customize", "shortcuts", "apps", "search", "favorites", "tools",
            "notes", "timer", "focus", "stopwatch", "workspaces", "home", "controlCenter", "controlCenterSettings", "floatingVolumeSettings", "today", "checklist", "deskclock", "presets", "device", "displayTest", "settingsSearch", "rotationSettings", "language", "shortcutShelf"));
    private static final String[] INT_KEYS = { THEME, ICON_SIZE, TEXT_SIZE, COLUMNS, GAP, WALLPAPER, ICON_SHAPE, ORDER, WORKSPACE, FONT, ACCENT_INDEX, STYLE_SCALE, CONTENT_INSET, FIT_MODE, "upGesture", "downGesture", "blankDoubleTap", "language" };
    private static final String[] BOOL_KEYS = { LABELS, ANIMATIONS, FAVORITES_ONLY, SHOW_CLOCK, SHOW_BATTERY, SHOW_DOCK, HAPTIC, KEEP_SCREEN_ON, VOLUME_RAIL, "edgeCards", "compactNavigation", "deskKeepAwake", "styleDock" };
    private static final String[] SET_KEYS = { FAVORITES, HIDDEN, "workspace1", "workspace2", "workspace3" };
    private final SharedPreferences prefs;
    private final Context appContext;
    private final ControlCenterPrefs controlCenter;
    private final ChecklistStore checklist;
    private final FloatingVolumePrefs floatingVolume;

    public DashboardPrefs(Context context) {
        appContext = context.getApplicationContext();
        prefs = context.getApplicationContext().getSharedPreferences("prisme_dashboard", Context.MODE_PRIVATE);
        controlCenter = new ControlCenterPrefs(context);
        checklist = new ChecklistStore(context);
        floatingVolume = new FloatingVolumePrefs(context);
        migrateToV2();
    }

    public ChecklistStore getChecklist() { return checklist; }
    public ControlCenterPrefs getControlCenter() { return controlCenter; }
    public FloatingVolumePrefs getFloatingVolume() { return floatingVolume; }

    /** Once on upgrade: release the old reserved strip; keep every other setting and saved note. */
    private void migrateToV2() {
        int version;
        try { version = prefs.getInt(FORMAT_VERSION, 0); }
        catch (ClassCastException ex) { version = 0; }
        if (version < 2) prefs.edit().putBoolean(VOLUME_RAIL, false).putInt(FORMAT_VERSION, 2).apply();
    }

    public int getInt(String key, int fallback) {
        if (!knownInt(key) && !launchKey(key)) return fallback;
        try { return bound(key, prefs.getInt(key, fallback)); }
        catch (ClassCastException ex) { return bound(key, fallback); }
    }

    public boolean getBoolean(String key, boolean fallback) {
        if (!knownBoolean(key)) return fallback;
        try { return prefs.getBoolean(key, fallback); }
        catch (ClassCastException ex) { return fallback; }
    }

    public void putInt(String key, int value) {
        if (knownInt(key) || launchKey(key)) prefs.edit().putInt(key, bound(key, value)).apply();
    }

    public void putBoolean(String key, boolean value) {
        if (knownBoolean(key)) prefs.edit().putBoolean(key, value).apply();
    }

    /** Missing slots retain the current style's default; an empty value restores that default. */
    public String getShortcut(int slot, String fallback) {
        String safeFallback = isValidShortcutAction(fallback) ? fallback : "apps";
        if (slot < 0 || slot >= SLOT_COUNT) return safeFallback;
        try {
            String value = prefs.getString(SHORTCUT_PREFIX + slot, "");
            return isValidShortcutAction(value) ? value : safeFallback;
        } catch (ClassCastException ex) { return safeFallback; }
    }

    public void setShortcut(int slot, String action) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        if (action == null || action.length() == 0) prefs.edit().remove(SHORTCUT_PREFIX + slot).apply();
        else if (isValidShortcutAction(action)) prefs.edit().putString(SHORTCUT_PREFIX + slot, action).apply();
    }

    public static boolean isValidShortcutAction(String action) {
        if (SHORTCUT_ACTIONS.contains(action)) return true;
        if (action == null || !action.startsWith("app:")) return false;
        String id = action.substring(4);
        if (!validId(id)) return false;
        int slash = id.indexOf('/');
        if (slash != id.lastIndexOf('/')) return false;
        String packageName = id.substring(0, slash), className = id.substring(slash + 1);
        if (className.startsWith(".")) className = packageName + className;
        return javaName(packageName) && javaName(className);
    }

    private static boolean javaName(String name) {
        if (name.length() == 0 || name.startsWith(".") || name.endsWith(".")) return false;
        boolean start = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '.') { if (start) return false; start = true; }
            else { if (start ? !Character.isJavaIdentifierStart(c) : !Character.isJavaIdentifierPart(c)) return false; start = false; }
        }
        return !start;
    }

    public Set<String> getSet(String key) {
        if (!Arrays.asList(SET_KEYS).contains(key)) return new HashSet<String>();
        try { return new HashSet<String>(prefs.getStringSet(key, new HashSet<String>())); }
        catch (ClassCastException ex) { return new HashSet<String>(); }
    }

    public synchronized void toggleSet(String key, String id) {
        if (!Arrays.asList(SET_KEYS).contains(key) || !validId(id)) return;
        Set<String> copy = getSet(key);
        if (!copy.remove(id)) copy.add(id);
        prefs.edit().putStringSet(key, copy).apply();
    }

    public boolean isFavorite(String id) { return getSet(FAVORITES).contains(id); }
    public boolean isHidden(String id) { return getSet(HIDDEN).contains(id); }

    public String getString(String key, String fallback) {
        if (!"notes".equals(key) && !labelKey(key)) return fallback;
        try { return prefs.getString(key, fallback); }
        catch (ClassCastException ex) { return fallback; }
    }

    public void putString(String key, String value) {
        if ("notes".equals(key)) {
            String safe = value == null ? "" : value;
            if (safe.length() > 2000) safe = safe.substring(0, 2000);
            prefs.edit().putString(key, safe).apply();
        } else if (labelKey(key)) {
            setCustomLabel(key.substring(LABEL_PREFIX.length()), value);
        }
    }

    public String getCustomLabel(String id) {
        if (!validId(id)) return "";
        try { return prefs.getString(LABEL_PREFIX + id, ""); }
        catch (ClassCastException ex) { return ""; }
    }

    public void setCustomLabel(String id, String label) {
        if (!validId(id)) return;
        String safe = cleanLabel(label);
        SharedPreferences.Editor editor = prefs.edit();
        if (safe.length() == 0) editor.remove(LABEL_PREFIX + id);
        else editor.putString(LABEL_PREFIX + id, safe);
        editor.apply();
    }

    public JSONObject exportJSONObject() throws JSONException {
        JSONObject root = new JSONObject(), settings = new JSONObject(), labels = new JSONObject(), launches = new JSONObject(), shortcuts = new JSONObject();
        root.put("schemaVersion", 7);
        root.put("organizer", checklist.read());
        for (String key : INT_KEYS) settings.put(key, getInt(key, defaultInt(key)));
        for (String key : BOOL_KEYS) settings.put(key, getBoolean(key, defaultBoolean(key)));
        root.put("settings", settings);
        for (String key : SET_KEYS) root.put(key, sortedArray(getSet(key)));
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getKey().startsWith(LABEL_PREFIX) && entry.getValue() instanceof String) {
                String id = entry.getKey().substring(LABEL_PREFIX.length());
                if (validId(id)) labels.put(id, cleanLabel((String) entry.getValue()));
            } else if (launchKey(entry.getKey()) && entry.getValue() instanceof Integer) {
                launches.put(entry.getKey().substring(LAUNCH_PREFIX.length()), Math.max(0, (Integer) entry.getValue()));
            }
        }
        root.put("customLabels", labels);
        root.put("launches", launches);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            String value = getShortcut(slot, "apps");
            if (prefs.contains(SHORTCUT_PREFIX + slot)) shortcuts.put(Integer.toString(slot), value);
        }
        root.put("shortcuts", shortcuts);
        root.put("controlCenter", controlCenter.exportJSONObject());
        root.put("floatingVolume", floatingVolume.exportJSONObject());
        root.put("notes", getString("notes", ""));
        root.put("v6", V6ProfileCodec.exportJSONObject(appContext));
        if (root.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65536)
            throw new JSONException("Sauvegarde trop volumineuse (maximum 64 Ko)");
        return root;
    }

    /** Atomic replacement: malformed files leave the existing dashboard untouched. */
    public synchronized void importJSONObject(JSONObject root) throws JSONException {
        if (root == null) throw new JSONException("Sauvegarde vide");
        if (root.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65536)
            throw new JSONException("Sauvegarde trop volumineuse (maximum 64 Ko)");
        Object version = root.opt("schemaVersion");
        if (!(version instanceof Number) || (((Number) version).doubleValue() != 1 && ((Number) version).doubleValue() != 2 && ((Number) version).doubleValue() != 3 && ((Number) version).doubleValue() != 4 && ((Number) version).doubleValue() != 5 && ((Number) version).doubleValue() != 6 && ((Number) version).doubleValue() != 7))
            throw new JSONException("Version de sauvegarde inconnue");
        int schema = ((Number) version).intValue();
        Set<String> allowedRoot = new HashSet<String>(Arrays.asList("schemaVersion", "settings", "customLabels", "launches", "notes"));
        if (schema >= 2) allowedRoot.add("shortcuts");
        if (schema >= 3) allowedRoot.add("controlCenter");
        if (schema >= 4) allowedRoot.add("floatingVolume");
        if (schema >= 6) allowedRoot.add("organizer");
        if (schema >= 7) allowedRoot.add("v6");
        allowedRoot.addAll(Arrays.asList(SET_KEYS));
        checkKeys(root, allowedRoot);
        JSONObject settings = root.getJSONObject("settings");
        HashSet<String> allowed = new HashSet<String>();
        allowed.addAll(Arrays.asList(INT_KEYS)); allowed.addAll(Arrays.asList(BOOL_KEYS));
        checkKeys(settings, allowed);
        Map<String, Integer> ints = new HashMap<String, Integer>();
        Map<String, Boolean> bools = new HashMap<String, Boolean>();
        for (String key : INT_KEYS) {
            if (!settings.has(key)) continue;
            Object value = settings.get(key);
            if (!(value instanceof Number)) throw new JSONException("Valeur numérique attendue : " + key);
            double number = ((Number) value).doubleValue();
            int integer = ((Number) value).intValue();
            if (!Double.isFinite(number) || number != integer || bound(key, integer) != integer)
                throw new JSONException("Valeur hors limites : " + key);
            ints.put(key, integer);
        }
        for (String key : BOOL_KEYS) {
            if (!settings.has(key)) continue;
            Object value = settings.get(key);
            if (!(value instanceof Boolean)) throw new JSONException("Valeur oui/non attendue : " + key);
            bools.put(key, (Boolean) value);
        }
        Map<String, Set<String>> sets = new HashMap<String, Set<String>>();
        for (String key : SET_KEYS) {
            if (root.has(key) && !(root.get(key) instanceof JSONArray)) throw new JSONException("Liste d’applications invalide : " + key);
            sets.put(key, readSet(root.optJSONArray(key)));
        }
        JSONObject custom = root.optJSONObject("customLabels");
        if (root.has("customLabels") && custom == null) throw new JSONException("Noms personnalisés invalides");
        Map<String, String> labels = new HashMap<String, String>();
        if (custom != null) {
            if (custom.length() > 512) throw new JSONException("Trop de noms personnalisés");
            Iterator<String> keys = custom.keys();
            while (keys.hasNext()) {
                String id = keys.next(); Object value = custom.get(id);
                if (!validId(id) || !(value instanceof String) || ((String) value).length() > 64)
                    throw new JSONException("Nom personnalisé invalide");
                String label = cleanLabel((String) value);
                if (label.length() > 0) labels.put(LABEL_PREFIX + id, label);
            }
        }
        Map<String, Integer> launches = new HashMap<String, Integer>();
        JSONObject history = root.optJSONObject("launches");
        if (root.has("launches") && history == null) throw new JSONException("Historique local invalide");
        if (history != null) {
            if (history.length() > 1024) throw new JSONException("Historique trop volumineux");
            Iterator<String> ids = history.keys();
            while (ids.hasNext()) {
                String id = ids.next(); Object value = history.get(id);
                if (!validId(id) || !(value instanceof Number)) throw new JSONException("Historique local invalide");
                double number = ((Number) value).doubleValue();
                int integer = ((Number) value).intValue();
                if (!Double.isFinite(number) || number != integer || integer < 0) throw new JSONException("Date locale invalide");
                launches.put(LAUNCH_PREFIX + id, integer);
            }
        }
        String notes = "";
        if (root.has("notes")) {
            Object value = root.get("notes");
            if (!(value instanceof String) || ((String) value).length() > 2000) throw new JSONException("Notes trop longues (maximum 2000 caractères)");
            notes = (String) value;
        }
        Map<String, String> shortcuts = new HashMap<String, String>();
        if (root.has("shortcuts")) {
            JSONObject savedShortcuts = root.optJSONObject("shortcuts");
            if (savedShortcuts == null) throw new JSONException("Raccourcis invalides");
            Iterator<String> slots = savedShortcuts.keys();
            while (slots.hasNext()) {
                String slot = slots.next();
                if (slot.length() != 1 || slot.charAt(0) < '0' || slot.charAt(0) >= '0' + SLOT_COUNT)
                    throw new JSONException("Emplacement de raccourci invalide");
                Object action = savedShortcuts.get(slot);
                if (!(action instanceof String) || !isValidShortcutAction((String) action))
                    throw new JSONException("Action de raccourci invalide");
                shortcuts.put(SHORTCUT_PREFIX + slot, (String) action);
            }
        }
        if(!ints.containsKey("language"))ints.put("language",getInt("language",0));
        JSONArray organizer = schema >= 6 ? ChecklistStore.validate(root.getJSONArray("organizer")) : null;
        // Decode the full centre configuration before creating an editor for either store.
        // Older profiles intentionally keep the current centre, which they could not represent.
        ControlCenterPrefs.Prepared center = schema >= 3
                ? ControlCenterPrefs.prepareImport(root.getJSONObject("controlCenter")) : null;
        FloatingVolumePrefs.Prepared volume = schema >= 4
                ? FloatingVolumePrefs.prepareImport(root.getJSONObject("floatingVolume")) : null;
        V6ProfileCodec.Prepared newModules = schema >= 7 ? V6ProfileCodec.prepareImport(root.getJSONObject("v6")) : null;
        // Explicitly imported legacy settings, including volumeRail, remain exact. Mark the
        // import migrated so a later activity recreation never changes an applied profile.
        SharedPreferences.Editor editor = prefs.edit().clear().putInt(FORMAT_VERSION, 2).putBoolean(VOLUME_RAIL, false);
        for (Map.Entry<String, Integer> entry : ints.entrySet()) editor.putInt(entry.getKey(), entry.getValue());
        for (Map.Entry<String, Boolean> entry : bools.entrySet()) editor.putBoolean(entry.getKey(), entry.getValue());
        for (Map.Entry<String, String> entry : labels.entrySet()) editor.putString(entry.getKey(), entry.getValue());
        for (Map.Entry<String, Integer> entry : launches.entrySet()) editor.putInt(entry.getKey(), entry.getValue());
        for (Map.Entry<String, Set<String>> entry : sets.entrySet()) editor.putStringSet(entry.getKey(), entry.getValue());
        for (Map.Entry<String, String> entry : shortcuts.entrySet()) editor.putString(entry.getKey(), entry.getValue());
        editor.putString("notes", notes).apply();
        if (center != null) controlCenter.applyPrepared(center);
        if (volume != null) floatingVolume.applyPrepared(volume);
        if (organizer != null) checklist.replace(organizer);
        if (newModules != null) V6ProfileCodec.applyPrepared(appContext, newModules);
    }

    public void reset() { int language=getInt("language",0);prefs.edit().clear().putInt(FORMAT_VERSION, 2).putBoolean(VOLUME_RAIL, false).putInt("language",language).apply(); }

    private static void checkKeys(JSONObject object, Set<String> allowed) throws JSONException {
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) if (!allowed.contains(keys.next())) throw new JSONException("Option inconnue dans la sauvegarde");
    }
    private static JSONArray sortedArray(Set<String> set) {
        JSONArray result = new JSONArray();
        for (String value : new TreeSet<String>(set)) result.put(value);
        return result;
    }
    private static Set<String> readSet(JSONArray array) throws JSONException {
        HashSet<String> result = new HashSet<String>();
        if (array == null) return result;
        if (array.length() > 1024) throw new JSONException("Trop d’applications dans la sauvegarde");
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (!(value instanceof String) || !validId((String) value)) throw new JSONException("Identifiant d’application invalide");
            result.add((String) value);
        }
        return result;
    }
    private static boolean validId(String id) {
        if (id == null || id.length() > 512 || id.length() < 3) return false;
        for (int i = 0; i < id.length(); i++) if (Character.isISOControl(id.charAt(i)) || Character.isWhitespace(id.charAt(i))) return false;
        ComponentName component = ComponentName.unflattenFromString(id);
        return component != null && component.getPackageName().length() > 0 && component.getClassName().length() > 0;
    }
    private static String cleanLabel(String value) {
        if (value == null) return "";
        StringBuilder clean = new StringBuilder();
        for (int i = 0; i < value.length() && clean.length() < 64; i++) {
            char c = value.charAt(i);
            if (!Character.isISOControl(c)) clean.append(c);
        }
        return clean.toString().trim();
    }
    private static boolean knownInt(String key) { return Arrays.asList(INT_KEYS).contains(key); }
    private static boolean knownBoolean(String key) { return Arrays.asList(BOOL_KEYS).contains(key); }
    private static boolean launchKey(String key) { return key != null && key.startsWith(LAUNCH_PREFIX) && validId(key.substring(LAUNCH_PREFIX.length())); }
    private static boolean labelKey(String key) { return key != null && key.startsWith(LABEL_PREFIX) && validId(key.substring(LABEL_PREFIX.length())); }
    private static int bound(String key, int value) {
        int min = 0, max = 0;
        if (THEME.equals(key)) max = ThemeSpec.ALL.length - 1;
        else if (ICON_SIZE.equals(key)) { min = 52; max = 100; }
        else if (TEXT_SIZE.equals(key)) { min = 12; max = 24; }
        else if (COLUMNS.equals(key)) { min = 3; max = 7; }
        else if (GAP.equals(key)) { min = 4; max = 22; }
        else if (WALLPAPER.equals(key)) max = 5;
        else if (ICON_SHAPE.equals(key) || ORDER.equals(key) || WORKSPACE.equals(key) || FONT.equals(key)) max = 3;
        else if (ACCENT_INDEX.equals(key)) max = 6;
        else if (STYLE_SCALE.equals(key)) { min = 75; max = 140; }
        else if (CONTENT_INSET.equals(key)) max = 24;
        else if (FIT_MODE.equals(key)) max = 2;
        else if ("language".equals(key)) max = 6;
        else if ("upGesture".equals(key) || "downGesture".equals(key) || "blankDoubleTap".equals(key)) max = 8;
        else if (launchKey(key)) max = Integer.MAX_VALUE;
        return Math.max(min, Math.min(max, value));
    }
    private static int defaultInt(String key) {
        if (ICON_SIZE.equals(key)) return 72;
        if (TEXT_SIZE.equals(key)) return 16;
        if (COLUMNS.equals(key)) return 5;
        if (GAP.equals(key)) return 12;
        if (STYLE_SCALE.equals(key)) return 100;
        if ("upGesture".equals(key) || "blankDoubleTap".equals(key)) return 1;
        return 0;
    }
    private static boolean defaultBoolean(String key) {
        return !"styleDock".equals(key) && !"deskKeepAwake".equals(key) && !FAVORITES_ONLY.equals(key) && !KEEP_SCREEN_ON.equals(key) && !VOLUME_RAIL.equals(key) && !"compactNavigation".equals(key);
    }
}

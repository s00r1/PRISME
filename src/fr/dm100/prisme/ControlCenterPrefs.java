package fr.dm100.prisme;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Local control-centre configuration. Validation never writes to the store. */
public final class ControlCenterPrefs {
    public static final int MAX_TILES = 24;
    private static final String TILES = "tiles";
    private static final String[] INT_KEYS = {"style", "opacity", "scale", "columns", "position", "width", "height",
            "edge", "opening", "closing", "edgeSize", "swipeDistance", "handlePosition", "radius", "accent", "customColor", "tileOpacity", "scrimOpacity", "closeDistance"};
    private static final int[] INT_MIN = {0, 20, 80, 2, 0, 65, 55, 0, 0, 0, 16, 24, 0, 0, 0, 0, 0, 0, 24};
    private static final int[] INT_MAX = {1, 100, 125, 5, 3, 100, 100, 3, 3, 3, 96, 140, 2, 36, 5, 0xFFFFFF, 100, 60, 120};
    private static final int[] INT_DEFAULT = {0, 68, 100, 4, 0, 100, 90, 0, 3, 0, 64, 36, 1, 24, 0, 0x62D9CC, 28, 8, 28};
    private static final String[] BOOL_KEYS = {"enabled", "dark", "labels", "status", "volume", "brightness", "customAccent", "compactHandle"};
    private static final List<String> DEFAULT_TILES = Arrays.asList("wifi", "bluetooth", "sound", "airplane", "location",
            "rotateGlobal", "screenlamp", "settings", "timer", "clock");
    private static final Set<String> TILE_IDS = new HashSet<String>(Arrays.asList("wifi", "bluetooth", "sound", "airplane",
            "data", "hotspot", "location", "rotation", "battery", "display", "volume", "dnd", "screenlamp", "camera",
            "alarms", "timer", "stopwatch", "calculator", "notes", "settings", "apps", "styles", "clock", "search",
            "music", "vpn", "accessibility", "capture", "volumepill", "today", "checklist", "deskclock", "device", "musicPrevious", "musicNext","rotateGlobal"));
    private final SharedPreferences prefs;

    public ControlCenterPrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("prisme_control_center", Context.MODE_PRIVATE);
    }

    public int getInt(String key, int fallback) {
        int index = intIndex(key);
        if (index < 0) return fallback;
        try { return bound(index, prefs.getInt(key, INT_DEFAULT[index])); }
        catch (ClassCastException ex) { return INT_DEFAULT[index]; }
    }

    public void putInt(String key, int value) {
        int index = intIndex(key);
        if (index >= 0) prefs.edit().putInt(key, bound(index, value)).apply();
    }

    public boolean getBoolean(String key, boolean fallback) {
        if (!Arrays.asList(BOOL_KEYS).contains(key)) return fallback;
        try { return prefs.getBoolean(key, defaultBoolean(key)); }
        catch (ClassCastException ex) { return defaultBoolean(key); }
    }

    public void putBoolean(String key, boolean value) {
        if (Arrays.asList(BOOL_KEYS).contains(key)) prefs.edit().putBoolean(key, value).apply();
    }

    /** Ordered and defensive. Legacy rotation aliases normalize on read without rewriting local storage. */
    public List<String> getTiles() {
        try {
            if (!prefs.contains(TILES)) return new ArrayList<String>(DEFAULT_TILES);
            String encoded = prefs.getString(TILES, "[]");
            if (encoded == null) throw new JSONException("Liste locale absente");
            JSONArray stored = new JSONArray(encoded);
            return readTiles(stored);
        } catch (JSONException | ClassCastException ex) {
            return new ArrayList<String>(DEFAULT_TILES);
        }
    }

    /** Invalid input leaves the existing order untouched. Duplicate entries keep their first position. */
    public void setTiles(List<String> tiles) {
        final List<String> validated;
        try { validated = normalizeTiles(tiles); }
        catch (JSONException ex) { throw new IllegalArgumentException(ex.getMessage(), ex); }
        prefs.edit().putString(TILES, tileArray(validated).toString()).apply();
    }

    public static boolean isValidTile(String tile) {
        return TILE_IDS.contains(tile) || (tile != null && tile.startsWith("app:") && DashboardPrefs.isValidShortcutAction(tile));
    }

    public JSONObject exportJSONObject() throws JSONException {
        JSONObject settings = new JSONObject();
        for (int i = 0; i < INT_KEYS.length; i++) settings.put(INT_KEYS[i], getInt(INT_KEYS[i], INT_DEFAULT[i]));
        for (String key : BOOL_KEYS) settings.put(key, getBoolean(key, defaultBoolean(key)));
        return new JSONObject().put("settings", settings).put(TILES, tileArray(getTiles()));
    }

    public static void validateJSONObject(JSONObject json) throws JSONException { prepareImport(json); }

    public synchronized void importJSONObject(JSONObject json) throws JSONException { applyPrepared(prepareImport(json)); }

    /** Package-private import plan lets a dashboard profile validate BOTH stores before either write. */
    static final class Prepared {
        final Map<String, Integer> ints = new HashMap<String, Integer>();
        final Map<String, Boolean> bools = new HashMap<String, Boolean>();
        List<String> tiles;
    }

    static Prepared prepareImport(JSONObject json) throws JSONException {
        if (json == null) throw new JSONException("Centre de contrôle absent");
        String encoded = json.toString();
        if (encoded == null || encoded.getBytes(StandardCharsets.UTF_8).length > 65536)
            throw new JSONException("Configuration du centre de contrôle trop volumineuse");
        checkKeys(json, new HashSet<String>(Arrays.asList("settings", TILES)));
        JSONObject settings = json.getJSONObject("settings");
        Set<String> allowed = new HashSet<String>(Arrays.asList(INT_KEYS));
        allowed.addAll(Arrays.asList(BOOL_KEYS));
        checkKeys(settings, allowed);
        Prepared prepared = new Prepared();
        for (int i = 0; i < INT_KEYS.length; i++) {
            String key = INT_KEYS[i];
            int value = INT_DEFAULT[i];
            if (settings.has(key)) {
                Object raw = settings.get(key);
                if (!(raw instanceof Number)) throw new JSONException("Valeur numérique attendue : " + key);
                double number = ((Number) raw).doubleValue();
                value = ((Number) raw).intValue();
                if (!Double.isFinite(number) || number != value || bound(i, value) != value)
                    throw new JSONException("Valeur hors limites : " + key);
            }
            prepared.ints.put(key, value);
        }
        for (String key : BOOL_KEYS) {
            boolean value = defaultBoolean(key);
            if (settings.has(key)) {
                Object raw = settings.get(key);
                if (!(raw instanceof Boolean)) throw new JSONException("Valeur oui/non attendue : " + key);
                value = (Boolean) raw;
            }
            prepared.bools.put(key, value);
        }
        // A present JSON object always describes its complete tile order, including an explicit empty list.
        prepared.tiles = readTiles(json.getJSONArray(TILES));
        return prepared;
    }

    synchronized void applyPrepared(Prepared prepared) {
        SharedPreferences.Editor editor = prefs.edit().clear();
        for (Map.Entry<String, Integer> entry : prepared.ints.entrySet()) editor.putInt(entry.getKey(), entry.getValue());
        for (Map.Entry<String, Boolean> entry : prepared.bools.entrySet()) editor.putBoolean(entry.getKey(), entry.getValue());
        editor.putString(TILES, tileArray(prepared.tiles).toString()).apply();
    }

    /** Keep activation, gesture controls and tile order while restoring only the panel's appearance. */
    public void resetAppearance() {
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> interaction = new HashSet<String>(Arrays.asList("edge", "opening", "closing", "edgeSize", "swipeDistance", "handlePosition", "closeDistance"));
        for (int i = 0; i < INT_KEYS.length; i++) if (!interaction.contains(INT_KEYS[i])) editor.remove(INT_KEYS[i]);
        for (String key : BOOL_KEYS) if (!"enabled".equals(key)) editor.remove(key);
        editor.apply();
    }

    public void resetAll() { prefs.edit().clear().apply(); }

    private static int intIndex(String key) { return Arrays.asList(INT_KEYS).indexOf(key); }
    private static int bound(int index, int value) { return Math.max(INT_MIN[index], Math.min(INT_MAX[index], value)); }
    private static boolean defaultBoolean(String key) { return !"customAccent".equals(key); }

    private static JSONArray tileArray(List<String> tiles) {
        JSONArray json = new JSONArray();
        for (String tile : tiles) json.put(tile);
        return json;
    }

    private static List<String> readTiles(JSONArray tiles) throws JSONException {
        if (tiles == null || tiles.length() > MAX_TILES) throw new JSONException("24 tuiles maximum");
        ArrayList<String> values = new ArrayList<String>();
        for (int i = 0; i < tiles.length(); i++) {
            Object raw = tiles.get(i);
            if (!(raw instanceof String)) throw new JSONException("Identifiant de tuile invalide");
            values.add((String) raw);
        }
        return normalizeTiles(values);
    }

    private static List<String> normalizeTiles(List<?> tiles) throws JSONException {
        if (tiles == null || tiles.size() > MAX_TILES) throw new JSONException("24 tuiles maximum");
        LinkedHashSet<String> unique = new LinkedHashSet<String>();
        for (Object raw : tiles) {
            if (!(raw instanceof String) || !isValidTile((String) raw)) throw new JSONException("Identifiant de tuile invalide");
            String tile = "rotation".equals(raw) ? "rotateGlobal" : (String) raw;
            if (tile.startsWith("app:")) {
                ComponentName component = ComponentName.unflattenFromString(tile.substring(4));
                if (component == null) throw new JSONException("Application de tuile invalide");
                tile = "app:" + component.flattenToString();
            }
            unique.add(tile);
        }
        return new ArrayList<String>(unique);
    }

    private static void checkKeys(JSONObject json, Set<String> allowed) throws JSONException {
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) if (!allowed.contains(keys.next())) throw new JSONException("Option du centre de contrôle inconnue");
    }
}

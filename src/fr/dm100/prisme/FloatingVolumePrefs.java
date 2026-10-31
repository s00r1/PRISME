package fr.dm100.prisme;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Appearance is portable. Overlay activation and boot consent always stay local to this installation. */
public final class FloatingVolumePrefs {
    private static final String ENABLED = "enabled", BOOT = "boot", ACTIVE_BOOT = "activeBoot";
    private static final String[] INT_KEYS = {"style", "shape", "layout", "size", "opacity", "expandedOpacity",
            "accent", "customColor", "stream", "xPercent", "yPercent", "collapseDelay", "repeatDelay", "repeatInterval", "idleDelay", "idleOpacity"};
    private static final int[] INT_MIN = {0, 0, 0, 40, 15, 15, 0, 0, 0, 0, 0, 1, 250, 70, 2, 20};
    private static final int[] INT_MAX = {5, 3, 2, 96, 100, 100, 5, 0xFFFFFF, 3, 100, 100, 15, 900, 300, 60, 90};
    private static final int[] INT_DEFAULT = {0, 0, 0, 56, 90, 95, 0, 0x62D9CC, 0, 100, 55, 4, 450, 140, 8, 40};
    private static final String[] BOOL_KEYS = {"customAccent", "snap", "locked", "autoCollapse", "showLevel", "showMute", "haptic", "idleFade"};
    private final SharedPreferences prefs;

    public FloatingVolumePrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("prisme_floating_volume", Context.MODE_PRIVATE);
    }

    public int getInt(String key, int fallback) {
        if (ACTIVE_BOOT.equals(key)) {
            try { return Math.max(-2, prefs.getInt(ACTIVE_BOOT, -2)); }
            catch (ClassCastException ex) { return -2; }
        }
        int index = intIndex(key);
        if (index < 0) return fallback;
        try { return bound(index, prefs.getInt(key, INT_DEFAULT[index])); }
        catch (ClassCastException ex) { return INT_DEFAULT[index]; }
    }

    public void putInt(String key, int value) {
        if (ACTIVE_BOOT.equals(key)) { markActiveBoot(value); return; }
        int index = intIndex(key);
        if (index >= 0) prefs.edit().putInt(key, bound(index, value)).apply();
    }

    public boolean getBoolean(String key, boolean fallback) {
        if (ENABLED.equals(key) || BOOT.equals(key)) {
            try { return prefs.getBoolean(key, false); }
            catch (ClassCastException ex) { return false; }
        }
        if (!Arrays.asList(BOOL_KEYS).contains(key)) return fallback;
        try { return prefs.getBoolean(key, defaultBoolean(key)); }
        catch (ClassCastException ex) { return defaultBoolean(key); }
    }

    public void putBoolean(String key, boolean value) {
        if (ENABLED.equals(key)) { setEnabled(value); return; }
        if (BOOT.equals(key) || Arrays.asList(BOOL_KEYS).contains(key)) prefs.edit().putBoolean(key, value).apply();
    }

    /** Stopping deliberately clears the current-session marker, while retaining the user's boot preference. */
    public void setEnabled(boolean enabled) {
        SharedPreferences.Editor editor = prefs.edit().putBoolean(ENABLED, enabled);
        if (!enabled) editor.putInt(ACTIVE_BOOT, -2);
        editor.apply();
    }

    public void markActiveBoot(int currentBoot) { prefs.edit().putInt(ACTIVE_BOOT, Math.max(-2, currentBoot)).apply(); }

    public boolean mayResume(int currentBoot) {
        return getBoolean(ENABLED, false) && (getBoolean(BOOT, false)
                || (currentBoot >= 0 && getInt(ACTIVE_BOOT, -2) == currentBoot));
    }

    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (listener != null) prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (listener != null) prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public JSONObject exportJSONObject() throws JSONException {
        JSONObject settings = new JSONObject();
        for (int i = 0; i < INT_KEYS.length; i++) settings.put(INT_KEYS[i], getInt(INT_KEYS[i], INT_DEFAULT[i]));
        for (String key : BOOL_KEYS) settings.put(key, getBoolean(key, defaultBoolean(key)));
        return new JSONObject().put("settings", settings);
    }

    public static void validateJSONObject(JSONObject json) throws JSONException { prepareImport(json); }

    public synchronized void importJSONObject(JSONObject json) throws JSONException { applyPrepared(prepareImport(json)); }

    static final class Prepared {
        final Map<String, Integer> ints = new HashMap<String, Integer>();
        final Map<String, Boolean> bools = new HashMap<String, Boolean>();
    }

    /** Pure preparation enables a full dashboard/control-centre/volume validation before any store changes. */
    static Prepared prepareImport(JSONObject json) throws JSONException {
        if (json == null) throw new JSONException("Configuration de la pastille absente");
        String encoded = json.toString();
        if (encoded == null || encoded.getBytes(StandardCharsets.UTF_8).length > 65536)
            throw new JSONException("Configuration de la pastille trop volumineuse");
        checkKeys(json, new HashSet<String>(Arrays.asList("settings")));
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
        return prepared;
    }

    synchronized void applyPrepared(Prepared prepared) {
        // Never clear this store: enabled/boot/activeBoot cannot be imported or disabled by a style profile.
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, Integer> entry : prepared.ints.entrySet()) editor.putInt(entry.getKey(), entry.getValue());
        for (Map.Entry<String, Boolean> entry : prepared.bools.entrySet()) editor.putBoolean(entry.getKey(), entry.getValue());
        editor.apply();
    }

    public void resetAppearance() {
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : INT_KEYS) editor.remove(key);
        for (String key : BOOL_KEYS) editor.remove(key);
        editor.apply();
    }

    public void resetAll() {
        // Explicit writes also notify listeners on Android 7, where clear() alone may not emit per-key changes.
        prefs.edit().clear().putBoolean(ENABLED, false).putBoolean(BOOT, false).putInt(ACTIVE_BOOT, -2).apply();
    }

    private static int intIndex(String key) { return Arrays.asList(INT_KEYS).indexOf(key); }
    private static int bound(int index, int value) { return Math.max(INT_MIN[index], Math.min(INT_MAX[index], value)); }
    private static boolean defaultBoolean(String key) { return "snap".equals(key) || "autoCollapse".equals(key) || "showLevel".equals(key); }
    private static void checkKeys(JSONObject json, Set<String> allowed) throws JSONException {
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) if (!allowed.contains(keys.next())) throw new JSONException("Option de la pastille inconnue ou non importable");
    }
}

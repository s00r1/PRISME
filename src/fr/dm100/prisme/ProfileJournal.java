package fr.dm100.prisme;

import android.content.*;
import java.util.*;
import org.json.*;

/** Durable private undo journal for profile changes across several preference files. */
public final class ProfileJournal {
  private ProfileJournal() {}

  static final String[] STORES = {
    "prisme_dashboard",
    "prisme_control_center",
    "prisme_floating_volume",
    "prisme_checklist",
    "prisme_battery_overlay",
    "prisme_music",
    "prisme_studio",
    "prisme_sense",
    "prisme_widgets",
    "prisme_edge",
    "prisme_home"
  };

  // Store names must match their owners; unknown preference files are never imported.
  public static synchronized void begin(Context c) throws JSONException {
    recover(c);
    JSONObject snapshot = new JSONObject();
    for (String store : STORES) {
      JSONObject values = new JSONObject();
      for (Map.Entry<String, ?> e : c.getSharedPreferences(store, 0).getAll().entrySet()) {
        Object value = e.getValue();
        JSONObject v = new JSONObject();
        String type =
            value instanceof Set
                ? "set"
                : value instanceof Integer
                    ? "int"
                    : value instanceof Long
                        ? "long"
                        : value instanceof Float
                            ? "float"
                            : value instanceof Boolean ? "boolean" : "string";
        v.put("type", type);
        if (value instanceof Set) {
          JSONArray set = new JSONArray();
          for (Object x : (Set<?>) value) set.put(x);
          v.put("value", set);
        } else v.put("value", value);
        values.put(e.getKey(), v);
      }
      snapshot.put(store, values);
    }
    if (!c.getSharedPreferences("prisme_profile_journal", 0)
        .edit()
        .putString("pending", snapshot.toString())
        .commit()) throw new JSONException("Impossible de protéger la restauration");
  }

  public static synchronized void finish(Context c) throws JSONException {
    for (String name : STORES)
      if (!c.getSharedPreferences(name, 0).edit().commit())
        throw new JSONException("Écriture des réglages impossible");
    if (!c.getSharedPreferences("prisme_profile_journal", 0).edit().remove("pending").commit())
      throw new JSONException("Validation de restauration impossible");
  }

  public static synchronized boolean recover(Context c) throws JSONException {
    SharedPreferences journal = c.getSharedPreferences("prisme_profile_journal", 0);
    String pending = journal.getString("pending", "");
    if (pending.isEmpty()) return false;
    JSONObject root = new JSONObject(pending);
    for (String name : STORES) {
      JSONObject values = root.getJSONObject(name);
      SharedPreferences.Editor e = c.getSharedPreferences(name, 0).edit().clear();
      Iterator<String> keys = values.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        JSONObject v = values.getJSONObject(key);
        String t = v.getString("type");
        if (t.equals("set")) {
          Set<String> set = new HashSet<>();
          JSONArray items = v.getJSONArray("value");
          for (int i = 0; i < items.length(); i++) set.add(items.getString(i));
          e.putStringSet(key, set);
        } else if (t.equals("int")) e.putInt(key, v.getInt("value"));
        else if (t.equals("long")) e.putLong(key, v.getLong("value"));
        else if (t.equals("float")) e.putFloat(key, (float) v.getDouble("value"));
        else if (t.equals("boolean")) e.putBoolean(key, v.getBoolean("value"));
        else e.putString(key, v.getString("value"));
      }
      if (!e.commit()) throw new JSONException("Récupération interrompue");
    }
    if (!journal.edit().remove("pending").commit())
      throw new JSONException("Récupération non validée");
    return true;
  }
}

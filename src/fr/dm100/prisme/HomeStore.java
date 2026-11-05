package fr.dm100.prisme;

import android.content.*;
import org.json.*;

public final class HomeStore {
  private final SharedPreferences p;

  public HomeStore(Context c) {
    p = c.getSharedPreferences("prisme_home", 0);
    // V6 must preserve the selected classic desktop. Cards require an explicit choice.
    if (PrefValues.number(p, "navigationVersion", 0) < 2) {
      p.edit().putBoolean("enabled", false).putInt("navigationVersion", 2).commit();
    }
  }

  public boolean enabled() {
    try {
      return p.getBoolean("enabled", false);
    } catch (RuntimeException e) {
      return false;
    }
  }

  public void enabled(boolean yes) {
    p.edit().putBoolean("enabled", yes).apply();
  }

  public JSONObject read() {
    try {
      return HomeLayout.validate(new JSONObject(p.getString("layout", "")));
    } catch (Exception e) {
      try {
        return HomeLayout.defaults();
      } catch (JSONException impossible) {
        throw new IllegalStateException(impossible);
      }
    }
  }

  public void save(JSONObject value) throws JSONException {
    String clean = HomeLayout.validate(value).toString();
    String before = read().toString();
    p.edit().putString("undo", before).remove("redo").putString("layout", clean).apply();
  }

  public boolean undo(boolean redo) {
    String key = redo ? "redo" : "undo";
    try {
      String next = p.getString(key, "");
      JSONObject valid = HomeLayout.validate(new JSONObject(next));
      p.edit()
          .putString(redo ? "undo" : "redo", read().toString())
          .putString("layout", valid.toString())
          .remove(key)
          .apply();
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}

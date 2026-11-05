package fr.dm100.prisme;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** One validated widget layout. Mutations use stable IDs, never stale screen positions. */
public final class WidgetStore {
  private final SharedPreferences preferences;

  public WidgetStore(Context context) {
    preferences = context.getSharedPreferences("prisme_widget_engine", Context.MODE_PRIVATE);
  }

  public JSONObject read() {
    try {
      return DashboardWidgets.validate(new JSONObject(PrefValues.text(preferences, "layout", "")));
    } catch (Exception error) {
      try {
        return DashboardWidgets.defaults();
      } catch (JSONException impossible) {
        throw new IllegalStateException(impossible);
      }
    }
  }

  public void save(JSONObject value) throws JSONException {
    String valid = DashboardWidgets.validate(value).toString();
    String current = read().toString();
    if (current.equals(valid)) return;
    preferences.edit().putString("undo", current).putString("layout", valid).apply();
  }

  public boolean undo() {
    try {
      JSONObject previous =
          DashboardWidgets.validate(new JSONObject(PrefValues.text(preferences, "undo", "")));
      String current = read().toString();
      preferences
          .edit()
          .putString("layout", previous.toString())
          .putString("undo", current)
          .apply();
      return true;
    } catch (Exception invalid) {
      return false;
    }
  }

  public JSONArray active(int theme) {
    JSONArray result = new JSONArray();
    JSONArray widgets = read().optJSONArray("widgets");
    for (int i = 0; i < widgets.length(); i++) {
      JSONObject widget = widgets.optJSONObject(i);
      if (DashboardWidgets.visible(widget, theme)) result.put(widget);
    }
    return result;
  }

  public static int indexOf(JSONObject layout, String id) throws JSONException {
    JSONArray widgets = layout.getJSONArray("widgets");
    for (int i = 0; i < widgets.length(); i++) {
      if (id.equals(widgets.getJSONObject(i).getString("id"))) return i;
    }
    throw new JSONException("Ce widget a été retiré. Rouvrir le catalogue.");
  }

  public JSONObject find(String id) throws JSONException {
    JSONObject layout = read();
    return layout.getJSONArray("widgets").getJSONObject(indexOf(layout, id));
  }

  public void set(String id, String key, Object value) throws JSONException {
    JSONObject layout = read();
    layout.getJSONArray("widgets").getJSONObject(indexOf(layout, id)).put(key, value);
    save(layout);
  }

  public void remove(String id) throws JSONException {
    JSONObject layout = read();
    layout.getJSONArray("widgets").remove(indexOf(layout, id));
    save(layout);
  }

  public void move(String id, int offset) throws JSONException {
    JSONObject layout = read();
    JSONArray widgets = layout.getJSONArray("widgets");
    int from = indexOf(layout, id), to = from + offset;
    if (to < 0 || to >= widgets.length()) return;
    Object moved = widgets.get(from);
    widgets.put(from, widgets.get(to));
    widgets.put(to, moved);
    save(layout);
  }
}

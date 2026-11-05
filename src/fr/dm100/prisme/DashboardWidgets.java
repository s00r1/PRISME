package fr.dm100.prisme;

import java.util.*;
import org.json.*;

/**
 * Portable widget appearance, without network consent, location, executable actions or runtime
 * state.
 */
public final class DashboardWidgets {
  public static final String[] TYPES = {
    "weather",
    "clock",
    "music",
    "battery",
    "tasks",
    "notes",
    "timers",
    "calendar",
    "world",
    "system",
    "focus",
    "shortcuts",
    "stopwatch"
  };
  public static final String[] NAMES = {
    "Météo",
    "Horloge",
    "Musique",
    "Batterie",
    "Tâches",
    "Carnet",
    "Minuteurs",
    "Calendrier",
    "Horloge mondiale",
    "Système",
    "Concentration",
    "Raccourcis",
    "Chronomètre"
  };
  public static final String[] SKINS = {
    "Signature du bureau", "Verre", "Carte pleine", "Contour", "Minimal", "Néon"
  };

  public static final class Bounds {
    public final int width, height;

    public Bounds(int w, int h) {
      width = w;
      height = h;
    }
  }

  public static Bounds rail(int width, int height, int requested) {
    if (width < 1 || height < 1) return new Bounds(0, 0);
    if (width < height) {
      int available = height - 240;
      if (available < 80) return new Bounds(0, 0);
      return new Bounds(width, Math.min(120, available));
    }
    int available = width - 320;
    if (available < 144) return new Bounds(0, 0);
    int w = Math.min(available, Math.max(144, Math.min(208, requested)));
    return new Bounds(w, height);
  }

  private DashboardWidgets() {}

  public static JSONObject widget(String type, boolean enabled) throws JSONException {
    return new JSONObject()
        .put("id", type)
        .put("type", type)
        .put("enabled", enabled)
        .put("skin", 0)
        .put("size", 1)
        .put("opacity", 100)
        .put("radius", 18)
        .put("accent", 0)
        .put("textScale", 100)
        .put("title", "")
        .put("showTitle", true)
        .put("seconds", false)
        .put("hour24", true)
        .put("detail", true)
        .put("variant", 0)
        .put("timezone", "UTC")
        .put("themes", new JSONArray());
  }

  public static JSONObject defaults() throws JSONException {
    JSONArray list = new JSONArray();
    list.put(widget("weather", true));
    list.put(widget("music", true));
    list.put(widget("shortcuts", false));
    list.put(widget("clock", false));
    list.put(widget("battery", false));
    list.put(widget("tasks", true));
    list.put(widget("timers", false));
    list.put(widget("calendar", false));
    list.put(widget("notes", false));
    list.put(widget("world", false));
    list.put(widget("system", false));
    list.put(widget("focus", false));
    return new JSONObject().put("version", 1).put("side", 1).put("width", 176).put("widgets", list);
  }

  static void keys(JSONObject o, String... names) throws JSONException {
    Set<String> s = new HashSet<>(Arrays.asList(names));
    Iterator<String> it = o.keys();
    while (it.hasNext())
      if (!s.contains(it.next())) throw new JSONException("Option de widget inconnue");
  }

  public static JSONObject validate(JSONObject o) throws JSONException {
    if (o == null || o.toString().length() > 24000)
      throw new JSONException("Configuration de widgets trop grande");
    keys(o, "version", "side", "width", "widgets");
    HomeLayout.integer(o, "version", 1, 1);
    HomeLayout.integer(o, "side", 0, 2);
    HomeLayout.integer(o, "width", 144, 208);
    JSONArray list = o.getJSONArray("widgets");
    if (list.length() > 16) throw new JSONException("16 widgets maximum");
    Set<String> ids = new HashSet<>();
    for (int i = 0; i < list.length(); i++) {
      JSONObject w = list.getJSONObject(i);
      keys(
          w,
          "id",
          "type",
          "enabled",
          "skin",
          "size",
          "opacity",
          "radius",
          "accent",
          "textScale",
          "title",
          "showTitle",
          "timezone",
          "themes",
          "seconds",
          "hour24",
          "detail",
          "variant");
      if (!(w.get("id") instanceof String) || !(w.get("type") instanceof String))
        throw new JSONException("Identifiant et type attendus");
      String id = w.getString("id"), type = w.getString("type");
      if (!id.matches("[A-Za-z0-9_-]{1,40}")
          || !ids.add(id)
          || !Arrays.asList(TYPES).contains(type)) throw new JSONException("Widget invalide");
      for (String key : new String[] {"enabled", "showTitle", "seconds", "hour24", "detail"})
        if (!(w.get(key) instanceof Boolean)) throw new JSONException("Booléen de widget attendu");
      HomeLayout.integer(w, "variant", 0, 3);
      HomeLayout.integer(w, "skin", 0, 5);
      HomeLayout.integer(w, "size", 0, 2);
      HomeLayout.integer(w, "opacity", 30, 100);
      HomeLayout.integer(w, "radius", 0, 28);
      HomeLayout.integer(w, "accent", 0, 6);
      HomeLayout.integer(w, "textScale", 80, 140);
      if (!(w.get("title") instanceof String) || w.getString("title").length() > 40)
        throw new JSONException("Titre de widget trop long");
      for (char character : w.getString("title").toCharArray())
        if (Character.isISOControl(character)) throw new JSONException("Titre de widget invalide");
      if (!(w.get("timezone") instanceof String)) throw new JSONException("Fuseau invalide");
      String zone = w.getString("timezone");
      if (zone.length() > 80 || !Arrays.asList(TimeZone.getAvailableIDs()).contains(zone))
        throw new JSONException("Fuseau invalide");
      JSONArray themes = w.getJSONArray("themes");
      if (themes.length() > 22) throw new JSONException("Liste de styles invalide");
      Set<Integer> used = new HashSet<>();
      for (int j = 0; j < themes.length(); j++) {
        Object n = themes.get(j);
        if (!(n instanceof Number)
            || ((Number) n).doubleValue() != ((Number) n).intValue()
            || ((Number) n).intValue() < 0
            || ((Number) n).intValue() > 21
            || !used.add(((Number) n).intValue())) throw new JSONException("Style invalide");
      }
    }
    return new JSONObject(o.toString());
  }

  public static boolean visible(JSONObject widget, int theme) {
    if (!widget.optBoolean("enabled")) return false;
    JSONArray list = widget.optJSONArray("themes");
    if (list == null || list.length() == 0) return true;
    for (int i = 0; i < list.length(); i++) if (list.optInt(i, -1) == theme) return true;
    return false;
  }

  public static String name(String type) {
    int i = Arrays.asList(TYPES).indexOf(type);
    return i < 0 ? type : NAMES[i];
  }
}

package fr.dm100.prisme;

import java.util.*;
import org.json.*;

/** Bounded declarative home layout; no actions, code or file paths from theme packs. */
public final class HomeLayout {
  public static final String[] KINDS = {
    "clock", "tasks", "timer", "music", "notes", "favorites", "device"
  };
  public static final String[] THEMES = {
    "Graphite", "Verre", "Papier", "Instrument", "Néon", "Minimal"
  };

  public static final class Card {
    public final String kind;
    public final int x, y, w, h;

    public Card(String kind, int x, int y, int w, int h) {
      this.kind = kind;
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
    }

    public JSONObject json() throws JSONException {
      return new JSONObject().put("kind", kind).put("x", x).put("y", y).put("w", w).put("h", h);
    }
  }

  public static JSONObject defaults() throws JSONException {
    JSONArray pages = new JSONArray();
    pages.put(
        page(
            "Principal",
            new Card("clock", 0, 0, 2, 2),
            new Card("tasks", 2, 0, 2, 1),
            new Card("music", 2, 1, 2, 1),
            new Card("timer", 0, 2, 2, 1),
            new Card("notes", 2, 2, 2, 1),
            new Card("favorites", 0, 3, 4, 1)));
    pages.put(
        page(
            "Travail",
            new Card("clock", 0, 0, 2, 1),
            new Card("timer", 2, 0, 2, 1),
            new Card("tasks", 0, 1, 4, 2),
            new Card("notes", 0, 3, 4, 1)));
    pages.put(
        page(
            "Multimédia",
            new Card("clock", 0, 0, 2, 1),
            new Card("device", 2, 0, 2, 1),
            new Card("music", 0, 1, 4, 2),
            new Card("favorites", 0, 3, 4, 1)));
    return new JSONObject()
        .put("version", 1)
        .put("theme", 0)
        .put("largeText", false)
        .put("pages", pages);
  }

  private static JSONObject page(String name, Card... cards) throws JSONException {
    JSONArray a = new JSONArray();
    for (Card c : cards) a.put(c.json());
    return new JSONObject().put("name", name).put("cards", a);
  }

  static int integer(JSONObject o, String key, int low, int high) throws JSONException {
    Object v = o.get(key);
    if (!(v instanceof Number)) throw new JSONException("Nombre attendu : " + key);
    double n = ((Number) v).doubleValue();
    int i = ((Number) v).intValue();
    if (!Double.isFinite(n) || i != n || i < low || i > high)
      throw new JSONException("Hors limites : " + key);
    return i;
  }

  private static void keys(JSONObject o, String... allowed) throws JSONException {
    Set<String> s = new HashSet<>(Arrays.asList(allowed));
    Iterator<String> i = o.keys();
    while (i.hasNext())
      if (!s.contains(i.next())) throw new JSONException("Option de disposition inconnue");
  }

  public static JSONObject validate(JSONObject o) throws JSONException {
    if (o == null || o.toString().length() > 12000)
      throw new JSONException("Disposition trop grande");
    keys(o, "version", "theme", "largeText", "pages");
    integer(o, "version", 1, 1);
    integer(o, "theme", 0, 5);
    if (!(o.get("largeText") instanceof Boolean))
      throw new JSONException("Taille de texte invalide");
    JSONArray pages = o.getJSONArray("pages");
    if (pages.length() < 1 || pages.length() > 5) throw new JSONException("1 à 5 pages");
    for (int p = 0; p < pages.length(); p++) {
      JSONObject page = pages.getJSONObject(p);
      keys(page, "name", "cards");
      Object name = page.get("name");
      if (!(name instanceof String)
          || ((String) name).trim().isEmpty()
          || ((String) name).length() > 24) throw new JSONException("Nom de page invalide");
      for (char c : ((String) name).toCharArray())
        if (Character.isISOControl(c)) throw new JSONException("Nom de page invalide");
      JSONArray cards = page.getJSONArray("cards");
      if (cards.length() > 8) throw new JSONException("8 blocs maximum");
      boolean[][] used = new boolean[4][4];
      for (int i = 0; i < cards.length(); i++) {
        JSONObject c = cards.getJSONObject(i);
        keys(c, "kind", "x", "y", "w", "h");
        if (!(c.get("kind") instanceof String)
            || !Arrays.asList(KINDS).contains(c.getString("kind")))
          throw new JSONException("Bloc inconnu");
        int x = integer(c, "x", 0, 3),
            y = integer(c, "y", 0, 3),
            w = integer(c, "w", 1, 4),
            h = integer(c, "h", 1, 4);
        if (x + w > 4 || y + h > 4) throw new JSONException("Bloc hors écran");
        for (int a = x; a < x + w; a++)
          for (int b = y; b < y + h; b++) {
            if (used[a][b]) throw new JSONException("Blocs superposés");
            used[a][b] = true;
          }
      }
    }
    return new JSONObject(o.toString());
  }
}

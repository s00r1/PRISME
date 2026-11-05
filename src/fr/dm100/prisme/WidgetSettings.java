package fr.dm100.prisme;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputFilter;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Watch-sized widget catalogue and editor. Open editors keep a stable widget identity. */
public final class WidgetSettings {
  private WidgetSettings() {}

  private interface Change {
    void run() throws JSONException;
  }

  private interface Selection {
    void changed(int value);
  }

  private static void apply(Activity activity, Change mutation, Runnable changed, Runnable redraw) {
    try {
      mutation.run();
      changed.run();
      if (redraw != null) redraw.run();
    } catch (JSONException error) {
      Toast.makeText(activity, error.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  /** Keep the current scroll position when a label, list order or catalogue entry changes. */
  private static void redraw(LinearLayout root, Runnable render) {
    ViewParent parent = root.getParent();
    ScrollView scroll = parent instanceof ScrollView ? (ScrollView) parent : null;
    int y = scroll == null ? 0 : scroll.getScrollY();
    render.run();
    if (scroll != null) scroll.post(() -> scroll.scrollTo(0, y));
  }

  public static void open(Activity activity, Runnable changed) {
    WidgetStore store = new WidgetStore(activity);
    LinearLayout root = V6Ui.column(activity);
    Runnable[] render = new Runnable[1];
    render[0] =
        () -> {
          root.removeAllViews();
          JSONObject layout = store.read();
          JSONArray widgets = layout.optJSONArray("widgets");
          root.addView(
              V6Ui.text(
                  activity,
                  "Une zone dédiée aux widgets, commune aux 22 bureaux. Ajouter, retirer,"
                      + " réordonner ou personnaliser chaque widget. Un appui long sur le widget"
                      + " ouvre ses réglages.",
                  16));
          V6Ui.add(
              root,
              "Ajouter un widget · " + widgets.length() + "/16",
              () -> {
                if (widgets.length() >= 16) {
                  Toast.makeText(
                          activity,
                          "16 widgets maximum : retirer un widget avant d’en ajouter un.",
                          Toast.LENGTH_LONG)
                      .show();
                  return;
                }
                new V6Ui.Builder(activity)
                    .setTitle("Catalogue de widgets")
                    .setItems(
                        DashboardWidgets.NAMES,
                        (dialog, selected) ->
                            apply(
                                activity,
                                () -> {
                                  JSONObject next = store.read();
                                  next.getJSONArray("widgets")
                                      .put(
                                          DashboardWidgets.widget(
                                                  DashboardWidgets.TYPES[selected], true)
                                              .put("id", UUID.randomUUID().toString()));
                                  store.save(next);
                                },
                                changed,
                                () -> redraw(root, render[0])))
                    .setNegativeButton("Annuler", null)
                    .show();
              });
          String[] positions = {"Gauche", "Droite", "Masquée"};
          V6Ui.add(
              root,
              "Zone · " + positions[layout.optInt("side", 1)],
              () ->
                  choose(
                      activity,
                      "Position de la zone",
                      new String[] {"À gauche", "À droite", "Masquer la zone"},
                      selected ->
                          apply(
                              activity,
                              () -> {
                                JSONObject next = store.read();
                                next.put("side", selected);
                                store.save(next);
                              },
                              changed,
                              () -> redraw(root, render[0]))));
          V6Ui.add(
              root,
              "Largeur paysage · " + layout.optInt("width", 176) + " px",
              () ->
                  choose(
                      activity,
                      "Largeur en paysage",
                      new String[] {"Compacte · 144 px", "Équilibrée · 176 px", "Confort · 208 px"},
                      selected ->
                          apply(
                              activity,
                              () -> {
                                JSONObject next = store.read();
                                next.put("width", new int[] {144, 176, 208}[selected]);
                                store.save(next);
                              },
                              changed,
                              () -> redraw(root, render[0]))));
          root.addView(
              V6Ui.text(
                  activity,
                  "En portrait, la zone devient une bande horizontale. Le contenu des bureaux garde"
                      + " sa propre surface et ses commandes.",
                  14));
          for (int i = 0; i < widgets.length(); i++) {
            JSONObject widget = widgets.optJSONObject(i);
            String id = widget.optString("id");
            LinearLayout row = new LinearLayout(activity);
            CheckBox enabled = new CheckBox(activity);
            String custom = widget.optString("title");
            enabled.setText(
                custom.isEmpty() ? DashboardWidgets.name(widget.optString("type")) : custom);
            enabled.setTextColor(V6Ui.INK);
            enabled.setMinHeight(52);
            enabled.setChecked(widget.optBoolean("enabled"));
            enabled.setOnCheckedChangeListener(
                (view, yes) -> apply(activity, () -> store.set(id, "enabled", yes), changed, null));
            row.addView(enabled, new LinearLayout.LayoutParams(0, 56, 1));
            row.addView(
                V6Ui.button(
                    activity,
                    "Régler",
                    () ->
                        customizeId(
                            activity,
                            id,
                            () -> {
                              changed.run();
                              redraw(root, render[0]);
                            })),
                new LinearLayout.LayoutParams(104, 52));
            root.addView(row);
          }
          V6Ui.add(
              root,
              "Annuler / rétablir la dernière modification",
              () -> {
                if (store.undo()) {
                  changed.run();
                  redraw(root, render[0]);
                } else
                  Toast.makeText(activity, "Aucune modification à annuler", Toast.LENGTH_SHORT)
                      .show();
              });
          V6Ui.add(
              root, "Ville et météo connectée", () -> WeatherPanel.settings(activity, changed));
        };
    render[0].run();
    V6Ui.dialog(activity, "Widgets · 22 styles", root);
  }

  /** Legacy callers may pass the current position; it is resolved once to the permanent ID. */
  public static void customize(Activity activity, int index, Runnable changed) {
    JSONObject widget =
        new WidgetStore(activity).read().optJSONArray("widgets").optJSONObject(index);
    if (widget == null) {
      Toast.makeText(activity, "Widget retiré : rouvrir le catalogue", Toast.LENGTH_SHORT).show();
      return;
    }
    customizeId(activity, widget.optString("id"), changed);
  }

  public static void customizeId(Activity activity, String id, Runnable changed) {
    WidgetStore store = new WidgetStore(activity);
    LinearLayout root = V6Ui.column(activity);
    Runnable[] render = new Runnable[1];
    AlertDialog[] owner = new AlertDialog[1];
    render[0] =
        () -> {
          root.removeAllViews();
          final JSONObject widget;
          try {
            widget = store.find(id);
          } catch (JSONException removed) {
            root.addView(
                V6Ui.text(activity, "Ce widget a été retiré. Fermer et rouvrir le catalogue.", 17));
            return;
          }
          String type = widget.optString("type");
          Runnable redraw = () -> redraw(root, render[0]);
          root.addView(
              V6Ui.text(activity, DashboardWidgets.name(type) + " · réglages indépendants", 18));
          V6Ui.add(
              root,
              "Apparence · " + DashboardWidgets.SKINS[widget.optInt("skin")],
              () ->
                  choose(
                      activity,
                      "Apparence",
                      DashboardWidgets.SKINS,
                      value ->
                          apply(activity, () -> store.set(id, "skin", value), changed, redraw)));
          String[] sizes = {"Compacte", "Équilibrée", "Détaillée"};
          V6Ui.add(
              root,
              "Taille · " + sizes[widget.optInt("size", 1)],
              () ->
                  choose(
                      activity,
                      "Densité du widget",
                      sizes,
                      value ->
                          apply(activity, () -> store.set(id, "size", value), changed, redraw)));
          String[] accents = {
            "Selon le bureau", "Menthe", "Bleu", "Lavande", "Orange", "Rose", "Or"
          };
          V6Ui.add(
              root,
              "Accent · " + accents[widget.optInt("accent")],
              () ->
                  choose(
                      activity,
                      "Couleur d’accent",
                      accents,
                      value ->
                          apply(activity, () -> store.set(id, "accent", value), changed, redraw)));
          slider(
              activity,
              root,
              "Opacité du fond",
              30,
              100,
              widget.optInt("opacity", 100),
              value -> apply(activity, () -> store.set(id, "opacity", value), changed, null));
          slider(
              activity,
              root,
              "Arrondis",
              0,
              28,
              widget.optInt("radius", 18),
              value -> apply(activity, () -> store.set(id, "radius", value), changed, null));
          slider(
              activity,
              root,
              "Taille du texte",
              80,
              140,
              widget.optInt("textScale", 100),
              value -> apply(activity, () -> store.set(id, "textScale", value), changed, null));
          toggle(
              activity,
              root,
              "Afficher le titre",
              widget.optBoolean("showTitle", true),
              yes -> apply(activity, () -> store.set(id, "showTitle", yes), changed, null));
          V6Ui.add(
              root,
              "Titre personnalisé",
              () -> {
                EditText input = new EditText(activity);
                input.setSingleLine(true);
                input.setText(widget.optString("title"));
                input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(40)});
                new V6Ui.Builder(activity)
                    .setTitle("Nom du widget")
                    .setView(input)
                    .setPositiveButton(
                        "Enregistrer",
                        (dialog, which) ->
                            apply(
                                activity,
                                () -> store.set(id, "title", input.getText().toString()),
                                changed,
                                redraw))
                    .setNegativeButton("Annuler", null)
                    .show();
              });
          V6Ui.add(
              root,
              "Styles d’affichage · "
                  + (widget.optJSONArray("themes").length() == 0
                      ? "Tous"
                      : widget.optJSONArray("themes").length()),
              () -> editThemes(activity, store, id, changed, redraw));
          String[] presentations =
              type.equals("clock") || type.equals("world")
                  ? new String[] {"Digitale", "Analogique", "Grands chiffres", "Date et heure"}
                  : type.equals("weather")
                      ? new String[] {
                        "Actuelle et tendance", "Compacte", "Cinq jours", "Prochaines heures"
                      }
                      : new String[] {"Équilibrée", "Compacte", "Étendue", "Minimaliste"};
          V6Ui.add(
              root,
              "Présentation · " + presentations[widget.optInt("variant")],
              () ->
                  choose(
                      activity,
                      "Présentation du widget",
                      presentations,
                      value ->
                          apply(activity, () -> store.set(id, "variant", value), changed, redraw)));
          toggle(
              activity,
              root,
              "Afficher les détails",
              widget.optBoolean("detail", true),
              yes -> apply(activity, () -> store.set(id, "detail", yes), changed, null));
          if (type.equals("clock") || type.equals("world")) {
            toggle(
                activity,
                root,
                "Afficher les secondes",
                widget.optBoolean("seconds"),
                yes -> apply(activity, () -> store.set(id, "seconds", yes), changed, null));
            toggle(
                activity,
                root,
                "Format 24 heures",
                widget.optBoolean("hour24", true),
                yes -> apply(activity, () -> store.set(id, "hour24", yes), changed, null));
          }
          if (type.equals("weather"))
            V6Ui.add(
                root,
                "Ville, prévisions et unités",
                () -> WeatherPanel.settings(activity, changed));
          if (type.equals("world")) {
            String[] zones = {
              "UTC",
              "Europe/Paris",
              "Europe/London",
              "America/New_York",
              "America/Los_Angeles",
              "Asia/Tokyo",
              "Asia/Shanghai",
              "Asia/Dubai",
              "Australia/Sydney"
            };
            V6Ui.add(
                root,
                "Fuseau · " + widget.optString("timezone", "UTC"),
                () ->
                    choose(
                        activity,
                        "Fuseau de l’horloge",
                        zones,
                        selected ->
                            apply(
                                activity,
                                () -> store.set(id, "timezone", zones[selected]),
                                changed,
                                redraw)));
          }
          V6Ui.add(
              root,
              "Monter dans la liste",
              () -> apply(activity, () -> store.move(id, -1), changed, redraw));
          V6Ui.add(
              root,
              "Descendre dans la liste",
              () -> apply(activity, () -> store.move(id, 1), changed, redraw));
          V6Ui.add(
              root,
              "Retirer ce widget",
              () ->
                  new V6Ui.Builder(activity)
                      .setTitle("Retirer le widget ?")
                      .setMessage("Les données de l’outil restent conservées.")
                      .setPositiveButton(
                          "Retirer",
                          (dialog, which) ->
                              apply(
                                  activity,
                                  () -> store.remove(id),
                                  changed,
                                  () -> {
                                    if (owner[0] != null) owner[0].dismiss();
                                  }))
                      .setNegativeButton("Annuler", null)
                      .show());
        };
    render[0].run();
    owner[0] = V6Ui.dialog(activity, "Personnaliser le widget", root);
  }

  private static void editThemes(
      Activity activity, WidgetStore store, String id, Runnable changed, Runnable redraw) {
    try {
      JSONObject widget = store.find(id);
      String[] names = new String[ThemeSpec.ALL.length];
      boolean[] checked = new boolean[names.length];
      JSONArray selected = widget.getJSONArray("themes");
      for (int i = 0; i < names.length; i++) {
        names[i] = ThemeSpec.ALL[i].name;
        checked[i] = selected.length() == 0;
        for (int j = 0; j < selected.length(); j++) if (selected.optInt(j) == i) checked[i] = true;
      }
      new V6Ui.Builder(activity)
          .setTitle("Styles du widget")
          .setMultiChoiceItems(names, checked, (dialog, index, yes) -> checked[index] = yes)
          .setPositiveButton(
              "Appliquer",
              (dialog, which) ->
                  apply(
                      activity,
                      () -> {
                        JSONArray themes = new JSONArray();
                        for (int i = 0; i < checked.length; i++) if (checked[i]) themes.put(i);
                        JSONObject layout = store.read();
                        JSONObject value =
                            layout
                                .getJSONArray("widgets")
                                .getJSONObject(WidgetStore.indexOf(layout, id));
                        value.put("themes", themes);
                        if (themes.length() == 0) value.put("enabled", false);
                        store.save(layout);
                      },
                      changed,
                      redraw))
          .setNegativeButton("Annuler", null)
          .show();
    } catch (JSONException missing) {
      Toast.makeText(activity, missing.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  private interface Flag {
    void changed(boolean yes);
  }

  private static void toggle(
      Activity activity, LinearLayout root, String label, boolean value, Flag action) {
    CheckBox checkbox = new CheckBox(activity);
    checkbox.setText(label);
    checkbox.setTextColor(V6Ui.INK);
    checkbox.setMinHeight(48);
    checkbox.setChecked(value);
    checkbox.setOnCheckedChangeListener((button, yes) -> action.changed(yes));
    root.addView(checkbox);
  }

  private static void choose(Activity activity, String title, String[] values, Selection selected) {
    new V6Ui.Builder(activity)
        .setTitle(title)
        .setItems(values, (dialog, index) -> selected.changed(index))
        .setNegativeButton("Fermer", null)
        .show();
  }

  /**
   * Save once on release: dragging does not flood backups, redraws or the one-step undo history.
   */
  private static void slider(
      Activity activity,
      LinearLayout root,
      String name,
      int min,
      int max,
      int value,
      Selection changed) {
    TextView label = V6Ui.text(activity, name + " · " + value, 16);
    root.addView(label);
    SeekBar slider = new SeekBar(activity);
    slider.setMax(max - min);
    slider.setProgress(value - min);
    slider.setContentDescription(name);
    slider.setMinimumHeight(48);
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          private boolean modified;

          public void onProgressChanged(SeekBar bar, int progress, boolean user) {
            if (user) {
              modified = true;
              label.setText(name + " · " + (progress + min));
            }
          }

          public void onStartTrackingTouch(SeekBar bar) {
            modified = false;
          }

          public void onStopTrackingTouch(SeekBar bar) {
            if (modified) changed.changed(bar.getProgress() + min);
          }
        });
    root.addView(slider, new LinearLayout.LayoutParams(-1, 48));
  }
}

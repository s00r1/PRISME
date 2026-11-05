package fr.dm100.prisme;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.text.*;
import java.util.*;
import org.json.*;

/** Native weather detail and explicit opt-in, with dated real provider data. */
public final class WeatherPanel {
  private static final int MUTED = 0xffadc3d8, LINE = 0xff344e67;

  private WeatherPanel() {}

  static boolean alive(Activity a) {
    return !a.isFinishing() && !a.isDestroyed();
  }

  public static String number(double n, String unit) {
    return Double.isFinite(n) ? String.format(I18n.locale(), "%.0f", n) + unit : "—";
  }

  static double temp(double c, boolean f) {
    return f ? c * 9 / 5 + 32 : c;
  }

  private static void update(
      Activity a, LinearLayout details, TextView status, WeatherData data, String error) {
    render(a, details, data);
    String cache =
        data == null
            ? "Aucun relevé en cache"
            : "Reçu "
                + new SimpleDateFormat("dd/MM HH:mm", I18n.locale())
                    .format(new Date(data.fetchedAt))
                + (data.stale(System.currentTimeMillis()) ? " · CACHE ANCIEN" : " · cache local");
    String network = WeatherRepository.allowed(a) ? "" : "Réseau météo désactivé · ";
    status.setText(network + (error == null || error.isEmpty() ? "" : error + "\n") + cache);
  }

  public static void open(Activity a) {
    LinearLayout root = V6Ui.column(a), details = V6Ui.column(a);
    details.setPadding(0, 4, 0, 4);
    TextView status = label(a, "", 12, MUTED);
    status.setPadding(10, 8, 10, 8);
    status.setBackground(V6Ui.surface(V6Ui.SURFACE, LINE));
    root.addView(status);
    root.addView(details);
    AlertDialog[] owner = new AlertDialog[1];
    Runnable refresh =
        () -> {
          status.setText("Actualisation… · le dernier cache reste affiché");
          WeatherRepository.refresh(
              a,
              true,
              (data, error) -> {
                if (alive(a) && owner[0] != null && owner[0].isShowing())
                  update(a, details, status, data, error);
              });
        };
    LinearLayout actions = new LinearLayout(a);
    Button reload = V6Ui.button(a, "Actualiser", refresh),
        settings =
            V6Ui.button(
                a,
                "Ville et réglages",
                () ->
                    settings(
                        a,
                        () -> {
                          if (alive(a) && owner[0] != null && owner[0].isShowing())
                            update(
                                a,
                                details,
                                status,
                                WeatherRepository.cached(a),
                                WeatherRepository.status(a));
                        }));
    LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(0, 48, 1);
    actionLp.rightMargin = 6;
    actions.addView(reload, actionLp);
    actions.addView(settings, new LinearLayout.LayoutParams(0, 48, 1));
    root.addView(actions);
    V6Ui.add(
        root,
        "Données météo · Open-Meteo.com",
        () -> {
          try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/")));
          } catch (RuntimeException unavailable) {
            Toast.makeText(
                    a, "Aucun navigateur disponible pour ouvrir Open-Meteo.com", Toast.LENGTH_LONG)
                .show();
          }
        });
    owner[0] = V6Ui.dialog(a, "Météo", root);
    update(a, details, status, WeatherRepository.cached(a), WeatherRepository.status(a));
    WeatherRepository.refresh(
        a,
        false,
        (data, error) -> {
          if (alive(a) && owner[0].isShowing()) update(a, details, status, data, error);
        });
  }

  private static TextView label(Activity a, String value, int size, int color) {
    TextView t = V6Ui.text(a, value, size);
    t.setTextColor(color);
    return t;
  }

  private static LinearLayout card(Activity a) {
    LinearLayout card = new LinearLayout(a);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(8, 8, 8, 8);
    card.setBackground(V6Ui.surface(V6Ui.SURFACE, LINE));
    return card;
  }

  private static void gap(LinearLayout root, View view) {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = 8;
    root.addView(view, lp);
  }

  private static void section(Activity a, LinearLayout root, String title) {
    TextView t = label(a, title, 12, V6Ui.ACCENT);
    t.setTypeface(null, Typeface.BOLD);
    t.setPadding(8, 14, 8, 8);
    root.addView(t);
  }

  private static void metric(Activity a, LinearLayout row, String title, String value) {
    LinearLayout col = card(a);
    col.addView(label(a, title, 11, MUTED));
    col.addView(label(a, value, 17, V6Ui.INK));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
    lp.setMargins(2, 2, 2, 2);
    row.addView(col, lp);
  }

  private static String at(JSONArray values, int index, String suffix) {
    return number(values == null ? Double.NaN : values.optDouble(index, Double.NaN), suffix);
  }

  private static String clock(JSONArray values, int index, TimeZone zone) {
    long time = values == null ? 0 : values.optLong(index, 0);
    if (time <= 0) return "—";
    SimpleDateFormat format = new SimpleDateFormat("HH:mm", I18n.locale());
    format.setTimeZone(zone);
    return format.format(new Date(time * 1000L));
  }

  static void render(Activity a, LinearLayout root, WeatherData data) {
    root.removeAllViews();
    if (data == null) {
      LinearLayout empty = card(a);
      empty.addView(label(a, "Votre météo, à votre rythme", 24, V6Ui.INK));
      empty.addView(
          label(
              a,
              WeatherRepository.allowed(a)
                  ? "Choisissez une ville dans les réglages. Le prochain relevé sera conservé sur"
                      + " la montre."
                  : "Activez la météo seulement si vous le souhaitez, puis choisissez une ville."
                      + " Aucun GPS ni compte requis.",
              16,
              MUTED));
      gap(root, empty);
      return;
    }
    boolean f = WeatherRepository.fahrenheit(a);
    JSONObject current = data.raw.optJSONObject("current"), daily = data.raw.optJSONObject("daily");
    TimeZone zone =
        TimeZone.getTimeZone(
            data.raw.optString(
                "timezone", PrefValues.text(WeatherRepository.options(a), "timezone", "UTC")));
    LinearLayout hero = card(a);
    GradientDrawable sky =
        new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] {data.day() ? 0xff214969 : 0xff26334f, 0xff152438});
    sky.setCornerRadius(24);
    hero.setBackground(sky);
    TextView city = label(a, data.city, 21, V6Ui.INK);
    city.setTypeface(null, Typeface.BOLD);
    hero.addView(city);
    LinearLayout now = new LinearLayout(a);
    now.setGravity(Gravity.CENTER_VERTICAL);
    WeatherIcon icon = new WeatherIcon(a);
    icon.weather(data.code(), data.day());
    now.addView(icon, new LinearLayout.LayoutParams(92, 86));
    LinearLayout words = new LinearLayout(a);
    words.setOrientation(LinearLayout.VERTICAL);
    words.addView(label(a, data.temperatureText(f), 44, V6Ui.INK));
    words.addView(label(a, WeatherData.condition(data.code()), 18, V6Ui.INK));
    now.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
    hero.addView(now);
    gap(root, hero);
    LinearLayout metrics = new LinearLayout(a);
    metric(
        a,
        metrics,
        "RESSENTI",
        number(temp(current.optDouble("apparent_temperature", Double.NaN), f), "°"));
    metric(
        a,
        metrics,
        "HUMIDITÉ",
        number(current.optDouble("relative_humidity_2m", Double.NaN), " %"));
    metric(a, metrics, "VENT", number(current.optDouble("wind_speed_10m", Double.NaN), " km/h"));
    gap(root, metrics);
    JSONObject hourly = data.raw.optJSONObject("hourly");
    if (hourly != null) {
      JSONArray times = hourly.optJSONArray("time"),
          temps = hourly.optJSONArray("temperature_2m"),
          rain = hourly.optJSONArray("precipitation_probability");
      if (times != null && temps != null) {
        LinearLayout hours = new LinearLayout(a);
        SimpleDateFormat hour = new SimpleDateFormat("HH:mm", I18n.locale());
        hour.setTimeZone(zone);
        int shown = 0;
        for (int i = 0; i < Math.min(times.length(), temps.length()) && shown < 12; i++) {
          long time = times.optLong(i, 0) * 1000L;
          if (time <= 0 || time < System.currentTimeMillis() - 1800000) continue;
          LinearLayout tile = card(a);
          tile.setGravity(Gravity.CENTER_HORIZONTAL);
          tile.addView(label(a, hour.format(new Date(time)), 13, MUTED));
          TextView temperature =
              label(a, number(temp(temps.optDouble(i, Double.NaN), f), "°"), 24, V6Ui.INK);
          temperature.setTypeface(null, Typeface.BOLD);
          tile.addView(temperature);
          tile.addView(label(a, "Pluie " + at(rain, i, " %"), 11, V6Ui.ACCENT));
          LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(96, -2);
          lp.rightMargin = 6;
          hours.addView(tile, lp);
          shown++;
        }
        if (shown > 0) {
          section(a, root, "PROCHAINES HEURES · GLISSER");
          HorizontalScrollView scroll = new HorizontalScrollView(a);
          scroll.setHorizontalScrollBarEnabled(false);
          scroll.addView(hours);
          gap(root, scroll);
        }
      }
    }
    if (daily != null) {
      JSONArray times = daily.optJSONArray("time"),
          max = daily.optJSONArray("temperature_2m_max"),
          min = daily.optJSONArray("temperature_2m_min"),
          codes = daily.optJSONArray("weather_code"),
          rain = daily.optJSONArray("precipitation_probability_max"),
          uv = daily.optJSONArray("uv_index_max"),
          rise = daily.optJSONArray("sunrise"),
          set = daily.optJSONArray("sunset");
      if (uv != null || rise != null || set != null) {
        section(a, root, "SOLEIL · HEURE DE LA VILLE");
        LinearLayout sun = new LinearLayout(a);
        metric(a, sun, "LEVER", clock(rise, 0, zone));
        metric(a, sun, "COUCHER", clock(set, 0, zone));
        metric(a, sun, "UV MAX", at(uv, 0, ""));
        gap(root, sun);
      }
      if (times != null && max != null && min != null) {
        section(a, root, "CINQ JOURS");
        SimpleDateFormat day = new SimpleDateFormat("EEE d MMM", I18n.locale());
        day.setTimeZone(zone);
        for (int i = 0;
            i < Math.min(5, Math.min(times.length(), Math.min(max.length(), min.length())));
            i++) {
          long time = times.optLong(i, 0) * 1000L;
          if (time <= 0) continue;
          LinearLayout row = card(a);
          row.setOrientation(LinearLayout.HORIZONTAL);
          row.setGravity(Gravity.CENTER_VERTICAL);
          int code = codes == null ? -1 : codes.optInt(i, -1);
          if (code >= 0) {
            WeatherIcon small = new WeatherIcon(a);
            small.weather(code, true);
            row.addView(small, new LinearLayout.LayoutParams(42, 48));
          }
          LinearLayout copy = new LinearLayout(a);
          copy.setOrientation(LinearLayout.VERTICAL);
          copy.addView(label(a, day.format(new Date(time)), 15, V6Ui.INK));
          copy.addView(
              label(
                  a,
                  WeatherData.condition(code)
                      + (rain == null ? "" : " · Pluie " + at(rain, i, " %")),
                  11,
                  MUTED));
          row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
          TextView range =
              label(
                  a,
                  number(temp(min.optDouble(i, Double.NaN), f), "°")
                      + " / "
                      + number(temp(max.optDouble(i, Double.NaN), f), "°"),
                  17,
                  V6Ui.INK);
          row.addView(range, new LinearLayout.LayoutParams(-2, -2));
          gap(root, row);
        }
      }
    }
  }

  public static void settings(Activity a, Runnable changed) {
    LinearLayout root = V6Ui.column(a);
    Runnable[] render = new Runnable[1];
    AlertDialog[] owner = new AlertDialog[1];
    Runnable notify =
        () -> {
          changed.run();
          if (alive(a) && owner[0] != null && owner[0].isShowing()) render[0].run();
        };
    render[0] =
        () -> {
          root.removeAllViews();
          root.addView(label(a, "Une ville choisie, aucune position GPS", 21, V6Ui.ACCENT));
          root.addView(
              V6Ui.text(
                  a,
                  "Open-Meteo reçoit l’adresse IP et la ville recherchée ou ses coordonnées. Aucun"
                      + " compte. API gratuite pour les usages non commerciaux.",
                  14));
          V6Ui.add(
              root,
              WeatherRepository.allowed(a)
                  ? "Connectée · désactiver le réseau"
                  : "Activer la météo connectée",
              () -> {
                if (WeatherRepository.allowed(a)) {
                  WeatherRepository.disable(a);
                  notify.run();
                  Toast.makeText(a, "Réseau météo désactivé · cache conservé", Toast.LENGTH_SHORT)
                      .show();
                } else
                  new V6Ui.Builder(a)
                      .setTitle("Activer Open-Meteo ?")
                      .setMessage(
                          "Des requêtes HTTPS seront envoyées pour rechercher votre ville et"
                              + " recevoir les prévisions. Les autres données PRISME restent"
                              + " locales.")
                      .setPositiveButton(
                          "Activer",
                          (d, w) -> {
                            a.getSharedPreferences("prisme_weather_consent", 0)
                                .edit()
                                .putBoolean("enabled", true)
                                .commit();
                            notify.run();
                            chooseCity(a, notify);
                          })
                      .setNegativeButton("Annuler", null)
                      .show();
              });
          V6Ui.add(
              root,
              "Ville · "
                  + (WeatherRepository.city(a).isEmpty() ? "à choisir" : WeatherRepository.city(a)),
              () -> chooseCity(a, notify));
          CheckBox units = new CheckBox(a);
          units.setText("Température en Fahrenheit (°F)");
          units.setTextColor(V6Ui.INK);
          units.setMinHeight(48);
          units.setChecked(WeatherRepository.fahrenheit(a));
          units.setOnCheckedChangeListener(
              (v, yes) -> {
                WeatherRepository.options(a).edit().putBoolean("fahrenheit", yes).apply();
                changed.run();
              });
          root.addView(units);
          V6Ui.add(
              root,
              "Actualisation au premier plan · "
                  + PrefValues.number(WeatherRepository.options(a), "interval", 45)
                  + " min",
              () ->
                  new V6Ui.Builder(a)
                      .setTitle("Fréquence au premier plan")
                      .setItems(
                          new String[] {
                            "15 minutes",
                            "30 minutes",
                            "45 minutes",
                            "60 minutes",
                            "120 minutes",
                            "180 minutes"
                          },
                          (d, n) -> {
                            WeatherRepository.options(a)
                                .edit()
                                .putInt("interval", new int[] {15, 30, 45, 60, 120, 180}[n])
                                .apply();
                            notify.run();
                          })
                      .show());
          root.addView(
              label(
                  a,
                  "Le cache garde sa date. Une connexion absente ne devient jamais une mesure"
                      + " actuelle. Les prévisions dépendent du fournisseur.",
                  13,
                  MUTED));
        };
    render[0].run();
    owner[0] = V6Ui.dialog(a, "Réglages météo", root);
  }

  public static void chooseCity(Activity a, Runnable changed) {
    if (!WeatherRepository.allowed(a)) {
      Toast.makeText(a, "Activer d’abord la météo connectée", Toast.LENGTH_SHORT).show();
      return;
    }
    LinearLayout root = V6Ui.column(a);
    EditText input = new EditText(a);
    input.setTextColor(V6Ui.INK);
    input.setHintTextColor(MUTED);
    input.setHint("Ville, par exemple Lyon");
    input.setSingleLine();
    input.setMinHeight(48);
    input.setImeOptions(
        EditorInfo.IME_FLAG_NO_EXTRACT_UI
            | EditorInfo.IME_FLAG_NO_FULLSCREEN
            | EditorInfo.IME_ACTION_SEARCH);
    input.setFilters(
        new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(80)});
    root.addView(input);
    TextView state = V6Ui.text(a, "Recherche manuelle · aucun GPS", 14);
    root.addView(state);
    LinearLayout results = V6Ui.column(a);
    root.addView(results);
    AlertDialog[] owner = new AlertDialog[1];
    int[] token = {0};
    Runnable search =
        () -> {
          int request = ++token[0];
          state.setText("Recherche…");
          results.removeAllViews();
          WeatherRepository.search(
              a,
              input.getText().toString(),
              (cities, error) -> {
                if (!alive(a) || owner[0] == null || !owner[0].isShowing() || request != token[0])
                  return;
                results.removeAllViews();
                state.setText(
                    error != null
                        ? error
                        : cities.length() == 0 ? "Aucune ville trouvée" : "Choisir une ville");
                for (int i = 0; i < cities.length(); i++) {
                  JSONObject city = cities.optJSONObject(i);
                  V6Ui.add(
                      results,
                      city.optString("name")
                          + " · "
                          + city.optString("region")
                          + " · "
                          + city.optString("country"),
                      () -> {
                        try {
                          WeatherRepository.select(a, city);
                          token[0]++;
                          results.removeAllViews();
                          state.setText("Ville enregistrée · actualisation…");
                          changed.run();
                          WeatherRepository.refresh(
                              a,
                              true,
                              (data, failure) -> {
                                if (alive(a)) {
                                  changed.run();
                                  if (owner[0].isShowing()) {
                                    state.setText(
                                        failure == null && data != null
                                            ? "Prévisions reçues · "
                                                + new SimpleDateFormat("HH:mm", I18n.locale())
                                                    .format(new Date(data.fetchedAt))
                                            : failure);
                                  }
                                }
                              });
                        } catch (JSONException invalid) {
                          state.setText(invalid.getMessage());
                        }
                      });
                }
              });
        };
    V6Ui.add(root, "Rechercher", search);
    input.setOnEditorActionListener(
        (v, action, event) -> {
          if (action == EditorInfo.IME_ACTION_SEARCH) {
            search.run();
            return true;
          }
          return false;
        });
    owner[0] = V6Ui.dialog(a, "Choisir une ville", root);
  }
}

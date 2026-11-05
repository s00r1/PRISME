package fr.dm100.prisme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;

/** Adaptive, independently scrollable widget area shared by all 22 desktops. */
public final class WidgetHost extends FrameLayout {
  public interface Host {
    void action(String action);

    void changed();
  }

  private final Activity a;
  private final Host host;
  private final WidgetStore store;
  private final LinearLayout cards;
  private final FrameLayout viewport;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final List<Runnable> updates = new ArrayList<>();
  private boolean live, horizontal;
  private long weatherAt, diagnosticsAt;
  private WeatherData weather;
  private DeviceSnapshot diagnostics;
  private int themeId = -1;
  private View scrollView;

  private final Runnable tick =
      new Runnable() {
        public void run() {
          if (!live || getVisibility() != VISIBLE) return;
          PowerManager power = (PowerManager) a.getSystemService(Context.POWER_SERVICE);
          if (power == null || power.isInteractive()) {
            MultiTimers.check(a);
            updateCards();
            if (System.currentTimeMillis() >= weatherAt && hasWeather()) {
              weatherAt = System.currentTimeMillis() + 60000;
              WeatherRepository.refresh(
                  a,
                  false,
                  (data, error) -> {
                    if (live && !a.isFinishing() && !a.isDestroyed()) {
                      weather = data;
                      updateCards();
                    }
                  });
            }
          }
          // Keep a light heartbeat during display sleep; resume is not guaranteed for every wake.
          handler.postDelayed(
              this, power != null && !power.isInteractive() ? 5000 : needsSeconds() ? 1000 : 5000);
        }
      };

  public WidgetHost(Activity activity, Host host) {
    super(activity);
    a = activity;
    this.host = host;
    store = new WidgetStore(a);
    viewport = new FrameLayout(a);
    addView(viewport, new LayoutParams(-1, -1));
    cards = new LinearLayout(a);
    weather = WeatherRepository.cached(a);
    addOnLayoutChangeListener(
        (v, l, t, r, b, ol, ot, or, ob) -> {
          if (r - l != or - ol || b - t != ob - ot) post(() -> refresh(Math.max(0, themeId)));
        });
  }

  public void resume() {
    if (getVisibility() != VISIBLE) return;
    live = true;
    weather = WeatherRepository.cached(a);
    weatherAt = 0;
    handler.removeCallbacks(tick);
    handler.post(tick);
  }

  public void pause() {
    live = false;
    handler.removeCallbacksAndMessages(null);
  }

  private void updateCards() {
    for (Runnable update : new ArrayList<>(updates)) update.run();
  }

  private boolean hasWeather() {
    JSONArray widgets = store.active(Math.max(0, themeId));
    for (int i = 0; i < widgets.length(); i++)
      if ("weather".equals(widgets.optJSONObject(i).optString("type"))) return true;
    return false;
  }

  private boolean needsSeconds() {
    if (MultiTimers.active(a) || TimerEngine.remaining(a) > 0) return true;
    JSONArray widgets = store.active(Math.max(0, themeId));
    for (int i = 0; i < widgets.length(); i++) {
      JSONObject w = widgets.optJSONObject(i);
      if ((w.optString("type").equals("clock") || w.optString("type").equals("world"))
          && w.optBoolean("seconds")) return true;
      if (w.optString("type").equals("music") && MediaAccess.enabled(a) && MediaAccess.granted(a))
        return true;
      if (w.optString("type").equals("stopwatch")
          && PrefValues.flag(
              a.getSharedPreferences("prisme_utilities", 0), "stopwatchRunning", false))
        return true;
    }
    return false;
  }

  public void refresh(int theme) {
    theme = Math.max(0, Math.min(ThemeSpec.ALL.length - 1, theme));
    boolean sameTheme = themeId == theme, wasHorizontal = horizontal;
    int x = scrollView == null ? 0 : scrollView.getScrollX(),
        y = scrollView == null ? 0 : scrollView.getScrollY();
    themeId = theme;
    weather = WeatherRepository.cached(a);
    updates.clear();
    viewport.removeAllViews();
    if (cards.getParent() instanceof ViewGroup) ((ViewGroup) cards.getParent()).removeView(cards);
    cards.removeAllViews();
    ThemeSpec palette = ThemeSpec.get(theme);
    horizontal = getWidth() > getHeight(); // The portrait-screen top band is wider than it is tall.
    setBackgroundColor(palette.bg);
    cards.setOrientation(horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
    cards.setPadding(5, 5, 5, 5);
    if (horizontal) {
      HorizontalScrollView scroll = new HorizontalScrollView(a);
      scroll.setHorizontalScrollBarEnabled(false);
      scroll.addView(cards, new HorizontalScrollView.LayoutParams(-2, -1));
      LayoutParams lp = new LayoutParams(-1, -1);
      lp.rightMargin = 48;
      viewport.addView(scroll, lp);
      scrollView = scroll;
    } else {
      ScrollView scroll = new ScrollView(a);
      scroll.setVerticalScrollBarEnabled(false);
      scroll.addView(cards, new ScrollView.LayoutParams(-1, -2));
      LayoutParams lp = new LayoutParams(-1, -1);
      lp.bottomMargin = 48;
      viewport.addView(scroll, lp);
      scrollView = scroll;
    }
    JSONArray widgets = store.active(theme);
    for (int i = 0; i < widgets.length(); i++) {
      JSONObject item = widgets.optJSONObject(i);
      int width = new int[] {158, 198, 238}[item.optInt("size", 1)];
      LinearLayout.LayoutParams lp =
          horizontal
              ? new LinearLayout.LayoutParams(width, -1)
              : new LinearLayout.LayoutParams(-1, -2);
      lp.setMargins(3, 3, 3, 3);
      cards.addView(card(item, palette), lp);
    }
    if (widgets.length() == 0) {
      TextView empty = text("Ajouter les widgets utiles à ce bureau", 14, palette.text);
      empty.setGravity(Gravity.CENTER);
      empty.setPadding(8, 8, 8, 8);
      cards.addView(
          empty,
          horizontal
              ? new LinearLayout.LayoutParams(190, -1)
              : new LinearLayout.LayoutParams(-1, 96));
    }
    Button manage =
        button(
            horizontal ? "＋" : "＋ Widgets",
            "Ajouter et personnaliser les widgets",
            palette,
            () ->
                WidgetSettings.open(
                    a,
                    () -> {
                      host.changed();
                      refresh(themeId);
                    }));
    LayoutParams manageLp =
        horizontal
            ? new LayoutParams(48, -1, Gravity.RIGHT)
            : new LayoutParams(-1, 48, Gravity.BOTTOM);
    viewport.addView(manage, manageLp);
    updateCards();
    View currentScroll = scrollView;
    if (sameTheme && wasHorizontal == horizontal)
      currentScroll.post(() -> currentScroll.scrollTo(x, y));
  }

  private TextView text(String value, int size, int color) {
    TextView view = new TextView(a);
    view.setText(value);
    view.setTextColor(color);
    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    view.setIncludeFontPadding(false);
    view.setPadding(1, 2, 1, 2);
    view.setEllipsize(TextUtils.TruncateAt.END);
    return view;
  }

  private Button button(String title, String description, ThemeSpec theme, Runnable action) {
    Button button = V6Ui.button(a, title, action);
    button.setTextSize(TypedValue.COMPLEX_UNIT_PX, 15);
    button.setTextColor(theme.text);
    button.setPadding(2, 0, 2, 0);
    button.setMinimumWidth(44);
    button.setMinWidth(44);
    button.setMinimumHeight(44);
    button.setMinHeight(44);
    button.setContentDescription(description);
    button.setBackground(V6Ui.surface(theme.surface, theme.accent));
    return button;
  }

  private static int mix(int foreground, int background, float amount) {
    return Color.rgb(
        Math.round(Color.red(foreground) * amount + Color.red(background) * (1 - amount)),
        Math.round(Color.green(foreground) * amount + Color.green(background) * (1 - amount)),
        Math.round(Color.blue(foreground) * amount + Color.blue(background) * (1 - amount)));
  }

  private static double luminance(int color) {
    double total = 0;
    double[] weight = {.2126, .7152, .0722};
    int[] rgb = {Color.red(color), Color.green(color), Color.blue(color)};
    for (int i = 0; i < 3; i++) {
      double n = rgb[i] / 255d;
      total += weight[i] * (n <= .04045 ? n / 12.92 : Math.pow((n + .055) / 1.055, 2.4));
    }
    return total;
  }

  private static int readable(int color, int background) {
    for (int i = 0; i < 10; i++) {
      double a = luminance(color), b = luminance(background);
      if ((Math.max(a, b) + .05) / (Math.min(a, b) + .05) >= 4.5) return color;
      color = mix(luminance(background) > .35 ? Color.BLACK : Color.WHITE, color, .18f);
    }
    return color;
  }

  private View card(JSONObject item, ThemeSpec theme) {
    String type = item.optString("type"), id = item.optString("id");
    int[] accents = {
      theme.accent, 0xff8de7cb, 0xff83b9ff, 0xffc6a9ff, 0xffffb17b, 0xffff9bc4, 0xffffd477
    };
    int skin = item.optInt("skin"),
        size = item.optInt("size", 1),
        variant = item.optInt("variant"),
        scale = item.optInt("textScale", 100);
    boolean detail = item.optBoolean("detail", true);
    int base = skin == 3 || skin == 4 ? theme.bg : theme.surface;
    int background = mix(base, theme.bg, item.optInt("opacity", 100) / 100f);
    int accent = readable(accents[item.optInt("accent")], background),
        ink = readable(theme.text, background);
    LinearLayout card = new LinearLayout(a);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(8, horizontal ? 6 : 9, 8, horizontal ? 6 : 9);
    card.setGravity(variant == 3 ? Gravity.CENTER : Gravity.CENTER_VERTICAL);
    GradientDrawable shape =
        new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] {background, skin == 1 ? mix(theme.bg2, background, .35f) : background});
    int radius = item.optInt("radius", 18);
    if (skin == 0
        && (theme.mode.equals("terminal")
            || theme.mode.equals("lcd")
            || theme.mode.equals("mechanics"))) radius = Math.min(5, radius);
    shape.setCornerRadius(radius);
    if (skin != 4)
      shape.setStroke(
          skin == 5 ? 3 : skin == 3 ? 2 : 1,
          skin == 3 || skin == 5 ? accent : mix(accent, background, .4f));
    card.setBackground(shape);
    card.setMinimumHeight(horizontal ? 80 : size == 0 || variant == 1 ? 82 : size == 2 ? 150 : 108);
    card.setFocusable(true);
    card.setClickable(true);
    card.setContentDescription(
        DashboardWidgets.name(type) + ". Toucher pour ouvrir. Appui long pour personnaliser.");
    card.setOnLongClickListener(
        v -> {
          WidgetSettings.customizeId(
              a,
              id,
              () -> {
                host.changed();
                refresh(themeId);
              });
          return true;
        });
    card.setOnClickListener(v -> open(type));
    if (item.optBoolean("showTitle", true)) {
      String custom = item.optString("title");
      TextView title =
          text(
              custom.isEmpty() ? DashboardWidgets.name(type).toUpperCase(I18n.locale()) : custom,
              Math.max(12, 12 * scale / 100),
              accent);
      title.setSingleLine(true);
      card.addView(title);
    }
    if (type.equals("weather"))
      addWeather(card, item, theme, ink, readable(theme.muted, background));
    else if ((type.equals("clock") || type.equals("world")) && variant == 1) {
      ClockFace face = new ClockFace(a);
      face.configure(
          accent,
          ink,
          type.equals("world") ? item.optString("timezone", "UTC") : null,
          item.optBoolean("seconds"));
      card.addView(
          face,
          new LinearLayout.LayoutParams(
              -1, horizontal ? 64 : size == 0 ? 82 : size == 2 ? 158 : 112));
      updates.add(face::refreshTime);
      if (detail && !horizontal) {
        TextView zone =
            text(type.equals("world") ? item.optString("timezone") : "Heure locale", 12, ink);
        zone.setGravity(Gravity.CENTER);
        card.addView(zone);
      }
    } else if (type.equals("music")) addMusic(card, item, theme, ink);
    else {
      boolean clock = type.equals("clock") || type.equals("world");
      int textSize = clock ? (variant == 2 ? 34 : variant == 3 ? 22 : 27) : variant == 3 ? 23 : 17;
      int desiredSize = Math.max(13, Math.min(horizontal ? 30 : 42, textSize * scale / 100));
      TextView content = text("", desiredSize, ink);
      content.setMaxLines(
          horizontal
              ? 2
              : variant == 3 || !detail ? 2 : variant == 2 ? 7 : size == 0 || variant == 1 ? 2 : 5);
      if (variant == 3) content.setGravity(Gravity.CENTER);
      card.addView(content);
      if (clock)
        content.addOnLayoutChangeListener(
            (v, l, t, r, b, ol, ot, or, ob) -> fitClock(content, desiredSize));
      updates.add(
          () -> {
            set(content, value(type, item));
            if (clock) fitClock(content, desiredSize);
          });
    }
    if (type.equals("shortcuts") && detail && !horizontal && variant != 3) {
      for (String label : new String[] {"Rechercher", "Applications", "22 styles"}) {
        String action =
            label.equals("Rechercher")
                ? "search"
                : label.equals("Applications") ? "apps" : "styles";
        card.addView(
            button(label, label, theme, () -> host.action(action)),
            new LinearLayout.LayoutParams(-1, 44));
      }
    }
    I18n.bind(card);
    return card;
  }

  private void addMusic(LinearLayout card, JSONObject item, ThemeSpec theme, int ink) {
    LinearLayout row = new LinearLayout(a);
    row.setGravity(Gravity.CENTER_VERTICAL);
    MediaArtworkView artwork = new MediaArtworkView(a);
    artwork.colors(theme.surface, theme.accent);
    LinearLayout.LayoutParams artLp =
        new LinearLayout.LayoutParams(horizontal ? 32 : 44, horizontal ? 32 : 44);
    artLp.rightMargin = 7;
    row.addView(artwork, artLp);
    TextView content =
        text("", Math.max(13, Math.min(22, 17 * item.optInt("textScale", 100) / 100)), ink);
    I18n.userText(content);
    content.setMaxLines(horizontal ? 2 : item.optInt("variant") == 1 ? 1 : 3);
    row.addView(content, new LinearLayout.LayoutParams(0, -2, 1));
    Button play =
        button(
            "▶",
            "Lecture ou pause",
            theme,
            () -> {
              MusicPlayback.command(a, MusicPlayback.TOGGLE);
              updateCards();
            });
    if (horizontal || !item.optBoolean("detail", true) || item.optInt("variant") == 3)
      row.addView(play, new LinearLayout.LayoutParams(44, 44));
    card.addView(row);
    if (!horizontal && item.optBoolean("detail", true) && item.optInt("variant") != 3) {
      card.addView(play, new LinearLayout.LayoutParams(-1, 44));
      LinearLayout transport = new LinearLayout(a);
      transport.addView(
          button(
              "⏮",
              "Piste précédente",
              theme,
              () -> MusicPlayback.command(a, MusicPlayback.PREVIOUS)),
          new LinearLayout.LayoutParams(0, 44, 1));
      transport.addView(
          button("⏭", "Piste suivante", theme, () -> MusicPlayback.command(a, MusicPlayback.NEXT)),
          new LinearLayout.LayoutParams(0, 44, 1));
      card.addView(transport);
    }
    updates.add(
        () -> {
          MusicPlayback.State music = MusicPlayback.state(a);
          String title =
              item.optInt("variant") == 3
                  ? (music.playing ? "En lecture" : "En pause")
                  : MediaPanel.title(music);
          if (item.optBoolean("detail", true)
              && item.optInt("variant") != 1
              && !music.artist.isEmpty()) title += "\n" + music.artist;
          if (item.optInt("variant") == 2 && !horizontal)
            title += "\n" + (music.hasSession ? "Session connectée" : MediaPanel.detail(music));
          artwork.artwork(music.artwork);
          set(content, title);
          set(play, music.playing ? "Ⅱ" : "▶");
        });
  }

  private void addWeather(LinearLayout card, JSONObject item, ThemeSpec theme, int ink, int muted) {
    int variant = item.optInt("variant"),
        size = item.optInt("size", 1),
        scale = item.optInt("textScale", 100);
    boolean detail = item.optBoolean("detail", true);
    LinearLayout hero = new LinearLayout(a);
    hero.setGravity(Gravity.CENTER_VERTICAL);
    WeatherIcon icon = new WeatherIcon(a);
    hero.addView(icon, new LinearLayout.LayoutParams(horizontal ? 36 : 46, horizontal ? 36 : 48));
    TextView degrees =
        text("—", Math.max(20, Math.min(horizontal ? 30 : 36, 30 * scale / 100)), ink);
    hero.addView(degrees, new LinearLayout.LayoutParams(0, -2, 1));
    card.addView(hero);
    TextView info = text("", Math.max(12, Math.min(horizontal ? 15 : 19, 13 * scale / 100)), ink);
    info.setMaxLines(
        horizontal ? 1 : variant == 2 ? 7 : variant == 3 ? 6 : variant == 1 || size == 0 ? 2 : 5);
    if (detail) card.addView(info);
    TextView source = text("", horizontal ? 10 : 11, muted);
    source.setMaxLines(horizontal ? 1 : 2);
    card.addView(source);
    updates.add(
        () -> {
          WeatherData data = weather;
          if (data == null) {
            set(degrees, "—");
            icon.weather(2, true);
            set(
                info,
                WeatherRepository.city(a).isEmpty()
                    ? "Choisir une ville"
                    : "Prévisions indisponibles");
            set(
                source,
                WeatherRepository.allowed(a)
                    ? "Configurer · Open-Meteo"
                    : "Activer la météo · Open-Meteo");
            return;
          }
          boolean f = WeatherRepository.fahrenheit(a);
          long now = System.currentTimeMillis();
          set(degrees, data.temperatureText(f));
          icon.weather(data.code(), data.day());
          String body = data.city;
          if (horizontal) body = variant >= 2 ? forecast(data, variant, f, 1) : data.city;
          else if (variant >= 2)
            body += "\n" + forecast(data, variant, f, variant == 2 ? 5 : size == 0 ? 2 : 4);
          else if (variant == 0) {
            body += "\n" + WeatherData.condition(data.code());
            JSONObject current = data.raw.optJSONObject("current");
            if (size > 0)
              body +=
                  "\nRessenti "
                      + WeatherPanel.number(
                          WeatherPanel.temp(
                              current.optDouble("apparent_temperature", Double.NaN), f),
                          "°");
            if (size == 2)
              body +=
                  "\nVent "
                      + WeatherPanel.number(
                          current.optDouble("wind_speed_10m", Double.NaN), " km/h")
                      + "\nHumidité "
                      + WeatherPanel.number(
                          current.optDouble("relative_humidity_2m", Double.NaN), " %");
          }
          set(info, body);
          String state =
              !WeatherRepository.allowed(a)
                  ? "Cache · réseau désactivé"
                  : data.stale(now)
                      ? "Cache ancien"
                      : WeatherRepository.status(a).isEmpty() ? "Actualisé" : "Cache · hors ligne";
          String when =
              new SimpleDateFormat("HH:mm", I18n.locale()).format(new Date(data.fetchedAt));
          set(source, state + (horizontal ? "" : " · " + when + "\nOpen-Meteo.com"));
        });
  }

  private String forecast(WeatherData data, int variant, boolean f, int count) {
    JSONObject block = data.raw.optJSONObject(variant == 2 ? "daily" : "hourly");
    if (block == null) return "Prévisions indisponibles";
    JSONArray times = block.optJSONArray("time"),
        high = block.optJSONArray(variant == 2 ? "temperature_2m_max" : "temperature_2m"),
        low = block.optJSONArray("temperature_2m_min");
    if (times == null || high == null) return "Prévisions indisponibles";
    SimpleDateFormat format = new SimpleDateFormat(variant == 2 ? "EEE" : "HH:mm", I18n.locale());
    format.setTimeZone(
        TimeZone.getTimeZone(PrefValues.text(WeatherRepository.options(a), "timezone", "UTC")));
    StringBuilder result = new StringBuilder();
    int shown = 0;
    for (int i = 0; i < Math.min(times.length(), high.length()) && shown < count; i++) {
      long time = times.optLong(i) * 1000;
      if (variant == 3 && time < System.currentTimeMillis() - 1800000) continue;
      if (shown++ > 0) result.append('\n');
      result.append(format.format(new Date(time))).append(" · ");
      if (variant == 2)
        result
            .append(
                WeatherPanel.number(
                    WeatherPanel.temp(low == null ? Double.NaN : low.optDouble(i, Double.NaN), f),
                    "°"))
            .append(" / ");
      result.append(WeatherPanel.number(WeatherPanel.temp(high.optDouble(i, Double.NaN), f), "°"));
    }
    return shown == 0 ? "Prévisions expirées · actualiser" : result.toString();
  }

  /** Keep seconds and AM/PM on one line even on the narrow rail at 140% text scale. */
  private static void fitClock(TextView view, int desiredSize) {
    int available = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
    if (available <= 0) return;
    String[] lines = view.getText().toString().split("\n");
    float longest = 0;
    float actual = view.getTextSize();
    for (String line : lines)
      if (line.indexOf(':') >= 0)
        longest = Math.max(longest, view.getPaint().measureText(line) * desiredSize / actual);
    float fitted =
        longest <= available ? desiredSize : Math.max(13, desiredSize * available / longest);
    if (Math.abs(fitted - actual) > .25f) view.setTextSize(TypedValue.COMPLEX_UNIT_PX, fitted);
  }

  private static long safeLong(SharedPreferences store, String key, long fallback) {
    try {
      return store.getLong(key, fallback);
    } catch (ClassCastException invalid) {
      return fallback;
    }
  }

  private static void set(TextView view, String text) {
    if (!text.contentEquals(view.getText())) view.setText(text);
  }

  private String value(String type, JSONObject item) {
    int variant = item.optInt("variant"), size = item.optInt("size", 1);
    boolean detail = item.optBoolean("detail", true) && variant != 3;
    boolean extended = detail && variant == 2 && !horizontal;
    switch (type) {
      case "clock":
      case "world":
        {
          boolean hour24 = item.optBoolean("hour24", true), seconds = item.optBoolean("seconds");
          String pattern =
              (hour24 ? "HH:mm" : "hh:mm") + (seconds ? ":ss" : "") + (hour24 ? "" : " a");
          TimeZone zone =
              type.equals("world")
                  ? TimeZone.getTimeZone(item.optString("timezone", "UTC"))
                  : TimeZone.getDefault();
          SimpleDateFormat time = new SimpleDateFormat(pattern, I18n.locale()),
              date = new SimpleDateFormat("EEE d MMM", I18n.locale());
          time.setTimeZone(zone);
          date.setTimeZone(zone);
          Date now = new Date();
          String result =
              variant == 3 ? date.format(now) + "\n" + time.format(now) : time.format(now);
          if (detail && variant != 1)
            result += "\n" + (type.equals("world") ? zone.getID() : date.format(now));
          return result;
        }
      case "battery":
        {
          BatteryLevelReader.Reading battery = BatteryLevelReader.read(a, null);
          return (battery.level < 0 ? "Mesure inconnue" : battery.level + " %")
              + (detail ? "\n" + (battery.charging ? "En charge" : "Sur batterie") : "")
              + (extended ? "\nMesure : " + battery.source : "");
        }
      case "tasks":
        {
          ChecklistStore checklist = new ChecklistStore(a);
          JSONArray tasks = checklist.read();
          int left = checklist.remaining();
          return left
              + " tâche"
              + (left == 1 ? "" : "s")
              + (detail ? " à faire" : "")
              + (detail && variant != 1 ? "\n" + (tasks.length() - left) + " terminée(s)" : "")
              + (extended ? "\n" + tasks.length() + " au total\nToucher pour gérer" : "");
        }
      case "notes":
        return "Carnet local"
            + (detail ? "\nOuvrir mes notes" : "")
            + (extended ? "\nTexte privé sur l’accueil" : "");
      case "timers":
      case "focus":
        {
          boolean multiple = type.equals("timers") && MultiTimers.active(a);
          long left = multiple ? MultiTimers.nextRemaining(a) : TimerEngine.remaining(a);
          return left > 0
              ? ExperienceController.duration(left)
                  + (detail
                      ? "\n" + (!multiple && TimerEngine.paused(a) ? "En pause" : "En cours")
                      : "")
              : type.equals("focus")
                  ? "Concentration" + (detail ? "\n5 · 15 · 25 · 45 min" : "")
                  : "Minuteurs" + (detail ? "\nCréer un compte à rebours" : "");
        }
      case "calendar":
        return new SimpleDateFormat(
                    variant == 3 ? "d MMM" : detail && variant != 1 ? "EEEE\nd MMMM" : "d MMMM",
                    I18n.locale())
                .format(new Date())
            + (extended
                ? "\nSemaine " + new SimpleDateFormat("w", I18n.locale()).format(new Date())
                : "");
      case "system":
        {
          if (diagnostics == null || SystemClock.elapsedRealtime() >= diagnosticsAt) {
            diagnostics = DeviceSnapshot.read(a);
            diagnosticsAt = SystemClock.elapsedRealtime() + 30000;
          }
          return "RAM "
              + (diagnostics.memoryUsedPct < 0 ? "inconnue" : diagnostics.memoryUsedPct + " %")
              + (detail
                  ? "\nLibre "
                      + (diagnostics.freeStorageGb < 0
                          ? "inconnu"
                          : String.format(I18n.locale(), "%.1f Go", diagnostics.freeStorageGb))
                  : "")
              + (detail && variant != 1
                  ? "\n" + (diagnostics.wifiConnected ? "Wi-Fi connecté" : "Hors ligne")
                  : "")
              + (extended ? "\nActualisation · 30 s" : "");
        }
      case "stopwatch":
        {
          SharedPreferences p = a.getSharedPreferences("prisme_utilities", 0);
          long elapsed = safeLong(p, "stopwatchAccumulated", 0);
          boolean running = PrefValues.flag(p, "stopwatchRunning", false);
          try {
            int boot =
                android.provider.Settings.Global.getInt(
                    a.getContentResolver(), android.provider.Settings.Global.BOOT_COUNT, -1);
            if (boot < 0 || PrefValues.number(p, "stopwatchBoot", -2) != boot) running = false;
          } catch (RuntimeException unavailable) {
            running = false;
          }
          if (running)
            elapsed +=
                Math.max(
                    0,
                    SystemClock.elapsedRealtime()
                        - safeLong(p, "stopwatchStart", SystemClock.elapsedRealtime()));
          return ExperienceController.duration(elapsed)
              + (detail ? "\n" + (running ? "En cours" : "Prêt · ouvrir les tours") : "");
        }
      case "shortcuts":
        return horizontal
            ? "Recherche · Apps · Styles"
            : detail && variant != 3 ? "Accès directs" : "Rechercher";
      default:
        return "Ouvrir";
    }
  }

  private void open(String type) {
    switch (type) {
      case "weather":
        WeatherPanel.open(a);
        return;
      case "clock":
      case "world":
        host.action("deskclock");
        return;
      case "music":
        host.action("mediaPanel");
        return;
      case "tasks":
        host.action("checklist");
        return;
      case "notes":
        host.action("notes");
        return;
      case "timers":
        host.action("multiTimers");
        return;
      case "calendar":
        host.action("calendarWidget");
        return;
      case "focus":
        host.action("focus");
        return;
      case "stopwatch":
        host.action("stopwatch");
        return;
      case "shortcuts":
        host.action("search");
        return;
      default:
        host.action("doctor");
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    pause();
    super.onDetachedFromWindow();
  }
}

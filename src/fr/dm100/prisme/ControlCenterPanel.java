package fr.dm100.prisme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Reusable control panel, hosted by PRISME or an Android overlay window. */
public final class ControlCenterPanel extends FrameLayout {
  public interface Listener {
    void onCustomize();

    void onClosed();
  }

  private static final int[] ACCENTS = {
    0xff62d9cc, 0xff59c9ef, 0xff78a9ff, 0xffbe9bff, 0xffffb275, 0xfff79fc2
  };
  private final Context activity;
  private final ControlCenterPrefs prefs;
  private final ControlCenterController controller;
  private final Listener listener;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final List<Tile> tiles = new ArrayList<Tile>();
  private PanelCard card;
  private LinearLayout header;
  private Button closeButton, customizeButton, footerCloseButton;
  private TextView status, volumeLabel, brightnessLabel, centerClock, mediaTitle, mediaArtist;
  private Button mediaPlay;
  private SeekBar volumeSlider, brightnessSlider;
  private VerticalLevel verticalVolume, verticalBrightness;
  private boolean opened, adjustingVolume, adjustingBrightness;
  private float outsideX, outsideY;
  private boolean outsideDown;
  private boolean insideTouch;
  private int ink, muted, accent, panelColor, tileColor;
  private int effectiveColumns;
  private int builtWidth;
  private final Runnable poll =
      new Runnable() {
        @Override
        public void run() {
          if (!opened) return;
          refresh();
          if (opened) handler.postDelayed(this, 1000);
        }
      };

  private static final class Tile {
    final String id;
    final LinearLayout box;
    final ControlCenterIcon icon;
    final TextView title, subtitle;
    String lastState;

    Tile(String id, LinearLayout box, ControlCenterIcon icon, TextView title, TextView subtitle) {
      this.id = id;
      this.box = box;
      this.icon = icon;
      this.title = title;
      this.subtitle = subtitle;
    }
  }

  public ControlCenterPanel(
      Context activity,
      ControlCenterPrefs prefs,
      ControlCenterController controller,
      Listener listener) {
    super(activity);
    if (prefs == null || controller == null || listener == null)
      throw new IllegalArgumentException("Centre de contrôle incomplet");
    this.activity = activity;
    this.prefs = prefs;
    this.controller = controller;
    this.listener = listener;
    setVisibility(GONE);
    setClickable(true);
    setFocusableInTouchMode(true);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    setContentDescription("Centre de contrôle PRISME");
  }

  public boolean isOpen() {
    return opened;
  }

  public void open() {
    open(false);
  }

  public void open(boolean preview) {
    if (!preview && !prefs.getBoolean("enabled", true)) return;
    handler.removeCallbacks(poll);
    opened = true;
    build();
    setVisibility(VISIBLE);
    bringToFront();
    requestFocus();
    applyLayout();
    refresh();
    handler.postDelayed(poll, 1000);
    announceForAccessibility("Centre de contrôle ouvert. Bouton Fermer en haut.");
  }

  public void close() {
    boolean wasOpen = opened;
    opened = false;
    outsideDown = insideTouch = false;
    adjustingVolume = adjustingBrightness = false;
    handler.removeCallbacks(poll);
    setVisibility(GONE);
    if (wasOpen) listener.onClosed();
  }

  public void onHostPause() {
    close();
  }

  @Override
  protected void onDetachedFromWindow() {
    close();
    super.onDetachedFromWindow();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (opened) {
      if (builtWidth != panelBoundsNow().width) {
        build();
        refresh();
      }
      applyLayout();
    }
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (opened && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
      if (event.getAction() == KeyEvent.ACTION_UP) close();
      return true;
    }
    return super.dispatchKeyEvent(event);
  }

  private void build() {
    removeAllViews();
    tiles.clear();
    adjustingVolume = adjustingBrightness = false;
    status = volumeLabel = brightnessLabel = centerClock = mediaTitle = mediaArtist = null;
    mediaPlay = null;
    volumeSlider = brightnessSlider = null;
    verticalVolume = verticalBrightness = null;
    boolean androidStyle = prefs.getInt("style", 0) == 1;
    builtWidth = panelBoundsNow().width;
    boolean dark = prefs.getBoolean("dark", true);
    ink = dark ? 0xfff4f8ff : 0xff132237;
    muted = dark ? 0xffbdcadb : 0xff455b72;
    panelColor =
        dark ? (androidStyle ? 0xff14181e : 0xff172334) : (androidStyle ? 0xffe8ecf4 : 0xffedf5ff);
    tileColor = dark ? (androidStyle ? 0xff30353d : 0xff3b4a5f) : 0xfff9fcff;
    accent =
        prefs.getBoolean("customAccent", false)
            ? 0xff000000 | (prefs.getInt("customColor", 0x62d9cc) & 0xffffff)
            : ACCENTS[Math.max(0, Math.min(ACCENTS.length - 1, prefs.getInt("accent", 0)))];
    setBackgroundColor(Color.argb(percentAlpha(prefs.getInt("scrimOpacity", 8)), 0, 0, 0));
    card = new PanelCard();
    card.setOrientation(LinearLayout.VERTICAL);
    card.setClickable(true);
    card.setPadding(u(10), u(8), u(10), u(8));
    int alpha = percentAlpha(prefs.getInt("opacity", 68));
    GradientDrawable glass =
        new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] {alpha(panelColor, alpha), alpha(dark ? 0xff0e1624 : 0xffdcecff, alpha)});
    glass.setCornerRadius(u(prefs.getInt("radius", 24)));
    glass.setStroke(1, alpha(ink, dark ? 48 : 38));
    card.setBackground(glass);
    card.setElevation(u(4));
    addView(card);

    header = new LinearLayout(activity);
    header.setOrientation(LinearLayout.VERTICAL);
    // Give the card a child touch target even on blank header space, so MOVE is intercepted.
    header.setClickable(true);
    View grip = new View(activity);
    grip.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    grip.setBackground(round(alpha(ink, 92), u(3), Color.TRANSPARENT));
    LinearLayout.LayoutParams gripLp = new LinearLayout.LayoutParams(u(43), Math.max(3, u(4)));
    gripLp.gravity = Gravity.CENTER_HORIZONTAL;
    gripLp.bottomMargin = u(4);
    header.addView(grip, gripLp);
    LinearLayout headerRow = new LinearLayout(activity);
    headerRow.setOrientation(LinearLayout.HORIZONTAL);
    headerRow.setGravity(Gravity.CENTER_VERTICAL);
    header.addView(headerRow, new LinearLayout.LayoutParams(-1, -2));
    TextView heading =
        text(androidStyle ? "" : "Centre de contrôle", androidStyle ? 30 : 18, ink, !androidStyle);
    if (androidStyle) centerClock = heading;
    heading.setMaxLines(2);
    heading.setEllipsize(TextUtils.TruncateAt.END);
    headerRow.addView(heading, new LinearLayout.LayoutParams(0, Math.max(48, u(48)), 1));
    customizeButton = button("⚙", "Personnaliser le centre de contrôle");
    customizeButton.setOnClickListener(
        v -> {
          close();
          listener.onCustomize();
        });
    headerRow.addView(
        customizeButton, new LinearLayout.LayoutParams(Math.max(44, u(44)), Math.max(44, u(44))));
    closeButton = button("×", "Fermer le centre de contrôle");
    closeButton.setOnClickListener(v -> close());
    LinearLayout.LayoutParams closeLp =
        new LinearLayout.LayoutParams(Math.max(44, u(44)), Math.max(44, u(44)));
    closeLp.leftMargin = u(5);
    headerRow.addView(closeButton, closeLp);
    card.addView(header, new LinearLayout.LayoutParams(-1, -2));

    ScrollView scroller = new ScrollView(activity);
    scroller.setFillViewport(false);
    scroller.setClipToPadding(false);
    scroller.setContentDescription("Commandes et curseurs du centre de contrôle");
    LinearLayout body = new LinearLayout(activity);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(0, u(4), 0, u(8));
    scroller.addView(body);
    card.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
    if (prefs.getBoolean("status", true)) {
      status = text("", 12, muted, false);
      status.setMaxLines(3);
      status.setPadding(u(4), u(4), u(4), u(10));
      body.addView(status);
    }
    List<String> selected = prefs.getTiles();
    if (androidStyle) buildAndroid(body, selected);
    else buildIos(body, selected);
    if (selected.isEmpty()) {
      TextView empty = text("Ajoute tes commandes avec le bouton Personnaliser.", 14, muted, false);
      empty.setPadding(u(8), u(14), u(8), u(14));
      body.addView(empty);
    }
    Button customize = button("Personnaliser", "Organiser les commandes et l’apparence");
    customize.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(14));
    customize.setOnClickListener(
        v -> {
          close();
          listener.onCustomize();
        });
    LinearLayout.LayoutParams customizeLp = new LinearLayout.LayoutParams(-1, Math.max(44, u(44)));
    customizeLp.topMargin = u(5);
    body.addView(customize, customizeLp);
    String[] arrows = {"↑", "↓", "←", "→"};
    footerCloseButton =
        button(
            "Fermer  " + arrows[prefs.getInt("position", 0)],
            "Fermer le centre. Toucher ou glisser vers l’extérieur depuis cette barre.");
    footerCloseButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(15));
    footerCloseButton.setOnClickListener(v -> close());
    LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(-1, Math.max(44, u(44)));
    footerLp.topMargin = u(5);
    card.addView(footerCloseButton, footerLp);
  }

  /** iOS uses discrete circular switches and independent vertical level modules. */
  private void buildIos(LinearLayout body, List<String> selected) {
    boolean showMedia = prefs.getBoolean("media", true);
    boolean showVolume = prefs.getBoolean("volume", true);
    boolean showBrightness = prefs.getBoolean("brightness", true);
    int first = Math.min(4, selected.size());
    int available = builtWidth - u(20);
    boolean wide = available >= u(540);
    boolean stacked = available < u(350);
    LinearLayout modules = new LinearLayout(activity);
    modules.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
    modules.setGravity(Gravity.CENTER_VERTICAL);
    int moduleHeight = Math.max(152, u(156));
    if (first > 0) {
      LinearLayout switches = new LinearLayout(activity);
      switches.setOrientation(LinearLayout.VERTICAL);
      switches.setPadding(u(6), u(6), u(6), u(2));
      switches.setBackground(moduleBackground(26));
      addTiles(switches, selected, 0, first, 2, Math.max(66, u(70)), 2);
      LinearLayout.LayoutParams lp =
          new LinearLayout.LayoutParams(stacked ? -1 : 0, moduleHeight, stacked ? 0 : 1.2f);
      modules.addView(switches, lp);
    }
    if (showMedia) {
      LinearLayout media = mediaModule(false);
      LinearLayout.LayoutParams lp =
          new LinearLayout.LayoutParams(stacked ? -1 : 0, moduleHeight, stacked ? 0 : 1.4f);
      if (first > 0) {
        if (stacked) lp.topMargin = u(8);
        else lp.leftMargin = u(8);
      }
      modules.addView(media, lp);
    }
    if (wide) {
      if (showBrightness) addVerticalLevel(modules, false, moduleHeight);
      if (showVolume) addVerticalLevel(modules, true, moduleHeight);
    }
    if (modules.getChildCount() > 0) body.addView(modules, new LinearLayout.LayoutParams(-1, -2));
    if (!wide && (showVolume || showBrightness)) {
      LinearLayout levels = new LinearLayout(activity);
      levels.setGravity(Gravity.CENTER_VERTICAL);
      TextView hint = text("Niveaux\nGlisser vers le haut ou le bas", 13, muted, false);
      hint.setMaxLines(3);
      hint.setPadding(u(8), u(4), u(10), u(4));
      levels.addView(hint, new LinearLayout.LayoutParams(0, -2, 1));
      if (showBrightness) addVerticalLevel(levels, false, Math.max(136, u(142)));
      if (showVolume) addVerticalLevel(levels, true, Math.max(136, u(142)));
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
      lp.topMargin = u(8);
      body.addView(levels, lp);
    }
    effectiveColumns = columnsFor(builtWidth);
    addTiles(
        body,
        selected,
        first,
        selected.size(),
        effectiveColumns,
        Math.max(64, u(prefs.getBoolean("labels", true) ? 76 : 58)),
        8);
  }

  /** Material quick settings keep wide horizontal pills and a horizontal light control. */
  private void buildAndroid(LinearLayout body, List<String> selected) {
    if (prefs.getBoolean("brightness", true)) addSlider(body, false);
    effectiveColumns = columnsFor(builtWidth);
    int first = Math.min(4, selected.size());
    addTiles(body, selected, 0, first, effectiveColumns, Math.max(72, u(76)), 8);
    if (prefs.getBoolean("media", true)) {
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
      lp.topMargin = u(2);
      lp.bottomMargin = u(8);
      body.addView(mediaModule(true), lp);
    }
    addTiles(body, selected, first, selected.size(), effectiveColumns, Math.max(72, u(76)), 8);
    if (prefs.getBoolean("volume", true)) addSlider(body, true);
  }

  private void addTiles(
      LinearLayout body,
      List<String> selected,
      int from,
      int to,
      int columns,
      int height,
      int gap) {
    LinearLayout row = null;
    for (int i = from; i < to; i++) {
      int column = (i - from) % columns;
      if (column == 0) {
        row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, height);
        lp.topMargin = u(gap);
        body.addView(row, lp);
      }
      Tile tile = makeTile(selected.get(i));
      tiles.add(tile);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
      if (column > 0) lp.leftMargin = u(6);
      row.addView(tile.box, lp);
      if (i == to - 1)
        for (int j = column + 1; j < columns; j++) {
          LinearLayout.LayoutParams empty = new LinearLayout.LayoutParams(0, 1, 1);
          empty.leftMargin = u(6);
          row.addView(new View(activity), empty);
        }
    }
  }

  private GradientDrawable moduleBackground(int radius) {
    return round(alpha(tileColor, surfaceAlpha()), u(radius), alpha(ink, 28));
  }

  private LinearLayout mediaModule(boolean androidStyle) {
    LinearLayout media = new LinearLayout(activity);
    media.setOrientation(LinearLayout.VERTICAL);
    media.setGravity(Gravity.CENTER_VERTICAL);
    media.setPadding(u(12), u(8), u(12), u(8));
    media.setBackground(moduleBackground(androidStyle ? 24 : 26));
    TextView heading =
        text(
            androidStyle ? "LECTEUR MULTIMÉDIA" : "À l’écoute",
            androidStyle ? 10 : 11,
            muted,
            true);
    media.addView(heading);
    mediaTitle = text("", androidStyle ? 16 : 14, ink, true);
    mediaTitle.setMaxLines(1);
    mediaTitle.setEllipsize(TextUtils.TruncateAt.END);
    media.addView(mediaTitle, new LinearLayout.LayoutParams(-1, -2));
    mediaArtist = text("", 11, muted, false);
    mediaArtist.setMaxLines(1);
    mediaArtist.setEllipsize(TextUtils.TruncateAt.END);
    media.addView(mediaArtist, new LinearLayout.LayoutParams(-1, -2));
    LinearLayout controls = new LinearLayout(activity);
    controls.setGravity(Gravity.CENTER);
    Button previous = button("‹", "Piste précédente"), next = button("›", "Piste suivante");
    mediaPlay = button("▶", "Lecture ou pause du lecteur Android");
    previous.setOnClickListener(v -> sendMedia(MusicPlayback.PREVIOUS));
    mediaPlay.setOnClickListener(v -> sendMedia(MusicPlayback.TOGGLE));
    next.setOnClickListener(v -> sendMedia(MusicPlayback.NEXT));
    for (Button control : new Button[] {previous, mediaPlay, next}) {
      control.setBackgroundColor(Color.TRANSPARENT);
      controls.addView(control, new LinearLayout.LayoutParams(0, Math.max(44, u(44)), 1));
    }
    media.addView(controls, new LinearLayout.LayoutParams(-1, Math.max(44, u(44))));
    return media;
  }

  private void sendMedia(int command) {
    if (opened) {
      MusicPlayback.command(activity, command);
      refresh();
    }
  }

  private void addVerticalLevel(LinearLayout row, boolean volume, int height) {
    VerticalLevel level = new VerticalLevel(volume);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.max(52, u(56)), height);
    lp.leftMargin = u(8);
    row.addView(level, lp);
    if (volume) verticalVolume = level;
    else verticalBrightness = level;
  }

  private Tile makeTile(final String id) {
    boolean androidStyle = prefs.getInt("style", 0) == 1;
    LinearLayout box = new LinearLayout(activity);
    box.setOrientation(androidStyle ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
    box.setGravity(androidStyle ? Gravity.LEFT | Gravity.CENTER_VERTICAL : Gravity.CENTER);
    box.setPadding(
        u(androidStyle ? 12 : 3),
        u(androidStyle ? 8 : 4),
        u(androidStyle ? 10 : 3),
        u(androidStyle ? 8 : 4));
    box.setClickable(true);
    box.setFocusable(true);
    box.setMinimumHeight(44);
    box.setMinimumWidth(44);
    ControlCenterIcon icon = new ControlCenterIcon(activity, id);
    icon.setInk(ink);
    icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    int iconSize = androidStyle ? u(30) : Math.max(44, u(44));
    if (!androidStyle) icon.setPadding(u(9), u(9), u(9), u(9));
    LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
    if (androidStyle) iconLp.rightMargin = u(10);
    else iconLp.bottomMargin = u(3);
    box.addView(icon, iconLp);
    LinearLayout copy = new LinearLayout(activity);
    copy.setOrientation(LinearLayout.VERTICAL);
    copy.setGravity(Gravity.CENTER_VERTICAL);
    box.addView(
        copy,
        new LinearLayout.LayoutParams(
            androidStyle ? 0 : -1, androidStyle ? -1 : -2, androidStyle ? 1 : 0));
    TextView title = text(ControlCenterController.label(id), androidStyle ? 14 : 11, ink, true);
    title.setGravity(androidStyle ? Gravity.LEFT : Gravity.CENTER);
    title.setMaxLines(1);
    title.setEllipsize(TextUtils.TruncateAt.END);
    copy.addView(title, new LinearLayout.LayoutParams(-1, -2));
    TextView subtitle = text("", 11, muted, false);
    subtitle.setGravity(androidStyle ? Gravity.LEFT : Gravity.CENTER);
    subtitle.setMaxLines(2);
    subtitle.setEllipsize(TextUtils.TruncateAt.END);
    copy.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));
    if (!androidStyle) subtitle.setVisibility(GONE);
    if (!prefs.getBoolean("labels", true)) {
      title.setVisibility(GONE);
      subtitle.setVisibility(GONE);
    }
    box.setOnClickListener(
        v -> {
          if (opened) {
            controller.perform(id);
            if (opened) refresh();
          }
        });
    box.setOnLongClickListener(
        v -> {
          if (opened) {
            controller.longPress(id);
            if (opened) refresh();
          }
          return true;
        });
    return new Tile(id, box, icon, title, subtitle);
  }

  private void addSlider(LinearLayout body, final boolean volume) {
    LinearLayout block = new LinearLayout(activity);
    block.setOrientation(LinearLayout.VERTICAL);
    block.setPadding(u(10), u(8), u(10), u(4));
    block.setBackground(round(alpha(tileColor, surfaceAlpha()), u(18), alpha(ink, 28)));
    TextView label = text("", 13, ink, true);
    block.addView(label);
    SeekBar slider = new SeekBar(activity);
    slider.setMax(volume ? Math.max(1, controller.volumeMax()) : 100);
    slider.setProgressTintList(ColorStateList.valueOf(accent));
    slider.setThumbTintList(ColorStateList.valueOf(accent));
    slider.setContentDescription(volume ? "Volume des médias" : "Luminosité de l’écran");
    LinearLayout.LayoutParams sliderLp = new LinearLayout.LayoutParams(-1, Math.max(44, u(44)));
    block.addView(slider, sliderLp);
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onStartTrackingTouch(SeekBar bar) {
            if (volume) adjustingVolume = true;
            else adjustingBrightness = true;
          }

          @Override
          public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
            if (!opened || !fromUser) return;
            if (volume) controller.setVolume(value);
            else controller.setBrightness(value);
            updateSliderLabels();
          }

          @Override
          public void onStopTrackingTouch(SeekBar bar) {
            if (volume) adjustingVolume = false;
            else adjustingBrightness = false;
            if (opened) refresh();
          }
        });
    LinearLayout.LayoutParams blockLp = new LinearLayout.LayoutParams(-1, -2);
    blockLp.topMargin = u(7);
    body.addView(block, blockLp);
    if (volume) {
      volumeLabel = label;
      volumeSlider = slider;
    } else {
      brightnessLabel = label;
      brightnessSlider = slider;
    }
  }

  public void refresh() {
    if (!opened) return;
    boolean androidStyle = prefs.getInt("style", 0) == 1;
    if (centerClock != null)
      setTextIfChanged(centerClock, DateFormat.getTimeFormat(activity).format(new Date()));
    if (status != null) setTextIfChanged(status, controller.statusText());
    for (Tile tile : tiles) {
      ControlCenterController.TileState state = controller.state(tile.id);
      String signature =
          state.title
              + "\n"
              + state.subtitle
              + "\n"
              + state.busy
              + state.available
              + state.active
              + state.direct;
      if (signature.equals(tile.lastState)) continue;
      tile.lastState = signature;
      tile.title.setText(state.title);
      tile.subtitle.setText(state.busy ? "En cours…" : state.subtitle);

      tile.box.setEnabled(state.available && !state.busy);
      tile.box.setAlpha(state.available ? 1f : .56f);
      int activeColor = androidStyle ? accent : iosActiveColor(tile.id);
      int surface =
          state.active ? alpha(activeColor, surfaceAlpha()) : alpha(tileColor, surfaceAlpha());
      int foreground =
          state.active
              ? readable(blend(panelColor, activeColor, prefs.getInt("tileOpacity", 28) / 100f))
              : ink;
      if (androidStyle) {
        tile.box.setBackground(
            round(
                surface, u(28), alpha(state.active ? activeColor : ink, state.active ? 160 : 24)));
        tile.icon.setBackground(null);
      } else {
        tile.box.setBackgroundColor(Color.TRANSPARENT);
        tile.icon.setBackground(
            round(
                state.active ? activeColor : alpha(tileColor, surfaceAlpha()),
                u(60),
                alpha(ink, 32)));
        foreground = state.active ? readable(activeColor) : ink;
      }
      tile.icon.setInk(foreground);
      tile.title.setTextColor(foreground);
      if (!androidStyle) tile.title.setTextColor(ink);
      tile.subtitle.setTextColor(state.active ? alpha(foreground, 215) : muted);
      tile.box.setContentDescription(
          state.title
              + ", "
              + (state.busy ? "En cours" : state.subtitle)
              + (!state.available
                  ? ", indisponible"
                  : state.direct ? ", commande directe" : ", ouvre la commande ou les réglages"));
    }
    if (volumeSlider != null && !adjustingVolume) {
      int max = Math.max(1, controller.volumeMax()), value = controller.volume();
      if (volumeSlider.getMax() != max) volumeSlider.setMax(max);
      if (volumeSlider.getProgress() != value) volumeSlider.setProgress(value);
    }
    if (brightnessSlider != null
        && !adjustingBrightness
        && brightnessSlider.getProgress() != controller.brightness())
      brightnessSlider.setProgress(controller.brightness());
    if (verticalVolume != null && !adjustingVolume) verticalVolume.updateValue();
    if (verticalBrightness != null && !adjustingBrightness) verticalBrightness.updateValue();
    if (mediaTitle != null) {
      MusicPlayback.State music = MusicPlayback.state(activity);
      setTextIfChanged(mediaTitle, music.title);
      setTextIfChanged(
          mediaArtist,
          music.hasSession
              ? (music.artist.isEmpty()
                  ? (music.playing ? "Lecture en cours" : "En pause")
                  : music.artist)
              : (music.playing
                  ? "Audio Android · titre non partagé"
                  : "Commandes du lecteur Android"));
      setTextIfChanged(mediaPlay, music.playing ? "Ⅱ" : "▶");
      mediaPlay.setContentDescription(
          music.playing
              ? "Mettre la lecture Android en pause"
              : "Lancer ou reprendre la lecture Android");
    }
    updateSliderLabels();
  }

  private int iosActiveColor(String id) {
    if (prefs.getBoolean("customAccent", false) || prefs.getInt("accent", 0) != 0) return accent;
    if ("wifi".equals(id) || "bluetooth".equals(id)) return 0xff328bff;
    if ("data".equals(id) || "hotspot".equals(id)) return 0xff38cf71;
    if ("airplane".equals(id)) return 0xffffa142;
    if ("dnd".equals(id)) return 0xffaf83ff;
    return accent;
  }

  private void updateSliderLabels() {
    if (volumeLabel != null)
      setTextIfChanged(
          volumeLabel,
          "Volume des médias   " + controller.volume() + " / " + controller.volumeMax());
    if (brightnessLabel != null)
      setTextIfChanged(brightnessLabel, "Luminosité   " + controller.brightnessLabel());
  }

  private static void setTextIfChanged(TextView view, String value) {
    if (!value.contentEquals(view.getText())) view.setText(value);
  }

  private void applyLayout() {
    if (card == null) return;
    ControlCenterGesture.PanelBounds bounds = panelBoundsNow();
    FrameLayout.LayoutParams lp =
        new FrameLayout.LayoutParams(bounds.width, bounds.height, Gravity.TOP | Gravity.LEFT);
    lp.leftMargin = bounds.left;
    lp.topMargin = bounds.top;
    card.setLayoutParams(lp);
  }

  private int columnsFor(int panelWidth) {
    int available = Math.max(1, panelWidth - u(20));
    if (prefs.getInt("style", 0) == 1) return available < Math.max(280, u(290)) ? 1 : 2;
    int fitted = Math.max(1, (available + u(7)) / (Math.max(56, u(65)) + u(7)));
    return Math.min(prefs.getInt("columns", 4), fitted);
  }

  private ControlCenterGesture.PanelBounds panelBoundsNow() {
    int width = getWidth(), height = getHeight();
    if ((width <= 0 || height <= 0) && getParent() instanceof View) {
      width = ((View) getParent()).getWidth();
      height = ((View) getParent()).getHeight();
    }
    if (width <= 0 || height <= 0) {
      width = getResources().getDisplayMetrics().widthPixels;
      height = getResources().getDisplayMetrics().heightPixels;
    }
    return ControlCenterGesture.panelBounds(
        width,
        height,
        prefs.getInt("width", 100),
        prefs.getInt("height", 90),
        prefs.getInt("position", 0));
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    if (!opened || card == null) return false;
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
      insideTouch = insideCard(event.getX(), event.getY());
    // An outward swipe must keep reaching the card after the pointer leaves its bounds.
    // Only gestures that started outside belong to the scrim.
    return !insideTouch;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!opened) return false;
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      outsideDown = !insideCard(event.getX(), event.getY());
      outsideX = event.getX();
      outsideY = event.getY();
    } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
      int closing = prefs.getInt("closing", 0);
      if (outsideDown
          && !insideCard(event.getX(), event.getY())
          && Math.abs(event.getX() - outsideX) < 12
          && Math.abs(event.getY() - outsideY) < 12
          && (closing == 0 || closing == 2)) close();
      outsideDown = false;
    } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) outsideDown = false;
    return true;
  }

  private boolean insideCard(float x, float y) {
    return card != null
        && x >= card.getLeft()
        && x < card.getRight()
        && y >= card.getTop()
        && y < card.getBottom();
  }

  private final class PanelCard extends LinearLayout {
    private float startX, startY;
    private boolean closingCandidate;

    PanelCard() {
      super(activity);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
      if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
        startX = event.getX();
        startY = event.getY();
        int closing = prefs.getInt("closing", 0);
        boolean overHeader =
            header != null && startY >= header.getTop() && startY <= header.getBottom();
        closingCandidate =
            (closing == 0 || closing == 1)
                && (overHeader || overView(footerCloseButton, startX, startY))
                && !overButton(closeButton, startX, startY)
                && !overButton(customizeButton, startX, startY);
      } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE
          && closingCandidate
          && ControlCenterGesture.isClosingSwipe(
              prefs.getInt("position", 0),
              event.getX() - startX,
              event.getY() - startY,
              prefs.getInt("closeDistance", 28))) {
        closingCandidate = false;
        close();
        return true;
      } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL
          || event.getActionMasked() == MotionEvent.ACTION_UP) {
        closingCandidate = false;
      }
      return super.onInterceptTouchEvent(event);
    }

    private boolean overButton(View button, float x, float y) {
      return overView(button, x, y);
    }

    private boolean overView(View button, float x, float y) {
      if (button == null) return false;
      Rect area = new Rect();
      button.getDrawingRect(area);
      offsetDescendantRectToMyCoords(button, area);
      return area.contains((int) x, (int) y);
    }
  }

  /** A real vertical slider, with Android range actions and 44 px minimum width. */
  private final class VerticalLevel extends View {
    private final boolean volume;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF area = new RectF();
    private final android.graphics.Path clip = new android.graphics.Path();
    private final ControlCenterIcon symbol;
    private int value, max;

    VerticalLevel(boolean volume) {
      super(activity);
      this.volume = volume;
      symbol = new ControlCenterIcon(activity, volume ? "volume" : "display");
      setFocusable(true);
      setClickable(true);
      setMinimumWidth(44);
      setMinimumHeight(44);
      setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
      updateValue();
    }

    void updateValue() {
      max = volume ? Math.max(1, controller.volumeMax()) : 100;
      value = Math.max(0, Math.min(max, volume ? controller.volume() : controller.brightness()));
      setContentDescription(
          (volume ? "Volume des médias, " : "Luminosité, ")
              + (volume ? value + " sur " + max : controller.brightnessLabel()));
      invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      area.set(0, 0, getWidth(), getHeight());
      paint.setColor(alpha(tileColor, surfaceAlpha()));
      paint.setStyle(Paint.Style.FILL);
      canvas.drawRoundRect(area, u(23), u(23), paint);
      clip.reset();
      clip.addRoundRect(area, u(23), u(23), android.graphics.Path.Direction.CW);
      int saved = canvas.save();
      canvas.clipPath(clip);
      float fillTop = getHeight() * (1 - value / (float) max);
      paint.setColor(alpha(0xfff2f7ff, 232));
      canvas.drawRect(0, fillTop, getWidth(), getHeight(), paint);
      canvas.restoreToCount(saved);
      paint.setColor(ink);
      paint.setTextAlign(Paint.Align.CENTER);
      paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
      paint.setTextSize(u(9));
      paint.setColor(fillTop < u(19) ? 0xff152136 : ink);
      canvas.drawText(volume ? "VOL." : "LUM.", getWidth() / 2f, u(18), paint);
      paint.setTextSize(u(12));
      paint.setColor(fillTop < u(36) ? 0xff152136 : ink);
      canvas.drawText(Math.round(100f * value / max) + "%", getWidth() / 2f, u(35), paint);
      int size = u(24), left = (getWidth() - size) / 2, top = getHeight() - u(40);
      symbol.setInk(fillTop < top + size / 2f ? 0xff152136 : ink);
      symbol.layout(0, 0, size, size);
      saved = canvas.save();
      canvas.translate(left, top);
      symbol.draw(canvas);
      canvas.restoreToCount(saved);
      paint.setColor(alpha(ink, 38));
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(1);
      area.inset(.5f, .5f);
      canvas.drawRoundRect(area, u(23), u(23), paint);
    }

    private void setLevel(int requested) {
      int next = Math.max(0, Math.min(max, requested));
      if (volume) controller.setVolume(next);
      else controller.setBrightness(next);
      updateValue();
      updateSliderLabels();
    }

    private void setFromY(float y) {
      setLevel(Math.round((1 - Math.max(0, Math.min(getHeight(), y)) / getHeight()) * max));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
      if (!opened) return false;
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
          getParent().requestDisallowInterceptTouchEvent(true);
          if (volume) adjustingVolume = true;
          else adjustingBrightness = true;
          setPressed(true);
          setFromY(event.getY());
          return true;
        case MotionEvent.ACTION_MOVE:
          setFromY(event.getY());
          return true;
        case MotionEvent.ACTION_UP:
          setFromY(event.getY());
          performClick();
          finishAdjustment();
          return true;
        case MotionEvent.ACTION_CANCEL:
          finishAdjustment();
          return true;
        default:
          return true;
      }
    }

    private void finishAdjustment() {
      setPressed(false);
      getParent().requestDisallowInterceptTouchEvent(false);
      if (volume) adjustingVolume = false;
      else adjustingBrightness = false;
      if (opened) refresh();
    }

    @Override
    public boolean performClick() {
      super.performClick();
      return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
        setLevel(value + (keyCode == KeyEvent.KEYCODE_DPAD_UP ? 1 : -1) * Math.max(1, max / 20));
        return true;
      }
      return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
      super.onInitializeAccessibilityNodeInfo(info);
      info.setClassName(SeekBar.class.getName());
      info.setRangeInfo(
          AccessibilityNodeInfo.RangeInfo.obtain(
              AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, 0, max, value));
      info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
      if (value < max)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
      if (value > 0)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
      if (!opened) return false;
      if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
          || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
        setLevel(
            value
                + (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? 1 : -1)
                    * Math.max(1, max / 20));
        return true;
      }
      if (action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()
          && arguments != null) {
        float requested = arguments.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE);
        if (Float.isNaN(requested) || Float.isInfinite(requested)) return false;
        setLevel(Math.round(requested));
        return true;
      }
      return super.performAccessibilityAction(action, arguments);
    }
  }

  private TextView text(String value, float size, int color, boolean bold) {
    TextView view = new TextView(activity);
    view.setText(value);
    view.setTextColor(color);
    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(size));
    view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
    return view;
  }

  private Button button(String title, String description) {
    Button button = new Button(activity);
    button.setText(title);
    button.setAllCaps(false);
    button.setTextColor(ink);
    button.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(24));
    button.setPadding(u(6), 0, u(6), 0);
    button.setMinWidth(0);
    button.setMinimumWidth(0);
    button.setMinHeight(0);
    button.setMinimumHeight(0);
    button.setContentDescription(description);
    button.setBackground(round(alpha(tileColor, surfaceAlpha()), u(16), alpha(ink, 30)));
    return button;
  }

  private int u(float value) {
    return Math.max(0, Math.round(value * prefs.getInt("scale", 100) / 100f));
  }

  private int surfaceAlpha() {
    return percentAlpha(prefs.getInt("tileOpacity", 28));
  }

  private static int percentAlpha(int percent) {
    return Math.round(Math.max(0, Math.min(100, percent)) * 255f / 100f);
  }

  private static int blend(int base, int color, float fraction) {
    return Color.rgb(
        Math.round(Color.red(base) * (1 - fraction) + Color.red(color) * fraction),
        Math.round(Color.green(base) * (1 - fraction) + Color.green(color) * fraction),
        Math.round(Color.blue(base) * (1 - fraction) + Color.blue(color) * fraction));
  }

  private static int alpha(int color, int alpha) {
    return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xffffff);
  }

  private static int readable(int color) {
    double luminance =
        .299 * Color.red(color) + .587 * Color.green(color) + .114 * Color.blue(color);
    return luminance > 145 ? 0xff10202c : Color.WHITE;
  }

  private static GradientDrawable round(int color, float radius, int stroke) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(radius);
    drawable.setStroke(1, stroke);
    return drawable;
  }
}

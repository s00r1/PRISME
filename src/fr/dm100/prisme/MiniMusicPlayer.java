package fr.dm100.prisme;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Common dashboard overlay for every PRISME theme; only its buttons consume touches. */
public final class MiniMusicPlayer extends LinearLayout {
  private static final int[] BG = {
    0xdc213344,
    0xe5223548,
    0xee060a0d,
    0xe0102330,
    0xe51d1238,
    0xee201c24,
    0xf0303943,
    0xeaf7f8fa,
    0xee39291e,
    0xe5201d39,
    0xe52f2730,
    0x80151b26
  };
  private static final int[] ACC = {
    0xff75f1e4,
    0xff80c9ff,
    0xffe6f0f7,
    0xff44ffdb,
    0xffff6fd0,
    0xffffb96b,
    0xffd5e4ed,
    0xff126d87,
    0xffffdc80,
    0xffc6a5ff,
    0xffffdca7,
    0xffffffff
  };
  private final MusicPrefs prefs;
  private TextView status, play;
  private MediaArtworkView artwork;
  private int currentStyle = -1, currentSize = -1;

  public MiniMusicPlayer(Context c, MusicPrefs prefs) {
    super(c);
    this.prefs = prefs;
    setOrientation(VERTICAL);
    setGravity(Gravity.CENTER);
    setClickable(false);
    build();
  }

  private TextView label(String value, int size, int color) {
    TextView tv = new TextView(getContext());
    tv.setText(value);
    tv.setGravity(Gravity.CENTER);
    tv.setTextColor(color);
    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    tv.setPadding(2, 2, 2, 2);
    return tv;
  }

  public int desiredWidth() {
    return Math.max(144, Math.round(178 * prefs.number("size", 85) / 100f));
  }

  public int desiredHeight() {
    int size = prefs.number("size", 85);
    return Math.max(44, Math.round(43 * size / 100f))
        + (prefs.flag("title", true) ? Math.max(28, Math.round(28 * size / 100f)) : 0)
        + 8;
  }

  public void build() {
    removeAllViews();
    int style = prefs.number("style", 0), size = prefs.number("size", 85);
    currentStyle = style;
    currentSize = size;
    int fg = ACC[style], bg = BG[style];
    if (prefs.flag("background", true)) {
      GradientDrawable background = new GradientDrawable();
      background.setColor(bg);
      background.setCornerRadius(Math.max(5, Math.round(13 * size / 100f)));
      background.setStroke(style == 7 ? 1 : 2, fg);
      setBackground(background);
    } else setBackgroundColor(Color.TRANSPARENT);
    setPadding(4, 4, 4, 4);
    int buttonPx = Math.max(17, Math.round(24 * size / 100f));
    if (prefs.flag("title", true)) {
      LinearLayout info = new LinearLayout(getContext());
      info.setGravity(Gravity.CENTER_VERTICAL);
      artwork = new MediaArtworkView(getContext());
      artwork.colors(bg, fg);
      int side = Math.max(22, Math.min(40, Math.round(24 * size / 100f)));
      LayoutParams artLp = new LayoutParams(side, side);
      artLp.rightMargin = 5;
      info.addView(artwork, artLp);
      status = label("", Math.max(12, Math.round(12 * size / 100f)), fg);
      I18n.userText(status);
      status.setSingleLine(true);
      status.setEllipsize(android.text.TextUtils.TruncateAt.END);
      info.addView(status, new LayoutParams(0, -1, 1));
      info.setMinimumHeight(28);
      info.setOnClickListener(v -> MediaPanel.launch(getContext()));
      info.setContentDescription("Ouvrir les titres, pochettes et réglages musicaux");
      addView(info, new LayoutParams(-1, Math.max(28, Math.round(28 * size / 100f))));
    } else {
      status = null;
      artwork = null;
    }
    LinearLayout controls = new LinearLayout(getContext());
    controls.setOrientation(HORIZONTAL);
    controls.setGravity(Gravity.CENTER);
    addView(controls, new LayoutParams(-1, Math.max(44, Math.round(43 * size / 100f))));
    TextView back = label("⏮", buttonPx, fg);
    play = label("▶", buttonPx + 2, fg);
    TextView next = label("⏭", buttonPx, fg);
    controls.addView(back, new LayoutParams(0, -1, 1));
    controls.addView(play, new LayoutParams(0, -1, 1));
    controls.addView(next, new LayoutParams(0, -1, 1));
    back.setContentDescription("Piste précédente");
    play.setContentDescription("Lecture ou pause");
    next.setContentDescription("Piste suivante");
    back.setOnClickListener(v -> MusicPlayback.command(getContext(), MusicPlayback.PREVIOUS));
    play.setOnClickListener(v -> MusicPlayback.command(getContext(), MusicPlayback.TOGGLE));
    next.setOnClickListener(v -> MusicPlayback.command(getContext(), MusicPlayback.NEXT));
    setAlpha(prefs.number("opacity", 100) / 100f);
  }

  public void update() {
    if (currentStyle != prefs.number("style", 0) || currentSize != prefs.number("size", 85))
      build();
    MusicPlayback.State snapshot = MusicPlayback.state(getContext());
    if (play != null) play.setText(snapshot.playing ? "Ⅱ" : "▶");
    if (artwork != null) artwork.artwork(snapshot.artwork);
    if (status != null) {
      String value = MediaPanel.title(snapshot);
      if (!snapshot.artist.isEmpty()) value += " · " + snapshot.artist;
      status.setText(value);
      status.setContentDescription(value + ". " + MediaPanel.detail(snapshot));
    }
  }
}

package fr.dm100.prisme;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** A modal child of PRISME's activity, never a system overlay or accessibility service. */
public final class ControlCenterPanel extends FrameLayout {
    public interface Listener {
        void onCustomize();
        void onClosed();
    }
    private static final int[] ACCENTS = {0xff62d9cc, 0xff59c9ef, 0xff78a9ff, 0xffbe9bff, 0xffffb275, 0xfff79fc2};
    private final Activity activity;
    private final ControlCenterPrefs prefs;
    private final ControlCenterController controller;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Tile> tiles = new ArrayList<Tile>();
    private PanelCard card;
    private LinearLayout header;
    private Button closeButton, customizeButton, footerCloseButton;
    private TextView status, volumeLabel, brightnessLabel;
    private SeekBar volumeSlider, brightnessSlider;
    private boolean opened, adjustingVolume, adjustingBrightness;
    private float outsideX, outsideY;
    private boolean outsideDown;
    private boolean insideTouch;
    private int ink, muted, accent, panelColor, tileColor;
    private int effectiveColumns;
    private final Runnable poll = new Runnable() {
        @Override public void run() {
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
            this.id = id; this.box = box; this.icon = icon; this.title = title; this.subtitle = subtitle;
        }
    }

    public ControlCenterPanel(Activity activity, ControlCenterPrefs prefs,
                              ControlCenterController controller, Listener listener) {
        super(activity);
        if (prefs == null || controller == null || listener == null) throw new IllegalArgumentException("Centre de contrôle incomplet");
        this.activity = activity; this.prefs = prefs; this.controller = controller; this.listener = listener;
        setVisibility(GONE); setClickable(true); setFocusableInTouchMode(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription("Centre de contrôle PRISME");
    }
    public boolean isOpen() { return opened; }
    public void open() { open(false); }
    public void open(boolean preview) {
        if (!preview && !prefs.getBoolean("enabled", true)) return;
        handler.removeCallbacks(poll);
        opened = true; build(); setVisibility(VISIBLE); bringToFront(); requestFocus();
        applyLayout(); refresh(); handler.postDelayed(poll, 1000);
        announceForAccessibility("Centre de contrôle ouvert. Bouton Fermer en haut.");
    }
    public void close() {
        boolean wasOpen = opened;
        opened = false; outsideDown = insideTouch = false; adjustingVolume = adjustingBrightness = false;
        handler.removeCallbacks(poll); setVisibility(GONE);
        if (wasOpen) listener.onClosed();
    }
    public void onHostPause() { close(); }
    @Override protected void onDetachedFromWindow() {
        close(); super.onDetachedFromWindow();
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (opened) {
            if (effectiveColumns != columnsFor(panelBoundsNow().width)) { build(); refresh(); }
            applyLayout();
        }
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (opened && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP) close();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
    private void build() {
        removeAllViews(); tiles.clear();
        adjustingVolume = adjustingBrightness = false;
        status = volumeLabel = brightnessLabel = null;
        volumeSlider = brightnessSlider = null;
        boolean dark = prefs.getBoolean("dark", true);
        ink = dark ? 0xfff4f8ff : 0xff132237;
        muted = dark ? 0xffbdcadb : 0xff455b72;
        panelColor = dark ? 0xff172334 : 0xffedf5ff;
        tileColor = dark ? 0xff26374b : 0xfff9fcff;
        accent = prefs.getBoolean("customAccent", false)
                ? 0xff000000 | (prefs.getInt("customColor", 0x62d9cc) & 0xffffff)
                : ACCENTS[Math.max(0, Math.min(ACCENTS.length - 1, prefs.getInt("accent", 0)))];
        setBackgroundColor(Color.argb(percentAlpha(prefs.getInt("scrimOpacity",8)), 0, 0, 0));
        card = new PanelCard(); card.setOrientation(LinearLayout.VERTICAL); card.setClickable(true);
        card.setPadding(u(10), u(8), u(10), u(8));
        int alpha = percentAlpha(prefs.getInt("opacity", 68));
        GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{alpha(panelColor, alpha), alpha(dark ? 0xff0e1624 : 0xffdcecff, alpha)});
        glass.setCornerRadius(u(prefs.getInt("radius", 24)));
        glass.setStroke(1, alpha(ink, dark ? 48 : 38));
        card.setBackground(glass); card.setElevation(u(4));
        addView(card);

        header = new LinearLayout(activity); header.setOrientation(LinearLayout.VERTICAL);
        // Give the card a child touch target even on blank header space, so MOVE is intercepted.
        header.setClickable(true);
        View grip = new View(activity); grip.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        grip.setBackground(round(alpha(ink, 92), u(3), Color.TRANSPARENT));
        LinearLayout.LayoutParams gripLp = new LinearLayout.LayoutParams(u(43), Math.max(3, u(4)));
        gripLp.gravity = Gravity.CENTER_HORIZONTAL; gripLp.bottomMargin = u(4); header.addView(grip, gripLp);
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL); headerRow.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(headerRow, new LinearLayout.LayoutParams(-1, -2));
        TextView heading = text(prefs.getInt("style", 0) == 0 ? "Centre de contrôle" : "Réglages rapides", 18, ink, true);
        heading.setMaxLines(2); heading.setEllipsize(TextUtils.TruncateAt.END);
        headerRow.addView(heading, new LinearLayout.LayoutParams(0, Math.max(48, u(48)), 1));
        customizeButton = button("⚙", "Personnaliser le centre de contrôle");
        customizeButton.setOnClickListener(v -> { close(); listener.onCustomize(); });
        headerRow.addView(customizeButton, new LinearLayout.LayoutParams(Math.max(44, u(44)), Math.max(44, u(44))));
        closeButton = button("×", "Fermer le centre de contrôle");
        closeButton.setOnClickListener(v -> close());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(Math.max(44, u(44)), Math.max(44, u(44)));
        closeLp.leftMargin = u(5); headerRow.addView(closeButton, closeLp);
        card.addView(header, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroller = new ScrollView(activity);
        scroller.setFillViewport(false); scroller.setClipToPadding(false);
        scroller.setContentDescription("Commandes et curseurs du centre de contrôle");
        LinearLayout body = new LinearLayout(activity); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, u(4), 0, u(8)); scroller.addView(body);
        card.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
        if (prefs.getBoolean("status", true)) {
            status = text("", 12, muted, false); status.setMaxLines(3);
            status.setPadding(u(4), u(4), u(4), u(10)); body.addView(status);
        }
        List<String> selected = prefs.getTiles();
        int columns = columnsFor(panelBoundsNow().width); effectiveColumns = columns;
        LinearLayout row = null;
        for (int i = 0; i < selected.size(); i++) {
            if (i % columns == 0) {
                row = new LinearLayout(activity); row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2); rowLp.bottomMargin = u(8);
                body.addView(row, rowLp);
            }
            Tile tile = makeTile(selected.get(i)); tiles.add(tile);
            LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, Math.max(70, u(prefs.getBoolean("labels", true) ? 91 : 66)), 1);
            if (i % columns > 0) cell.leftMargin = u(7); row.addView(tile.box, cell);
            if (i == selected.size() - 1) for (int j = (i % columns) + 1; j < columns; j++) {
                View spacer = new View(activity); LinearLayout.LayoutParams empty = new LinearLayout.LayoutParams(0, 1, 1);
                empty.leftMargin = u(7); row.addView(spacer, empty);
            }
        }
        if (selected.isEmpty()) {
            TextView empty = text("Ajoute tes commandes avec le bouton Personnaliser.", 14, muted, false);
            empty.setPadding(u(8), u(14), u(8), u(14)); body.addView(empty);
        }
        if (prefs.getBoolean("volume", true)) addSlider(body, true);
        if (prefs.getBoolean("brightness", true)) addSlider(body, false);
        Button customize = button("Personnaliser", "Organiser les commandes et l’apparence");
        customize.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(14));
        customize.setOnClickListener(v -> { close(); listener.onCustomize(); });
        LinearLayout.LayoutParams customizeLp = new LinearLayout.LayoutParams(-1, Math.max(44, u(44)));
        customizeLp.topMargin = u(5); body.addView(customize, customizeLp);
        String[] arrows={"↑","↓","←","→"};
        footerCloseButton=button("Fermer  "+arrows[prefs.getInt("position",0)],"Fermer le centre. Toucher ou glisser vers l’extérieur depuis cette barre.");
        footerCloseButton.setTextSize(TypedValue.COMPLEX_UNIT_PX,u(15));
        footerCloseButton.setOnClickListener(v->close());
        LinearLayout.LayoutParams footerLp=new LinearLayout.LayoutParams(-1,Math.max(44,u(44)));
        footerLp.topMargin=u(5);card.addView(footerCloseButton,footerLp);
    }
    private Tile makeTile(final String id) {
        boolean androidStyle = prefs.getInt("style", 0) == 1;
        LinearLayout box = new LinearLayout(activity); box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(androidStyle ? Gravity.LEFT | Gravity.CENTER_VERTICAL : Gravity.CENTER);
        box.setPadding(u(9), u(7), u(9), u(7)); box.setClickable(true); box.setFocusable(true);
        ControlCenterIcon icon = new ControlCenterIcon(activity, id);
        icon.setInk(ink); icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(u(29), u(29));
        iconLp.bottomMargin = u(4); box.addView(icon, iconLp);
        TextView title = text(ControlCenterController.label(id), 13, ink, true);
        title.setGravity(androidStyle ? Gravity.LEFT : Gravity.CENTER);
        title.setMaxLines(1); title.setEllipsize(TextUtils.TruncateAt.END);
        box.addView(title, new LinearLayout.LayoutParams(-1, -2));
        TextView subtitle = text("", 10, muted, false);
        subtitle.setGravity(androidStyle ? Gravity.LEFT : Gravity.CENTER);
        subtitle.setMaxLines(2); subtitle.setEllipsize(TextUtils.TruncateAt.END);
        box.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));
        if (!prefs.getBoolean("labels", true)) { title.setVisibility(GONE); subtitle.setVisibility(GONE); }
        box.setOnClickListener(v -> { if (opened) { controller.perform(id); if (opened) refresh(); } });
        box.setOnLongClickListener(v -> { if (opened) { controller.longPress(id); if (opened) refresh(); } return true; });
        return new Tile(id, box, icon, title, subtitle);
    }
    private void addSlider(LinearLayout body, final boolean volume) {
        LinearLayout block = new LinearLayout(activity); block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(u(10), u(8), u(10), u(4));
        block.setBackground(round(alpha(tileColor, surfaceAlpha()), u(18), alpha(ink, 28)));
        TextView label = text("", 13, ink, true); block.addView(label);
        SeekBar slider = new SeekBar(activity); slider.setMax(volume ? Math.max(1, controller.volumeMax()) : 100);
        slider.setContentDescription(volume ? "Volume des médias" : "Luminosité de l’écran");
        LinearLayout.LayoutParams sliderLp = new LinearLayout.LayoutParams(-1, Math.max(44, u(44))); block.addView(slider, sliderLp);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar bar) {
                if (volume) adjustingVolume = true; else adjustingBrightness = true;
            }
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (!opened || !fromUser) return;
                if (volume) controller.setVolume(value); else controller.setBrightness(value);
                updateSliderLabels();
            }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                if (volume) adjustingVolume = false; else adjustingBrightness = false;
                if (opened) refresh();
            }
        });
        LinearLayout.LayoutParams blockLp = new LinearLayout.LayoutParams(-1, -2); blockLp.topMargin = u(7); body.addView(block, blockLp);
        if (volume) { volumeLabel = label; volumeSlider = slider; }
        else { brightnessLabel = label; brightnessSlider = slider; }
    }
    public void refresh() {
        if (!opened) return;
        if (status != null) setTextIfChanged(status,controller.statusText());
        for (Tile tile : tiles) {
            ControlCenterController.TileState state = controller.state(tile.id);
            String signature=state.title+"\n"+state.subtitle+"\n"+state.busy+state.available+state.active+state.direct;
            if(signature.equals(tile.lastState))continue;
            tile.lastState=signature;
            tile.title.setText(state.title);
            tile.subtitle.setText(state.busy ? "En cours…" : state.subtitle);

            tile.box.setEnabled(state.available && !state.busy);
            tile.box.setAlpha(state.available ? 1f : .56f);
            int surface = state.active ? alpha(accent, surfaceAlpha()) : alpha(tileColor, surfaceAlpha());
            int foreground = state.active ? readable(blend(panelColor,accent,prefs.getInt("tileOpacity",28)/100f)) : ink;
            tile.box.setBackground(round(surface, u(prefs.getInt("style", 0) == 0 ? 20 : 13), alpha(state.active ? accent : ink, state.active ? 180 : 32)));
            tile.icon.setInk(foreground); tile.title.setTextColor(foreground);
            tile.subtitle.setTextColor(state.active ? alpha(foreground, 215) : muted);
            tile.box.setContentDescription(state.title + ", " + (state.busy ? "En cours" : state.subtitle)
                    + (!state.available ? ", indisponible" : state.direct ? ", commande directe" : ", ouvre la commande ou les réglages"));
        }
        if (volumeSlider != null && !adjustingVolume) {
            int max=Math.max(1,controller.volumeMax()),value=controller.volume();
            if(volumeSlider.getMax()!=max)volumeSlider.setMax(max);
            if(volumeSlider.getProgress()!=value)volumeSlider.setProgress(value);
        }
        if (brightnessSlider != null && !adjustingBrightness&&brightnessSlider.getProgress()!=controller.brightness()) brightnessSlider.setProgress(controller.brightness());
        updateSliderLabels();
    }
    private void updateSliderLabels() {
        if (volumeLabel != null) setTextIfChanged(volumeLabel,"Volume des médias   " + controller.volume() + " / " + controller.volumeMax());
        if (brightnessLabel != null) setTextIfChanged(brightnessLabel,"Luminosité   " + controller.brightnessLabel());
    }
    private static void setTextIfChanged(TextView view,String value){if(!value.contentEquals(view.getText()))view.setText(value);}
    private void applyLayout() {
        if (card == null) return;
        ControlCenterGesture.PanelBounds bounds = panelBoundsNow();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(bounds.width, bounds.height, Gravity.TOP | Gravity.LEFT);
        lp.leftMargin = bounds.left; lp.topMargin = bounds.top; card.setLayoutParams(lp);
    }
    private int columnsFor(int panelWidth) {
        int available = Math.max(1, panelWidth - u(20));
        int fitted = Math.max(1, (available + u(7)) / (Math.max(56, u(65)) + u(7)));
        return Math.min(prefs.getInt("columns", 4), fitted);
    }
    private ControlCenterGesture.PanelBounds panelBoundsNow() {
        int width = getWidth(), height = getHeight();
        if ((width <= 0 || height <= 0) && getParent() instanceof View) {
            width = ((View)getParent()).getWidth(); height = ((View)getParent()).getHeight();
        }
        if (width <= 0 || height <= 0) {
            width = getResources().getDisplayMetrics().widthPixels; height = getResources().getDisplayMetrics().heightPixels;
        }
        return ControlCenterGesture.panelBounds(width, height,
                prefs.getInt("width", 100), prefs.getInt("height", 90), prefs.getInt("position", 0));
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!opened || card == null) return false;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) insideTouch = insideCard(event.getX(), event.getY());
        // An outward swipe must keep reaching the card after the pointer leaves its bounds.
        // Only gestures that started outside belong to the scrim.
        return !insideTouch;
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!opened) return false;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            outsideDown = !insideCard(event.getX(), event.getY()); outsideX = event.getX(); outsideY = event.getY();
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            int closing = prefs.getInt("closing", 0);
            if (outsideDown && !insideCard(event.getX(), event.getY())
                    && Math.abs(event.getX() - outsideX) < 12 && Math.abs(event.getY() - outsideY) < 12
                    && (closing == 0 || closing == 2)) close();
            outsideDown = false;
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) outsideDown = false;
        return true;
    }
    private boolean insideCard(float x, float y) {
        return card != null && x >= card.getLeft() && x < card.getRight() && y >= card.getTop() && y < card.getBottom();
    }
    private final class PanelCard extends LinearLayout {
        private float startX, startY;
        private boolean closingCandidate;
        PanelCard() { super(activity); }
        @Override public boolean onInterceptTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                startX = event.getX(); startY = event.getY();
                int closing = prefs.getInt("closing", 0);
                boolean overHeader = header != null && startY >= header.getTop() && startY <= header.getBottom();
                closingCandidate = (closing == 0 || closing == 1) && (overHeader || overView(footerCloseButton,startX,startY))
                        && !overButton(closeButton, startX, startY) && !overButton(customizeButton, startX, startY);
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && closingCandidate
                    && ControlCenterGesture.isClosingSwipe(prefs.getInt("position", 0), event.getX() - startX,
                    event.getY() - startY, prefs.getInt("closeDistance", 28))) {
                closingCandidate = false; close(); return true;
            } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL || event.getActionMasked() == MotionEvent.ACTION_UP) {
                closingCandidate = false;
            }
            return super.onInterceptTouchEvent(event);
        }
        private boolean overButton(View button, float x, float y) {
            return overView(button, x, y);
        }
        private boolean overView(View button, float x, float y) {
            if (button == null) return false;
            Rect area = new Rect(); button.getDrawingRect(area); offsetDescendantRectToMyCoords(button, area);
            return area.contains((int)x, (int)y);
        }
    }
    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(activity); view.setText(value); view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(size));
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }
    private Button button(String title, String description) {
        Button button = new Button(activity); button.setText(title); button.setAllCaps(false);
        button.setTextColor(ink); button.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(24));
        button.setPadding(u(6), 0, u(6), 0); button.setMinWidth(0); button.setMinimumWidth(0);
        button.setMinHeight(0); button.setMinimumHeight(0);
        button.setContentDescription(description); button.setBackground(round(alpha(tileColor, surfaceAlpha()), u(16), alpha(ink, 30)));
        return button;
    }
    private int u(float value) { return Math.max(0, Math.round(value * prefs.getInt("scale", 100) / 100f)); }
    private int surfaceAlpha(){return percentAlpha(prefs.getInt("tileOpacity",28));}
    private static int percentAlpha(int percent){return Math.round(Math.max(0,Math.min(100,percent))*255f/100f);}
    private static int blend(int base,int color,float fraction){
        return Color.rgb(Math.round(Color.red(base)*(1-fraction)+Color.red(color)*fraction),
            Math.round(Color.green(base)*(1-fraction)+Color.green(color)*fraction),Math.round(Color.blue(base)*(1-fraction)+Color.blue(color)*fraction));
    }
    private static int alpha(int color, int alpha) { return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xffffff); }
    private static int readable(int color) {
        double luminance = .299 * Color.red(color) + .587 * Color.green(color) + .114 * Color.blue(color);
        return luminance > 145 ? 0xff10202c : Color.WHITE;
    }
    private static GradientDrawable round(int color, float radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(radius);
        drawable.setStroke(1, stroke); return drawable;
    }
}

package fr.dm100.prisme.volume;
import fr.dm100.prisme.I18n;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.Toast;
import fr.dm100.prisme.FloatingVolumePrefs;
import java.util.ArrayList;
import java.util.List;

/** One small service-owned window. It never captures the screen or touches outside its own controls. */
final class FloatingVolumeOverlay {
    private static final int[] STREAMS = {AudioManager.STREAM_MUSIC, AudioManager.STREAM_RING, AudioManager.STREAM_ALARM, AudioManager.STREAM_VOICE_CALL};
    private static final String[] STREAM_NAMES = {"Médias", "Sonnerie", "Alarme", "Appel en cours"};
    private static final String[] STREAM_SHORT = {"MÉD", "SON", "ALR", "APL"};
    private static final int[] ACCENTS = {0xff62d9cc, 0xff59c9ef, 0xff78a9ff, 0xffbe9bff, 0xffffb275, 0xfff79fc2};
    private final FloatingVolumeService owner;
    private final FloatingVolumePrefs prefs;
    private final WindowManager windows;
    private final AudioManager audio;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Key> keys = new ArrayList<Key>();
    private LinearLayout root;
    private WindowManager.LayoutParams params;
    private FloatingVolumeGeometry geometry;
    private boolean added, expanded, touching, gestureCancelled, blocked, levelKnown;
    private int current, maximum, accent, foreground, fill, fill2, border;
    private String geometrySource = "non mesurée", signature = "", collapsePolicy = "";
    private int appliedX = -1, appliedY = -1;
    private Runnable repeat, longPress;
    private final Runnable idleFade=new Runnable(){public void run(){if(added&&root!=null&&!touching&&!expanded&&prefs.getBoolean("idleFade",false))root.setAlpha(prefs.getInt("idleOpacity",40)/100f);}};
    private void scheduleIdle(){handler.removeCallbacks(idleFade);if(root!=null)root.setAlpha(1f);if(prefs.getBoolean("idleFade",false))handler.postDelayed(idleFade,prefs.getInt("idleDelay",8)*1000L);}

    private final Runnable collapse = new Runnable() { @Override public void run() {
        if (added && !touching && expanded && prefs.getInt("layout", 0) == 2 && prefs.getBoolean("autoCollapse", true)) {
            expanded = false; rebuild();
        }
    }};

    FloatingVolumeOverlay(FloatingVolumeService owner) {
        this.owner = owner; prefs = new FloatingVolumePrefs(owner);
        windows = (WindowManager)owner.getSystemService(Context.WINDOW_SERVICE);
        audio = (AudioManager)owner.getSystemService(Context.AUDIO_SERVICE);
    }
    boolean show() {
        if (windows == null || audio == null) { FloatingVolumeService.report("Affichage ou audio indisponible."); return false; }
        return buildWindow();
    }
    private boolean buildWindow() {
        Point real = realSize();
        geometry = new FloatingVolumeGeometry(real.x, real.y, prefs.getInt("size", 56), prefs.getInt("layout", 0),
                expanded, prefs.getBoolean("showMute", false), prefs.getInt("shape", 0));
        signature = configuration(); collapsePolicy = collapsePolicy(); selectPalette();
        root = new LinearLayout(owner); root.setOrientation(geometry.horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        root.setGravity(Gravity.TOP | Gravity.LEFT); root.setMotionEventSplittingEnabled(false);
        root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int shape = prefs.getInt("shape", 0);
        if (shape == 0 || shape == 2) {
            GradientDrawable shell = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{fill, fill2});
            shell.setCornerRadius(shape == 0 ? Math.min(geometry.width, geometry.height) / 2f : geometry.cellSize * .24f);
            shell.setStroke(prefs.getInt("style", 0) == 2 ? 2 : 1, border); root.setBackground(shell);
        } else root.setBackgroundColor(Color.TRANSPARENT);
        root.setElevation(4);
        keys.clear();
        for (FloatingVolumeGeometry.Cell cell : geometry.cells) {
            Key key = new Key(cell.role); keys.add(key);
            int width = Math.max(1, Math.round(cell.width)), height = Math.max(1, Math.round(cell.height));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
            if (geometry.horizontal) {
                lp.leftMargin = Math.max(0, Math.round(cell.left - (keys.size() == 1 ? 0 : geometry.cells[keys.size() - 2].left + geometry.cellSize)));
                lp.topMargin = Math.max(0, Math.round(cell.top)); lp.bottomMargin = Math.max(0, geometry.height - height - lp.topMargin);
            } else {
                lp.topMargin = Math.max(0, Math.round(cell.top - (keys.size() == 1 ? 0 : geometry.cells[keys.size() - 2].top + geometry.cellSize)));
                lp.leftMargin = Math.max(0, Math.round(cell.left)); lp.rightMargin = Math.max(0, geometry.width - width - lp.leftMargin);
            }
            root.addView(key, lp);
        }
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(final View view) {
                view.postOnAnimation(new Runnable() { @Override public void run() {
                    if (root == view && added) owner.stateChanged();
                }});
            }
            @Override public void onViewDetachedFromWindow(View view) {
                if (root == view) { added = false; cancelTimers(); owner.stateChanged(); }
            }
        });
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        params = new WindowManager.LayoutParams(geometry.width, geometry.height, FloatingVolumeService.windowType(), flags, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT; params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
        params.alpha = (expanded && prefs.getInt("layout", 0) == 2 ? prefs.getInt("expandedOpacity", 95) : prefs.getInt("opacity", 90)) / 100f;
        params.setTitle("fr.dm100.prisme:FloatingVolume");
        appliedX = prefs.getInt("xPercent", 100); appliedY = prefs.getInt("yPercent", 55);
        params.x = geometry.xAt(appliedX); params.y = geometry.yAt(appliedY);
        try {
            windows.addView(root, params); added = true; refreshLevel(); scheduleCollapse(); return true;
        } catch (RuntimeException ex) {
            FloatingVolumeService.report("Fenêtre de volume refusée : " + ex.getClass().getSimpleName());
            destroy(); return false;
        }
    }
    void refresh(boolean applyPosition) {
        if (!configuration().equals(signature) || !isShowing()) { rebuild(); return; }
        String policy = collapsePolicy();
        if (!policy.equals(collapsePolicy)) { collapsePolicy = policy; scheduleCollapse(); }
        if (applyPosition && !touching && params != null && geometry != null) {
            int x = prefs.getInt("xPercent", 100), y = prefs.getInt("yPercent", 55);
            if (x != appliedX || y != appliedY) {
                appliedX = x; appliedY = y; params.x = geometry.xAt(x); params.y = geometry.yAt(y); updatePosition();
            }
        }
        refreshLevel();
    }
    private void rebuild() {
        removeWindow();
        if (prefs.getInt("layout", 0) != 2) expanded = false;
        buildWindow(); owner.stateChanged();
    }
    void refreshLevel() {
        if (audio == null) return;
        try {
            current = audio.getStreamVolume(stream()); maximum = audio.getStreamMaxVolume(stream());
            levelKnown = true;
            for (Key key : keys) { key.updateDescription(); key.invalidate(); }
        } catch (RuntimeException ex) {
            FloatingVolumeService.report("Lecture du volume refusée : " + ex.getClass().getSimpleName());
            levelKnown = false; blocked = true; for (Key key : keys) { key.updateDescription(); key.invalidate(); }
        }
    }
    private boolean adjust(int direction) {
        if (!added) return false;
        try {
            int before = audio.getStreamVolume(stream()), max = audio.getStreamMaxVolume(stream());
            if (direction > 0 && before >= max || direction < 0 && before <= 0) { cancelRepeat(); refreshLevel(); return false; }
            audio.adjustStreamVolume(stream(), direction > 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
            blocked = false; FloatingVolumeService.report(""); refreshLevel(); owner.stateChanged();
            if (current == before || direction > 0 && current >= maximum || direction < 0 && current <= 0) { cancelRepeat(); return false; }
            return true;
        } catch (SecurityException ex) {
            blocked = true; cancelRepeat();
            FloatingVolumeService.report("Le système ou Ne pas déranger bloque ce volume. Choisis Médias ou règle ce mode dans Android.");
            I18n.makeToast(owner, "Volume bloqué par Android / Ne pas déranger.", Toast.LENGTH_LONG).show();
            refreshLevel(); openSettings(); return false;
        } catch (RuntimeException ex) {
            blocked = true; cancelRepeat(); FloatingVolumeService.report("Réglage audio refusé : " + ex.getClass().getSimpleName());
            refreshLevel(); owner.stateChanged(); return false;
        }
    }
    private void toggleMute() {
        try {
            audio.adjustStreamVolume(stream(), audio.isStreamMute(stream()) ? AudioManager.ADJUST_UNMUTE : AudioManager.ADJUST_MUTE, 0);
            blocked = false; FloatingVolumeService.report(""); refreshLevel(); owner.stateChanged();
        } catch (SecurityException ex) {
            blocked = true; FloatingVolumeService.report("Android / Ne pas déranger refuse le mode muet de ce canal."); openSettings();
        } catch (RuntimeException ex) { blocked = true; FloatingVolumeService.report("Mode muet refusé : " + ex.getClass().getSimpleName()); owner.stateChanged(); }
    }
    private void activate(int role) {
        buzz();
        if (role == FloatingVolumeGeometry.PLUS || role == FloatingVolumeGeometry.MINUS) adjust(role);
        else if (role == FloatingVolumeGeometry.MUTE) toggleMute();
        else if (prefs.getInt("layout", 0) == 2) { expanded = !expanded; rebuild(); }
        else openSettings();
        scheduleCollapse();
    }
    private void openSettings() {
        cancelTimers();
        try { owner.startActivity(FloatingVolumeService.settingsIntent(owner)); }
        catch (RuntimeException ex) { FloatingVolumeService.report("Réglages PRISME indisponibles : " + ex.getClass().getSimpleName()); }
        owner.stateChanged();
    }
    private void scheduleCollapse() {
        scheduleIdle();
        handler.removeCallbacks(collapse);
        if (added && expanded && !touching && prefs.getInt("layout", 0) == 2 && prefs.getBoolean("autoCollapse", true))
            handler.postDelayed(collapse, prefs.getInt("collapseDelay", 4) * 1000L);
    }
    private void cancelRepeat() { if (repeat != null) handler.removeCallbacks(repeat); repeat = null; }
    private void cancelLongPress() { if (longPress != null) handler.removeCallbacks(longPress); longPress = null; }
    void cancelTimers() { cancelRepeat(); cancelLongPress(); handler.removeCallbacks(collapse);handler.removeCallbacks(idleFade);if(root!=null)root.setAlpha(1f); touching = false; gestureCancelled = true; }
    private void removeWindow() {
        cancelTimers();
        LinearLayout old = root;
        if (old != null && added) { try { windows.removeViewImmediate(old); } catch (RuntimeException ignored) { } }
        added = false; root = null; keys.clear();
    }
    void destroy() { removeWindow(); handler.removeCallbacksAndMessages(null); }
    boolean isShowing() { return added && root != null && root.isAttachedToWindow(); }
    private boolean updatePosition() {
        if (!added || root == null) return false;
        try { windows.updateViewLayout(root, params); return true; }
        catch (RuntimeException ex) { FloatingVolumeService.report("Déplacement refusé : " + ex.getClass().getSimpleName()); destroy(); owner.stateChanged(); return false; }
    }
    private void savePosition() {
        if (params == null || geometry == null) return;
        appliedX = geometry.xPercent(params.x); appliedY = geometry.yPercent(params.y);
        prefs.putInt("xPercent", appliedX); prefs.putInt("yPercent", appliedY);
    }
    private Point realSize() {
        Point result = new Point();
        if (windows != null) {
            try { windows.getDefaultDisplay().getRealSize(result); geometrySource = "getRealSize"; } catch (RuntimeException ignored) { }
            if (result.x <= 0 || result.y <= 0) try { windows.getDefaultDisplay().getSize(result); geometrySource = "getSize (repli)"; } catch (RuntimeException ignored) { }
        }
        if (result.x <= 0 || result.y <= 0) {
            result.set(owner.getResources().getDisplayMetrics().widthPixels, owner.getResources().getDisplayMetrics().heightPixels);
            geometrySource = "DisplayMetrics (repli)";
        }
        result.x = Math.max(1, result.x); result.y = Math.max(1, result.y); return result;
    }
    private int index() { return Math.max(0, Math.min(3, prefs.getInt("stream", 0))); }
    private int stream() { return STREAMS[index()]; }
    private String configuration() {
        StringBuilder key = new StringBuilder();
        for (String name : new String[]{"style", "shape", "layout", "size", "opacity", "expandedOpacity", "accent", "customColor"}) key.append(prefs.getInt(name, 0)).append(':');
        for (String name : new String[]{"customAccent", "showLevel", "showMute"}) key.append(prefs.getBoolean(name, false)).append(':');
        return key.toString();
    }
    private String collapsePolicy() { return prefs.getBoolean("idleFade",false)+":"+prefs.getInt("idleDelay",8)+":"+prefs.getInt("idleOpacity",40)+":"+prefs.getBoolean("autoCollapse", true) + ":" + prefs.getInt("collapseDelay", 4); }
    private void selectPalette() {
        accent = prefs.getBoolean("customAccent", false) ? 0xff000000 | (prefs.getInt("customColor", 0x62d9cc) & 0xffffff)
                : ACCENTS[Math.max(0, Math.min(5, prefs.getInt("accent", 0)))];
        foreground = 0xfff3f8ff; fill = 0xf0192b3d; fill2 = 0xe6101926; border = alpha(accent, 160);
        switch (prefs.getInt("style", 0)) {
            case 1: fill = 0xff202c39; fill2 = 0xff18212c; border = 0xff70869b; break;
            case 2: fill = 0xee101624; fill2 = 0xee15192b; border = accent; break;
            case 3: fill = 0x8c101a24; fill2 = 0x8c101a24; border = alpha(accent, 130); break;
            case 4: fill = 0xff4b5867; fill2 = 0xff18232d; border = 0xffbdc9d3; break;
            case 5: fill = 0xf5f6fbff; fill2 = 0xf5e1edf9; border = 0xff7792ab; foreground = 0xff172739; break;
            default: break;
        }
    }
    private void buzz() {
        if (!prefs.getBoolean("haptic", false)) return;
        try { Vibrator v = (Vibrator)owner.getSystemService(Context.VIBRATOR_SERVICE); if (v != null && v.hasVibrator()) v.vibrate(10); }
        catch (RuntimeException ignored) { }
    }
    String diagnostics() {
        return displayReport(owner) + (geometry == null ? "" : "\nZone : " + geometry.screenWidth + "×" + geometry.screenHeight + " px (" + geometrySource + ")"
                + "\nPastille : " + geometry.width + "×" + geometry.height + " px · touches " + Math.round(geometry.cellSize) + " px"
                + "\nPosition : " + (params == null ? "—" : params.x + ", " + params.y)
                + "\nDéploiement : " + (expanded ? "ouvert" : "compact"));
    }
    static String displayReport(Context context) {
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE); Point app = new Point(), real = new Point();
        String appText = "indisponible", realText = "indisponible", rotation = "indisponible";
        if (wm != null) {
            try { wm.getDefaultDisplay().getSize(app); appText = app.x + "×" + app.y; } catch (RuntimeException ignored) { }
            try { wm.getDefaultDisplay().getRealSize(real); realText = real.x + "×" + real.y; } catch (RuntimeException ignored) { }
            try { rotation = String.valueOf(wm.getDefaultDisplay().getRotation()); } catch (RuntimeException ignored) { }
        }
        return "getSize : " + appText + " px\ngetRealSize : " + realText + " px\nRotation : " + rotation
                + "\nDensité contexte : " + context.getResources().getDisplayMetrics().density + " (aucune densité système modifiée)";
    }

    private final class Touch implements View.OnTouchListener {
        private final int role;
        private float downX, downY;
        private int startX, startY;
        private boolean dragging, repeated, longFired;
        Touch(int role) { this.role = role; }
        @Override public boolean onTouch(final View view, MotionEvent event) {
            if (!added || params == null) { cancelTimers(); return true; }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    cancelTimers(); touching = true; gestureCancelled = false; dragging = repeated = longFired = false;
                    downX = event.getRawX(); downY = event.getRawY(); startX = params.x; startY = params.y;
                    view.setPressed(true);
                    if (role == FloatingVolumeGeometry.PLUS || role == FloatingVolumeGeometry.MINUS) {
                        repeat = new Runnable() { @Override public void run() {
                            if (gestureCancelled || dragging || !added || !touching) return;
                            if (!repeated) buzz(); repeated = true;
                            if (adjust(role) && repeat == this) handler.postDelayed(this, prefs.getInt("repeatInterval", 140));
                        }};
                        handler.postDelayed(repeat, prefs.getInt("repeatDelay", 450));
                    } else {
                        longPress = new Runnable() { @Override public void run() {
                            if (gestureCancelled || dragging || !added || !touching) return;
                            longFired = true; view.performLongClick();
                        }};
                        handler.postDelayed(longPress, 520);
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (gestureCancelled) return true;
                    float dx = event.getRawX() - downX, dy = event.getRawY() - downY;
                    int slop = Math.max(8, ViewConfiguration.get(owner).getScaledTouchSlop());
                    if (!dragging && dx * dx + dy * dy > slop * slop) {
                        cancelRepeat(); cancelLongPress();
                        if (prefs.getBoolean("locked", false)) { gestureCancelled = true; view.setPressed(false); return true; }
                        dragging = true; view.setPressed(false);
                    }
                    if (dragging) {
                        params.x = FloatingVolumeGeometry.clamp(Math.round(startX + dx), 0, geometry.maxX());
                        params.y = FloatingVolumeGeometry.clamp(Math.round(startY + dy), 0, geometry.maxY());
                        updatePosition();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    cancelRepeat(); cancelLongPress(); touching = false; view.setPressed(false);
                    if (!gestureCancelled && dragging && added) {
                        if (prefs.getBoolean("snap", true)) params.x = geometry.snapX(params.x);
                        if (updatePosition()) savePosition();
                    } else if (!gestureCancelled && !repeated && !longFired) view.performClick();
                    scheduleCollapse(); return true;
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_OUTSIDE:
                    cancelTimers(); view.setPressed(false); scheduleCollapse(); return true;
                default: return true;
            }
        }
    }
    private final class Key extends View {
        private final int role;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint letters = new Paint(Paint.ANTI_ALIAS_FLAG);
        Key(int role) {
            super(owner); this.role = role;
            setClickable(true); setLongClickable(role != FloatingVolumeGeometry.PLUS && role != FloatingVolumeGeometry.MINUS);
            setFocusable(true); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            setOnClickListener(v -> activate(role));
            if (isLongClickable()) setOnLongClickListener(v -> { buzz(); openSettings(); return true; });
            setOnTouchListener(new Touch(role)); updateDescription();
        }
        void updateDescription() {
            String suffix = I18n.t(owner,STREAM_NAMES[index()]) + (levelKnown ? ", " + current + " sur " + maximum : ", niveau indisponible");
            if (role == FloatingVolumeGeometry.PLUS) setContentDescription(I18n.t(owner,"Augmenter ") + suffix + ". Maintenir pour répéter. Glisser pour déplacer.");
            else if (role == FloatingVolumeGeometry.MINUS) setContentDescription("Diminuer " + suffix + ". Maintenir pour répéter. Glisser pour déplacer.");
            else if (role == FloatingVolumeGeometry.MUTE) setContentDescription("Muet / rétablir le son. " + suffix + ". Appui long pour personnaliser.");
            else setContentDescription(suffix + ". " + (prefs.getInt("layout", 0) == 2 ? geometry != null && geometry.collapsed ? "Toucher pour déployer." : "Toucher pour replier." : "Toucher pour personnaliser.") + " Appui long : réglages. Glisser pour déplacer.");
        }
        @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info); info.setClassName("android.widget.Button");
        }
        @Override public boolean performClick() { super.performClick(); return true; }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); float width = getWidth(), height = getHeight(), side = Math.min(width, height);
            float cx = width / 2, cy = height / 2;
            int shape = prefs.getInt("shape", 0), style = prefs.getInt("style", 0);
            Path outline = new Path(); RectF area = new RectF(1.5f, 1.5f, width - 1.5f, height - 1.5f);
            if (shape == 1) outline.addCircle(cx, cy, side / 2 - 1.5f, Path.Direction.CW);
            else if (shape == 3) { outline.moveTo(cx, 1.5f); outline.lineTo(width - 1.5f, cy); outline.lineTo(cx, height - 1.5f); outline.lineTo(1.5f, cy); outline.close(); }
            else outline.addRoundRect(area, shape == 0 ? side * .5f : side * .22f, shape == 0 ? side * .5f : side * .22f, Path.Direction.CW);
            if (shape == 1 || shape == 3 || isPressed()) {
                paint.setStyle(Paint.Style.FILL);
                paint.setShader(new LinearGradient(0, 0, width, height, isPressed() ? blend(fill, accent, .28f) : fill, fill2, Shader.TileMode.CLAMP));
                c.drawPath(outline, paint); paint.setShader(null);
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(style == 2 ? 2 : 1); paint.setColor(isPressed() ? accent : border); c.drawPath(outline, paint); paint.setStyle(Paint.Style.FILL);
            }
            paint.setColor(foreground); paint.setStrokeWidth(Math.max(2, side * .045f)); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStyle(Paint.Style.STROKE);
            if (role == FloatingVolumeGeometry.PLUS || role == FloatingVolumeGeometry.MINUS) {
                c.drawLine(cx - side * .18f, cy, cx + side * .18f, cy, paint);
                if (role == FloatingVolumeGeometry.PLUS) c.drawLine(cx, cy - side * .18f, cx, cy + side * .18f, paint);
            } else if (role == FloatingVolumeGeometry.MUTE) {
                speaker(c, cx, cy, side * .43f);
                boolean muted = false; try { muted = audio.isStreamMute(stream()); } catch (RuntimeException ignored) { }
                if (muted) { paint.setColor(accent); c.drawLine(cx - side * .24f, cy + side * .25f, cx + side * .24f, cy - side * .25f, paint); }
            } else if (prefs.getBoolean("showLevel", true)) {
                text(c, STREAM_SHORT[index()], cx, cy - side * .23f, Math.max(7, Math.min(10, side * .16f)), accent);
                text(c, levelKnown ? current + "/" + maximum : "—/—", cx, cy + side * .035f, Math.max(10, Math.min(19, side * .27f)), foreground);
                text(c, blocked ? "!" : levelKnown && maximum > 0 ? Math.round(current * 100f / maximum) + " %" : "—", cx, cy + side * .29f,
                        Math.max(8, Math.min(12, side * .18f)), blocked ? 0xffffbc86 : foreground);
            } else speaker(c, cx, cy, side * .48f);
            paint.setStyle(Paint.Style.FILL);
        }
        private void speaker(Canvas c, float x, float y, float size) {
            Path speaker = new Path(); speaker.moveTo(x - size * .44f, y - size * .18f); speaker.lineTo(x - size * .18f, y - size * .18f);
            speaker.lineTo(x + size * .12f, y - size * .44f); speaker.lineTo(x + size * .12f, y + size * .44f);
            speaker.lineTo(x - size * .18f, y + size * .18f); speaker.lineTo(x - size * .44f, y + size * .18f); speaker.close(); c.drawPath(speaker, paint);
            c.drawArc(new RectF(x - size * .12f, y - size * .34f, x + size * .52f, y + size * .34f), -60, 120, false, paint);
        }
        private void text(Canvas c, String value, float x, float y, float size, int color) {
            letters.setColor(color); letters.setTextAlign(Paint.Align.CENTER); letters.setTextSize(size); letters.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            I18n.drawText(c,I18n.t(owner,value),x,y,letters);
        }
    }
    private static int alpha(int color, int opacity) { return (Math.max(0, Math.min(255, opacity)) << 24) | (color & 0xffffff); }
    private static int blend(int a, int b, float ratio) {
        return Color.rgb((int)(Color.red(a) * (1-ratio) + Color.red(b) * ratio), (int)(Color.green(a) * (1-ratio) + Color.green(b) * ratio), (int)(Color.blue(a) * (1-ratio) + Color.blue(b) * ratio));
    }
}

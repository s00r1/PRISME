package fr.dm100.prisme;

import android.content.Context;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.accessibility.AccessibilityEvent;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** A pixel-sized, offline launcher canvas. Every visible control owns one touch region. */
public final class DashboardView extends View {
    public interface Listener {
        void onApp(AppEntry app);
        void onAppLongPress(AppEntry app);
        void onAction(String action);
    }
    private final DashboardPrefs prefs;
    private final ControlCenterPrefs controlCenterPrefs;
    private final ControlCenterGesture controlCenterGesture = new ControlCenterGesture();
    private final Listener listener;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final List<AppEntry> allApps = new ArrayList<AppEntry>();
    private final List<AppEntry> shortcutCatalog = new ArrayList<AppEntry>();
    private final List<AppEntry> apps = new ArrayList<AppEntry>();
    private final List<Hit> hits = new ArrayList<Hit>();
    private final Handler handler = new Handler();
    private final Typeface sans = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface bold = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface mono = Typeface.create("monospace", Typeface.NORMAL);
    private final Typeface serif = Typeface.create("serif", Typeface.NORMAL);
    private ThemeSpec theme;
    private int accent, fontChoice;
    private Bitmap carbonTile;
    private BitmapShader carbonShader;
    private int carbonTextColor, carbonAccentColor;
    private DeviceSnapshot snapshot;
    private float w, surfaceWidth, surfaceHeight, h, scroll, maxScroll, downX, downY, lastY;
    private ViewportLayout viewport;
    private boolean drawingStyle;
    private boolean gestureInContent;
    private static final String[] SHORTCUT_DEFAULTS = {"apps", "styles", "search", "tools", "customize"};
    private int page, pageCount = 1, capacity = 1, activeTheme = -1;
    private boolean listMode, dragging, longFired, hostActive = true;
    private Hit pressed;
    private long transitionStarted;
    private int accessibilityFocused = -1;
    private final AccessibilityNodeProvider accessibilityNodes = new AccessibilityNodeProvider() {
        @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int id) {
            if (id == AccessibilityNodeProvider.HOST_VIEW_ID) {
                AccessibilityNodeInfo host = AccessibilityNodeInfo.obtain(DashboardView.this);
                DashboardView.this.onInitializeAccessibilityNodeInfo(host);
                for (int i = 0; i < hits.size(); i++) host.addChild(DashboardView.this, i);
                host.setScrollable(pageCount > 1 || maxScroll > 0);
                if (host.isScrollable()) {
                    host.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    host.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                }
                return host;
            }
            if (id < 0 || id >= hits.size()) return null;
            Hit hit = hits.get(id);
            AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
            node.setSource(DashboardView.this, id); node.setParent(DashboardView.this);
            node.setPackageName(getContext().getPackageName());
            node.setClassName("android.widget.Button");
            node.setContentDescription(controlLabel(hit));
            node.setEnabled(isEnabled()); node.setClickable(true); node.setFocusable(true);
            node.setVisibleToUser(isShown());
            node.setLongClickable(hit.shortcutSlot >= 0 || hit.app != null || "controlCenter".equals(hit.action) || hit.fixed&&"today".equals(hit.action));
            node.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (node.isLongClickable()) node.addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
            node.setAccessibilityFocused(id == accessibilityFocused);
            node.addAction(id == accessibilityFocused ? AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS : AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            Rect bounds = new Rect(); hit.r.roundOut(bounds); node.setBoundsInParent(bounds);
            int[] location = new int[2]; getLocationOnScreen(location);
            bounds.offset(location[0], location[1]); node.setBoundsInScreen(bounds);
            return node;
        }
        @Override public AccessibilityNodeInfo findFocus(int focus) {
            return focus == AccessibilityNodeInfo.FOCUS_ACCESSIBILITY && accessibilityFocused >= 0
                    ? createAccessibilityNodeInfo(accessibilityFocused) : null;
        }
        @Override public boolean performAction(int id, int action, Bundle args) {
            if (id == AccessibilityNodeProvider.HOST_VIEW_ID
                    && (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
                int direction = action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? 1 : -1;
                if (listMode && viewport != null) {
                    scroll = Math.max(0, Math.min(maxScroll, scroll + direction * h * .60f)); invalidate();
                } else changePage(direction);
                accessibilityFocused = -1;
                sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
                return true;
            }
            if (id < 0 || id >= hits.size()) return false;
            Hit selected = hits.get(id);
            if (action == AccessibilityNodeInfo.ACTION_CLICK) {
                if (selected.app != null) listener.onApp(selected.app); else dispatch(selected.action);
                accessibilityEvent(id, AccessibilityEvent.TYPE_VIEW_CLICKED); return true;
            }
            if (action == AccessibilityNodeInfo.ACTION_LONG_CLICK) {
                if (selected.shortcutSlot >= 0) listener.onAction("editShortcut:" + selected.shortcutSlot);
                else if ("controlCenter".equals(selected.action)) listener.onAction("controlCenterSettings");
                else if(selected.fixed&&"today".equals(selected.action))listener.onAction("shortcutShelf");
                else if (selected.app != null) listener.onAppLongPress(selected.app);
                else return false;
                return true;
            }
            if (action == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) {
                accessibilityFocused = id; accessibilityEvent(id, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED); invalidate(); return true;
            }
            if (action == AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS) {
                accessibilityFocused = -1; accessibilityEvent(id, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED); invalidate(); return true;
            }
            return false;
        }
    };
    private static final class Hit {
        final RectF r;
        final String action;
        final AppEntry app;
        final int shortcutSlot;
        final boolean fixed;
        Hit(RectF r, String action, AppEntry app, int shortcutSlot, boolean fixed) {
            this.r = new RectF(r); this.action = action; this.app = app;
            this.shortcutSlot = shortcutSlot; this.fixed = fixed;
        }
    }
    private long blankTapTime;
    private float blankTapX,blankTapY;
    private final Runnable studioPulse=new Runnable(){public void run(){if(hostActive&&previewTheme<0&&activeTheme==20&&TimerEngine.deadline(getContext())>0){invalidate();handler.postDelayed(this,1000);}}};
    private final Runnable longPress = new Runnable() {
        @Override public void run() {
            if (hostActive && pressed != null && (pressed.app != null || pressed.shortcutSlot >= 0 || "controlCenter".equals(pressed.action) || pressed.fixed&&"today".equals(pressed.action)) && !dragging) {
                longFired = true;
                controlCenterGesture.cancel();
                if (prefs.getBoolean("haptic", true)) performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                if (pressed.shortcutSlot >= 0) listener.onAction("editShortcut:" + pressed.shortcutSlot);
                else if ("controlCenter".equals(pressed.action)) {
                    controlCenterGesture.cancel(); listener.onAction("controlCenterSettings");
                } else if(pressed.fixed&&"today".equals(pressed.action))listener.onAction("shortcutShelf");else listener.onAppLongPress(pressed.app);
                pressed = null;
                invalidate();
            }
        }
    };

    public DashboardView(Context context, DashboardPrefs prefs, Listener listener) {
        super(context);
        this.prefs = prefs;
        this.controlCenterPrefs = new ControlCenterPrefs(context);
        this.listener = listener;
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription("PRISME. Balayez pour parcourir les applications. Cadrans, Styles, Aujourd’hui et Réglages restent accessibles en bas. Appui long sur un raccourci pour le modifier.");
        refresh();
    }
    public void setApps(List<AppEntry> entries) {
        allApps.clear();
        if (entries != null) allApps.addAll(entries);
        rebuildApps();
    }
    /** Shortcut labels and icons do not depend on the currently selected workspace. */
    public void setShortcutCatalog(List<AppEntry> entries) {
        shortcutCatalog.clear();
        if (entries != null) shortcutCatalog.addAll(entries);
    }
    public void setSnapshot(DeviceSnapshot value) { snapshot = value; invalidate(); }
    public void refresh() {
        handler.removeCallbacks(studioPulse);if(hostActive&&previewTheme<0&&prefs.getInt("theme",0)==20&&TimerEngine.deadline(getContext())>0&&isAttachedToWindow())handler.postDelayed(studioPulse,1000);
        updateViewport();
        int id = clamp(previewTheme>=0?previewTheme:prefs.getInt("theme", 0), 0, ThemeSpec.ALL.length - 1);
        theme = ThemeSpec.get(id);
        final int[] accents = {theme.accent, 0xff66d9ef, 0xff75e8b4, 0xffffaa68, 0xffc29bff, 0xffff7b85, 0xffe8c56d};
        accent = accents[clamp(prefs.getInt("accentIndex", 0), 0, accents.length - 1)];
        if (id == 9 || id == 11 || id == 18) accent = prefs.getInt("accentIndex", 0) == 0 ? theme.accent : blend(theme.text, accent, .26f);
        fontChoice = prefs.getInt("font", 0);
        if (activeTheme != id) { activeTheme = id; page = 0; scroll = 0; }
        rebuildApps();
        invalidate();
    }
    public void onHostResume() { hostActive = true; refresh(); }
    public void onHostPause() { handler.removeCallbacks(studioPulse);hostActive = false; controlCenterGesture.cancel(); clearTouch(); }
    @Override protected void onDetachedFromWindow() { controlCenterGesture.cancel(); clearTouch(); super.onDetachedFromWindow(); }
    private void rebuildApps() {
        apps.clear();
        final boolean only = prefs.getBoolean("favoritesOnly", false);
        for (AppEntry a : allApps) if (!prefs.isHidden(a.id) && (!only || prefs.isFavorite(a.id))) apps.add(a);
        final int order = prefs.getInt("order", 0);
        final Collator french = Collator.getInstance(I18n.locale());
        french.setStrength(Collator.SECONDARY);
        Collections.sort(apps, new Comparator<AppEntry>() {
            @Override public int compare(AppEntry a, AppEntry b) {
                if (order == 2 || activeTheme == 8) {
                    int f = (prefs.isFavorite(b.id) ? 1 : 0) - (prefs.isFavorite(a.id) ? 1 : 0);
                    if (f != 0) return f;
                }
                if (order == 3) {
                    int recent = Integer.compare(prefs.getInt("launch:" + b.id, 0), prefs.getInt("launch:" + a.id, 0));
                    if (recent != 0) return recent;
                }
                int alpha = french.compare(label(a), label(b));
                if (alpha == 0) alpha = a.id.compareTo(b.id);
                return order == 1 ? -alpha : alpha;
            }
        });
        invalidate();
    }
    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        surfaceWidth = width; surfaceHeight = height;
        updateViewport(); scroll = 0; controlCenterGesture.cancel(); clearTouch();
        super.onSizeChanged(width, height, oldWidth, oldHeight);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (viewport == null || w < 1 || h < 1) return;
        hits.clear(); maxScroll = 0; listMode = false;
        // Fill every physical pixel, including optional safety margins and the volume rail.
        p.setShader(new LinearGradient(0, 0, surfaceWidth, surfaceHeight, theme.bg, theme.bg2, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, surfaceWidth, surfaceHeight, p); p.setShader(null);
        c.save();
        c.clipRect(viewport.contentLeft, viewport.contentTop,
                viewport.contentLeft + viewport.contentWidth, viewport.contentTop + viewport.contentHeight);
        c.translate(viewport.contentLeft, viewport.contentTop);
        c.scale(viewport.scale, viewport.scale);
        drawingStyle=true;
        background(c);
        boolean transition = transitionStarted > 0 && prefs.getBoolean("animations", true)
                && !getContext().getSharedPreferences("prisme_sense",0).getBoolean("powerSaveActive",false)
                && activeTheme != 11;
        int alpha = transition ? Math.min(255, 150 + (int)((System.currentTimeMillis() - transitionStarted) * .7f)) : 255;
        if (transition && alpha < 255) c.saveLayerAlpha(0, 0, w, h, alpha, Canvas.ALL_SAVE_FLAG);
        else transition = false;
        studioStyle(c);
        if (transition) { c.restore(); postInvalidateDelayed(16); }
        else transitionStarted = 0;
        drawingStyle=false;
        c.restore();
        navigation(c);
        controlCenterHandle(c);
        if (pressed != null && !dragging) {
            fill(c, pressed.r, color(accent, .19f), 13);
            stroke(c, pressed.r, color(accent, .75f), 13, 1.5f);
        }
    }
    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider() { return accessibilityNodes; }
    private String controlLabel(Hit hit) {
        String text = hit.app != null ? label(hit.app) : "__next".equals(hit.action) ? "Page suivante"
                : "__previous".equals(hit.action) ? "Page précédente" : shortcutLabel(hit.action);
        return hit.app!=null?text:I18n.t(getContext(),hit.shortcutSlot >= 0 ? text + ", raccourci " + (hit.shortcutSlot + 1) + ". Appui long pour modifier." : text);
    }
    private void accessibilityEvent(int id, int type) {
        if (getParent() == null) return;
        AccessibilityEvent event = AccessibilityEvent.obtain(type);
        event.setPackageName(getContext().getPackageName()); event.setClassName("android.widget.Button");
        event.setSource(this, id); getParent().requestSendAccessibilityEvent(this, event);
    }
    private void background(Canvas c) {
        float fullWidth = w;
        int wallpaper = prefs.getInt("wallpaper", 0);
        int a = theme.bg, b = theme.bg2;
        if (wallpaper == 5 || wallpaper == 2 || ((activeTheme == 9 || activeTheme == 11 || activeTheme == 18) && wallpaper == 0)) c.drawColor(a);
        else {
            if (wallpaper == 1) b = blend(a, accent, activeTheme == 9 || activeTheme == 11 || activeTheme == 18 ? .08f : .17f);
            p.setShader(new LinearGradient(0, 0, fullWidth, h, a, b, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, fullWidth, h, p); p.setShader(null);
        }
        if (wallpaper == 2 || (wallpaper == 0 && (activeTheme == 4 || activeTheme == 7))) {
            p.setColor(color(accent, activeTheme == 9 || activeTheme == 11 || activeTheme == 18 ? .10f : .065f)); p.setStrokeWidth(1);
            for (float x = 16; x < fullWidth; x += 24) c.drawLine(x, 0, x, h, p);
            for (float y = 16; y < h; y += 24) c.drawLine(0, y, fullWidth, y, p);
        } else if (wallpaper == 3) {
            c.save(); c.rotate(-24, fullWidth * .62f, h * .36f);
            p.setColor(color(accent, .13f)); p.setStrokeWidth(1); p.setStyle(Paint.Style.STROKE);
            for (int i = 1; i <= 5; i++) {
                float rx = fullWidth * (.16f + i * .115f), ry = h * (.06f + i * .052f);
                rect.set(fullWidth * .62f - rx, h * .36f - ry, fullWidth * .62f + rx, h * .36f + ry); c.drawOval(rect, p);
            }
            p.setStyle(Paint.Style.FILL); c.restore();
        } else if (wallpaper == 4) {
            for (int i = 0; i < 52; i++) {
                float x = ((i * 137 + 41) % 997) / 997f * fullWidth;
                float y = ((i * 223 + 83) % 991) / 991f * h;
                p.setColor(color(accent, .09f + (i % 4) * .025f));
                c.drawCircle(x, y, 1 + (i % 3) * .7f, p);
            }
        }
    }





    private void carbonTexture(Canvas c) {
        if (carbonShader == null || carbonTextColor != theme.text || carbonAccentColor != accent) {
            carbonTextColor = theme.text;
            carbonAccentColor = accent;
            carbonTile = Bitmap.createBitmap(18, 12, Bitmap.Config.ARGB_8888);
            Canvas tileCanvas = new Canvas(carbonTile);
            Paint tilePaint = new Paint();
            tilePaint.setColor(color(theme.text, .035f));
            tileCanvas.drawRect(0, 0, 9, 5, tilePaint);
            tilePaint.setColor(color(accent, .025f));
            tileCanvas.drawRect(9, 6, 18, 11, tilePaint);
            carbonShader = new BitmapShader(carbonTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        }
        boolean oldFilter = p.isFilterBitmap();
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.WHITE);
        p.setFilterBitmap(false);
        p.setShader(carbonShader);
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
        p.setFilterBitmap(oldFilter);
    }























    private void grid(Canvas c, float x, float y, float width, float height, int fixedCols, int style, boolean cards, boolean desktop) {
        height = Math.max(64, height);
        int cols = fixedCols > 0 ? fixedCols : columns(width);
        cols = Math.max(1, Math.min(cols, Math.max(1, (int)(width / 64))));
        int requested = iconSize();
        int gap = clamp(prefs.getInt("gap", 12), 4, 28);
        float cellW = width / cols;
        float idealH = Math.max(67, Math.min(requested, cellW - 23) + (labels() ? textSize() + 18 : 18));
        if (style == 1) idealH = Math.max(80, idealH);
        if (style == 2) idealH = Math.max(91, idealH);
        int rows = Math.max(1, (int)(height / idealH));
        float cellH = height / rows;
        setCapacity(rows * cols);
        int start = page * capacity, end = Math.min(apps.size(), start + capacity);
        for (int i = start; i < end; i++) {
            int local = i - start, col = local % cols, row = local / cols;
            if(I18n.rtl())col=cols-1-col;
            float left = x + col * cellW, top = y + row * cellH;
            RectF cell = r(left + gap / 2f, top + gap / 2f, cellW - gap, cellH - gap);
            AppEntry app = apps.get(i);
            if (cards) {
                fill(c, cell, activeTheme == 6 ? theme.surface : color(theme.surface, activeTheme == 10 ? .96f : .78f), activeTheme == 6 ? 0 : activeTheme == 7 ? 9 : 17);
                if (activeTheme == 6) {
                    stroke(c, cell, theme.text, 0, 2);
                    txt(c, String.format(Locale.ROOT, "%02d", i + 1), cell.left + 7, cell.top + 16, 9, theme.muted, mono, Paint.Align.LEFT);
                    if (prefs.isFavorite(app.id)) txt(c, "★", cell.right - 7, cell.top + 17, 12, accent, mono, Paint.Align.RIGHT);
                } else if (activeTheme == 7) {
                    line(c, cell.left + 9, cell.top + 6, cell.right - 9, cell.top + 6, color(accent, .38f), 1);
                }
            }
            float size = Math.min(requested, Math.min(cellW - Math.max(18, gap + 3), cellH - (labels() ? textSize() + 22 : 12)));
            size = Math.max(26, size);
            if (style == 2) size = Math.min(size, 49);
            float ix = left + (cellW - size) / 2;
            float iy = top + (cellH - size - (labels() ? textSize() + 8 : 0)) / 2 - (desktop ? 2 : 0);
            drawIcon(c, app, ix, iy, size);
            if (labels()) shortUserTxt(c, label(app), left + cellW / 2, iy + size + textSize() + 6, cellW - 12, textSize(), theme.text, activeTheme == 6 ? mono : sans, Paint.Align.CENTER);
            hit(r(left + 2, top + 2, cellW - 4, cellH - 4), null, app);
        }
        empty(c, r(x, y, width, height));
    }
    private void drawList(Canvas c, float x, float y, float width, float height, float rowH, int style) {
        listMode = true;
        pageCount = 1; page = 0;
        height = Math.max(rowH, height);
        float listIcon = iconSize();
        rowH = Math.max(listIcon + 14, Math.max(rowH, textSize() + 29));
        maxScroll = Math.max(0, apps.size() * rowH - height);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        c.save(); c.clipRect(x, y, x + width, y + height);
        int start = Math.max(0, (int)(scroll / rowH));
        int end = Math.min(apps.size(), (int)((scroll + height) / rowH) + 1);
        for (int i = start; i < end; i++) {
            AppEntry app = apps.get(i);
            float top = y + i * rowH - scroll;
            RectF row = r(x, top, width - 8, rowH - 2);
            if (style == 5) {
                txt(c, String.format(Locale.ROOT, "%02d", i + 1), x + 3, top + rowH / 2 + 5, 10, theme.muted, mono, Paint.Align.LEFT);
                drawIcon(c, app, x + 31, top + (rowH - listIcon) / 2, listIcon);
                shortTxt(c, "open " + label(app), x + 44 + listIcon, top + rowH / 2 + 5, width - 84 - listIcon, textSize(), accent, mono, Paint.Align.LEFT);
                txt(c, "↵", x + width - 23, top + rowH / 2 + 6, 16, theme.muted, mono, Paint.Align.CENTER);
                line(c, x + 1, top + rowH - 1, x + width - 12, top + rowH - 1, color(accent, .13f), 1);
            } else if (style == 9) {
                String initial = label(app).length() == 0 ? "·" : label(app).substring(0, 1).toUpperCase(I18n.locale());
                txt(c, initial, x + 3, top + rowH / 2 + 7, 24, theme.muted, serif, Paint.Align.LEFT);
                drawIcon(c, app, x + 36, top + (rowH - listIcon) / 2, listIcon);
                shortUserTxt(c, label(app), x + 49 + listIcon, top + rowH / 2 + 6, width - 85 - listIcon, textSize() + 1, theme.text, serif, Paint.Align.LEFT);
                txt(c, "↗", x + width - 24, top + rowH / 2 + 6, 17, theme.muted, sans, Paint.Align.CENTER);
                line(c, x, top + rowH - 1, x + width - 13, top + rowH - 1, color(theme.text, .15f), 1);
            } else {
                drawIcon(c, app, x + 5, top + (rowH - listIcon) / 2, listIcon);
                shortUserTxt(c, label(app), x + 18 + listIcon, top + rowH / 2 + 5, width - 46 - listIcon, textSize(), theme.text, mono, Paint.Align.LEFT);
                txt(c, ">", x + width - 22, top + rowH / 2 + 5, 15, theme.text, mono, Paint.Align.CENTER);
                line(c, x, top + rowH - 1, x + width - 12, top + rowH - 1, color(theme.text, .3f), 1);
            }
            RectF clipped = new RectF(row);
            if (clipped.intersect(x, y, x + width, y + height) && clipped.height() >= 42) hit(clipped, null, app);
        }
        c.restore();
        if (maxScroll > 0) {
            float barH = Math.max(26, height * height / (apps.size() * rowH));
            fill(c, r(x + width - 4, y + scroll / maxScroll * (height - barH), 3, barH), color(accent, .55f), 2);
        }
        empty(c, r(x, y, width, height));
    }
    private void favoriteDock(Canvas c, float y) {
        ArrayList<AppEntry> fav = new ArrayList<AppEntry>();
        for (AppEntry app : apps) if (prefs.isFavorite(app.id)) fav.add(app);
        int limit = Math.min(7, Math.max(3, (int)((w - 75) / 61)));
        if (fav.isEmpty()) for (int i = 0; i < Math.min(limit - 1, apps.size()); i++) fav.add(apps.get(i));
        int count = Math.min(limit - 1, fav.size());
        float cell = Math.min(64, (w - 66) / Math.max(1, count + 1));
        float total = (count + 1) * cell + 17, x = (w - total) / 2;
        fill(c, r(x, y, total, 71), color(theme.surface, .86f), 24);
        stroke(c, r(x, y, total, 71), color(theme.text, .15f), 24, 1);
        for (int i = 0; i < count; i++) {
            float xx = x + 9 + i * cell;
            drawIcon(c, fav.get(i), xx + (cell - 45) / 2, y + 12, 45);
            hit(r(xx, y + 5, cell, 61), null, fav.get(i));
        }
        RectF last = r(x + 9 + count * cell, y + 6, cell, 60);
        glyph(c, "apps", last.centerX(), last.top + 23, theme.text, 18);
        txt(c, "APPS", last.centerX(), last.bottom - 8, 9, theme.muted, mono, Paint.Align.CENTER);
        hit(last, "apps", null);
    }

    /** The five editable shortcuts are shared by all styles; the fixed navigation is separate. */
    private void toolbar(Canvas c, float y, boolean glass, boolean square) {
        float x = 12, width = w - 24, unit = width / DashboardPrefs.SLOT_COUNT;
        float height = Math.max(54, 44 / viewport.scale);
        if (y > h / 2) y = viewport.logicalHeight - height - 10;
        int bg = activeTheme == 9 || activeTheme == 11 || activeTheme == 18 ? theme.bg : color(theme.surface, glass ? .9f : .97f);
        fill(c, r(x, y, width, height), bg, square ? 7 : 21);
        stroke(c, r(x, y, width, height), color(theme.text, .14f), square ? 7 : 21, 1);
        for (int i = 0; i < DashboardPrefs.SLOT_COUNT; i++) {
            String action = prefs.getShortcut(i, SHORTCUT_DEFAULTS[i]);
            AppEntry app = shortcutApp(action);
            float left = x + i * unit;
            int ink = i == 0 ? accent : theme.text;
            float icon = Math.max(16, 18 / viewport.scale);
            float iconY = y + height * .31f;
            if (app != null) drawIcon(c, app, left + (unit - icon * 1.35f) / 2, iconY - icon * .68f, icon * 1.35f);
            else glyph(c, action, left + unit / 2, iconY, ink, icon);
            shortTxt(c, shortcutLabel(action), left + unit / 2, y + height * .86f,
                    unit - 10, Math.max(12, 12 / viewport.scale), ink, sans, Paint.Align.CENTER);
            hit(r(left, y, unit, height), action, null, i);
        }
    }
    private AppEntry shortcutApp(String action) {
        if (action == null || !action.startsWith("app:")) return null;
        ComponentName component = ComponentName.unflattenFromString(action.substring(4));
        if (component == null) return null;
        for (AppEntry app : shortcutCatalog) if (component.equals(app.component)) return app;
        for (AppEntry app : allApps) if (component.equals(app.component)) return app;
        return null;
    }
    private String shortcutLabel(String action) {
        AppEntry app = shortcutApp(action);
        if (app != null) return label(app);
        if (action != null && action.startsWith("app:")) return "Appli absente";
        if ("clock".equals(action) || "home".equals(action)) return "Cadrans";
        if ("controlCenter".equals(action)) return "Contrôles";
        if ("controlCenterSettings".equals(action)) return "Personnaliser les contrôles";
        if ("styles".equals(action)) return "Styles";
        if ("customize".equals(action)) return "Réglages";
        if ("search".equals(action)) return "Chercher";
        if ("favorites".equals(action)) return "Favoris";
        if ("tools".equals(action)) return "Outils";
        if ("notes".equals(action)) return "Notes";
        if ("timer".equals(action)) return "Minuteur";
        if ("stopwatch".equals(action)) return "Chrono";
        if ("focus".equals(action)) return "Focus";
        if ("workspaces".equals(action)) return "Espaces";
        if ("floatingVolumeSettings".equals(action)) return "Volume";
        if(action!=null&&action.startsWith("__pick:")){try{int index=Integer.parseInt(action.substring(7));return "Inspecter "+(index>=0&&index<apps.size()?label(apps.get(index)):"");}catch(NumberFormatException ignored){return "Inspecter";}}
        if ("shortcuts".equals(action)) return "Raccourcis";
        if ("today".equals(action)) return "Aujourd’hui";
        if ("checklist".equals(action)) return "Tâches";
        if ("deskclock".equals(action)) return "Horloge";
        if ("presets".equals(action)) return "Confort";
        if ("device".equals(action)) return "État montre";
        if ("settingsSearch".equals(action)) return "Réglages";
        return "Applications";
    }
    private void navigation(Canvas c) {
        float top = viewport.navigationTop, height = viewport.navigationHeight;
        fill(c, r(0, top, surfaceWidth, height), theme.surface, 0);
        line(c, 0, top, surfaceWidth, top, color(accent, .34f), 1);
        String[] actions = {"clock", "styles", "today", "customize"};
        String[] titles = {"Cadrans", "Styles", "Aujourd’hui", "Réglages"};
        float unit = surfaceWidth / 4f;
        for (int i = 0; i < actions.length; i++) {
            int visual=I18n.rtl()?actions.length-1-i:i;float cx = (visual + .5f) * unit;
            glyph(c, actions[i], cx, top + height * .31f, i == 0 ? accent : theme.text, 17);
            txt(c, titles[i], cx, top + height * .84f, surfaceWidth < 400 ? 12 : 13,
                    theme.text, bold, Paint.Align.CENTER);
            hits.add(new Hit(r(visual * unit, top, unit, height), actions[i], null, -1, true));
        }
    }
    private void controlCenterHandle(Canvas c) {
        if(ControlCenterService.isShowing())return;
        int opening = controlCenterPrefs.getInt("opening", 3);
        if (!controlCenterPrefs.getBoolean("enabled", true) || (opening != 2 && opening != 3)) return;
        int edge = controlCenterPrefs.getInt("edge", 0), alignment = controlCenterPrefs.getInt("handlePosition", 1);
        float bodyBottom = viewport.navigationTop;
        RectF visible, target;
        if (edge <= 1) {
            float width = Math.min(112, surfaceWidth - 16);
            float x = alignment == 0 ? 8 : alignment == 2 ? surfaceWidth - width - 8 : (surfaceWidth - width) / 2;
            float y = edge == 0 ? 4 : Math.max(0, bodyBottom - 38);
            visible = r(x, y, width, 34);
            target = r(x - 2, edge == 0 ? 0 : Math.max(0, bodyBottom - 48), width + 4, 48);
            if(controlCenterPrefs.getBoolean("compactHandle",true)){fill(c,box(visible.centerX()-27,edge==0?7:bodyBottom-12,54,5),color(accent,.9f),3);}else{fill(c, visible, color(theme.surface, .95f), 12);
            stroke(c, visible, color(accent, .6f), 12, 1);
            txt(c, "Contrôles", visible.centerX(), y + 23, 12, theme.text, bold, Paint.Align.CENTER);}
        } else {
            float height = Math.min(72, bodyBottom - 16);
            float y = alignment == 0 ? 8 : alignment == 2 ? bodyBottom - height - 8 : (bodyBottom - height) / 2;
            float x = edge == 2 ? 0 : surfaceWidth - 25;
            visible = r(x, y, 25, height);
            target = r(edge == 2 ? 0 : surfaceWidth - 40, y - 2, 40, height + 4);
            fill(c, visible, color(theme.surface, .95f), 12);
            stroke(c, visible, color(accent, .6f), 12, 1);
            for (int i = 0; i < 3; i++) line(c, x + 8, y + height / 2 - 6 + i * 6,
                    x + 17, y + height / 2 - 6 + i * 6, theme.text, 2);
        }
        // Always above the fixed navigation: no button can cover Cadrans or Réglages.
        if (target.intersect(0, 0, surfaceWidth, bodyBottom))
            hits.add(new Hit(target, "controlCenter", null, -1, true));
    }
    private void glyph(Canvas c, String name, float x, float y, int ink, float s) {
        p.setColor(ink); p.setStrokeWidth(1.8f); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
        if ("search".equals(name)) {
            c.drawCircle(x - 2, y - 2, s * .34f, p); c.drawLine(x + 2, y + 3, x + s * .48f, y + s * .48f, p);
        } else if ("today".equals(name)||"checklist".equals(name)) {
            rect.set(x-8,y-7,x+8,y+8);c.drawRoundRect(rect,2,2,p);c.drawLine(x-8,y-2,x+8,y-2,p);
            c.drawLine(x-4,y-10,x-4,y-5,p);c.drawLine(x+4,y-10,x+4,y-5,p);c.drawLine(x-4,y+3,x+4,y+3,p);
        } else if ("customize".equals(name)) {
            for (int i = 0; i < 3; i++) { float xx = x + (i - 1) * 6; c.drawLine(xx, y - 7, xx, y + 7, p); }
            p.setStyle(Paint.Style.FILL); c.drawCircle(x - 6, y - 3, 2.6f, p); c.drawCircle(x, y + 3, 2.6f, p); c.drawCircle(x + 6, y - 1, 2.6f, p);
        } else if ("clock".equals(name) || "home".equals(name)) {
            c.drawCircle(x, y, s * .46f, p);
            c.drawLine(x, y, x, y - s * .27f, p);
            c.drawLine(x, y, x + s * .23f, y + s * .13f, p);
        } else if ("shortcuts".equals(name)) {
            rect.set(x - s * .47f, y - s * .3f, x + s * .14f, y + s * .3f);
            c.drawRoundRect(rect, s * .15f, s * .15f, p);
            c.drawLine(x - s * .04f, y, x + s * .46f, y, p);
            c.drawLine(x + s * .24f, y - s * .2f, x + s * .46f, y, p);
            c.drawLine(x + s * .24f, y + s * .2f, x + s * .46f, y, p);
        } else if ("styles".equals(name)) {
            c.save(); c.rotate(-12, x, y); rect.set(x - 7, y - 7, x + 7, y + 7); c.drawRoundRect(rect, 3, 3, p); c.restore();
            p.setStyle(Paint.Style.FILL); c.drawCircle(x - 2, y - 2, 1.5f, p); c.drawCircle(x + 3, y + 3, 1.5f, p);
        } else if ("favorites".equals(name)) {
            Path heart = new Path(); heart.moveTo(x, y + 7); heart.cubicTo(x - 17, y - 2, x - 5, y - 14, x, y - 5); heart.cubicTo(x + 5, y - 14, x + 17, y - 2, x, y + 7); c.drawPath(heart, p);
        } else {
            p.setStyle(Paint.Style.FILL);
            for (int row = 0; row < 2; row++) for (int col = 0; col < 2; col++) { rect.set(x - 7 + col * 9, y - 7 + row * 9, x - 2 + col * 9, y - 2 + row * 9); c.drawRoundRect(rect, 1.5f, 1.5f, p); }
        }
        p.setStyle(Paint.Style.FILL); p.setStrokeCap(Paint.Cap.BUTT);
    }
    private void title(Canvas c, String title, String subtitle, float x, float y, boolean technical) {
        txt(c, title, x, y + 11, technical ? 23 : 26, theme.text, technical ? mono : bold, Paint.Align.LEFT);
        txt(c, subtitle, x + 1, y + 32, 10, theme.muted, technical ? mono : sans, Paint.Align.LEFT);
    }
    private void sideButton(Canvas c, float x, float y, float width, String title, String action, boolean selected) {
        button(c, r(x, y, width, 44), title, action, selected ? blend(theme.surface, accent, .18f) : Color.TRANSPARENT, selected ? accent : theme.muted, width < 70 ? 11 : 13, false);
    }
    private void actionLink(Canvas c, String title, String action, float x, float y, float width, float height) {
        button(c, r(x, y, width, height), title, action, Color.TRANSPARENT, theme.text, 13, false);
    }
    private void button(Canvas c, RectF box, String title, String action, int background, int foreground, float size, boolean square) {
        fill(c, box, background, square ? 3 : 12);
        shortTxt(c, title, box.centerX(), box.centerY() + size * .35f, box.width() - 8, size, foreground, activeTheme == 5 || activeTheme == 6 || activeTheme == 11 ? mono : bold, Paint.Align.CENTER);
        hit(box, action, null);
    }
    private void badge(Canvas c, String title, float x, float y, float width, float height, int color) {
        fill(c, r(x, y, width, height), color(color, .12f), 6);
        txt(c, title, x + width / 2, y + height / 2 + 4, 10, color, mono, Paint.Align.CENTER);
    }
    private void pageDots(Canvas c, float y, int ink) {
        if (pageCount <= 1) return;
        int shown = Math.min(9, pageCount), first = Math.max(0, Math.min(page - shown / 2, pageCount - shown));
        float left = w / 2 - (shown - 1) * 6;
        for (int i = 0; i < shown; i++) { p.setColor(color(ink, first + i == page ? .95f : .25f)); c.drawCircle(left + i * 12, y, first + i == page ? 3.2f : 2.2f, p); }
        hit(r(12, y - 17, 42, 34), "__previous", null);
        hit(r(w - 54, y - 17, 42, 34), "__next", null);
        txt(c, "‹", 33, y + 7, 25, color(ink, .75f), sans, Paint.Align.CENTER);
        txt(c, "›", w - 33, y + 7, 25, color(ink, .75f), sans, Paint.Align.CENTER);
    }
    private void setCapacity(int value) {
        capacity = Math.max(1, value);
        pageCount = Math.max(1, (apps.size() + capacity - 1) / capacity);
        page = clamp(page, 0, pageCount - 1);
    }
    private void empty(Canvas c, RectF region) {
        if (!apps.isEmpty()) return;
        txt(c, prefs.getBoolean("favoritesOnly", false) ? "Votre sélection commence ici." : "Aucune application visible.", region.centerX(), region.centerY() - 11, 16, theme.text, sans, Paint.Align.CENTER);
        txt(c, "Apps → appui long pour choisir vos favoris", region.centerX(), region.centerY() + 14, 11, theme.muted, sans, Paint.Align.CENTER);
    }
    private void drawIcon(Canvas c, AppEntry app, float x, float y, float size) {
        int shape = prefs.getInt("iconShape", 0);
        float radius = shape == 2 || shape == 0 && activeTheme == 13 ? size / 2 : shape == 3 ? 0 : shape == 1 ? size * .24f : activeTheme == 6 || activeTheme == 11 ? 3 : size * .24f;
        RectF box = r(x, y, size, size);
        fill(c, box, color(theme.text, activeTheme == 9 || activeTheme == 11 || activeTheme == 18 ? .07f : .09f), radius);
        Bitmap icon = app.icon;
        if (icon != null && !icon.isRecycled()) {
            c.save();
            Path path = new Path(); path.addRoundRect(box, radius, radius, Path.Direction.CW); c.clipPath(path);
            p.setColor(Color.WHITE); p.setAlpha(255); c.drawBitmap(icon, null, box, p); c.restore();
        } else {
            String s = label(app);
            txt(c, s.length() > 0 ? s.substring(0, 1).toUpperCase(I18n.locale()) : "·", x + size / 2, y + size * .66f, size * .45f, theme.text, bold, Paint.Align.CENTER);
        }
    }
    private void metric(Canvas c, String name, String value, float x, float y, float width, float percent, int accent) {
        txt(c, name, x, y, 9, theme.muted, mono, Paint.Align.LEFT);
        txt(c, value, x, y + 25, 21, theme.text, mono, Paint.Align.LEFT);
        fill(c, r(x, y + 35, width, 4), color(accent, .13f), 2);
        fill(c, r(x, y + 35, width * Math.max(0, Math.min(1, percent)), 4), accent, 2);
    }
    private void gauge(Canvas c, RectF box, String title, String value, float percent, int accent) {
        fill(c, box, color(theme.surface, .92f), 10); stroke(c, box, color(theme.text, .13f), 10, 1);
        for (int i = 0; i < 4; i++) { float x = (i % 2 == 0 ? box.left + 7 : box.right - 7), y = (i < 2 ? box.top + 7 : box.bottom - 7); p.setColor(color(theme.text, .3f)); c.drawCircle(x, y, 1.8f, p); }
        txt(c, title, box.centerX(), box.top + 23, 10, theme.muted, mono, Paint.Align.CENTER);
        txt(c, value, box.centerX(), box.top + 62, Math.min(29, box.width() * .2f), theme.text, mono, Paint.Align.CENTER);
        float xx = box.left + 15, yy = box.bottom - 26, width = box.width() - 30;
        for (int i = 0; i < 15; i++) {
            int col = percent < 0 ? color(accent, .3f) : i < percent * 15 ? accent : color(accent, .13f);
            fill(c, r(xx + i * width / 15, yy, width / 15 - 2, 10), col, 1);
        }
        if (percent < 0) { p.setColor(accent); c.drawCircle(box.centerX(), box.bottom - 21, 3, p); }
    }
    private void utilityCard(Canvas c, RectF box, String title, String label) {
        fill(c, box, color(theme.surface, .92f), 10);
        stroke(c, box, color(theme.text, .13f), 10, 1);
        txt(c, title, box.centerX(), box.top + 23, 10, theme.muted, mono, Paint.Align.CENTER);
        txt(c, label, box.centerX(), box.top + 62, Math.min(23, box.width() * .16f), theme.text, mono, Paint.Align.CENTER);
        txt(c, "OUVRIR  ↗", box.centerX(), box.bottom - 18, 10, accent, mono, Paint.Align.CENTER);
    }
    private void miniStatus(Canvas c, float x, float y, Paint.Align align) {
        if (prefs.getBoolean("showBattery", true)) txt(c, battery() + (snapshot != null && snapshot.charging ? " · EN CHARGE" : ""), x, y, 10, theme.muted, sans, align);
    }
    private void orbGlow(Canvas c, float x, float y, float radius, int col, float opacity) {
        int wallpaper = prefs.getInt("wallpaper", 0);
        if (wallpaper != 0 && wallpaper != 3) return;
        for (int i = 5; i >= 1; i--) { p.setColor(color(col, opacity / 5)); c.drawCircle(x, y, radius * i / 5, p); }
    }
    private void txt(Canvas c, String text, float x, float baseline, float size, int col, Typeface font, Paint.Align align) {
        text=I18n.t(getContext(),text);
        textPaint.setColor(col); textPaint.setTextSize(size); textPaint.setTypeface(selectedFont(font)); textPaint.setTextAlign(align);
        I18n.drawText(c,text == null ? "" : text,x,baseline,textPaint);
    }
    private void shortTxt(Canvas c, String value, float x, float baseline, float maxWidth, float size, int col, Typeface font, Paint.Align align) {
        textPaint.setTextSize(size); textPaint.setTypeface(selectedFont(font));
        String text = I18n.t(getContext(),value == null ? "" : value);
        if (textPaint.measureText(text) > maxWidth) {
            int count = textPaint.breakText(text, true, Math.max(0, maxWidth - textPaint.measureText("…")), null);
            text = text.substring(0, Math.max(0, count)) + "…";
        }
        txt(c, text, x, baseline, size, col, font, align);
    }
    private void fill(Canvas c, RectF box, int col, float radius) { p.setColor(col); p.setStyle(Paint.Style.FILL); c.drawRoundRect(box, cardRadius(box,radius), cardRadius(box,radius), p); }
    private void stroke(Canvas c, RectF box, int col, float radius, float width) { p.setColor(col); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(width); c.drawRoundRect(box, cardRadius(box,radius), cardRadius(box,radius), p); p.setStyle(Paint.Style.FILL); }
    private void line(Canvas c, float x, float y, float x2, float y2, int col, float width) { p.setColor(col); p.setStrokeWidth(width); c.drawLine(x, y, x2, y2, p); }
    private RectF r(float x, float y, float width, float height) {
        if(drawingStyle&&prefs.getBoolean("edgeCards",true)){
            float[] e=ViewportLayout.edgeCard(x,y,width,height,w,viewport.logicalHeight);
            return new RectF(e[0],e[1],e[2],e[3]);
        }
        return new RectF(x,y,x+Math.max(0,width),y+Math.max(0,height));
    }
    private float cardRadius(RectF box,float radius){
        if(drawingStyle&&prefs.getBoolean("edgeCards",true)&&box.left<=0&&box.right>=w
                &&(box.top<=0||box.bottom>=viewport.logicalHeight))return 0;
        return radius;
    }
    private void hit(RectF area, String action, AppEntry app) { hit(area, action, app, -1); }
    private void hit(RectF area, String action, AppEntry app, int shortcutSlot) {
        if (viewport == null || area.width() <= 0 || area.height() <= 0) return;
        RectF physical = new RectF(viewport.toViewX(area.left), viewport.toViewY(area.top),
                viewport.toViewX(area.right), viewport.toViewY(area.bottom));
        if (physical.intersect(viewport.contentLeft, viewport.contentTop,
                viewport.contentLeft + viewport.contentWidth, viewport.contentTop + viewport.contentHeight))
            hits.add(new Hit(physical, action, app, shortcutSlot, false));
    }
    private void updateViewport() {
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return;
        viewport = new ViewportLayout(surfaceWidth, surfaceHeight,
                prefs.getInt(DashboardPrefs.STYLE_SCALE, 100), prefs.getInt(DashboardPrefs.CONTENT_INSET, 0),
                prefs.getInt(DashboardPrefs.FIT_MODE, 0), prefs.getBoolean("volumeRail", false),prefs.getBoolean("compactNavigation",false));
        w = viewport.logicalWidth;
        h = viewport.styleHeight;
    }
    public ViewportLayout getViewportLayout() { return viewport; }
    private boolean favoriteDockEnabled() { return prefs.getBoolean("showDock", true) && h >= 520; }
    private int columns(float width) {
        int wanted = prefs.getInt("columns", theme.recommendedColumns);
        if (wanted <= 0) wanted = theme.recommendedColumns;
        if (prefs.getInt(DashboardPrefs.FIT_MODE, 0) == 2) wanted = Math.min(wanted, 4);
        return Math.max(2, Math.min(wanted, (int)(width / 72)));
    }
    private int iconSize() { return clamp(prefs.getInt("iconSize", theme.recommendedIconSize), 52, 100) + (prefs.getInt(DashboardPrefs.FIT_MODE, 0) == 2 ? 12 : 0); }
    private int textSize() { return clamp(prefs.getInt("textSize", theme.recommendedTextSize), 12, 24) + (prefs.getInt(DashboardPrefs.FIT_MODE, 0) == 2 ? 2 : 0); }
    private boolean labels() { return prefs.getBoolean("labels", true); }
    private String label(AppEntry app) { String custom = prefs.getCustomLabel(app.id); return custom != null && custom.trim().length() > 0 ? custom.trim() : app.label; }
    private long now() { return snapshot != null && snapshot.deviceTimeMillis > 0 ? snapshot.deviceTimeMillis : System.currentTimeMillis(); }
    private Typeface selectedFont(Typeface fallback) { return fontChoice == 1 ? sans : fontChoice == 2 ? mono : fontChoice == 3 ? serif : fallback; }
    private boolean clockVisible() { return prefs.getBoolean("showClock", true); }
    private boolean batteryVisible() { return prefs.getBoolean("showBattery", true); }
    private String time() { return clockVisible() ? new SimpleDateFormat("HH:mm", I18n.locale()).format(new Date(now())) : ""; }
    private String dateShort() { return new SimpleDateFormat("EEE d MMM", I18n.locale()).format(new Date(now())); }
    private String dateLong() { return new SimpleDateFormat("EEEE d MMMM", I18n.locale()).format(new Date(now())); }
    private String battery() { return !batteryVisible() ? "" : snapshot == null || snapshot.batteryPct < 0 ? "— %" : snapshot.batteryPct + " %"; }
    private String memory() { return snapshot == null || snapshot.memoryUsedPct < 0 ? "— %" : snapshot.memoryUsedPct + " %"; }
    private String storage() { return snapshot == null || snapshot.freeStorageGb < 0 ? "— Go" : String.format(I18n.locale(), "%.1f Go", snapshot.freeStorageGb); }
    private float batteryRatio() { return !batteryVisible() || snapshot == null ? 0 : Math.max(0, snapshot.batteryPct) / 100f; }
    private float memoryRatio() { return snapshot == null ? 0 : Math.max(0, snapshot.memoryUsedPct) / 100f; }
    private static int color(int col, float opacity) { return (Math.max(0, Math.min(255, (int)(255 * opacity))) << 24) | (col & 0x00ffffff); }
    private static int blend(int a, int b, float amount) { return Color.rgb((int)(Color.red(a) * (1 - amount) + Color.red(b) * amount), (int)(Color.green(a) * (1 - amount) + Color.green(b) * amount), (int)(Color.blue(a) * (1 - amount) + Color.blue(b) * amount)); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x; downY = lastY = y; dragging = false; longFired = false; pressed = null;
                for (int i = hits.size() - 1; i >= 0; i--) if (hits.get(i).r.contains(x, y)) { pressed = hits.get(i); break; }
                gestureInContent = viewport != null && viewport.containsContent(x, y) && (pressed == null || !pressed.fixed);
                int edge = controlCenterPrefs.getInt("edge", 0);
                boolean eligible = controlCenterPrefs.getBoolean("enabled", true)
                        && (pressed == null || !pressed.fixed || "controlCenter".equals(pressed.action) || pressed.fixed&&"today".equals(pressed.action));
                // Bottom means the edge just above PRISME's fixed navigation, which remains tappable.
                float gestureHeight = edge == 1 && viewport != null ? viewport.navigationTop : surfaceHeight;
                boolean centerCandidate = controlCenterGesture.begin(x, y, surfaceWidth, gestureHeight,
                        event.getEventTime(), eligible, edge, controlCenterPrefs.getInt("edgeSize", 64),
                        controlCenterPrefs.getInt("opening", 3), controlCenterPrefs.getInt("swipeDistance", 36));
                if (pressed != null && (!centerCandidate || controlCenterPrefs.getInt("opening", 3) != 1 || "controlCenter".equals(pressed.action) || pressed.fixed&&"today".equals(pressed.action))
                        && (pressed.app != null || pressed.shortcutSlot >= 0 || "controlCenter".equals(pressed.action) || pressed.fixed&&"today".equals(pressed.action)))
                    handler.postDelayed(longPress, 520);
                invalidate(); return true;
            case MotionEvent.ACTION_MOVE:
                int centerMove = controlCenterGesture.move(x, y);
                if (centerMove == ControlCenterGesture.OPEN) { openControlCenter(); return true; }
                if (centerMove == ControlCenterGesture.CLAIM) {
                    if (Math.abs(x - downX) > 11 || Math.abs(y - downY) > 11) {
                        handler.removeCallbacks(longPress); pressed = null;
                    }
                    lastY = y; invalidate(); return true;
                }
                float dx = x - downX, dy = y - downY;
                if (Math.abs(dx) > 11 || Math.abs(dy) > 11) { dragging = true; handler.removeCallbacks(longPress); pressed = null; }
                if (dragging && gestureInContent && listMode && Math.abs(dy) > Math.abs(dx)) {
                    scroll = Math.max(0, Math.min(maxScroll, scroll - (y - lastY) / viewport.scale)); invalidate();
                }
                lastY = y; return true;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(longPress);
                int centerUp = controlCenterGesture.end(x, y, event.getEventTime());
                if (centerUp == ControlCenterGesture.OPEN) { openControlCenter(); return true; }
                if (centerUp == ControlCenterGesture.CLAIM) { clearTouch(); return true; }
                if (dragging) {
                    float horizontal = x - downX, vertical = y - downY;
                    if(gestureInContent&&activeTheme==19&&Math.abs(vertical)>46&&Math.abs(vertical)>Math.abs(horizontal)*1.2f)changePage(vertical<0?1:-1);
                    else if(gestureInContent&&!listMode&&vertical < -64&&Math.abs(vertical)>Math.abs(horizontal)*1.4f){
                        String[] choices={"","today","search","checklist","deskclock"};String action=choices[prefs.getInt("upGesture",1)];if(!action.isEmpty())listener.onAction(action);
                    }
                    else if (gestureInContent && !listMode && Math.abs(horizontal) > 46 && Math.abs(horizontal) > Math.abs(vertical)) changePage((horizontal < 0)!=I18n.rtl()?1:-1);
                } else if (!longFired && pressed != null && pressed.r.contains(x, y)) {
                    Hit selected = pressed;
                    if (selected.app != null) listener.onApp(selected.app); else dispatch(selected.action);
                    performClick();
                }
                if(!dragging&&!longFired&&pressed==null&&gestureInContent){
                    long now=event.getEventTime();if(now-blankTapTime<350&&Math.abs(x-blankTapX)<32&&Math.abs(y-blankTapY)<32){
                        String[] actions={"","today","controlCenter","deskclock","search"};String action=actions[prefs.getInt("blankDoubleTap",1)];if(!action.isEmpty())listener.onAction(action);blankTapTime=0;
                    }else{blankTapTime=now;blankTapX=x;blankTapY=y;}
                }
                clearTouch(); return true;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                controlCenterGesture.cancel(); clearTouch(); return true;
        }
        return true;
    }
    private void openControlCenter() {
        controlCenterGesture.cancel(); clearTouch(); listener.onAction("controlCenter");
    }
    private void dispatch(String action) {
        if(action!=null&&action.startsWith("__pick:")){try{studioSelected=Math.max(0,Math.min(apps.size()-1,Integer.parseInt(action.substring(7))));invalidate();}catch(NumberFormatException ignored){}}
        else if ("__next".equals(action)) changePage(1);
        else if ("__previous".equals(action)) changePage(-1);
        else if (action != null) listener.onAction(action);
    }
    private void changePage(int delta) {
        int target = Math.max(0, Math.min(pageCount - 1, page + delta));
        if (target != page) { page = target; transitionStarted = System.currentTimeMillis(); if (prefs.getBoolean("haptic", true)) performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY); }
        invalidate();
    }
    private void clearTouch() { handler.removeCallbacks(longPress); pressed = null; dragging = false; invalidate(); }
    @Override public boolean performClick() { super.performClick(); return true; }
    // PRISME 5: each scene owns its application arrangement and navigation model.
    private int studioSelected;
    private int previewTheme=-1;
    private AppEntry studioApp(int offset,int perPage){setCapacity(perPage);int index=page*capacity+offset;return index>=0&&index<apps.size()?apps.get(index):null;}
    private RectF box(float x,float y,float width,float height){return new RectF(x,y,x+Math.max(1,width),y+Math.max(1,height));}
    private void studioCard(Canvas c,RectF area,int tint,float radius){fill(c,area,tint,radius);stroke(c,area,color(theme.text,.12f),radius,1);}
    private void userTxt(Canvas c,String value,float x,float baseline,float size,int ink,Typeface face,Paint.Align align){textPaint.setColor(ink);textPaint.setTextSize(size);textPaint.setTypeface(selectedFont(face));textPaint.setTextAlign(align);I18n.drawUserText(c,value,x,baseline,textPaint);}
    private void shortUserTxt(Canvas c,String value,float x,float baseline,float maxWidth,float size,int ink,Typeface face,Paint.Align align){
        textPaint.setTextSize(size);textPaint.setTypeface(selectedFont(face));String out=value==null?"":value;
        if(textPaint.measureText(out)>maxWidth){int length=textPaint.breakText(out,true,Math.max(1,maxWidth-textPaint.measureText("…")),null);out=out.substring(0,Math.max(0,length))+"…";}
        userTxt(c,out,x,baseline,size,ink,face,align);
    }
    private void studioName(Canvas c,AppEntry app,float x,float y,float width,float size,Paint.Align align){if(app!=null)shortUserTxt(c,label(app),x,y,width,size,theme.text,bold,align);}
    private void studioAppHit(RectF area,AppEntry app){if(app!=null)hit(area,null,app);}
    private void studioIcon(Canvas c,AppEntry app,RectF area,float maxSize){if(app==null)return;float size=Math.max(12,Math.min(maxSize*iconSize()/72f,Math.min(area.width()-12,area.height()-12)));drawIcon(c,app,area.centerX()-size/2,area.centerY()-size/2,size);}
    private void studioLabelCard(Canvas c,AppEntry app,RectF area,boolean horizontal){
        if(app==null)return;studioCard(c,area,color(theme.surface,.88f),18);
        float icon=Math.min(iconSize(),Math.min(horizontal?area.height()-20:area.height()-50,area.width()*.52f));
        if(horizontal){drawIcon(c,app,area.left+14,area.centerY()-icon/2,icon);studioName(c,app,area.left+icon+30,area.centerY()+5,area.width()-icon-54,textSize()+5,Paint.Align.LEFT);}
        else{drawIcon(c,app,area.centerX()-icon/2,area.top+14,icon);studioName(c,app,area.centerX(),area.bottom-18,area.width()-18,textSize()+1,Paint.Align.CENTER);}
        studioAppHit(area,app);
    }
    private void studioPager(Canvas c,float y){
        float height=Math.max(52,44/viewport.scale);float width=Math.max(64,height);
        button(c,box(18,y,width,height),"‹","__previous",color(theme.surface,.85f),theme.text,27,false);
        button(c,box(w-width-18,y,width,height),"›","__next",color(theme.surface,.85f),theme.text,27,false);
        txt(c,(page+1)+" / "+pageCount,w/2,y+height*.65f,16,theme.muted,mono,Paint.Align.CENTER);
    }
    private void studioSlots(Canvas c){if(prefs.getBoolean("styleDock",false))toolbar(c,h-64,activeTheme==5||activeTheme==6||activeTheme==11,false);}
    private void studioCaption(Canvas c,String title,String subtitle){txt(c,title,22,31,16,accent,bold,Paint.Align.LEFT);if(subtitle!=null)txt(c,subtitle,w-22,31,12,theme.muted,mono,Paint.Align.RIGHT);}
    private void studioEmpty(Canvas c){if(apps.isEmpty())txt(c,"Aucune application visible",w/2,h*.52f,22,theme.muted,sans,Paint.Align.CENTER);}
    private void studioList(Canvas c,RectF area,int mode){
        listMode=true;page=0;pageCount=1;float row=Math.max(mode==0?76:66,46/viewport.scale);maxScroll=Math.max(0,apps.size()*row-area.height());scroll=Math.max(0,Math.min(maxScroll,scroll));
        c.save();c.clipRect(area);int start=Math.max(0,(int)(scroll/row)),end=Math.min(apps.size(),(int)((scroll+area.height())/row)+1);
        for(int i=start;i<end;i++){AppEntry app=apps.get(i);float y=area.top+i*row-scroll;
            if(mode==0){String initial=label(app).isEmpty()?"·":label(app).substring(0,1);userTxt(c,initial,area.left+7,y+48,43,accent,serif,Paint.Align.LEFT);studioName(c,app,area.left+67,y+43,area.width()-90,textSize()+9,Paint.Align.LEFT);line(c,area.left+67,y+row-6,area.right-6,y+row-6,color(theme.muted,.25f),1);}
            else{txt(c,String.format(Locale.ROOT,"%02d",i+1)+" >",area.left+4,y+25,15,accent,mono,Paint.Align.LEFT);studioName(c,app,area.left+63,y+25,area.width()-77,textSize()+2,Paint.Align.LEFT);shortUserTxt(c,app.packageName,area.left+63,y+47,area.width()-77,11,theme.muted,mono,Paint.Align.LEFT);}
            RectF touch=box(area.left,y,area.width(),row-2);if(touch.intersect(area))studioAppHit(touch,app);
        }c.restore();
    }
    private void studioStyle(Canvas c){
        float savedHeight=h;if(!prefs.getBoolean("styleDock",false))h=viewport.logicalHeight+64-Math.max(0,44/viewport.scale-60);
        switch(activeTheme){case 1:studioCalendar(c);break;case 2:studioDesktop(c);break;case 3:studioDock(c);break;case 4:studioInspector(c);break;case 5:studioTerminal(c);break;case 6:studioArcade(c);break;case 7:studioMechanics(c);break;case 8:studioOrbit(c);break;case 9:studioPaper(c);break;case 10:studioMasonry(c);break;case 11:studioLcd(c);break;case 12:studioUltra(c);break;case 13:studioCloud(c);break;case 14:studioFields(c);break;case 15:studioRacing(c);break;case 16:studioHex(c);break;case 17:studioTickets(c);break;case 18:studioNotebook(c);break;case 19:studioWheel(c);break;case 20:studioDaily(c);break;case 21:studioSilence(c);break;default:studioPhone(c);}
        studioEmpty(c);h=savedHeight;studioSlots(c);
    }
    private void studioPhone(Canvas c){
        studioCaption(c,dateShort(),battery());txt(c,time(),24,102,64,theme.text,bold,Paint.Align.LEFT);
        button(c,box(w*.58f,57,w*.36f,62),"Aujourd’hui","today",color(accent,.18f),theme.text,19,false);
        button(c,box(20,139,w-40,50),"⌕ Rechercher une application","search",color(theme.surface,.8f),theme.muted,17,false);
        grid(c,18,205,w-36,Math.max(80,h-350),0,0,false,false);studioPager(c,h-134);
    }
    private void studioCalendar(Canvas c){
        float left=w*.52f;studioCard(c,box(0,0,left,h-76),color(theme.surface,.9f),0);txt(c,"AGENDA",20,32,18,accent,bold,Paint.Align.LEFT);
        Calendar month=Calendar.getInstance();int today=month.get(Calendar.DAY_OF_MONTH);month.set(Calendar.DAY_OF_MONTH,1);int offset=(month.get(Calendar.DAY_OF_WEEK)+5)%7,days=month.getActualMaximum(Calendar.DAY_OF_MONTH);
        txt(c,new SimpleDateFormat("MMMM yyyy",I18n.locale()).format(month.getTime()),20,76,22,theme.text,bold,Paint.Align.LEFT);
        float cw=(left-30)/7f,ch=Math.max(34,Math.min(49,(h-205)/7));String[] week=new java.text.DateFormatSymbols(I18n.locale()).getShortWeekdays();
        for(int i=0;i<7;i++)shortTxt(c,week[(i+1)%7+1],18+(i+.5f)*cw,111,cw-2,11,theme.muted,sans,Paint.Align.CENTER);
        for(int day=1;day<=days;day++){int cell=offset+day-1;float x=18+(cell%7+.5f)*cw,y=145+(cell/7)*ch;if(day==today){p.setColor(accent);c.drawCircle(x,y-7,Math.min(cw,ch)*.42f,p);}txt(c,Integer.toString(day),x,y,16,day==today?theme.bg:theme.text,bold,Paint.Align.CENTER);}
        hit(box(0,40,left,h-125),"today",null);txt(c,time(),left+18,56,37,theme.text,bold,Paint.Align.LEFT);
        float x=left+14,width=w-left-28,top=85,height=(h-241)/2f;for(int i=0;i<4;i++){RectF area=box(x+i%2*width/2,top+i/2*(height+6),width/2-6,height);studioLabelCard(c,studioApp(i,4),area,false);}studioPager(c,h-134);
    }
    private void studioDesktop(Canvas c){
        studioCard(c,box(0,0,w,45),theme.surface,0);txt(c,"PRISME  /  BUREAU",18,29,15,theme.text,mono,Paint.Align.LEFT);txt(c,time(),w-18,29,17,accent,mono,Paint.Align.RIGHT);
        float desktop=w*.56f,top=67,cellW=(desktop-20)/2,cellH=(h-205)/3;for(int i=0;i<6;i++){AppEntry app=studioApp(i,6);if(app==null)continue;float x=16+i/3*cellW,y=top+i%3*cellH;studioIcon(c,app,box(x,y,cellW-12,cellH-26),Math.min(76,iconSize()));studioName(c,app,x+(cellW-12)/2,y+cellH-10,cellW-12,textSize(),Paint.Align.CENTER);studioAppHit(box(x,y,cellW-12,cellH-3),app);}
        RectF window=box(desktop+12,85,w-desktop-28,h-260);studioCard(c,window,color(theme.surface,.94f),5);fill(c,box(window.left,window.top,window.width(),28),accent,0);txt(c,"EXPLORATEUR",window.left+12,window.top+20,11,theme.bg,mono,Paint.Align.LEFT);txt(c,dateShort(),window.left+15,window.top+67,18,theme.text,bold,Paint.Align.LEFT);
        button(c,box(window.left+12,window.bottom-78,window.width()-24,60),"Toutes les applications","apps",theme.bg,theme.text,17,true);studioPager(c,h-134);
    }
    private void studioDock(Canvas c){
        fill(c,box(0,0,w,43),color(theme.surface,.65f),0);txt(c,"PRISME  ·  BIBLIOTHÈQUE",18,27,14,theme.text,bold,Paint.Align.LEFT);txt(c,time(),w-18,27,14,theme.text,sans,Paint.Align.RIGHT);
        txt(c,"Tes applications au premier plan",w/2,105,27,theme.text,sans,Paint.Align.CENTER);
        float top=150,body=Math.max(120,h-300),unit=(w-56)/3;fill(c,box(16,top-12,w-32,body+30),color(theme.surface,.65f),30);
        for(int i=0;i<3;i++){AppEntry app=studioApp(i,3);if(app==null)continue;float size=Math.min(i==1?135:92,body-50),cx=28+(i+.5f)*unit;drawIcon(c,app,cx-size/2,top+body*.45f-size/2,size);studioName(c,app,cx,top+body-7,unit-15,textSize()+3,Paint.Align.CENTER);studioAppHit(box(28+i*unit,top,unit,body),app);}studioPager(c,h-134);
    }
    private void studioInspector(Canvas c){
        studioCaption(c,"KALI  /  APP INSPECTOR",time());float split=w*.43f;studioCard(c,box(12,58,split-18,h-190),theme.surface,4);setCapacity(5);float rh=(h-205)/5f;
        for(int i=0;i<5;i++){int index=page*5+i;AppEntry app=studioApp(i,5);if(app==null)continue;RectF area=box(18,69+i*rh,split-30,rh-4);boolean picked=studioSelected==index;fill(c,area,picked?color(accent,.25f):color(theme.bg,.5f),2);studioName(c,app,area.left+10,area.centerY()+5,area.width()-20,textSize()+1,Paint.Align.LEFT);hit(area,"__pick:"+index,null);}
        if(!apps.isEmpty()){studioSelected=Math.max(page*5,Math.min(Math.min(apps.size()-1,page*5+4),studioSelected));AppEntry app=apps.get(studioSelected);float x=split+15,width=w-split-30;txt(c,"APPLICATION SÉLECTIONNÉE",x,85,12,accent,mono,Paint.Align.LEFT);float ih=h<520?55:95;studioIcon(c,app,box(x,105,width,ih),78);studioName(c,app,x,145+ih,width,textSize()+8,Paint.Align.LEFT);shortUserTxt(c,app.packageName,x,168+ih,width,12,theme.muted,mono,Paint.Align.LEFT);if(h>=520)txt(c,prefs.isFavorite(app.id)?"FAVORI":"APPLICATION INSTALLÉE",x,197+ih,12,accent,mono,Paint.Align.LEFT);RectF open=box(x,Math.min(322,h-205),width,65);studioCard(c,open,color(accent,.2f),3);txt(c,"Ouvrir",open.centerX(),open.centerY()+7,21,theme.text,bold,Paint.Align.CENTER);studioAppHit(open,app);}studioPager(c,h-134);
    }
    private void studioTerminal(Canvas c){
        txt(c,"prisme@watch:~ $",20,37,22,accent,mono,Paint.Align.LEFT);txt(c,"apps --installed --launch",20,72,17,theme.text,mono,Paint.Align.LEFT);line(c,18,90,w-18,90,color(accent,.35f),1);studioList(c,box(17,102,w-34,h-207),1);txt(c,"> toucher une ligne pour lancer",20,h-82,14,accent,mono,Paint.Align.LEFT);
    }
    private void studioArcade(Canvas c){
        stroke(c,box(12,12,w-24,h-91),accent,0,4);txt(c,"ARCADE SELECT",w/2,49,25,accent,mono,Paint.Align.CENTER);txt(c,time(),w-26,85,13,theme.muted,mono,Paint.Align.RIGHT);AppEntry app=studioApp(0,1);
        if(app!=null){RectF poster=box(w*.2f,99,w*.6f,Math.max(130,h-310));fill(c,poster,theme.surface,0);for(int i=0;i<10;i++)line(c,poster.left+7,poster.top+12+i*poster.height()/10,poster.right-7,poster.top+12+i*poster.height()/10,color(accent,.09f),1);studioIcon(c,app,poster,130);studioName(c,app,w/2,h-188,w-80,29,Paint.Align.CENTER);RectF start=box(w*.3f,h-169,w*.4f,64);fill(c,start,accent,0);txt(c,"START",start.centerX(),start.centerY()+9,26,theme.bg,mono,Paint.Align.CENTER);studioAppHit(start,app);}
        button(c,box(22,h*.43f,70,76),"◀","__previous",theme.bg,accent,30,true);button(c,box(w-92,h*.43f,70,76),"▶","__next",theme.bg,accent,30,true);
    }
    private void studioMechanics(Canvas c){
        studioCaption(c,"MECA  /  CONTROL DECK",battery());txt(c,time(),24,101,51,theme.text,mono,Paint.Align.LEFT);float unit=(w-48)/3,cy=(h-20)/2,radius=Math.min(unit*.37f,Math.max(35,(h-300)*.33f));
        for(int i=0;i<3;i++){AppEntry app=studioApp(i,3);if(app==null)continue;float cx=24+(i+.5f)*unit;RectF dial=box(cx-radius,cy-radius,radius*2,radius*2);fill(c,dial,theme.surface,radius);stroke(c,dial,color(accent,.48f),radius,3);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(accent);c.drawArc(dial,-220,260,false,p);p.setStyle(Paint.Style.FILL);studioIcon(c,app,box(cx-radius*.58f,cy-radius*.58f,radius*1.16f,radius*1.16f),75);studioName(c,app,cx,cy+radius+28,unit-10,textSize()+2,Paint.Align.CENTER);studioAppHit(box(cx-radius-5,cy-radius-5,radius*2+10,radius*2+45),app);}studioPager(c,h-134);
    }
    private void studioOrbit(Canvas c){
        studioCaption(c,"ORBIT",dateShort());setCapacity(6);float cx=w/2,cy=(h-110)/2,rx=w*.34f,ry=Math.max(62,(h-250)*.34f),size=Math.min(77,ry*.84f);
        p.setColor(color(accent,.25f));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);c.drawOval(box(cx-rx,cy-ry,rx*2,ry*2),p);p.setStyle(Paint.Style.FILL);
        txt(c,time(),cx,cy+8,38,theme.text,sans,Paint.Align.CENTER);hit(box(cx-70,cy-38,140,76),"today",null);
        for(int i=0;i<6;i++){AppEntry app=studioApp(i,6);if(app==null)continue;double angle=-Math.PI/2+i*Math.PI/3;float x=cx+(float)Math.cos(angle)*rx,y=cy+(float)Math.sin(angle)*ry;RectF node=box(x-size/2,y-size/2,size,size);studioIcon(c,app,node,size-6);studioName(c,app,x,y+size*.68f,Math.max(90,size+25),12,Paint.Align.CENTER);studioAppHit(box(x-size*.65f,y-size*.58f,size*1.3f,size*1.4f),app);}studioPager(c,h-134);
    }
    private void studioPaper(Canvas c){
        Calendar today=Calendar.getInstance();txt(c,String.valueOf(today.get(Calendar.DAY_OF_MONTH)),20,120,103,theme.text,serif,Paint.Align.LEFT);txt(c,new SimpleDateFormat("MMMM yyyy",I18n.locale()).format(new Date(now())),w*.35f,75,26,accent,serif,Paint.Align.LEFT);txt(c,"LE RÉPERTOIRE",w*.35f,113,16,theme.muted,mono,Paint.Align.LEFT);line(c,20,148,w-20,148,theme.text,2);studioList(c,box(20,165,w-40,h-255),0);
    }
    private void studioMasonry(Canvas c){
        studioCaption(c,"AURORA",dateShort());txt(c,time(),22,113,70,theme.text,sans,Paint.Align.LEFT);float top=156,height=Math.max(100,h-306),left=w*.43f;
        studioLabelCard(c,studioApp(0,5),box(16,top,left-24,height),false);float remaining=w-left-25,cw=remaining/2,ch=(height-9)/2;
        for(int i=1;i<5;i++)studioLabelCard(c,studioApp(i,5),box(left+(i-1)%2*(cw+3),top+(i-1)/2*(ch+9),cw-6,ch),false);studioPager(c,h-134);
    }
    private void studioLcd(Canvas c){
        studioCard(c,box(15,15,w-30,116),theme.surface,3);txt(c,"LCD  /  APP KEYPAD",28,41,13,theme.muted,mono,Paint.Align.LEFT);txt(c,time(),27,111,68,theme.text,mono,Paint.Align.LEFT);txt(c,battery(),w-28,108,24,accent,mono,Paint.Align.RIGHT);
        int keyRows=h<510?2:3;int count=keyRows*3;float top=154,cw=(w-40)/3,ch=(h-304)/keyRows;for(int i=0;i<count;i++){AppEntry app=studioApp(i,count);if(app==null)continue;RectF key=box(18+i%3*cw,top+i/3*ch,cw-5,ch-5);studioCard(c,key,theme.surface,2);txt(c,String.format(Locale.ROOT,"%02d",page*count+i+1),key.left+8,key.top+18,11,theme.muted,mono,Paint.Align.LEFT);studioName(c,app,key.centerX(),key.centerY()+10,key.width()-13,textSize()+1,Paint.Align.CENTER);studioAppHit(key,app);}studioPager(c,h-134);
    }
    private void studioUltra(Canvas c){
        studioCaption(c,"ULTRA / ACTION",battery());txt(c,time(),22,113,77,theme.text,bold,Paint.Align.LEFT);button(c,box(w*.64f,62,w*.32f,65),"ACTION","today",accent,theme.bg,21,false);
        int count=h<510?2:3;float top=160,row=(h-308)/count;for(int i=0;i<count;i++){AppEntry app=studioApp(i,count);RectF card=box(14,top+i*row,w-28,row-8);studioLabelCard(c,app,card,true);fill(c,box(card.left,card.top,6,card.height()),accent,0);}studioPager(c,h-134);
    }
    private void studioCloud(Canvas c){
        txt(c,time(),w/2,102,69,theme.text,sans,Paint.Align.CENTER);txt(c,dateShort(),w/2,138,19,theme.muted,sans,Paint.Align.CENTER);float top=174,body=Math.max(100,h-324),size=Math.min(87,body/3f-4);
        float[] xs={.33f,.67f,.17f,.5f,.83f,.33f,.67f};int[] rows={0,0,1,1,1,2,2};for(int i=0;i<7;i++){AppEntry app=studioApp(i,7);if(app==null)continue;float x=w*xs[i],y=top+(rows[i]+.5f)*body/3;RectF bubble=box(x-size/2,y-size/2,size,size);p.setColor(color(accent,i==3?.25f:.12f));c.drawCircle(x,y,size*.57f,p);studioIcon(c,app,bubble,size-8);studioAppHit(box(x-size*.6f,y-size*.6f,size*1.2f,size*1.2f),app);}studioPager(c,h-134);
    }
    private void studioFields(Canvas c){
        studioCaption(c,"MARQ / INSTRUMENTS",time());txt(c,dateShort(),22,88,23,theme.text,serif,Paint.Align.LEFT);float top=118,cw=(w-42)/2,ch=(h-266)/2;
        for(int i=0;i<4;i++){AppEntry app=studioApp(i,4);if(app==null)continue;RectF field=box(17+i%2*(cw+7),top+i/2*(ch+6),cw-4,ch-5);stroke(c,field,accent,0,1.3f);txt(c,"FIELD "+(i+1),field.left+13,field.top+24,11,theme.muted,mono,Paint.Align.LEFT);studioName(c,app,field.left+13,field.centerY()+8,field.width()-26,textSize()+12,Paint.Align.LEFT);shortUserTxt(c,app.packageName,field.left+13,field.bottom-19,field.width()-26,10,theme.muted,mono,Paint.Align.LEFT);studioAppHit(field,app);}studioPager(c,h-134);
    }
    private void studioRacing(Canvas c){
        studioCaption(c,"TAG / STARTING GRID",time());for(int n=0;n<24;n++)fill(c,box(n*w/24,55,w/24,12),n%2==0?theme.text:theme.bg,0);
        float top=100,body=Math.max(140,h-250),cw=(w-58)/3,cardH=body*.67f;
        for(int i=0;i<3;i++){AppEntry app=studioApp(i,3);if(app==null)continue;RectF card=box(18+i*(cw+9),top+i*(body-cardH)/2,cw,cardH);Path shape=new Path();shape.moveTo(card.left+13,card.top);shape.lineTo(card.right,card.top);shape.lineTo(card.right-13,card.bottom);shape.lineTo(card.left,card.bottom);shape.close();p.setColor(theme.surface);c.drawPath(shape,p);p.setColor(accent);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawPath(shape,p);p.setStyle(Paint.Style.FILL);txt(c,"0"+(page*3+i+1),card.left+24,card.top+34,26,accent,mono,Paint.Align.LEFT);studioIcon(c,app,box(card.left+15,card.top+44,card.width()-30,Math.max(50,card.height()-95)),84);studioName(c,app,card.centerX(),card.bottom-18,card.width()-32,textSize(),Paint.Align.CENTER);studioAppHit(card,app);}studioPager(c,h-134);
    }
    private void studioHex(Canvas c){
        studioCaption(c,"FUSION / TRIAD",battery());float body=Math.max(170,h-240),size=Math.min(w*.22f,body*.4f);float[] xs={w/2,w*.25f,w*.75f},ys={70+size*.6f,h-170-size*.6f,h-170-size*.6f};
        for(int i=0;i<3;i++){AppEntry app=studioApp(i,3);if(app==null)continue;float x=xs[i],y=ys[i];Path hex=new Path();for(int k=0;k<6;k++){double a=Math.PI/3*k;float xx=x+(float)Math.cos(a)*size*.54f,yy=y+(float)Math.sin(a)*size*.54f;if(k==0)hex.moveTo(xx,yy);else hex.lineTo(xx,yy);}hex.close();p.setColor(theme.surface);c.drawPath(hex,p);p.setColor(accent);p.setStrokeWidth(2);p.setStyle(Paint.Style.STROKE);c.drawPath(hex,p);p.setStyle(Paint.Style.FILL);studioIcon(c,app,box(x-size*.34f,y-size*.36f,size*.68f,size*.68f),80);studioName(c,app,x,y+size*.61f,size*1.35f,textSize()+1,Paint.Align.CENTER);studioAppHit(box(x-size*.62f,y-size*.62f,size*1.24f,size*1.35f),app);}studioPager(c,h-134);
    }
    private void studioTickets(Canvas c){
        studioCaption(c,"TAMBOUR / TRAVEL TAGS",dateShort());txt(c,time(),22,104,49,theme.text,serif,Paint.Align.LEFT);float top=136,height=Math.max(110,h-286),unit=(w-32)/4;
        for(int i=0;i<4;i++){AppEntry app=studioApp(i,4);if(app==null)continue;RectF tag=box(17+i*unit,top,unit-7,height);studioCard(c,tag,theme.surface,16);p.setColor(theme.bg);c.drawCircle(tag.centerX(),tag.top+14,4,p);line(c,tag.centerX(),tag.top-18,tag.centerX(),tag.top+9,accent,1);studioIcon(c,app,box(tag.left+8,tag.top+30,tag.width()-16,Math.min(80,tag.height()*.35f)),67);studioName(c,app,tag.centerX(),tag.top+Math.min(142,tag.height()*.62f),tag.width()-14,textSize(),Paint.Align.CENTER);int hash=app.id.hashCode();for(int n=0;n<19;n++)if(((hash>>(n%16))&1)!=0)fill(c,box(tag.left+10+n*(tag.width()-20)/19,tag.bottom-43,Math.max(1,(tag.width()-20)/38),23),theme.muted,0);studioAppHit(tag,app);}studioPager(c,h-134);
    }
    private void studioNotebook(Canvas c){
        studioCaption(c,"SUMMIT / CARNET",dateShort());float split=w*.62f;RectF paper=box(18,67,split-29,h-162);studioCard(c,paper,theme.surface,3);for(float y=paper.top+42;y<paper.bottom;y+=28)line(c,paper.left+10,y,paper.right-10,y,color(theme.muted,.23f),1);
        txt(c,"Ma note",paper.left+18,paper.top+31,25,accent,serif,Paint.Align.LEFT);String note=prefs.getString("notes","");if(note.trim().isEmpty())txt(c,"Une page à remplir…",paper.left+18,paper.top+91,21,theme.muted,serif,Paint.Align.LEFT);else studioNote(c,note,box(paper.left+16,paper.top+64,paper.width()-32,paper.height()-75),20);
        hit(paper,"notes",null);float row=(h-171)/4;for(int i=0;i<4;i++){AppEntry app=studioApp(i,4);if(app==null)continue;RectF mark=box(split+6,77+i*row,w-split-23,row-10);fill(c,mark,color(accent,.1f),3);txt(c,"0"+(page*4+i+1),mark.left+10,mark.top+22,12,accent,mono,Paint.Align.LEFT);studioName(c,app,mark.left+10,mark.centerY()+12,mark.width()-20,textSize()+2,Paint.Align.LEFT);studioAppHit(mark,app);}txt(c,(page+1)+" / "+pageCount,w-25,h-82,13,theme.muted,mono,Paint.Align.RIGHT);
    }
    private void studioWheel(Canvas c){
        studioCaption(c,"ULTIMATE / CROWN",time());AppEntry center=studioApp(0,1);float x=w/2,cy=(h+5)/2;
        if(center!=null){float size=Math.min(115,(h-240)*.47f);studioIcon(c,center,box(x-size/2,cy-size/2,size,size),size);studioName(c,center,x,cy+size*.7f,w-80,textSize()+13,Paint.Align.CENTER);studioAppHit(box(35,cy-size*.65f,w-70,Math.min(size*1.6f,h-150-(cy-size*.65f))),center);}
        AppEntry previous=page>0?apps.get(page-1):null,next=page+1<apps.size()?apps.get(page+1):null;
        if(previous!=null){studioName(c,previous,x,100,w-110,textSize(),Paint.Align.CENTER);hit(box(24,62,w-48,62),"__previous",null);}if(next!=null){studioName(c,next,x,h-110,w-110,textSize(),Paint.Align.CENTER);hit(box(24,h-146,w-48,62),"__next",null);}
        txt(c,"↑  /  ↓",w-23,cy,16,accent,mono,Paint.Align.RIGHT);txt(c,(page+1)+" / "+pageCount,23,cy,13,theme.muted,mono,Paint.Align.LEFT);
    }
    private void studioNote(Canvas c,String note,RectF area,float size){
        textPaint.setTextSize(size);textPaint.setTypeface(selectedFont(serif));String rest=note.replace('\n',' ');float baseline=area.top+size;
        c.save();c.clipRect(area);while(!rest.isEmpty()&&baseline<area.bottom){int count=textPaint.breakText(rest,true,area.width(),null);if(count<1)break;userTxt(c,rest.substring(0,count),area.left,baseline,size,theme.text,serif,Paint.Align.LEFT);rest=rest.substring(count);baseline+=size*1.55f;}c.restore();
    }
    private void studioDaily(Canvas c){
        studioCaption(c,"NOVA / TON QUOTIDIEN",battery());txt(c,time(),22,108,69,theme.text,bold,Paint.Align.LEFT);button(c,box(w*.68f,57,w*.28f,65),"Aujourd’hui","today",accent,theme.bg,18,false);
        float top=137,widgetH=Math.min(105,(h-300)*.48f),half=(w-45)/2;RectF task=box(16,top,half,widgetH),timer=box(28+half,top,half,widgetH);
        studioCard(c,task,theme.surface,17);txt(c,"TÂCHES",task.left+13,task.top+25,12,accent,bold,Paint.Align.LEFT);txt(c,Integer.toString(prefs.getChecklist().remaining()),task.left+14,task.bottom-18,38,theme.text,bold,Paint.Align.LEFT);hit(task,"checklist",null);
        studioCard(c,timer,theme.surface,17);txt(c,"MINUTEUR",timer.left+13,timer.top+25,12,accent,bold,Paint.Align.LEFT);long left=TimerEngine.remaining(getContext());txt(c,left>0?ExperienceController.duration(left):"Prêt",timer.left+13,timer.bottom-19,29,theme.text,mono,Paint.Align.LEFT);hit(timer,"timer",null);
        float dockH=Math.max(72,46/viewport.scale),dockY=h-74-dockH;RectF note=box(16,top+widgetH+10,w-32,Math.max(36,dockY-top-widgetH-18));studioCard(c,note,color(theme.surface,.7f),12);String value=prefs.getString("notes","");if(value.isEmpty())txt(c,"Ton carnet est prêt…",note.left+14,note.centerY()+6,18,theme.muted,sans,Paint.Align.LEFT);else shortUserTxt(c,value.replace('\n',' '),note.left+14,note.centerY()+6,note.width()-28,18,theme.text,serif,Paint.Align.LEFT);hit(note,"notes",null);
        for(int i=0;i<4;i++){AppEntry app=studioApp(i,4);if(app==null)continue;RectF tile=box(16+i*(w-32)/4,dockY,(w-32)/4-5,dockH);studioIcon(c,app,box(tile.left,tile.top,tile.width(),tile.height()-21),48);studioName(c,app,tile.centerX(),tile.bottom-4,tile.width()-5,12,Paint.Align.CENTER);studioAppHit(tile,app);}
    }
    private void studioSilence(Canvas c){
        txt(c,dateLong(),w/2,43,17,theme.muted,sans,Paint.Align.CENTER);txt(c,time(),w/2,150,105,theme.text,sans,Paint.Align.CENTER);AppEntry app=studioApp(0,1);
        if(app!=null){studioName(c,app,w/2,h*.57f,w-65,textSize()+25,Paint.Align.CENTER);txt(c,"Toucher pour ouvrir",w/2,h*.57f+37,15,theme.muted,sans,Paint.Align.CENTER);studioAppHit(box(26,180,w-52,Math.max(78,h-360)),app);}studioPager(c,h-134);
    }
    private org.json.JSONObject lastPreviewMetrics=new org.json.JSONObject();
    public org.json.JSONObject stylePreviewMetrics(){return lastPreviewMetrics;}
    public Bitmap createStylePreview(int id){return createStylePreview(id,320,240);}
    public Bitmap createStylePreview(int id,int imageWidth,int imageHeight){
        DashboardView view=new DashboardView(getContext(),prefs,listener);view.previewTheme=id;view.hostActive=false;view.setShortcutCatalog(shortcutCatalog);view.setApps(allApps);view.setSnapshot(snapshot);view.refresh();
        view.measure(MeasureSpec.makeMeasureSpec(640,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(480,MeasureSpec.EXACTLY));view.layout(0,0,640,480);
        Bitmap bitmap=Bitmap.createBitmap(imageWidth,imageHeight,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.scale(imageWidth/640f,imageHeight/480f);view.draw(canvas);
        try{org.json.JSONArray controls=new org.json.JSONArray();int appsCount=0,invalid=0;for(Hit control:view.hits){RectF a=control.r;if(a.left<-.1||a.top<-.1||a.right>640.1||a.bottom>480.1||a.width()<=0||a.height()<=0)invalid++;if(control.app!=null){appsCount++;controls.put(new org.json.JSONArray().put(Math.round(a.left)).put(Math.round(a.top)).put(Math.round(a.right)).put(Math.round(a.bottom)));}}
            lastPreviewMetrics=new org.json.JSONObject().put("styleId",id).put("directAppControls",appsCount).put("invalidControls",invalid).put("listMode",view.listMode).put("pageCapacity",view.capacity).put("appRectangles",controls).put("renderWidth",640).put("renderHeight",480);
        }catch(org.json.JSONException ignored){}view.handler.removeCallbacksAndMessages(null);return bitmap;
    }

}

package fr.dm100.prisme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.LruCache;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Package enumeration and icon decoding never block drawing or touches. */
public final class AppRepository {
    public interface Callback { void loaded(List<AppEntry> entries); }

    private final Context context;
    private final PackageManager pm;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicInteger generation = new AtomicInteger();
    private final LruCache<String, Bitmap> icons = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };
    private volatile boolean closed;

    public AppRepository(Context context) {
        this.context = context.getApplicationContext();
        pm = this.context.getPackageManager();
    }

    public void load(final Callback callback) {
        if (callback == null || closed) return;
        final int request = generation.incrementAndGet();
        worker.execute(new Runnable() {
            @Override public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                final ArrayList<AppEntry> result = new ArrayList<AppEntry>();
                final Set<String> seen = new HashSet<String>();
                final Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                try {
                    List<ResolveInfo> activities = pm.queryIntentActivities(query, 0);
                    for (ResolveInfo info : activities) {
                        if (closed || request != generation.get() || Thread.currentThread().isInterrupted()) return;
                        if (info.activityInfo == null || !info.activityInfo.enabled
                                || !info.activityInfo.applicationInfo.enabled
                                || !info.activityInfo.exported
                                || context.getPackageName().equals(info.activityInfo.packageName)) continue;
                        ComponentName component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                        if (!seen.add(component.flattenToString())) continue;
                        String label;
                        try { label = String.valueOf(info.loadLabel(pm)); }
                        catch (RuntimeException ex) { label = info.activityInfo.packageName; }
                        if ("null".equals(label)) label = info.activityInfo.packageName;
                        Bitmap icon = icons.get(component.flattenToString());
                        if (icon == null) {
                            Drawable drawable;
                            try { drawable = info.loadIcon(pm); }
                            catch (RuntimeException ex) { drawable = pm.getDefaultActivityIcon(); }
                            icon = rasterize(drawable);
                            if (icon != null) icons.put(component.flattenToString(), icon);
                        }
                        result.add(new AppEntry(component, label, icon));
                    }
                } catch (RuntimeException ignored) {
                    // Keep any entries already found if an OEM package disappears during enumeration.
                }
                final Collator fr = Collator.getInstance(I18n.locale());
                fr.setStrength(Collator.PRIMARY);
                Collections.sort(result, new Comparator<AppEntry>() {
                    @Override public int compare(AppEntry a, AppEntry b) {
                        int label = fr.compare(a.label, b.label);
                        return label != 0 ? label : a.id.compareTo(b.id);
                    }
                });
                main.post(new Runnable() {
                    @Override public void run() {
                        if (!closed && request == generation.get()) callback.loaded(Collections.unmodifiableList(result));
                    }
                });
            }
        });
    }

    private Bitmap rasterize(Drawable drawable) {
        if (drawable == null) return null;
        try {
            final int side = 96;
            Bitmap bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Rect previous = new Rect(drawable.getBounds());
            int width = drawable.getIntrinsicWidth(), height = drawable.getIntrinsicHeight();
            if (width <= 0 || height <= 0) { width = side; height = side; }
            float factor = Math.min((float) side / width, (float) side / height);
            int w = Math.max(1, Math.round(width * factor)), h = Math.max(1, Math.round(height * factor));
            drawable.setBounds((side - w) / 2, (side - h) / 2, (side + w) / 2, (side + h) / 2);
            drawable.draw(canvas);
            drawable.setBounds(previous);
            return bitmap;
        } catch (RuntimeException ex) { return null; }
    }

    public void close() {
        closed = true;
        generation.incrementAndGet();
        worker.shutdownNow();
        main.removeCallbacksAndMessages(null);
        icons.evictAll();
    }
}

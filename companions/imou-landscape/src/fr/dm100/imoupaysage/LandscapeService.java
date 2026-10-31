package fr.dm100.imoupaysage;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.KeyguardManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import java.util.List;

/** Orientation companion: no activity content, account data or global settings are modified. */
public final class LandscapeService extends Service {
    private static final String CHANNEL = "orientation";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windows;
    private UsageStatsManager usage;
    private PowerManager power;
    private KeyguardManager keyguard;
    private View anchor;
    private String foreground;
    private long cursor;
    private boolean registered;
    private boolean managedByPrisme;
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            update();
            if (Companion.enabled(LandscapeService.this)) handler.postDelayed(this, 750);
        }
    };
    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) {
                removeAnchor();
                foreground = null;
                cursor = 0;
            } else if (Intent.ACTION_SCREEN_ON.equals(i.getAction())) {
                cursor = 0;
                handler.removeCallbacks(poll);
                handler.post(poll);
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        windows = (WindowManager) getSystemService(WINDOW_SERVICE);
        usage = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        power = (PowerManager) getSystemService(POWER_SERVICE);
        keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screen, f);
        registered = true;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && Companion.STOP.equals(intent.getAction())) {
            Companion.enabled(this, false);
            removeAnchor();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Companion.enabled(this) || !Companion.permissions(this)) {
            removeAnchor();
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(1, notification());
        handler.removeCallbacks(poll);
        handler.post(poll);
        return START_STICKY;
    }

    private Notification notification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Imou Paysage", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Maintien en paysage uniquement dans Imou");
            nm.createNotificationChannel(ch);
        }
        Intent settings = new Intent(this, MainActivity.class).putExtra("reglages", true);
        PendingIntent open = PendingIntent.getActivity(this, 0, settings, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, LandscapeService.class).setAction(Companion.STOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setSmallIcon(R.drawable.ic_camera).setContentTitle(managedByPrisme ? "Imou Paysage en attente" : "Imou Paysage activé")
                .setContentText(managedByPrisme ? "Orientation gérée par PRISME · reprise automatique" : "Paysage dans Imou uniquement · toucher pour régler")
                .setContentIntent(open).setOngoing(true).setShowWhen(false)
                .addAction(new Notification.Action.Builder(R.drawable.ic_camera, "Arrêter", stop).build()).build();
    }

    private void update() {
        if (!Companion.enabled(this) || !Companion.permissions(this)) {
            foreground = null;
            removeAnchor();
            stopForeground(true);
            stopSelf();
            return;
        }
        if (power == null || !power.isInteractive() || keyguard == null || keyguard.isKeyguardLocked()) {
            foreground = null;
            cursor = 0;
            removeAnchor();
            return;
        }
        try {
            if (usage == null) throw new IllegalStateException("Usage access absent");
            long now = System.currentTimeMillis();
            // A short overlap makes identical millisecond boundaries harmless: replay in order,
            // then decide once. At first start/wake reconstruct the latest app from local history.
            long from = cursor == 0 || cursor > now ? now - 24L * 60 * 60 * 1000 : Math.max(0, cursor - 3000);
            UsageEvents events = usage.queryEvents(from, now);
            if (events == null) throw new IllegalStateException("No usage events");
            UsageEvents.Event event = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    foreground = event.getPackageName();
                } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND
                        && event.getPackageName().equals(foreground)) {
                    foreground = null;
                }
            }
            cursor = now;
            if (Companion.isImou(foreground)) {
                boolean managed = prismeControlsOrientation();
                if (managed) removeAnchor(); else addAnchor();
                setManagedByPrisme(managed);
            } else {
                removeAnchor();
                setManagedByPrisme(false);
            }
        } catch (RuntimeException problem) {
            // Fail open: never leave a forced orientation after a read/window error.
            foreground = null;
            cursor = 0;
            removeAnchor();
        }
    }

    /** Android 7 service-management API; other apps' services are unavailable from API 26 onward. */
    private boolean prismeControlsOrientation() {
        if (Build.VERSION.SDK_INT >= 26) return false;
        try {
            ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (manager == null) return false;
            List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
            if (services == null) return false;
            for (ActivityManager.RunningServiceInfo service : services) {
                if (service != null && service.foreground && service.service != null
                        && "fr.dm100.prisme".equals(service.service.getPackageName())
                        && "fr.dm100.prisme.GlobalRotationService".equals(service.service.getClassName())) return true;
            }
        } catch (RuntimeException unavailable) { /* Preserve the historical Imou behavior. */ }
        return false;
    }

    private void setManagedByPrisme(boolean managed) {
        if (managedByPrisme == managed) return;
        managedByPrisme = managed;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(1, notification());
    }

    private void addAnchor() {
        if (anchor != null || !Settings.canDrawOverlays(this)) return;
        View v = new View(this);
        v.setBackgroundColor(Color.TRANSPARENT);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.LEFT;
        p.x = 0; p.y = 0; p.alpha = 1f;
        p.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        p.setTitle("Imou Paysage — orientation");
        try {
            windows.addView(v, p);
            anchor = v;
        } catch (RuntimeException problem) {
            try { windows.removeViewImmediate(v); } catch (RuntimeException ignored) { }
            anchor = null;
        }
    }

    private void removeAnchor() {
        View old = anchor;
        anchor = null;
        if (old != null && windows != null) {
            try { windows.removeViewImmediate(old); } catch (RuntimeException ignored) { }
        }
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        removeAnchor();
        if (registered) {
            try { unregisterReceiver(screen); } catch (RuntimeException ignored) { }
        }
        stopForeground(true);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}

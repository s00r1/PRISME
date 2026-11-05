package fr.dm100.prisme.volume;
import fr.dm100.prisme.I18n;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import fr.dm100.prisme.FloatingVolumePrefs;
import fr.dm100.prisme.MainActivity;

/** A visible foreground service owns the window; leaving PRISME never creates a second owner. */
public final class FloatingVolumeService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String ACTION_STATE_CHANGED = "fr.dm100.prisme.volume.STATE_CHANGED";
    private static final String START = "fr.dm100.prisme.volume.START", STOP = "fr.dm100.prisme.volume.STOP", REFRESH = "fr.dm100.prisme.volume.REFRESH";
    private static final int NOTIFICATION = 73;
    private static final String CHANNEL = "prisme_floating_volume";
    private static volatile FloatingVolumeService instance;
    private static volatile String error = "";
    private static boolean manualSessionWithoutBootCount;
    private static int requestGeneration;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FloatingVolumePrefs prefs;
    private FloatingVolumeOverlay overlay;
    private boolean stopping, registered, pausedForPermission, restartRequested;
    private int restartGeneration;
    private final Runnable reconcileLater = new Runnable() { @Override public void run() { reconcile(false); } };
    private final BroadcastReceiver events = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null || stopping) return;
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) removeOverlay();
            else if ("android.media.VOLUME_CHANGED_ACTION".equals(action)
                    || android.media.AudioManager.RINGER_MODE_CHANGED_ACTION.equals(action)) {
                if (overlay != null) overlay.refreshLevel();
            } else reconcile(false);
        }
    };

    public static boolean begin(Context context) {
        Context app = context.getApplicationContext();
        int generation = ++requestGeneration;
        try {
            if (!Settings.canDrawOverlays(app)) { report("Autorise d’abord la superposition de PRISME."); return false; }
            FloatingVolumePrefs prefs = new FloatingVolumePrefs(app);
            manualSessionWithoutBootCount = true;
            prefs.markActiveBoot(currentBoot(app)); prefs.setEnabled(true);
            FloatingVolumeService previous = instance;
            if (previous != null && previous.stopping) {
                // A new explicit request can arrive between stopSelf() and onDestroy().
                // Defer only that request until destruction; this is not an OEM restart watchdog.
                previous.restartRequested = true; previous.restartGeneration = generation; return true;
            }
            error = "";
            boolean started = start(app, START);
            if (!started) { manualSessionWithoutBootCount = false; prefs.setEnabled(false); }
            return started;
        } catch (RuntimeException ex) {
            new FloatingVolumePrefs(app).setEnabled(false);
            report("Démarrage refusé : " + ex.getClass().getSimpleName()); return false;
        }
    }
    private static boolean start(Context context, String action) {
        try {
            Intent intent = new Intent(context, FloatingVolumeService.class).setAction(action);
            if (Build.VERSION.SDK_INT >= 26) Api26.start(context, intent); else context.startService(intent);
            return true;
        } catch (RuntimeException ex) {
            report("Android a refusé le service : " + ex.getClass().getSimpleName()); return false;
        }
    }
    public static void stop(Context context) {
        requestGeneration++;
        manualSessionWithoutBootCount = false;
        new FloatingVolumePrefs(context).setEnabled(false);
        FloatingVolumeService service = instance;
        if (service != null) service.restartRequested = false;
        if (service != null) service.runOnMain(new Runnable() { @Override public void run() {
            if (!service.prefs.getBoolean("enabled", false)) service.finishStop();
        } });
        else context.stopService(new Intent(context, FloatingVolumeService.class));
    }
    public static void refresh(Context context) {
        if(fr.dm100.prisme.OverlayPolicy.homeVisible||fr.dm100.prisme.OverlayPolicy.suspended(context)){context.stopService(new Intent(context,FloatingVolumeService.class));return;}
        FloatingVolumeService service = instance;
        if (service != null) {
            service.runOnMain(new Runnable() { @Override public void run() {
                if (service.stopping) return;
                service.pausedForPermission = false; service.reconcile(true);
            }});
            return;
        }
        FloatingVolumePrefs p = new FloatingVolumePrefs(context);
        try { if (p.mayResume(currentBoot(context)) && Settings.canDrawOverlays(context)) start(context.getApplicationContext(), REFRESH); }
        catch (RuntimeException ex) { report("Relance refusée : " + ex.getClass().getSimpleName()); }
    }
    public static void suspendForPermission(Context context) {
        FloatingVolumeService service = instance;
        if (service == null) return;
        service.runOnMain(new Runnable() { @Override public void run() {
            service.pausedForPermission = true; service.removeOverlay(); service.stateChanged();
        }});
    }
    private void runOnMain(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) action.run(); else handler.post(action);
    }
    public static boolean isRunning() { FloatingVolumeService s = instance; return s != null && !s.stopping; }
    public static boolean isShowing() { FloatingVolumeService s = instance; return s != null && s.overlay != null && s.overlay.isShowing(); }
    public static String lastError() { return error; }
    public static String diagnostics(Context context) {
        FloatingVolumeService s = instance;
        return (s != null && s.overlay != null ? s.overlay.diagnostics() : FloatingVolumeOverlay.displayReport(context))
                + "\nService : " + (isRunning() ? "actif" : "arrêté") + " · fenêtre : " + (isShowing() ? "affichée" : "masquée")
                + (s != null && s.pausedForPermission ? "\nMasquée pendant une autorisation Android." : "")
                + (error.length() == 0 ? "" : "\nDernier message : " + error);
    }
    static int currentBoot(Context context) {
        try { return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1); }
        catch (RuntimeException ex) { return -1; }
    }
    static void report(String message) { error = message == null ? "" : message; }
    static Intent settingsIntent(Context context) {
        return new Intent(context, MainActivity.class).putExtra("volumeSettings", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }
    @Override public void onCreate() {
        super.onCreate(); instance = this; prefs = new FloatingVolumePrefs(this);
        try { startForeground(NOTIFICATION, notification()); }
        catch (RuntimeException ex) {
            report("Notification du service refusée : " + ex.getClass().getSimpleName());
            manualSessionWithoutBootCount = false; prefs.setEnabled(false); stopping = true;
            stateChanged(); stopSelf(); return;
        }
        prefs.registerListener(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF); filter.addAction(Intent.ACTION_SCREEN_ON); filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction("android.media.VOLUME_CHANGED_ACTION"); filter.addAction(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION);
        registerReceiver(events, filter); registered = true;
        stateChanged();
    }
    private Notification notification() {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) b = Api26.builder(this); else b = new Notification.Builder(this);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent open = PendingIntent.getActivity(this, 73, settingsIntent(this), flags);
        PendingIntent stop = PendingIntent.getService(this, 74, new Intent(this, FloatingVolumeService.class).setAction(STOP), flags);
        int icon = getApplicationInfo().icon != 0 ? getApplicationInfo().icon : android.R.drawable.ic_lock_silent_mode_off;
        return b.setSmallIcon(icon).setContentTitle(I18n.t(this,"Volume flottant PRISME"))
                .setContentText(I18n.t(this,"Boutons de volume · toucher pour personnaliser"))
                .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE).setPriority(Notification.PRIORITY_LOW)
                .addAction(new Notification.Action.Builder(0, I18n.t(this,"Arrêter"), stop).build()).build();
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (stopping) { stopSelf(); return START_NOT_STICKY; }
        if (intent != null && STOP.equals(intent.getAction())) { prefs.setEnabled(false); finishStop(); return START_NOT_STICKY; }
        if (intent != null && (START.equals(intent.getAction()) || REFRESH.equals(intent.getAction()))) pausedForPermission = false;
        if (!wanted()) { finishStop(); return START_NOT_STICKY; }
        reconcile(true); return START_STICKY;
    }
    private boolean wanted() {
        int boot = currentBoot(this);
        return prefs.getBoolean("enabled", false) && (prefs.mayResume(boot) || boot < 0 && manualSessionWithoutBootCount);
    }
    private void reconcile(boolean applyPosition) {
        if(fr.dm100.prisme.OverlayPolicy.homeVisible||fr.dm100.prisme.OverlayPolicy.suspended(this)){removeOverlay();return;}
        if (stopping) return;
        if (!wanted()) { finishStop(); return; }
        boolean interactive = true;
        try { PowerManager power = (PowerManager)getSystemService(POWER_SERVICE); interactive = power != null && power.isInteractive(); }
        catch (RuntimeException ignored) { }
        if (pausedForPermission || !interactive) { removeOverlay(); stateChanged(); return; }
        if (!Settings.canDrawOverlays(this)) {
            report("L’autorisation de superposition n’est plus disponible."); removeOverlay(); stateChanged(); return;
        }
        if (overlay == null) {
            overlay = new FloatingVolumeOverlay(this);
            if (!overlay.show()) { overlay.destroy(); overlay = null; }
        } else overlay.refresh(applyPosition);
        stateChanged();
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences store, String key) {
        if (stopping || "activeBoot".equals(key) || "xPercent".equals(key) || "yPercent".equals(key)) return;
        if ("boot".equals(key) && !prefs.getBoolean("boot", false) && prefs.getBoolean("enabled", false)) prefs.markActiveBoot(currentBoot(this));
        handler.removeCallbacks(reconcileLater); handler.postDelayed(reconcileLater, 40);
    }
    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration); removeOverlay(); reconcile(false);
    }
    private void removeOverlay() { if (overlay != null) { overlay.destroy(); overlay = null; } }
    private void finishStop() {
        if (stopping) return;
        stopping = true; handler.removeCallbacksAndMessages(null); removeOverlay(); stopForeground(true); stateChanged(); stopSelf();
    }
    void stateChanged() {
        try { sendBroadcast(new Intent(ACTION_STATE_CHANGED).setPackage(getPackageName())); }
        catch (RuntimeException ignored) { }
    }
    @Override public void onDestroy() {
        final int restart = restartRequested ? restartGeneration : -1;
        final Context app = getApplicationContext();
        stopping = true; handler.removeCallbacksAndMessages(null); removeOverlay();
        if (prefs != null) prefs.unregisterListener(this);
        if (registered) { unregisterReceiver(events); registered = false; }
        if (instance == this) instance = null;
        stopForeground(true); stateChanged(); super.onDestroy();
        if (restart >= 0) new Handler(Looper.getMainLooper()).post(new Runnable() { @Override public void run() {
            if (requestGeneration != restart) return;
            FloatingVolumePrefs p = new FloatingVolumePrefs(app);
            if (!p.getBoolean("enabled", false)) return;
            if (!start(app, START)) { manualSessionWithoutBootCount = false; p.setEnabled(false); }
        }});
    }
    @Override public void onTaskRemoved(Intent intent) { super.onTaskRemoved(intent); }
    @Override public IBinder onBind(Intent intent) { return null; }
    static int windowType() { return Build.VERSION.SDK_INT >= 26 ? Api26.windowType() : android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT; }
    /** Isolated references keep Android 7.1 verification free of Android 8-only method bodies. */
    private static final class Api26 {
        static void start(Context context, Intent intent) { context.startForegroundService(intent); }
        static int windowType() { return android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY; }
        static Notification.Builder builder(Context context) {
            NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL, I18n.t(context,"Volume flottant PRISME"), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(I18n.t(context,"Commande de volume active, avec action Arrêter.")); channel.setSound(null, null); channel.enableVibration(false);
            if (manager != null) manager.createNotificationChannel(channel);
            return new Notification.Builder(context, CHANNEL);
        }
    }
}

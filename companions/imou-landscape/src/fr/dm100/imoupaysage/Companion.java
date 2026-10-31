package fr.dm100.imoupaysage;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;

final class Companion {
    static final String PHONE = "com.mm.android.smartlifeiot";
    static final String HD = "com.imoulife.pad";
    static final String STOP = "fr.dm100.imoupaysage.STOP";
    private Companion() { }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("paysage", Context.MODE_PRIVATE); }
    static boolean enabled(Context c) { return prefs(c).getBoolean("enabled", false); }
    static void enabled(Context c, boolean value) { prefs(c).edit().putBoolean("enabled", value).apply(); }
    static boolean isImou(String pkg) { return HD.equals(pkg) || PHONE.equals(pkg); }
    static boolean usageAllowed(Context c) {
        try {
            AppOpsManager manager = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
            return manager != null && manager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), c.getPackageName()) == AppOpsManager.MODE_ALLOWED;
        } catch (RuntimeException ignored) { return false; }
    }
    static boolean permissions(Context c) { return Settings.canDrawOverlays(c) && usageAllowed(c); }
    static void start(Context c) {
        Intent i = new Intent(c, LandscapeService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }
    static void stop(Context c) {
        enabled(c, false);
        c.stopService(new Intent(c, LandscapeService.class));
    }
}

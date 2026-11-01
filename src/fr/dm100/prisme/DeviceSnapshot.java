package fr.dm100.prisme;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;

/** A cheap, read-only dashboard snapshot; no polling service or network requests. */
public final class DeviceSnapshot {
    public final int batteryPct;
    public final boolean charging, wifiConnected;
    public final int memoryUsedPct;
    public final double freeStorageGb;
    public final long deviceTimeMillis;

    private DeviceSnapshot(int batteryPct, boolean charging, boolean wifiConnected,
                           int memoryUsedPct, double freeStorageGb, long deviceTimeMillis) {
        this.batteryPct = batteryPct; this.charging = charging; this.wifiConnected = wifiConnected;
        this.memoryUsedPct = memoryUsedPct; this.freeStorageGb = freeStorageGb;
        this.deviceTimeMillis = deviceTimeMillis;
    }

    public static DeviceSnapshot read(Context context) {
        int battery = -1, memory = -1;
        boolean charging = false, wifi = false;
        double free = -1;
        BatteryLevelReader.Reading reading=BatteryLevelReader.read(context,null);
        battery=reading.level;charging=reading.charging;
        try {
            ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo network = connectivity == null ? null : connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            wifi = network != null && network.isConnected();
        } catch (RuntimeException ignored) { }
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            if (manager != null) {
                manager.getMemoryInfo(info);
                if (info.totalMem > 0) memory = (int) Math.max(0, Math.min(100, Math.round(100d * (info.totalMem - info.availMem) / info.totalMem)));
            }
        } catch (RuntimeException ignored) { }
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            free = stat.getAvailableBytes() / (1024d * 1024d * 1024d);
        } catch (RuntimeException ignored) { }
        return new DeviceSnapshot(battery, charging, wifi, memory, free, System.currentTimeMillis());
    }
}

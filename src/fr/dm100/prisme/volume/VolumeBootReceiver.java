package fr.dm100.prisme.volume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import fr.dm100.prisme.FloatingVolumePrefs;
import fr.dm100.prisme.ControlCenterService;

/** The user must opt in explicitly; this does not bypass an Android/OEM force-stop. */
public final class VolumeBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()))) return;
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))fr.dm100.prisme.GlobalRotationService.boot(context);
        else if(fr.dm100.prisme.GlobalRotationService.mode(context)>0)fr.dm100.prisme.GlobalRotationService.apply(context,fr.dm100.prisme.GlobalRotationService.mode(context));
        ControlCenterService.refresh(context);
        FloatingVolumePrefs prefs = new FloatingVolumePrefs(context);
        if (!prefs.getBoolean("enabled", false) || !prefs.getBoolean("boot", false)) return;
        try { if (Settings.canDrawOverlays(context)) FloatingVolumeService.begin(context); }
        catch (RuntimeException ignored) { /* No alternate service or permission workaround. */ }
    }
}

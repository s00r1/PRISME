package fr.dm100.prisme;

import android.content.*;

/** One device-local master switch; it never imports activation from a theme pack. */
public final class OverlayPolicy {
  public static boolean homeVisible;

  private OverlayPolicy() {}

  public static boolean suspended(Context c) {
    return PrefValues.flag(c.getSharedPreferences("prisme_overlay_policy",0),"suspended",false);
  }

  public static void suspend(Context c) {
    c.getSharedPreferences("prisme_overlay_policy", 0)
        .edit()
        .putBoolean("suspended", true)
        .commit();
    c.stopService(new Intent(c, ControlCenterService.class));
    c.stopService(new Intent(c, EdgeDockService.class));
    c.stopService(new Intent(c, GlobalRotationService.class));
    c.stopService(new Intent(c, fr.dm100.prisme.volume.FloatingVolumeService.class));
  }

  public static void resume(Context c) {
    c.getSharedPreferences("prisme_overlay_policy", 0)
        .edit()
        .putBoolean("suspended", false)
        .commit();
    ControlCenterService.refresh(c);
    EdgeDockService.refresh(c);
    fr.dm100.prisme.volume.FloatingVolumeService.refresh(c);
    if (GlobalRotationService.mode(c) > 0)
      GlobalRotationService.apply(c, GlobalRotationService.mode(c));
  }
}

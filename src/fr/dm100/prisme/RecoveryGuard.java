package fr.dm100.prisme;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;

/** Crash recovery guard: no process kill loop, telemetry or sensitive exception persistence. */
public final class RecoveryGuard {
  private static boolean installed;

  private RecoveryGuard() {}

  public static void install(Context c) {
    if (installed) return;
    installed = true;
    Context app = c.getApplicationContext();
    Thread.UncaughtExceptionHandler prior = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler(
        (thread, error) -> {
          try {
            android.content.SharedPreferences p = app.getSharedPreferences("prisme_recovery", 0);
            p.edit().putInt("crashes", Math.min(3, p.getInt("crashes", 0) + 1)).commit();
          } catch (RuntimeException ignored) {
          }
          if (prior != null) prior.uncaughtException(thread, error);
        });
  }

  public static boolean needed(Context c) {
    try {
      return c.getSharedPreferences("prisme_recovery", 0).getInt("crashes", 0) >= 3;
    } catch (RuntimeException e) {
      return true;
    }
  }

  public static void clear(Context c) {
    c.getSharedPreferences("prisme_recovery", 0).edit().clear().commit();
  }

  public static void force(Context c) {
    c.getSharedPreferences("prisme_recovery", 0).edit().putInt("crashes", 3).commit();
  }

  public static void show(Activity a) {
    LinearLayout root = V6Ui.column(a);
    root.addView(V6Ui.text(a, "PRISME · Mode de secours", 26));
    root.addView(
        V6Ui.text(
            a,
            "Les notes et tâches sont conservées. Choisissez une reprise sans les fonctions"
                + " flottantes si le problème revient.",
            18));
    a.stopService(new Intent(a, EdgeDockService.class));
    a.stopService(new Intent(a, ControlCenterService.class));
    a.stopService(new Intent(a, GlobalRotationService.class));
    a.stopService(new Intent(a, fr.dm100.prisme.volume.FloatingVolumeService.class));
    V6Ui.add(
        root,
        "Réessayer l’accueil",
        () -> {
          clear(a);
          a.recreate();
        });
    V6Ui.add(
        root,
        "Bureau classique et fonctions flottantes désactivées",
        () -> {
          a.getSharedPreferences("prisme_overlay_policy", 0)
              .edit()
              .putBoolean("suspended", true)
              .apply();
          a.getSharedPreferences("prisme_media_access", 0)
              .edit()
              .putBoolean("enabled", false)
              .apply();
          new HomeStore(a).enabled(false);
          new ControlCenterPrefs(a).putBoolean("enabled", false);
          new FloatingVolumePrefs(a).putBoolean("enabled", false);
          a.getSharedPreferences("prisme_edge", 0).edit().putBoolean("enabled", false).apply();
          a.getSharedPreferences("prisme_battery_overlay", 0)
              .edit()
              .putBoolean("enabled", false)
              .apply();
          a.getSharedPreferences("prisme_sense", 0)
              .edit()
              .putBoolean("enabled", false)
              .putBoolean("powerSaver", false)
              .apply();
          clear(a);
          a.recreate();
        });
    V6Ui.add(root, "Diagnostic local", () -> PrismeDoctor.show(a));
    a.setContentView(root);
    I18n.bind(root);
  }
}

package fr.dm100.prisme;

import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import org.json.*;

/** Eight named alarm-backed countdowns. Active runs stay device-local, out of theme imports. */
public final class MultiTimers {
  private MultiTimers() {}

  private static android.content.SharedPreferences store(Context c) {
    return c.getSharedPreferences("prisme_multi_timers", 0);
  }

  static int boot(Context c) {
    try {
      return Settings.Global.getInt(c.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
    } catch (RuntimeException e) {
      return -1;
    }
  }

  public static JSONObject read(Context c, int id) {
    try {
      return new JSONObject(store(c).getString("t" + id, "{}"));
    } catch (Exception e) {
      return new JSONObject();
    }
  }

  private static boolean write(Context c, int id, JSONObject o) {
    return store(c).edit().putString("t" + id, o.toString()).commit();
  }

  public static long remaining(Context c, JSONObject t) {
    return Countdown.remaining(
        t.optLong("wall"),
        t.optLong("elapsed"),
        t.optInt("boot", -1),
        boot(c),
        System.currentTimeMillis(),
        SystemClock.elapsedRealtime(),
        t.optLong("paused"));
  }

  public static synchronized int create(Context c, String name, long duration)
      throws JSONException {
    Countdown.validateName(name);
    if (!Countdown.validDuration(duration))
      throw new JSONException("Durée : 1 seconde à 24 heures");
    for (int i = 0; i < 8; i++)
      if (!read(c, i).has("name")) {
        JSONObject t =
            new JSONObject()
                .put("name", name.trim())
                .put("duration", duration)
                .put("paused", duration)
                .put("token", 0);
        if (!write(c, i, t)) throw new JSONException("Enregistrement impossible");
        return i;
      }
    throw new JSONException("8 minuteurs maximum");
  }

  private static PendingIntent alarm(Context c, int id, long token) {
    return PendingIntent.getBroadcast(
        c,
        600 + id,
        new Intent(c, TimerReceiver.class)
            .setAction("fr.dm100.prisme.MULTI_TIMER")
            .putExtra("id", id)
            .putExtra("token", token),
        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public static synchronized boolean start(Context c, int id, boolean restart) {
    if (id < 0 || id >= 8) return false;
    JSONObject t = read(c, id);
    long duration = restart ? t.optLong("duration") : remaining(c, t);
    if (!t.has("name") || !Countdown.validDuration(duration)) return false;
    try {
      t.put("wall", System.currentTimeMillis() + duration)
          .put("elapsed", SystemClock.elapsedRealtime() + duration)
          .put("boot", boot(c))
          .put("paused", 0)
          .put("token", t.optLong("token") + 1)
          .put("finished", false);
      if (!write(c, id, t)) return false;
      return schedule(c, id);
    } catch (JSONException e) {
      return false;
    }
  }

  public static synchronized boolean schedule(Context c, int id) {
    JSONObject t = read(c, id);
    if (!t.has("name") || t.optLong("wall") == 0) return true;
    long left = remaining(c, t);
    if (left == 0) {
      finish(c, id, t.optLong("token"));
      return true;
    }
    AlarmManager m = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    if (m == null) return false;
    try {
      t.put("elapsed", SystemClock.elapsedRealtime() + left).put("boot", boot(c));
      write(c, id, t);
      m.setExactAndAllowWhileIdle(
          AlarmManager.ELAPSED_REALTIME_WAKEUP,
          SystemClock.elapsedRealtime() + left,
          alarm(c, id, t.optLong("token")));
      return true;
    } catch (RuntimeException | JSONException e) {
      return false;
    }
  }

  public static synchronized void pause(Context c, int id) {
    JSONObject t = read(c, id);
    if (!t.has("name")) return;
    long left = remaining(c, t);
    cancelAlarm(c, id, t);
    try {
      t.put("wall", 0).put("elapsed", 0).put("paused", left).put("token", t.optLong("token") + 1);
      write(c, id, t);
    } catch (JSONException ignored) {
    }
  }

  private static void cancelAlarm(Context c, int id, JSONObject t) {
    AlarmManager m = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    if (m != null) m.cancel(alarm(c, id, t.optLong("token")));
  }

  public static synchronized void remove(Context c, int id) {
    if (id < 0 || id >= 8) return;
    JSONObject t = read(c, id);
    cancelAlarm(c, id, t);
    store(c).edit().remove("t" + id).commit();
    NotificationManager n = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    if (n != null) n.cancel(600 + id);
  }

  public static synchronized void finish(Context c, int id, long token) {
    if (id < 0 || id >= 8) return;
    JSONObject t = read(c, id);
    if (t.optLong("wall") == 0 || t.optLong("token", -1) != token || remaining(c, t) > 0) return;
    try {
      t.put("wall", 0).put("elapsed", 0).put("paused", 0).put("finished", true);
      if (write(c, id, t)) TimerReceiver.notifyNamed(c, 600 + id, t.optString("name"));
    } catch (JSONException ignored) {
    }
  }

  public static boolean active(Context c) {
    for (int i = 0; i < 8; i++) if (read(c, i).optLong("wall") > 0) return true;
    return false;
  }

  public static long nextRemaining(Context c) {
    long best = Long.MAX_VALUE;
    for (int i = 0; i < 8; i++) {
      JSONObject t = read(c, i);
      if (t.optLong("wall") > 0) best = Math.min(best, remaining(c, t));
    }
    return best == Long.MAX_VALUE ? 0 : best;
  }

  public static void check(Context c) {
    for (int i = 0; i < 8; i++) {
      JSONObject t = read(c, i);
      if (t.optLong("wall") > 0 && remaining(c, t) == 0) finish(c, i, t.optLong("token"));
    }
  }

  public static void restore(Context c) {
    for (int i = 0; i < 8; i++) schedule(c, i);
  }
}

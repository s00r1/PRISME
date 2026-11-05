package fr.dm100.prisme.qa;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.session.*;
import android.os.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.json.*;

/** Same-signature test harness. Original user preferences are restored in finally. */
public final class WatchValidation extends Instrumentation {
  int count;
  Context c;
  Map<String, Map<String, ?>> saved = new HashMap<>();

  void check(boolean b, String label) {
    count++;
    if (!b) throw new AssertionError(label);
  }

  Object call(String name, String method, Class<?>[] types, Object... args) throws Exception {
    return Class.forName("fr.dm100.prisme." + name, true, c.getClassLoader())
        .getMethod(method, types)
        .invoke(null, args);
  }

  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  void snapshot() {
    File dir = new File(c.getApplicationInfo().dataDir, "shared_prefs");
    File[] files = dir.listFiles();
    if (files != null)
      for (File f : files)
        if (f.getName().endsWith(".xml")) {
          String name = f.getName().substring(0, f.getName().length() - 4);
          saved.put(name, new HashMap<>(c.getSharedPreferences(name, 0).getAll()));
        }
  }

  void restore() {
    Set<String> names = new HashSet<>(saved.keySet());
    File[] files = new File(c.getApplicationInfo().dataDir, "shared_prefs").listFiles();
    if (files != null)
      for (File f : files)
        if (f.getName().endsWith(".xml"))
          names.add(f.getName().substring(0, f.getName().length() - 4));
    for (String name : names) {
      SharedPreferences.Editor e = c.getSharedPreferences(name, 0).edit().clear();
      Map<String, ?> values = saved.get(name);
      if (values != null)
        for (Map.Entry<String, ?> v : values.entrySet()) {
          Object o = v.getValue();
          String k = v.getKey();
          if (o instanceof String) e.putString(k, (String) o);
          else if (o instanceof Boolean) e.putBoolean(k, (Boolean) o);
          else if (o instanceof Integer) e.putInt(k, (Integer) o);
          else if (o instanceof Long) e.putLong(k, (Long) o);
          else if (o instanceof Float) e.putFloat(k, (Float) o);
          else if (o instanceof Set) e.putStringSet(k, new HashSet<>((Set<String>) o));
        }
      if (!e.commit()) throw new AssertionError("restoration persistence");
    }
  }

  @Override
  public void onStart() {
    c = getTargetContext();
    Bundle result = new Bundle();
    MediaSession session = null;
    int timer = -1;
    try {
      snapshot();
      JSONObject defaults = (JSONObject) call("HomeLayout", "defaults", new Class[] {});
      check(defaults.getJSONArray("pages").length() == 3, "home defaults");
      Class<?> prefsType =
          Class.forName("fr.dm100.prisme.DashboardPrefs", true, c.getClassLoader());
      Object prefs = prefsType.getConstructor(Context.class).newInstance(c);
      JSONObject profile = (JSONObject) prefsType.getMethod("exportJSONObject").invoke(prefs);
      check(profile.getInt("schemaVersion") == 7, "profile schema");
      String priorNote = profile.optString("notes");
      File legacyDir = new File(c.getExternalFilesDir(null), "sauvegardes");
      File[] legacy = legacyDir.listFiles((d, n) -> n.matches("PRISME-[0-9]{8}-[0-9]{6}\\.json"));
      if (legacy != null && legacy.length > 0) {
        Arrays.sort(legacy, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
        if (legacy[0].length() <= 65536) {
          ByteArrayOutputStream bytes = new ByteArrayOutputStream();
          try (InputStream in = new FileInputStream(legacy[0])) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
              if (bytes.size() + n > 65536) throw new IOException("legacy size");
              bytes.write(buf, 0, n);
            }
          }
          JSONObject oldProfile = new JSONObject(new String(bytes.toByteArray(), "UTF-8"));
          prefsType.getMethod("importJSONObject", JSONObject.class).invoke(prefs, oldProfile);
          JSONObject migrated = (JSONObject) prefsType.getMethod("exportJSONObject").invoke(prefs);
          check(migrated.getInt("schemaVersion") == 7, "V5 schema migration");
          check(
              migrated.optString("notes").equals(oldProfile.optString("notes")),
              "V5 notes preserved");
          check(
              migrated
                  .getJSONArray("organizer")
                  .toString()
                  .equals(oldProfile.getJSONArray("organizer").toString()),
              "V5 tasks preserved");
          prefsType.getMethod("importJSONObject", JSONObject.class).invoke(prefs, profile);
          result.putString("migration", "real V5 export imported, notes/tasks preserved");
        }
      }
      JSONObject hostile = new JSONObject(profile.toString());
      hostile.getJSONObject("v6").getJSONObject("home").getJSONObject("layout").put("theme", 999);
      try {
        prefsType.getMethod("importJSONObject", JSONObject.class).invoke(prefs, hostile);
        throw new AssertionError("hostile accepted");
      } catch (InvocationTargetException expected) {
        check(expected.getCause() instanceof JSONException, "hostile rejected");
      }
      check(
          ((JSONObject) prefsType.getMethod("exportJSONObject").invoke(prefs))
              .optString("notes")
              .equals(priorNote),
          "notes preserved after invalid import");
      call("ProfileJournal", "begin", new Class[] {Context.class}, c);
      c.getSharedPreferences("prisme_music", 0).edit().putInt("style", 7).commit();
      check(
          (Boolean) call("ProfileJournal", "recover", new Class[] {Context.class}, c),
          "journal rollback");
      char[] pass = "PRISME QA temporary password".toCharArray();
      JSONObject vault =
          (JSONObject)
              call(
                  "BackupVault",
                  "seal",
                  new Class[] {JSONObject.class, char[].class},
                  profile,
                  pass);
      JSONObject unlocked =
          (JSONObject)
              call(
                  "BackupVault", "open", new Class[] {JSONObject.class, char[].class}, vault, pass);
      check(unlocked.optString("notes").equals(priorNote), "AES/GCM Android25 round trip");
      Arrays.fill(pass, '\0');
      timer =
          (Integer)
              call(
                  "MultiTimers",
                  "create",
                  new Class[] {Context.class, String.class, long.class},
                  c,
                  "PRISME QA",
                  1500L);
      check(
          (Boolean)
              call(
                  "MultiTimers",
                  "start",
                  new Class[] {Context.class, int.class, boolean.class},
                  c,
                  timer,
                  true),
          "AlarmManager scheduled");
      Thread.sleep(600);
      call("MultiTimers", "pause", new Class[] {Context.class, int.class}, c, timer);
      JSONObject paused =
          (JSONObject)
              call("MultiTimers", "read", new Class[] {Context.class, int.class}, c, timer);
      long left =
          (Long)
              call(
                  "MultiTimers",
                  "remaining",
                  new Class[] {Context.class, JSONObject.class},
                  c,
                  paused);
      check(left > 0 && left < 1500, "paused remaining");
      check(
          (Boolean)
              call(
                  "MultiTimers",
                  "start",
                  new Class[] {Context.class, int.class, boolean.class},
                  c,
                  timer,
                  false),
          "subsecond resume");
      long waitStart = SystemClock.elapsedRealtime();
      JSONObject finished;
      do {
        Thread.sleep(500);
        finished =
            (JSONObject)
                call("MultiTimers", "read", new Class[] {Context.class, int.class}, c, timer);
      } while (!finished.optBoolean("finished")
          && SystemClock.elapsedRealtime() - waitStart < 20000);
      result.putLong("alarm_wait_ms", SystemClock.elapsedRealtime() - waitStart);
      check(finished.optBoolean("finished"), "alarm delivered and finished");
      check(
          !(Boolean) call("ExternalActions", "allowed", new Class[] {String.class}, "safeMode"),
          "unsafe external action blocked");
      session = new MediaSession(c, "PRISME_QA");
      final int[] commands = {0};
      session.setCallback(
          new MediaSession.Callback() {
            @Override
            public void onPlay() {
              commands[0]++;
            }
          },
          new Handler(Looper.getMainLooper()));
      session.setMetadata(
          new MediaMetadata.Builder()
              .putString(MediaMetadata.METADATA_KEY_TITLE, "PRISME QA audio")
              .putString(MediaMetadata.METADATA_KEY_ARTIST, "Local test")
              .build());
      session.setPlaybackState(
          new PlaybackState.Builder()
              .setActions(
                  PlaybackState.ACTION_PLAY
                      | PlaybackState.ACTION_PAUSE
                      | PlaybackState.ACTION_SKIP_TO_NEXT)
              .setState(PlaybackState.STATE_PAUSED, 0, 1)
              .build());
      session.setActive(true);
      c.getSharedPreferences("prisme_media_access", 0)
          .edit()
          .putBoolean("enabled", true)
          .putString("player", c.getPackageName())
          .commit();
      Thread.sleep(700);
      java.util.List<?> sessions =
          (java.util.List<?>) call("MediaAccess", "sessions", new Class[] {Context.class}, c);
      if (!sessions.isEmpty()) {
        check(
            (Boolean) call("MediaAccess", "command", new Class[] {Context.class, int.class}, c, 3),
            "media play dispatched");
        Thread.sleep(500);
        check(commands[0] > 0, "media callback reached");
        result.putString("media", "authorized session commands verified");
      } else result.putString("media", "session access not granted; rich mode not verified");
      result.putString("result", "PASS");
      result.putInt("assertions", count);
    } catch (Throwable e) {
      result.putString("result", "FAIL");
      result.putString("failure", e.getClass().getSimpleName() + ": " + e.getMessage());
      result.putInt("assertions", count);
    } finally {
      if (session != null) session.release();
      try {
        if (timer >= 0)
          call("MultiTimers", "remove", new Class[] {Context.class, int.class}, c, timer);
        restore();
        call("MultiTimers", "restore", new Class[] {Context.class}, c);
        call("TimerEngine", "schedule", new Class[] {Context.class}, c);
        result.putString("preferences", "restored");
      } catch (Throwable restoreError) {
        result.putString("preferences", "RESTORATION FAILED");
        result.putString("result", "FAIL");
      }
    }
    finish(Activity.RESULT_OK, result);
  }
}

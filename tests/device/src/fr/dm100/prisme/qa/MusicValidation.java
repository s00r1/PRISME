package fr.dm100.prisme.qa;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.SystemClock;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controlled music metadata regression checks. No audio focus, transport command, notification
 * content or system permission change. The user's media preference store is restored in finally.
 */
public final class MusicValidation extends Instrumentation {
  private Context context;
  private int assertions;
  private Class<?> playbackType, accessType;
  private Method fromController;
  private Map<String, ?> saved;

  @Override
  public void onCreate(Bundle arguments) {
    super.onCreate(arguments);
    start();
  }

  private void check(boolean success, String label) {
    assertions++;
    if (!success) throw new AssertionError(label);
  }

  private Object access(String method) throws Exception {
    return accessType.getMethod(method, Context.class).invoke(null, context);
  }

  private Object state() throws Exception {
    return playbackType.getMethod("state", Context.class).invoke(null, context);
  }

  private Object fixtureState(MediaController controller) throws Exception {
    return fromController.invoke(null, context, controller);
  }

  private static Object field(Object state, String key) throws Exception {
    Field field = state.getClass().getField(key);
    return field.get(state);
  }

  private static boolean flag(Object state, String key) throws Exception {
    return (Boolean) field(state, key);
  }

  private static String text(Object state, String key) throws Exception {
    return (String) field(state, key);
  }

  private static Bitmap artwork(Object state) throws Exception {
    return (Bitmap) field(state, "artwork");
  }

  private interface Condition {
    boolean accepts(Object state) throws Exception;
  }

  private Object waitFor(MediaController fixture, Condition condition, String label)
      throws Exception {
    long start = SystemClock.elapsedRealtime();
    Object value;
    do {
      value = fixture == null ? state() : fixtureState(fixture);
      if (condition.accepts(value)) return value;
      Thread.sleep(75);
    } while (SystemClock.elapsedRealtime() - start < 6500);
    throw new AssertionError(label + " timed out");
  }

  private static Bitmap cover(int color) {
    Bitmap bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(color);
    return bitmap;
  }

  private static boolean colorMatches(Object state, int color) throws Exception {
    Bitmap bitmap = artwork(state);
    return bitmap != null
        && !bitmap.isRecycled()
        && bitmap.getPixel(bitmap.getWidth() / 2, bitmap.getHeight() / 2) == color;
  }

  private void enableFixture() {
    context
        .getSharedPreferences("prisme_media_access", 0)
        .edit()
        .putBoolean("enabled", true)
        .putString("player", context.getPackageName())
        .commit();
  }

  private void restore() {
    SharedPreferences.Editor editor =
        context.getSharedPreferences("prisme_media_access", 0).edit().clear();
    for (Map.Entry<String, ?> entry : saved.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof String) editor.putString(key, (String) value);
      else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
      else if (value instanceof Integer) editor.putInt(key, (Integer) value);
      else if (value instanceof Long) editor.putLong(key, (Long) value);
      else if (value instanceof Float) editor.putFloat(key, (Float) value);
      else if (value instanceof Set)
        editor.putStringSet(key, new HashSet<String>((Set<String>) value));
    }
    check(editor.commit(), "original music preferences persisted");
  }

  @Override
  public void onStart() {
    context = getTargetContext();
    Bundle report = new Bundle();
    MediaSession session = null;
    try {
      saved = new HashMap<>(context.getSharedPreferences("prisme_media_access", 0).getAll());
      playbackType = Class.forName("fr.dm100.prisme.MusicPlayback", true, context.getClassLoader());
      accessType = Class.forName("fr.dm100.prisme.MediaAccess", true, context.getClassLoader());
      fromController =
          playbackType.getDeclaredMethod("fromController", Context.class, MediaController.class);
      fromController.setAccessible(true);

      Method safeUri =
          Class.forName("fr.dm100.prisme.MediaArtwork", true, context.getClassLoader())
              .getDeclaredMethod("safeUri", String.class);
      safeUri.setAccessible(true);
      for (String uri :
          new String[] {
            "content://media/external/audio/albumart/1",
            "android.resource://fr.dm100.prisme/drawable/ic_prisme"
          }) check(safeUri.invoke(null, uri) != null, "local artwork URI accepted");
      for (String uri :
          new String[] {
            "https://invalid.example/cover.png",
            "http://invalid.example/cover.png",
            "file:///sdcard/private.png",
            "data:image/png;base64,AA==",
            "javascript:alert(1)",
            "content:///missing-authority",
            ""
          }) check(safeUri.invoke(null, uri) == null, "non-local or invalid artwork URI refused");
      check(safeUri.invoke(null, (Object) null) == null, "null artwork URI refused");
      char[] excessive = new char[2049];
      java.util.Arrays.fill(excessive, 'x');
      check(
          safeUri.invoke(null, "content://media/" + new String(excessive)) == null,
          "excessive artwork URI refused");

      context
          .getSharedPreferences("prisme_media_access", 0)
          .edit()
          .putBoolean("enabled", false)
          .commit();
      check(!(Boolean) access("enabled"), "enriched access opt-in disabled");
      check(((List<?>) access("sessions")).isEmpty(), "disabled opt-in exposes no sessions");
      check((Integer) access("accessStatus") == 0, "disabled access has explicit status");
      Object disabled = state();
      check(!flag(disabled, "hasSession"), "disabled mode has no enriched session");
      check(artwork(disabled) == null, "disabled mode exposes no artwork");

      enableFixture();
      boolean granted = (Boolean) access("granted");
      check((Boolean) access("enabled"), "fixture opt-in recorded locally");
      if (!granted) {
        check((Integer) access("accessStatus") == 1, "missing Android permission is explicit");
        check(((List<?>) access("sessions")).isEmpty(), "missing permission exposes no sessions");
        Object missing = state();
        check(!flag(missing, "accessGranted"), "missing permission represented in state");
        check(!flag(missing, "hasSession"), "missing permission cannot expose another player");
        check(artwork(missing) == null, "missing permission exposes no artwork");
        report.putString(
            "manager", "SKIPPED: Android media access not granted; parser checks still executed");
      } else {
        check((Integer) access("accessStatus") == 2, "granted Android permission is explicit");
        report.putString("manager", "Android access available; selected-session checks included");
      }

      // Paused and without media-button flags: never requests focus or controls the user's player.
      session = new MediaSession(context, "PRISME_QA_METADATA_NOT_PLAYING");
      session.setFlags(0);
      session.setPlaybackState(
          new PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED, 0, 0).build());
      session.setActive(true);
      MediaController controller = new MediaController(context, session.getSessionToken());

      session.setMetadata(
          new MediaMetadata.Builder()
              .putString(MediaMetadata.METADATA_KEY_TITLE, "QA Song Alpha")
              .putString(MediaMetadata.METADATA_KEY_ARTIST, "QA Artist")
              .putString(MediaMetadata.METADATA_KEY_ALBUM, "QA Album")
              .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, cover(Color.RED))
              .build());
      Object alpha = waitFor(controller, s -> colorMatches(s, Color.RED), "DISPLAY_ICON artwork");
      check(text(alpha, "title").equals("QA Song Alpha"), "TITLE extracted");
      check(text(alpha, "artist").equals("QA Artist"), "ARTIST extracted");
      check(text(alpha, "album").equals("QA Album"), "ALBUM extracted");
      check(flag(alpha, "hasSession"), "own controller can be read without manager permission");
      check(!flag(alpha, "playing"), "paused fixture remains paused");
      check(flag(alpha, "accessEnabled"), "state records local opt-in");
      check(colorMatches(alpha, Color.RED), "DISPLAY_ICON bitmap obtained");
      check(
          artwork(alpha).getWidth() <= 256 && artwork(alpha).getHeight() <= 256,
          "artwork dimensions bounded for watch memory");
      check(!flag(alpha, "artworkLoading"), "decoded artwork not left loading");

      session.setMetadata(
          new MediaMetadata.Builder()
              .putString(MediaMetadata.METADATA_KEY_TITLE, " \t ")
              .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, "QA Display Title")
              .putString(MediaMetadata.METADATA_KEY_ARTIST, "   ")
              .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, "QA Album Artist")
              .putBitmap(MediaMetadata.METADATA_KEY_ART, cover(Color.BLUE))
              .build());
      Object blue = waitFor(controller, s -> colorMatches(s, Color.BLUE), "ART artwork");
      check(text(blue, "title").equals("QA Display Title"), "blank TITLE uses DISPLAY_TITLE");
      check(text(blue, "artist").equals("QA Album Artist"), "blank ARTIST uses ALBUM_ARTIST");
      check(colorMatches(blue, Color.BLUE), "ART bitmap obtained after track change");
      check(!colorMatches(blue, Color.RED), "previous track artwork replaced");

      session.setMetadata(
          new MediaMetadata.Builder()
              .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, "QA فحص")
              .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, "QA Subtitle Artist")
              .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover(Color.GREEN))
              .build());
      Object green = waitFor(controller, s -> colorMatches(s, Color.GREEN), "ALBUM_ART artwork");
      check(text(green, "title").equals("QA فحص"), "Unicode DISPLAY_TITLE retained");
      check(
          text(green, "artist").equals("QA Subtitle Artist"),
          "missing artist uses DISPLAY_SUBTITLE");
      check(colorMatches(green, Color.GREEN), "ALBUM_ART bitmap obtained");

      session.setMetadata(
          new MediaMetadata.Builder()
              .putString(MediaMetadata.METADATA_KEY_TITLE, "QA Track Without Cover")
              .build());
      Object empty =
          waitFor(
              controller,
              s -> text(s, "title").equals("QA Track Without Cover"),
              "track without artwork");
      check(artwork(empty) == null, "new track never keeps previous artwork");
      check(text(empty, "artist").isEmpty(), "new track never keeps previous artist");
      check(text(empty, "album").isEmpty(), "new track never keeps previous album");

      session.setMetadata(
          new MediaMetadata.Builder()
              .putString(MediaMetadata.METADATA_KEY_TITLE, "QA No Remote Download")
              .putString(
                  MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
                  "https://invalid.example/prisme-qa-cover.png")
              .build());
      Object remote =
          waitFor(
              controller,
              s -> text(s, "title").equals("QA No Remote Download") && !flag(s, "artworkLoading"),
              "remote artwork URI refused");
      check(artwork(remote) == null, "remote URI not downloaded");

      session.setMetadata(new MediaMetadata.Builder().build());
      Object absent =
          waitFor(
              controller,
              s -> artwork(s) == null && !text(s, "title").equals("QA No Remote Download"),
              "empty metadata");
      check(!text(absent, "title").trim().isEmpty(), "empty metadata has honest fallback title");
      check(!text(absent, "title").startsWith("QA "), "empty metadata never keeps the old title");
      check(artwork(absent) == null, "empty metadata has no invented cover");

      if (granted) {
        session.setMetadata(
            new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, "QA Selected Session")
                .putBitmap(MediaMetadata.METADATA_KEY_ART, cover(Color.BLUE))
                .build());
        Object selected =
            waitFor(
                null,
                s -> text(s, "title").equals("QA Selected Session") && colorMatches(s, Color.BLUE),
                "selected session via real manager");
        check(
            text(selected, "packageName").equals(context.getPackageName()),
            "preferred player selected through real manager");
        check(flag(selected, "accessGranted"), "granted manager reflected in state");
        check(colorMatches(selected, Color.BLUE), "real-manager path supplies artwork");
      }

      context
          .getSharedPreferences("prisme_media_access", 0)
          .edit()
          .putBoolean("enabled", false)
          .commit();
      Object revoked = state();
      check(!flag(revoked, "hasSession"), "logical revoke removes enriched session immediately");
      check(!flag(revoked, "accessEnabled"), "logical revoke represented in state");
      check(artwork(revoked) == null, "logical revoke removes cached artwork immediately");
      check(((List<?>) access("sessions")).isEmpty(), "logical revoke prevents session lookup");
      check((Integer) access("accessStatus") == 0, "logical revoke restores disabled status");
      report.putString("result", "PASS");
      report.putString(
          "metadata", "controlled paused MediaSession, three bitmap keys and track changes");
    } catch (Throwable error) {
      report.putString("result", "FAIL");
      report.putString("failure", error.getClass().getSimpleName() + ": " + error.getMessage());
    } finally {
      if (session != null) session.release();
      try {
        if (saved != null) restore();
        report.putString("preferences", "restored");
        // Re-read the actual user selection after releasing the fixture; never send a command.
        if (playbackType != null) state();
      } catch (Throwable restoreError) {
        report.putString("result", "FAIL");
        report.putString("preferences", "RESTORATION FAILED");
      }
      report.putInt("assertions", assertions);
    }
    finish(Activity.RESULT_OK, report);
  }
}

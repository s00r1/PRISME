package fr.dm100.prisme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Bounded in-memory cover art. No HTTP, arbitrary file access, disk cache or image uploads. */
final class MediaArtwork {
  private static final int SIDE = 256, MAX_BYTES = 2 * 1024 * 1024;
  private static final Object LOCK = new Object();
  private static final ThreadPoolExecutor WORKER =
      new ThreadPoolExecutor(
          1,
          1,
          30,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<Runnable>(1),
          new ThreadPoolExecutor.DiscardOldestPolicy());
  private static String currentKey = "", source = "";
  private static Bitmap image;
  private static boolean loading;
  private static long generation;

  private MediaArtwork() {}

  static void clear() {
    synchronized (LOCK) {
      generation++;
      currentKey = "";
      source = "";
      image = null;
      loading = false;
      WORKER.getQueue().clear();
    }
  }

  static Bitmap get(String key) {
    synchronized (LOCK) {
      return currentKey.equals(key) ? image : null;
    }
  }

  static boolean loading(String key) {
    synchronized (LOCK) {
      return currentKey.equals(key) && loading;
    }
  }

  static void request(
      Context context,
      String key,
      MediaMetadata metadata,
      MediaDescription description,
      boolean changed) {
    Bitmap supplied = null;
    if (metadata != null) {
      for (String field :
          new String[] {
            MediaMetadata.METADATA_KEY_DISPLAY_ICON,
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_ALBUM_ART
          }) {
        try {
          Bitmap candidate = metadata.getBitmap(field);
          if (candidate != null && !candidate.isRecycled()) {
            supplied = candidate;
            break;
          }
        } catch (RuntimeException | OutOfMemoryError ignored) {
        }
      }
    }
    if (supplied == null && description != null) {
      try {
        supplied = description.getIconBitmap();
      } catch (RuntimeException ignored) {
      }
    }
    Uri uri = null;
    if (supplied == null && metadata != null) {
      for (String field :
          new String[] {
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI
          }) {
        try {
          Uri candidate = safeUri(metadata.getString(field));
          if (candidate != null) {
            uri = candidate;
            break;
          }
        } catch (RuntimeException ignored) {
        }
      }
    }
    if (supplied == null && uri == null && description != null) {
      try {
        uri =
            safeUri(description.getIconUri() == null ? null : description.getIconUri().toString());
      } catch (RuntimeException ignored) {
      }
    }
    String signature =
        supplied != null
            ? "bitmap:"
                + supplied.getWidth()
                + "x"
                + supplied.getHeight()
                + ":"
                + supplied.getGenerationId()
            : uri == null ? "none" : uri.toString();
    final long request;
    synchronized (LOCK) {
      boolean newTrack = !currentKey.equals(key);
      if (!newTrack && !changed && source.equals(signature)) return;
      generation++;
      currentKey = key;
      source = signature;
      if (newTrack || supplied == null && uri == null) image = null;
      loading = supplied != null || uri != null;
      request = generation;
      WORKER.getQueue().clear();
      if (!loading) return;
    }
    final Bitmap bitmap = supplied;
    final Uri location = uri;
    final Context app = context.getApplicationContext();
    WORKER.execute(
        () -> {
          synchronized (LOCK) {
            if (request != generation) return;
          }
          Bitmap result = null;
          try {
            if (bitmap != null) result = fit(bitmap);
            else if (location != null) result = decode(app, location);
          } catch (Exception | OutOfMemoryError ignored) {
          }
          synchronized (LOCK) {
            if (request == generation && currentKey.equals(key)) {
              image = result;
              loading = false;
            }
          }
          // Never recycle provider/returned bitmaps: a view may still be drawing one.
        });
  }

  static Uri safeUri(String value) {
    if (value == null || value.length() > 2048) return null;
    Uri uri = Uri.parse(value);
    String scheme = uri.getScheme();
    if (!("content".equals(scheme) || "android.resource".equals(scheme))) return null;
    if (uri.getAuthority() == null || uri.getAuthority().isEmpty()) return null;
    return uri;
  }

  private static Bitmap fit(Bitmap bitmap) {
    if (bitmap == null || bitmap.isRecycled()) return null;
    int width = bitmap.getWidth(), height = bitmap.getHeight();
    if (width < 1 || height < 1 || width > 16384 || height > 16384) return null;
    float ratio = Math.min(1f, SIDE / (float) Math.max(width, height));
    Bitmap bounded =
        ratio == 1f
            ? bitmap
            : Bitmap.createScaledBitmap(
                bitmap,
                Math.max(1, Math.round(width * ratio)),
                Math.max(1, Math.round(height * ratio)),
                true);
    // Own a small immutable software copy, independent of the player's bitmap lifecycle/config.
    return bounded.copy(Bitmap.Config.ARGB_8888, false);
  }

  private static Bitmap decode(Context context, Uri uri) throws Exception {
    byte[] bytes;
    try (InputStream input = context.getContentResolver().openInputStream(uri)) {
      if (input == null) return null;
      ByteArrayOutputStream data = new ByteArrayOutputStream();
      byte[] buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) != -1) {
        if (data.size() + read > MAX_BYTES) return null;
        data.write(buffer, 0, read);
      }
      bytes = data.toByteArray();
    }
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    if (options.outWidth < 1
        || options.outHeight < 1
        || options.outWidth > 16384
        || options.outHeight > 16384) return null;
    int sample = 1;
    while (options.outWidth / sample > SIDE || options.outHeight / sample > SIDE) sample *= 2;
    options.inJustDecodeBounds = false;
    options.inSampleSize = sample;
    options.inPreferredConfig = Bitmap.Config.RGB_565;
    return fit(BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options));
  }
}

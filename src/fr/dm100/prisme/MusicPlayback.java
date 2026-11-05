package fr.dm100.prisme;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;

/** Native music commands and bounded, session-scoped metadata/artwork snapshots. */
public final class MusicPlayback {
  public static final int TOGGLE = 0, PREVIOUS = 1, NEXT = 2, PLAY = 3;
  private static final Object LOCK = new Object();
  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static Observer observed;

  private MusicPlayback() {}

  public static final class State {
    public final String title, artist, album, packageName;
    public final boolean playing,
        connected,
        hasSession,
        accessEnabled,
        accessGranted,
        artworkLoading;

    /** Shared immutable presentation bitmap. Views must never recycle it. */
    public final Bitmap artwork;

    private State(
        String title,
        String artist,
        String album,
        String pkg,
        boolean playing,
        boolean connected,
        boolean session,
        boolean enabled,
        boolean granted,
        Bitmap art,
        boolean loading) {
      this.title = title;
      this.artist = artist;
      this.album = album;
      packageName = pkg;
      this.playing = playing;
      this.connected = connected;
      hasSession = session;
      accessEnabled = enabled;
      accessGranted = granted;
      artwork = art;
      artworkLoading = loading;
    }
  }

  private static final class Details {
    final String title, artist, album, key;

    Details(String title, String artist, String album, String key) {
      this.title = title;
      this.artist = artist;
      this.album = album;
      this.key = key;
    }
  }

  /** No raw MediaMetadata/large provider bitmap is retained in this observer. */
  private static final class Observer {
    final Context context;
    final MediaController controller;
    final MediaSession.Token token;
    final String pkg;
    final MediaController.Callback callback;
    Details details;
    PlaybackState playback;
    long lastRead;

    Observer(Context c, MediaController controller) {
      context = c.getApplicationContext();
      this.controller = controller;
      token = controller.getSessionToken();
      pkg = controller.getPackageName();
      details = new Details("Titre non fourni par le lecteur", "", "", pkg + "#empty");
      callback =
          new MediaController.Callback() {
            @Override
            public void onMetadataChanged(MediaMetadata metadata) {
              synchronized (LOCK) {
                if (observed == Observer.this) {
                  if (!permitted()) clearLocked();
                  else update(metadata, true);
                }
              }
            }

            @Override
            public void onPlaybackStateChanged(PlaybackState value) {
              synchronized (LOCK) {
                if (observed == Observer.this) {
                  if (!permitted()) clearLocked();
                  else playback = value;
                }
              }
            }

            @Override
            public void onSessionDestroyed() {
              synchronized (LOCK) {
                if (observed == Observer.this) clearLocked();
              }
            }
          };
    }

    private boolean permitted() {
      // A controller for this app's own session requires no notification permission.
      return pkg.equals(context.getPackageName())
          || MediaAccess.accessStatus(context) == MediaAccess.ACCESS_READY;
    }

    void refresh() {
      // Callbacks give immediate changes; periodic reconciliation covers old OEM/player bugs.
      lastRead = SystemClock.elapsedRealtime();
      try {
        playback = controller.getPlaybackState();
      } catch (RuntimeException ignored) {
        playback = null;
      }
      try {
        update(controller.getMetadata(), false);
      } catch (RuntimeException | OutOfMemoryError ignored) {
        update(null, false);
      }
    }

    void update(MediaMetadata metadata, boolean changed) {
      lastRead = SystemClock.elapsedRealtime();
      MediaDescription description = null;
      try {
        if (metadata != null) description = metadata.getDescription();
      } catch (RuntimeException ignored) {
      }
      String title =
          first(
              text(metadata, MediaMetadata.METADATA_KEY_TITLE),
              text(metadata, MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
              description == null ? "" : clean(description.getTitle()));
      String artist =
          first(
              text(metadata, MediaMetadata.METADATA_KEY_ARTIST),
              text(metadata, MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
              text(metadata, MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),
              description == null ? "" : clean(description.getSubtitle()));
      String album =
          first(
              text(metadata, MediaMetadata.METADATA_KEY_ALBUM),
              text(metadata, MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION),
              description == null ? "" : clean(description.getDescription()));
      String id =
          first(
              text(metadata, MediaMetadata.METADATA_KEY_MEDIA_ID),
              description == null ? "" : clean(description.getMediaId()));
      String key =
          pkg + "#" + token.hashCode() + "|" + part(id) + part(title) + part(artist) + part(album);
      details =
          new Details(
              title.isEmpty() ? "Titre non fourni par le lecteur" : title, artist, album, key);
      MediaArtwork.request(context, key, metadata, description, changed);
    }
  }

  static String clean(CharSequence value) {
    if (value == null) return "";
    String input = value.toString().trim();
    StringBuilder result = new StringBuilder(Math.min(512, input.length()));
    for (int i = 0; i < input.length() && result.length() < 512; i++) {
      char ch = input.charAt(i);
      if (Character.isISOControl(ch)) {
        if (ch == '\n' || ch == '\r' || ch == '\t') result.append(' ');
      } else result.append(ch);
    }
    return result.toString().trim();
  }

  private static String text(MediaMetadata metadata, String key) {
    try {
      return metadata == null ? "" : clean(metadata.getText(key));
    } catch (RuntimeException ignored) {
      return "";
    }
  }

  private static String first(String... values) {
    for (String value : values) if (value != null && !value.isEmpty()) return value;
    return "";
  }

  private static String part(String value) {
    return value.length() + ":" + value;
  }

  private static void clearLocked() {
    if (observed != null) {
      try {
        observed.controller.unregisterCallback(observed.callback);
      } catch (RuntimeException ignored) {
      }
      observed = null;
    }
    MediaArtwork.clear();
  }

  static void invalidate() {
    synchronized (LOCK) {
      clearLocked();
    }
  }

  public static State state(Context context) {
    MediaController controller = MediaAccess.current(context);
    if (controller != null) {
      try {
        return fromController(context, controller);
      } catch (RuntimeException ignored) {
      }
    }
    invalidate();
    AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    boolean active = false;
    try {
      active = audio != null && audio.isMusicActive();
    } catch (RuntimeException ignored) {
    }
    return new State(
        active ? "Audio actif" : "Aucune musique détectée",
        "",
        "",
        "",
        active,
        false,
        false,
        MediaAccess.enabled(context),
        MediaAccess.granted(context),
        null,
        false);
  }

  /** Normal session path, also usable with a controller for an app's own MediaSession. */
  static State fromController(Context context, MediaController controller) {
    synchronized (LOCK) {
      MediaSession.Token token = controller.getSessionToken();
      if (observed == null || !observed.token.equals(token)) {
        clearLocked();
        Observer next = new Observer(context, controller);
        observed = next;
        try {
          controller.registerCallback(next.callback, MAIN);
        } catch (RuntimeException ignored) {
        }
        next.refresh();
      } else if (SystemClock.elapsedRealtime() - observed.lastRead >= 5000) observed.refresh();
      Details details = observed.details;
      PlaybackState playback = observed.playback;
      return new State(
          details.title,
          details.artist,
          details.album,
          observed.pkg,
          playback != null && playback.getState() == PlaybackState.STATE_PLAYING,
          true,
          true,
          MediaAccess.enabled(context),
          MediaAccess.granted(context),
          MediaArtwork.get(details.key),
          MediaArtwork.loading(details.key));
    }
  }

  public static void command(Context c, int command) {
    if (MediaAccess.command(c, command)) return;
    AudioManager audio = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
    if (audio == null) return;
    int key =
        command == PREVIOUS
            ? KeyEvent.KEYCODE_MEDIA_PREVIOUS
            : command == NEXT
                ? KeyEvent.KEYCODE_MEDIA_NEXT
                : command == PLAY ? KeyEvent.KEYCODE_MEDIA_PLAY : KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
    try {
      long now = SystemClock.uptimeMillis();
      audio.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, key, 0));
      audio.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, key, 0));
    } catch (RuntimeException ignored) {
    }
  }

  /** The real player, Android and firmware own background playback and audio focus. */
  public static void prepareHomeTransition(Context c) {}
}

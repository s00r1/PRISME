package fr.dm100.prisme;

import android.content.ComponentName;
import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Optional media sessions only. Notification messages are never inspected or persisted. */
public final class MediaAccess extends NotificationListenerService {
  public static final int ACCESS_DISABLED = 0;
  public static final int ACCESS_PERMISSION_REQUIRED = 1;
  public static final int ACCESS_READY = 2;

  public static boolean enabled(Context c) {
    return PrefValues.flag(c.getSharedPreferences("prisme_media_access", 0), "enabled", false);
  }

  /** Android's grant and PRISME's opt-in are independent, device-local choices. */
  public static boolean granted(Context c) {
    try {
      String allowed =
          Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
      if (allowed == null || allowed.isEmpty()) return false;
      ComponentName ours = new ComponentName(c, MediaAccess.class);
      for (String component : allowed.split(":"))
        if (ours.equals(ComponentName.unflattenFromString(component))) return true;
    } catch (RuntimeException ignored) {
    }
    return false;
  }

  public static int accessStatus(Context c) {
    if (!enabled(c)) return ACCESS_DISABLED;
    return granted(c) ? ACCESS_READY : ACCESS_PERMISSION_REQUIRED;
  }

  public static List<MediaController> sessions(Context c) {
    if (accessStatus(c) != ACCESS_READY) return Collections.emptyList();
    try {
      MediaSessionManager manager =
          (MediaSessionManager) c.getSystemService(Context.MEDIA_SESSION_SERVICE);
      if (manager == null) return Collections.emptyList();
      List<MediaController> active =
          manager.getActiveSessions(new ComponentName(c, MediaAccess.class));
      if (active == null || active.isEmpty()) return Collections.emptyList();
      List<MediaController> result = new ArrayList<>();
      for (MediaController controller : active) if (controller != null) result.add(controller);
      return result;
    } catch (RuntimeException ignored) {
      return Collections.emptyList();
    }
  }

  public static MediaController current(Context c) {
    List<MediaController> list = sessions(c);
    String wanted = PrefValues.text(c.getSharedPreferences("prisme_media_access", 0), "player", "");
    MediaController fallback = null, buffering = null;
    for (MediaController controller : list) {
      try {
        if (!wanted.isEmpty() && wanted.equals(controller.getPackageName())) return controller;
      } catch (RuntimeException ignored) {
      }
    }
    for (MediaController controller : list) {
      try {
        PlaybackState state = controller.getPlaybackState();
        if (state != null && state.getState() == PlaybackState.STATE_PLAYING) return controller;
        if (state != null
            && (state.getState() == PlaybackState.STATE_BUFFERING
                || state.getState() == PlaybackState.STATE_CONNECTING)) buffering = controller;
        if (fallback == null) fallback = controller;
      } catch (RuntimeException ignored) {
      }
    }
    return buffering != null ? buffering : fallback;
  }

  public static boolean command(Context c, int command) {
    MediaController controller = current(c);
    if (controller == null) return false;
    try {
      PlaybackState state = controller.getPlaybackState();
      long actions = state == null ? 0 : state.getActions();
      MediaController.TransportControls controls = controller.getTransportControls();
      if (command == 1 && (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0)
        controls.skipToPrevious();
      else if (command == 2 && (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0)
        controls.skipToNext();
      else if (command == 3
          && (actions & (PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE)) != 0)
        controls.play();
      else if (command == 0) {
        if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
          if ((actions & (PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE)) == 0)
            return false;
          controls.pause();
        } else {
          if ((actions & (PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE)) == 0)
            return false;
          controls.play();
        }
      } else return false;
      return true;
    } catch (RuntimeException ignored) {
      return false;
    }
  }

  @Override
  public void onListenerConnected() {
    super.onListenerConnected();
    MusicPlayback.invalidate();
  }

  @Override
  public void onListenerDisconnected() {
    MusicPlayback.invalidate();
    super.onListenerDisconnected();
  }

  @Override
  public void onDestroy() {
    MusicPlayback.invalidate();
    super.onDestroy();
  }
}

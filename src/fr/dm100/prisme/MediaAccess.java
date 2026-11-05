package fr.dm100.prisme;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.session.*;
import android.os.*;
import android.service.notification.NotificationListenerService;
import java.util.*;

/** Optional media access. Notification bodies are never read, logged or persisted. */
public final class MediaAccess extends NotificationListenerService {
  static MediaController selected;

  public static List<MediaController> sessions(Context c) {
    if (!PrefValues.flag(c.getSharedPreferences("prisme_media_access", 0), "enabled", false))
      return Collections.emptyList();
    try {
      MediaSessionManager m =
          (MediaSessionManager) c.getSystemService(Context.MEDIA_SESSION_SERVICE);
      return m == null
          ? Collections.emptyList()
          : m.getActiveSessions(new ComponentName(c, MediaAccess.class));
    } catch (RuntimeException e) {
      return Collections.emptyList();
    }
  }

  public static MediaController current(Context c) {
    List<MediaController> list = sessions(c);
    String wanted = PrefValues.text(c.getSharedPreferences("prisme_media_access", 0), "player", "");
    for (MediaController m : list) if (m.getPackageName().equals(wanted)) return m;
    for (MediaController m : list) {
      PlaybackState s = m.getPlaybackState();
      if (s != null && s.getState() == PlaybackState.STATE_PLAYING) return m;
    }
    return list.isEmpty() ? null : list.get(0);
  }

  public static boolean command(Context c, int cmd) {
    MediaController m = current(c);
    if (m == null) return false;
    PlaybackState s = m.getPlaybackState();
    long actions = s == null ? 0 : s.getActions();
    MediaController.TransportControls t = m.getTransportControls();
    try {
      if (cmd == 1 && (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0) t.skipToPrevious();
      else if (cmd == 2 && (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) t.skipToNext();
      else if (cmd == 3 && (actions & PlaybackState.ACTION_PLAY) != 0) t.play();
      else if (cmd == 0) {
        if (s != null && s.getState() == PlaybackState.STATE_PLAYING) {
          if ((actions & (PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE)) == 0)
            return false;
          t.pause();
        } else {
          if ((actions & (PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE)) == 0)
            return false;
          t.play();
        }
      } else return false;
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }
}

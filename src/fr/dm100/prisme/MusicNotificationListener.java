package fr.dm100.prisme;

import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import java.util.ArrayList;
import java.util.List;

/**
 * Explicit user-granted notification access exposes active media sessions.
 * A narrowly armed, one-shot resume can counter a pause caused by switching home.
 * This does not override DRM, background-video policies, or a killed player.
 */
public final class MusicNotificationListener extends NotificationListenerService {
    private static volatile MusicNotificationListener instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final List<MediaController> tracked=new ArrayList<>();
    private MediaSessionManager manager;
    private String recentlyPlayingPackage="";
    private long recentlyPlayingAt;
    private long handoffUntil;
    private String handoffPackage="";
    private boolean pending;
    private final Runnable recover=new Runnable(){@Override public void run(){
        pending=false;
        if(SystemClock.elapsedRealtime()>handoffUntil||handoffPackage.isEmpty())return;
        if(!new MusicPrefs(MusicNotificationListener.this).flag("preserve",true))return;
        MediaController target=find(handoffPackage);
        if(target==null)return; // The other application has stopped entirely: cannot resurrect it.
        PlaybackState state=target.getPlaybackState();
        if(state==null)return;
        int s=state.getState();
        if(s==PlaybackState.STATE_PAUSED){
            try{target.getTransportControls().play();}catch(RuntimeException ignored){}
        }
    }};
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsChanged=controllers->bindSessions(controllers);
    private final MediaController.Callback callback=new MediaController.Callback(){
        @Override public void onPlaybackStateChanged(PlaybackState state){
            if(state!=null&&state.getState()==PlaybackState.STATE_PLAYING)capturePlaying();
        }
    };
    @Override public void onListenerConnected(){
        super.onListenerConnected();instance=this;
        try{
            manager=(MediaSessionManager)getSystemService(Context.MEDIA_SESSION_SERVICE);
            if(manager!=null){
                manager.addOnActiveSessionsChangedListener(sessionsChanged,
                        new android.content.ComponentName(this,MusicNotificationListener.class),handler);
                bindSessions(manager.getActiveSessions(new android.content.ComponentName(this,MusicNotificationListener.class)));
            }
        }catch(RuntimeException ignored){}
    }
    @Override public void onListenerDisconnected(){
        handler.removeCallbacks(recover);
        if(manager!=null)try{manager.removeOnActiveSessionsChangedListener(sessionsChanged);}catch(RuntimeException ignored){}
        bindSessions(null);if(instance==this)instance=null;super.onListenerDisconnected();
    }
    @Override public void onDestroy(){
        handler.removeCallbacks(recover);
        if(manager!=null)try{manager.removeOnActiveSessionsChangedListener(sessionsChanged);}catch(RuntimeException ignored){}
        bindSessions(null);if(instance==this)instance=null;super.onDestroy();
    }
    private void bindSessions(List<MediaController> controllers){
        for(MediaController ctrl:tracked)try{ctrl.unregisterCallback(callback);}catch(RuntimeException ignored){}
        tracked.clear();
        if(controllers!=null)for(MediaController ctrl:controllers){
            if(ctrl==null)continue;
            tracked.add(ctrl);
            try{ctrl.registerCallback(callback,handler);}catch(RuntimeException ignored){}
        }
        capturePlaying();
    }
    private MediaController find(String pkg){
        for(MediaController ctrl:tracked)if(pkg.equals(ctrl.getPackageName()))return ctrl;
        return null;
    }
    private void capturePlaying(){
        for(MediaController ctrl:tracked){
            PlaybackState state=ctrl.getPlaybackState();
            if(state!=null&&state.getState()==PlaybackState.STATE_PLAYING){
                recentlyPlayingPackage=ctrl.getPackageName();recentlyPlayingAt=SystemClock.elapsedRealtime();return;
            }
        }
    }
    public static void prepareHomeTransition(Context context){
        MusicNotificationListener listener=instance;
        if(listener==null)return;
        listener.arm();
    }
    private void arm(){
        long now=SystemClock.elapsedRealtime();
        capturePlaying();
        if(recentlyPlayingPackage.isEmpty()||now-recentlyPlayingAt>4000)return;
        handoffUntil=now+3500;
        handoffPackage=recentlyPlayingPackage;
        handler.removeCallbacks(recover);pending=true;
        handler.postDelayed(recover,1100);
    }
}

package fr.dm100.prisme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.SystemClock;
import android.view.KeyEvent;
import java.util.List;

/** Reads/commands the actual Android media session when notification access is granted. */
public final class MusicPlayback {
    public static final int TOGGLE=0,PREVIOUS=1,NEXT=2,PLAY=3;
    private MusicPlayback(){}
    public static final class State {
        public final String title,artist,packageName;
        public final boolean playing,connected,hasSession;
        State(String title,String artist,String pkg,boolean playing,boolean connected,boolean hasSession){
            this.title=title;this.artist=artist;this.packageName=pkg;this.playing=playing;
            this.connected=connected;this.hasSession=hasSession;
        }
    }
    static List<MediaController> sessions(Context context){
        try{
            MediaSessionManager m=(MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if(m==null)return null;
            ComponentName component=new ComponentName(context,MusicNotificationListener.class);
            return m.getActiveSessions(component);
        }catch(RuntimeException e){return null;}
    }
    static MediaController preferred(Context c){
        List<MediaController> list=sessions(c);
        if(list==null)return null;
        MediaController paused=null,any=null;
        for(MediaController ctrl:list){
            if(ctrl==null)continue;
            PlaybackState state=ctrl.getPlaybackState();
            if(state!=null&&state.getState()==PlaybackState.STATE_PLAYING)return ctrl;
            if(paused==null&&state!=null&&(state.getState()==PlaybackState.STATE_PAUSED||
                    state.getState()==PlaybackState.STATE_BUFFERING))paused=ctrl;
            if(any==null)any=ctrl;
        }
        return paused!=null?paused:any;
    }
    public static State state(Context c){
        List<MediaController> list=sessions(c);
        MediaController ctrl=preferred(c);
        if(ctrl!=null){
            PlaybackState playback=ctrl.getPlaybackState();
            boolean playing=playback!=null&&playback.getState()==PlaybackState.STATE_PLAYING;
            MediaMetadata metadata=ctrl.getMetadata();
            String title=metadata==null?null:metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            String artist=metadata==null?null:metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            if(title==null||title.trim().isEmpty())title="Lecture multimédia";
            if(artist==null)artist="";
            return new State(title,artist,ctrl.getPackageName(),playing,true,true);
        }
        AudioManager audio=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        boolean audible=audio!=null&&audio.isMusicActive();
        return new State(audible?"Lecture en cours":"Aucun lecteur détecté",audible?"Commandes média Android":"Autorise l’accès aux notifications pour les titres","",audible,list!=null,false);
    }
    public static void command(Context c,int command){
        MediaController ctrl=preferred(c);
        if(ctrl!=null){
            try{
                MediaController.TransportControls t=ctrl.getTransportControls();
                switch(command){
                    case PREVIOUS:t.skipToPrevious();break;
                    case NEXT:t.skipToNext();break;
                    case PLAY:t.play();break;
                    default:{
                        PlaybackState state=ctrl.getPlaybackState();
                        if(state!=null&&state.getState()==PlaybackState.STATE_PLAYING)t.pause();
                        else t.play();
                    }
                }
                return;
            }catch(RuntimeException ignored){}
        }
        AudioManager audio=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        if(audio==null)return;
        int key=command==PREVIOUS?KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                command==NEXT?KeyEvent.KEYCODE_MEDIA_NEXT:
                command==PLAY?KeyEvent.KEYCODE_MEDIA_PLAY:KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
        try{
            long now=SystemClock.uptimeMillis();
            audio.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,key,0));
            audio.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_UP,key,0));
        }catch(RuntimeException ignored){}
    }
    /** Armed only during an explicitly requested home transition. Never restarts a killed app. */
    public static void prepareHomeTransition(Context c){
        if(new MusicPrefs(c).flag("preserve",true))MusicNotificationListener.prepareHomeTransition(c);
    }
}

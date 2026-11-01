package fr.dm100.prisme;

import android.content.Context;
import android.media.AudioManager;
import android.os.SystemClock;
import android.view.KeyEvent;

/**
 * Lightweight music controls without notification access.
 * Does not request accessibility, notifications, or other sensitive access.
 * Uses Android's native multimedia key dispatcher to target the current player.
 */
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

    public static State state(Context context) {
        AudioManager audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        boolean active=false;
        try{active=audio!=null&&audio.isMusicActive();}catch(RuntimeException ignored){}
        return new State(active?"Audio actif":"Aucune musique détectée",
                active?"Lecture Android · titre indisponible sans accès aux notifications":"",
                "",active,false,false);
    }

    public static void command(Context c,int command) {
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

    /**
     * PRISME does not stop its launched app or request audio focus during a
     * launcher transition. Android and the actual player own background playback.
     */
    public static void prepareHomeTransition(Context c) {
        // Intentionally no media session or notification access.
    }
}

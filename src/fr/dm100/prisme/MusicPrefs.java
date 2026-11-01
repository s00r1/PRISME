package fr.dm100.prisme;

import android.content.Context;
import android.content.SharedPreferences;

/** Independent, user-controlled PRISME dashboard player settings. */
public final class MusicPrefs {
    public static final String[] STYLES={"Verre iOS","Android Material","OLED noir","Néon cyan","Cyberpunk","Rétro pixels","Titane","Minimal clair","Or premium","Violet spatial","Radio classique","Compact transparent"};
    private final SharedPreferences p;
    public MusicPrefs(Context context){p=context.getApplicationContext().getSharedPreferences("prisme_music",Context.MODE_PRIVATE);}
    public boolean flag(String key,boolean fallback){try{return p.getBoolean(key,fallback);}catch(ClassCastException e){return fallback;}}
    public void putBoolean(String key,boolean value){p.edit().putBoolean(key,value).apply();}
    public int number(String key,int fallback){
        int value;try{value=p.getInt(key,fallback);}catch(ClassCastException ex){value=fallback;}
        int min=0,max=100;
        if("style".equals(key)){max=STYLES.length-1;}
        else if("size".equals(key)){min=50;max=180;}
        else if("opacity".equals(key)){min=30;max=100;}
        return Math.min(max,Math.max(min,value));
    }
    public void putInt(String key,int value){p.edit().putInt(key,value).apply();}
    public SharedPreferences store(){return p;}
}

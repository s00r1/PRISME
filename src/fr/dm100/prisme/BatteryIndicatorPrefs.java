package fr.dm100.prisme;

import android.content.Context;
import android.content.SharedPreferences;

/** Independent settings for the non-interactive, global battery indicator. */
public final class BatteryIndicatorPrefs {
    public static final String STORE="prisme_battery_overlay";
    public static final String[] STYLES={
        "Classique horizontal","Capsule iOS","Anneau","Arc panoramique",
        "Segments","Vertical","Pourcentage XXL","Digital",
        "Néon","Pixels rétro","Jauge mécanique","Micro-barre",
        "Terminal","Hexagone","Éclair","Onde",
        "Points lumineux","Radar","Bouclier","Matrix",
        "Cyber futuriste","OLED minimal","Ligne fine","Chronographe"
    };
    public static final String[] PALETTES={
        "Classique vert / orange / rouge","Glacier cyan / bleu","Cyberpunk rose / violet",
        "Néon électrique","Or et cuivre","Monochrome blanc",
        "Océan turquoise","Lavande","Tropical","Rouge sportif",
        "Pastel doux","Graphite premium"
    };
    /** critical, medium, healthy, full, charging */
    private static final int[][] COLORS={
        {0xffff4f63,0xffffb64f,0xff4de09d,0xff5af5b1,0xff72d6ff},
        {0xfff783a7,0xff98c9ff,0xff49dcf3,0xffb6faff,0xff7daaff},
        {0xffff4587,0xffb76eff,0xffe57dfc,0xffffacd6,0xff7af5ed},
        {0xffff425c,0xffffeb3b,0xff39ff9e,0xffb6ff4d,0xff54f8ff},
        {0xffc0503a,0xffd49b55,0xffffd67f,0xffffefa6,0xffffc36a},
        {0xff969696,0xffc5c5c5,0xffeeeeee,0xffffffff,0xffffffff},
        {0xfff16b7a,0xff54b6ba,0xff1ed6c3,0xffa5f7ed,0xff7fc6ff},
        {0xffe18fa9,0xffb49cfe,0xff9ac4ff,0xffded4ff,0xfff7baf7},
        {0xffff6e67,0xffffd166,0xff82dc7b,0xffd2ff9b,0xff79e7ec},
        {0xfffb355c,0xfffe6d4e,0xffffa55b,0xffffd4a5,0xffff6a78},
        {0xfff9a8b5,0xfff5cba0,0xffa1e6ce,0xffd4fae6,0xffb4dfff},
        {0xffa66560,0xffb3a68d,0xffa9c3ce,0xffeef3f8,0xffcfd7e3}
    };
    private final SharedPreferences store;
    public BatteryIndicatorPrefs(Context c){store=c.getApplicationContext().getSharedPreferences(STORE,Context.MODE_PRIVATE);}
    public SharedPreferences preferences(){return store;}
    public boolean bool(String key,boolean fallback){try{return store.getBoolean(key,fallback);}catch(ClassCastException error){return fallback;}}
    public void putBoolean(String key,boolean value){store.edit().putBoolean(key,value).apply();}
    public int integer(String key,int fallback){
        int value;try{value=store.getInt(key,fallback);}catch(ClassCastException error){value=fallback;}
        int min=0,max=100;
        if("style".equals(key)){max=STYLES.length-1;}
        else if("scope".equals(key)){max=2;}
        else if("palette".equals(key)){max=PALETTES.length-1;}
        else if("size".equals(key)){min=35;max=200;}
        else if("opacity".equals(key)){min=25;max=100;}
        else if("low".equals(key)){min=5;max=40;}
        else if("high".equals(key)){min=45;max=90;}
        else if("contrastMode".equals(key)){max=2;}
        return Math.max(min,Math.min(max,value));
    }
    public void putInt(String key,int value){
        if(!("style".equals(key)||"palette".equals(key)||"size".equals(key)||
                "opacity".equals(key)||"x".equals(key)||"y".equals(key)||"low".equals(key)||"high".equals(key)||"scope".equals(key)||"contrastMode".equals(key)))return;
        int clamped=Math.max(0,Math.min(200,value));
        if("style".equals(key))clamped=Math.min(STYLES.length-1,clamped);
        if("scope".equals(key))clamped=Math.min(2,clamped);
        if("contrastMode".equals(key))clamped=Math.min(2,clamped);
        if("palette".equals(key))clamped=Math.min(PALETTES.length-1,clamped);
        if("size".equals(key))clamped=Math.max(35,clamped);
        if("opacity".equals(key))clamped=Math.max(25,Math.min(100,clamped));
        if("x".equals(key)||"y".equals(key))clamped=Math.min(100,clamped);
        if("low".equals(key))clamped=Math.max(5,Math.min(40,clamped));
        if("high".equals(key))clamped=Math.max(45,Math.min(90,clamped));
        store.edit().putInt(key,clamped).apply();
    }
    /** 0 = hidden, 1 = PRISME dashboard only, 2 = all applications. */
    public int displayMode(){
        if(!bool("enabled",true))return 0;
        return integer("scope",2);
    }
    public void setDisplayMode(int value){
        int safe=Math.max(0,Math.min(2,value));
        store.edit().putBoolean("enabled",safe!=0).putInt("scope",safe).apply();
    }
    public boolean enabled(){return displayMode()!=0;}
    public int color(int level,boolean charging){
        int[] colors=COLORS[integer("palette",0)];
        if(level<0)return colors[2];
        if(charging&&bool("chargingColor",true))return colors[4];
        if(level<=integer("low",20))return colors[0];
        if(level<integer("high",65))return colors[1];
        if(level>=95)return colors[3];
        return colors[2];
    }
}

package fr.dm100.prisme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.Calendar;

/** Time/battery/charge rules evaluated locally. Disabled by default, never monitors apps. */
public final class SenseEngine {
    private SenseEngine(){}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("prisme_sense",Context.MODE_PRIVATE);}
    public static boolean apply(Context c,DashboardPrefs dashboard){
        SharedPreferences state=prefs(c);
        if(!PrefValues.flag(state,"enabled",false)&&!PrefValues.flag(state,"powerSaver",false)){
            if(PrefValues.flag(state,"powerSaveActive",false))state.edit().putBoolean("powerSaveActive",false).apply();
            return false;
        }
        BatteryLevelReader.Reading battery=BatteryLevelReader.read(c,null);
        int threshold=PrefValues.number(state,"lowThreshold",20);
        boolean low=battery.level>=0&&battery.level<=threshold;
        boolean save=PrefValues.flag(state,"powerSaver",false)&&low;
        if(PrefValues.flag(state,"powerSaveActive",false)!=save)state.edit().putBoolean("powerSaveActive",save).apply();
        if(!PrefValues.flag(state,"enabled",false))return false;
        int hour=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int day=PrefValues.number(state,"dayStart",7),night=PrefValues.number(state,"nightStart",21);
        boolean daylight=day<=night?(hour>=day&&hour<night):(hour>=day||hour<night);
        int chosen=PrefValues.number(state,daylight?"dayTheme":"nightTheme",daylight?0:21);
        if(PrefValues.flag(state,"chargeRule",false)&&battery.charging)chosen=PrefValues.number(state,"chargeTheme",20);
        if(PrefValues.flag(state,"batteryRule",true)&&low)chosen=PrefValues.number(state,"lowTheme",21);
        chosen=Math.max(0,Math.min(ThemeSpec.ALL.length-1,chosen));
        if(dashboard.getInt("theme",0)==chosen)return false;
        dashboard.putInt("theme",chosen);return true;
    }
    public static void open(Activity activity,DashboardPrefs dashboard,Runnable changed){
        SharedPreferences store=prefs(activity);
        if(!store.contains("dayTheme"))store.edit().putInt("dayTheme",dashboard.getInt("theme",0)).apply();
        ScrollView scroll=new ScrollView(activity);
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(12,12,12,12);
        scroll.addView(root);
        TextView note=new TextView(activity);note.setText("Règles locales. Aucune connexion ni surveillance des applications. Les styles manuels restent disponibles en désactivant Sense.");
        note.setTextColor(0xffdee7ed);root.addView(note);
        check(activity,root,store,"enabled","Activer PRISME Sense",false,changed);
        check(activity,root,store,"batteryRule","Style spécial batterie faible",true,changed);
        check(activity,root,store,"chargeRule","Style spécial pendant la charge",false,changed);
        check(activity,root,store,"powerSaver","Économiser les animations en batterie faible",false,changed);
        choice(activity,root,store,"dayTheme","Style du jour",dashboard.getInt("theme",0),changed);
        choice(activity,root,store,"nightTheme","Style de nuit",21,changed);
        choice(activity,root,store,"lowTheme","Style batterie faible",21,changed);
        choice(activity,root,store,"chargeTheme","Style pendant la charge",20,changed);
        number(activity,root,store,"dayStart","Début du jour",7,23,changed);
        number(activity,root,store,"nightStart","Début de la nuit",21,23,changed);
        number(activity,root,store,"lowThreshold","Batterie faible à",20,40,changed);
        new V6Ui.Builder(activity).setTitle("◈ PRISME Sense V6")
            .setView(scroll).setPositiveButton("Enregistrer",(d,w)->{apply(activity,dashboard);changed.run();}).show();
    }
    private static void check(Activity a,LinearLayout root,SharedPreferences s,String key,String label,boolean fallback,Runnable onChange){
        CheckBox c=new CheckBox(a);c.setText(label);c.setTextColor(0xfff1ffff);c.setChecked(PrefValues.flag(s,key,fallback));
        c.setOnCheckedChangeListener((v,yes)->{s.edit().putBoolean(key,yes).apply();onChange.run();});root.addView(c);
    }
    private static void choice(Activity a,LinearLayout root,SharedPreferences s,String key,String label,int fallback,Runnable onChange){
        TextView t=new TextView(a);t.setText(label);t.setTextColor(0xffe7ffff);root.addView(t);
        Spinner spinner=new Spinner(a);String[] names=new String[ThemeSpec.ALL.length];
        for(int i=0;i<names.length;i++)names[i]=ThemeSpec.ALL[i].name;
        ArrayAdapter<String> adapter=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);spinner.setSelection(Math.max(0,Math.min(names.length-1,PrefValues.number(s,key,fallback))));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                if(PrefValues.number(s,key,fallback)!=position){s.edit().putInt(key,position).apply();onChange.run();}
            }
            public void onNothingSelected(AdapterView<?> parent){}
        });root.addView(spinner);
    }
    private static void number(Activity a,LinearLayout root,SharedPreferences s,String key,String label,int fallback,int max,Runnable onChange){
        TextView t=new TextView(a);t.setText(label+" : "+PrefValues.number(s,key,fallback));t.setTextColor(0xffe7ffff);root.addView(t);
        android.widget.SeekBar seek=new android.widget.SeekBar(a);seek.setMax(max);seek.setProgress(PrefValues.number(s,key,fallback));
        seek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(android.widget.SeekBar b,int value,boolean user){if(user){int safe="lowThreshold".equals(key)?Math.max(5,value):value;s.edit().putInt(key,safe).apply();t.setText(label+" : "+safe);onChange.run();}}
            public void onStartTrackingTouch(android.widget.SeekBar b){}
            public void onStopTrackingTouch(android.widget.SeekBar b){}
        });root.addView(seek);
    }
}

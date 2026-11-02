package fr.dm100.prisme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.View;
import android.util.TypedValue;

/** Opt-in watch-only edge dock. Service owns exactly one overlay window. */
public final class EdgeDockService extends Service {
    private static final int NOTICE=108;
    private WindowManager manager;
    private View overlay;
    private boolean expanded,registered;
    private SharedPreferences prefs;
    private final BroadcastReceiver screen=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent intent){
            if(intent!=null&&Intent.ACTION_SCREEN_OFF.equals(intent.getAction()))remove();
            else draw();
        }
    };
    public static boolean enable(Context c){
        if(!Settings.canDrawOverlays(c))return false;
        c.getSharedPreferences("prisme_edge",0).edit().putBoolean("enabled",true).apply();
        refresh(c);return true;
    }
    public static void disable(Context c){
        c.getSharedPreferences("prisme_edge",0).edit().putBoolean("enabled",false).apply();
        c.stopService(new Intent(c,EdgeDockService.class));
    }
    public static void refresh(Context c){
        SharedPreferences p=c.getSharedPreferences("prisme_edge",0);
        Intent intent=new Intent(c,EdgeDockService.class);
        if(!p.getBoolean("enabled",false)){c.stopService(intent);return;}
        if(!Settings.canDrawOverlays(c))return;
        try{if(Build.VERSION.SDK_INT>=26)Api26.start(c,intent);else c.startService(intent);}
        catch(RuntimeException ignored){}
    }
    @Override public void onCreate(){
        super.onCreate();prefs=getSharedPreferences("prisme_edge",0);
        manager=(WindowManager)getSystemService(WINDOW_SERVICE);
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_SCREEN_OFF);filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screen,filter);registered=true;
        startForeground(NOTICE,notification());
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){draw();return START_STICKY;}
    private Notification notification(){
        NotificationManager m=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?Api26.builder(this,m):new Notification.Builder(this);
        Intent open=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi=PendingIntent.getActivity(this,NOTICE,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return b.setSmallIcon(R.drawable.ic_prisme).setContentTitle("PRISME · Dock")
            .setContentText("Dock de la montre actif").setContentIntent(pi)
            .setOngoing(true).setPriority(Notification.PRIORITY_LOW).build();
    }
    private void remove(){
        if(overlay!=null&&manager!=null){try{manager.removeView(overlay);}catch(RuntimeException ignored){}overlay=null;}
    }
    private TextView button(String label,int px){
        TextView text=new TextView(this);text.setText(label);text.setGravity(Gravity.CENTER);
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX,px);text.setTextColor(Color.WHITE);
        text.setContentDescription(label);text.setClickable(true);
        return text;
    }
    private void draw(){
        remove();
        if(!prefs.getBoolean("enabled",false)||!Settings.canDrawOverlays(this)||manager==null){stopSelf();return;}
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        if(pm!=null&&!pm.isInteractive())return;
        Point display=new Point();manager.getDefaultDisplay().getRealSize(display);
        int size=Math.max(30,Math.min(90,prefs.getInt("size",44)));
        int w=expanded?Math.max(92,size*2):size,h=expanded?Math.min(display.y,size*6):size;
        w=Math.min(display.x,w);h=Math.min(display.y,h);
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setGravity(Gravity.CENTER);
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xeb142b3c);bg.setCornerRadius(12);
        bg.setStroke(2,0xff7fe8dc);shell.setBackground(bg);
        TextView toggle=button(expanded?"×":"◈",Math.round(size*.55f));
        toggle.setOnClickListener(v->{expanded=!expanded;draw();});
        shell.addView(toggle,new LinearLayout.LayoutParams(-1,Math.min(size,h)));
        if(expanded){
            String[] labels={"⌂","☷","⏱","♫","↻"};
            String[] actions={"home","notes","timer","music","rotation"};
            for(int i=0;i<labels.length;i++){
                final String action=actions[i];TextView tile=button(labels[i],Math.round(size*.48f));
                tile.setContentDescription(action);tile.setOnClickListener(v->{
                    expanded=false;
                    if("music".equals(action))MusicPlayback.command(this,MusicPlayback.TOGGLE);
                    else if("rotation".equals(action))GlobalRotationService.apply(this,GlobalRotationService.oppositeMode(this));
                    else{
                        Intent launch=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        launch.putExtra("utilityAction",action.equals("home")?"today":action);
                        try{startActivity(launch);}catch(RuntimeException ignored){}
                    }
                    draw();
                });
                shell.addView(tile,new LinearLayout.LayoutParams(-1,Math.max(1,(h-size)/5)));
            }
        }
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,
            Build.VERSION.SDK_INT>=26?Api26.windowType():WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.LEFT|Gravity.TOP;
        int location=Math.max(0,Math.min(3,prefs.getInt("position",1)));
        p.x=(location%2==1)?Math.max(0,display.x-w):0;
        p.y=Math.max(0,Math.round((display.y-h)*(location<2?.5f:.72f)));
        p.alpha=Math.max(.2f,Math.min(1f,prefs.getInt("opacity",90)/100f));
        p.setTitle("PRISME:WatchEdgeDock");
        try{manager.addView(shell,p);overlay=shell;}catch(RuntimeException ex){overlay=null;}
    }
    @Override public void onDestroy(){remove();if(registered)unregisterReceiver(screen);stopForeground(true);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
    private static final class Api26 {
        static void start(Context c,Intent i){c.startForegroundService(i);}
        static int windowType(){return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;}
        static Notification.Builder builder(Context c,NotificationManager manager){
            NotificationChannel channel=new NotificationChannel("prisme_edge","PRISME Dock",NotificationManager.IMPORTANCE_LOW);
            if(manager!=null)manager.createNotificationChannel(channel);
            return new Notification.Builder(c,"prisme_edge");
        }
    }
    public static void settings(Activity a,Runnable change){
        SharedPreferences p=a.getSharedPreferences("prisme_edge",0);
        LinearLayout content=new LinearLayout(a);content.setPadding(12,12,12,12);content.setOrientation(LinearLayout.VERTICAL);
        CheckBox enabled=new CheckBox(a);enabled.setText("Activer le dock sur les applications");
        enabled.setTextColor(0xfff2ffff);enabled.setChecked(p.getBoolean("enabled",false));
        enabled.setOnCheckedChangeListener((v,yes)->{
            if(yes&&!enable(a)){enabled.setChecked(false);
                new AlertDialog.Builder(a).setMessage("Autorise les superpositions Android dans les réglages de PRISME.")
                    .setPositiveButton("Réglages",(d,w)->{try{a.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:"+a.getPackageName())));}catch(RuntimeException ignored){}})
                    .setNegativeButton("Fermer",null).show();
            }else if(!yes)disable(a);change.run();
        });content.addView(enabled);
        CheckBox boot=new CheckBox(a);boot.setText("Relancer au démarrage de la montre");
        boot.setTextColor(0xffe5ffff);boot.setChecked(p.getBoolean("boot",false));
        boot.setOnCheckedChangeListener((v,yes)->p.edit().putBoolean("boot",yes).apply());content.addView(boot);
        seek(a,content,p,"size","Taille",44,30,90,change);
        seek(a,content,p,"opacity","Opacité",90,20,100,change);
        seek(a,content,p,"position","Position (gauche/droite et hauteur)",1,0,3,change);
        new AlertDialog.Builder(a).setTitle("◈ Dock flottant V6").setView(content).setPositiveButton("Fermer",null).show();
    }
    private static void seek(Activity a,LinearLayout root,SharedPreferences p,String key,String label,int fallback,int min,int max,Runnable changed){
        TextView title=new TextView(a);title.setTextColor(0xfff3ffff);title.setText(label+" : "+p.getInt(key,fallback));root.addView(title);
        SeekBar bar=new SeekBar(a);bar.setMax(max-min);bar.setProgress(Math.max(0,p.getInt(key,fallback)-min));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar v,int progress,boolean user){
                if(!user)return;int value=progress+min;p.edit().putInt(key,value).apply();title.setText(label+" : "+value);refresh(a);changed.run();
            }
            public void onStartTrackingTouch(SeekBar v){}
            public void onStopTrackingTouch(SeekBar v){}
        });root.addView(bar);
    }
}

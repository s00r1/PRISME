package fr.dm100.prisme;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;

/** A non-touchable orientation window above app windows; release restores their normal policy. */
public final class GlobalRotationService extends Service {
    private static final String STOP="fr.dm100.prisme.RELEASE_ORIENTATION";
    private static final String TOGGLE="fr.dm100.prisme.TOGGLE_ORIENTATION";
    private static boolean running;
    private WindowManager manager;private View overlay;
    public static boolean isRunning(){return running;}
    public static int oppositeMode(Context c){
        WindowManager wm=(WindowManager)c.getSystemService(WINDOW_SERVICE);
        android.graphics.Point size=new android.graphics.Point();
        if(wm!=null)wm.getDefaultDisplay().getRealSize(size);
        if(size.x>0&&size.y>0)return size.x>=size.y?1:2;
        return c.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE?1:2;
    }
    public static boolean matches(Context c,int selected){
        WindowManager wm=(WindowManager)c.getSystemService(WINDOW_SERVICE);if(wm==null)return false;
        android.graphics.Point size=new android.graphics.Point();wm.getDefaultDisplay().getRealSize(size);
        return (selected==1||selected==3)?size.y>size.x:size.x>size.y;
    }
    public static int mode(Context c){return Math.max(0,Math.min(4,c.getSharedPreferences("prisme_rotation",0).getInt("mode",0)));}
    public static String label(Context c){String[] names={"Système","Portrait forcé","Paysage forcé","Portrait inversé","Paysage inversé"};return I18n.t(c,names[mode(c)]);}
    public static boolean apply(Context c,int mode){
        int safe=Math.max(0,Math.min(4,mode));if(safe==0){release(c);return true;}if(!Settings.canDrawOverlays(c))return false;
        int previous=mode(c);c.getSharedPreferences("prisme_rotation",0).edit().putInt("mode",safe).commit();
        Intent i=new Intent(c,GlobalRotationService.class);
        try{if(Build.VERSION.SDK_INT>=26)Api26.start(c,i);else c.startService(i);return true;}
        catch(RuntimeException ex){c.getSharedPreferences("prisme_rotation",0).edit().putInt("mode",previous).apply();return false;}
    }
    public static void release(Context c){c.getSharedPreferences("prisme_rotation",0).edit().putInt("mode",0).apply();c.stopService(new Intent(c,GlobalRotationService.class));}
    public static void boot(Context c){if(c.getSharedPreferences("prisme_rotation",0).getBoolean("boot",false)&&mode(c)>0)apply(c,mode(c));else c.getSharedPreferences("prisme_rotation",0).edit().putInt("mode",0).apply();}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onCreate(){super.onCreate();manager=(WindowManager)getSystemService(WINDOW_SERVICE);}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null&&STOP.equals(intent.getAction())){getSharedPreferences("prisme_rotation",0).edit().putInt("mode",0).apply();stopSelf();return START_NOT_STICKY;}
        if(intent!=null&&TOGGLE.equals(intent.getAction()))getSharedPreferences("prisme_rotation",0).edit().putInt("mode",oppositeMode(this)).commit();
        int selected=mode(this);if(selected==0||!Settings.canDrawOverlays(this)||manager==null){stopSelf();return START_NOT_STICKY;}
        startForeground(74,notification());
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(1,1,Build.VERSION.SDK_INT>=26?Api26.windowType():WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.START;p.alpha=.01f;p.setTitle("PRISME:GlobalRotation");
        p.screenOrientation=selected==1?ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:selected==2?ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:selected==3?ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        try{if(overlay==null){overlay=new View(this);manager.addView(overlay,p);}else manager.updateViewLayout(overlay,p);running=true;}
        catch(RuntimeException ex){getSharedPreferences("prisme_rotation",0).edit().putInt("mode",0).apply();stopSelf();return START_NOT_STICKY;}
        return START_STICKY;
    }
    private Notification notification(){
        NotificationManager m=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);Notification.Builder b=Build.VERSION.SDK_INT>=26?Api26.builder(this,m):new Notification.Builder(this);
        PendingIntent stop=PendingIntent.getService(this,74,new Intent(this,GlobalRotationService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent open=PendingIntent.getActivity(this,74,new Intent(this,MainActivity.class).putExtra("utilityAction","rotationSettings"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent toggle=PendingIntent.getService(this,75,new Intent(this,GlobalRotationService.class).setAction(TOGGLE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return b.setSmallIcon(R.drawable.ic_prisme).setContentTitle(I18n.t(this,"PRISME · ")+label(this)).setContentText(I18n.t(this,"Orientation maintenue dans les autres applications"))
                .setOngoing(true).setContentIntent(open).setPriority(Notification.PRIORITY_LOW).addAction(0,I18n.t(this,"Portrait / Paysage"),toggle).addAction(0,I18n.t(this,"Libérer l’orientation"),stop).build();
    }
    @Override public void onDestroy(){running=false;if(overlay!=null&&manager!=null){try{manager.removeViewImmediate(overlay);}catch(RuntimeException ignored){}overlay=null;}stopForeground(true);super.onDestroy();}
    private static final class Api26 {
        static void start(Context c,Intent i){c.startForegroundService(i);}
        static int windowType(){return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;}
        static Notification.Builder builder(Context c,NotificationManager m){NotificationChannel channel=new NotificationChannel("prisme_rotation",I18n.t(c,"Orientation globale"),NotificationManager.IMPORTANCE_LOW);m.createNotificationChannel(channel);return new Notification.Builder(c,"prisme_rotation");}
    }
}

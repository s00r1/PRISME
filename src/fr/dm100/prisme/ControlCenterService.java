package fr.dm100.prisme;

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
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/** Foreground owner of the Control Center handle and panel, independent of the dashboard. */
public final class ControlCenterService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String CHANNEL="prisme_control_center_overlay";
    private static final int NOTIFICATION=91;
    private static volatile ControlCenterService instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable delayedRefresh=new Runnable(){@Override public void run(){reconcile();}};
    private final BroadcastReceiver screen=new BroadcastReceiver(){
        @Override public void onReceive(Context context,Intent intent){
            if(intent==null)return;
            if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){closePanel();removeHandle();}
            else reconcile();
        }
    };
    private ControlCenterPrefs prefs;
    private ControlCenterController controller;
    private AppRepository apps;
    private SharedPreferences store;
    private WindowManager windows;
    private TextView handle;
    private ControlCenterPanel panel;
    private boolean registered;
    private float downX,downY;
    private long downAt,lastTap;

    public static boolean isShowing(){
        ControlCenterService s=instance;
        return s!=null&&(s.handle!=null||s.panel!=null);
    }
    public static void refresh(Context context){
        Context app=context.getApplicationContext();
        ControlCenterPrefs prefs=new ControlCenterPrefs(app);
        if(!prefs.getBoolean("enabled",true)){
            app.stopService(new Intent(app,ControlCenterService.class));
            return;
        }
        if(!Settings.canDrawOverlays(app))return;
        Intent intent=new Intent(app,ControlCenterService.class);
        try{
            if(Build.VERSION.SDK_INT>=26)Api26.start(app,intent);
            else app.startService(intent);
        }catch(RuntimeException ignored){ /* OEM foreground-service restrictions are respected. */ }
    }
    @Override public void onCreate(){
        super.onCreate();
        instance=this;
        prefs=new ControlCenterPrefs(this);
        windows=(WindowManager)getSystemService(WINDOW_SERVICE);
        controller=new ControlCenterController(this,new ControlCenterController.Host(){
            @Override public void onPrismeAction(String action){launchPrisme(action);}
            @Override public void onCustomize(){launchPrisme("controlCenterSettings");}
        });
        apps=new AppRepository(this);
        apps.load(entries->{if(controller!=null){controller.setApps(entries);if(panel!=null)panel.refresh();}});
        try{startForeground(NOTIFICATION,notification());}
        catch(RuntimeException e){stopSelf();return;}
        store=getSharedPreferences("prisme_control_center",MODE_PRIVATE);
        store.registerOnSharedPreferenceChangeListener(this);
        IntentFilter f=new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screen,f);registered=true;
    }
    private Notification notification(){
        Notification.Builder b=Build.VERSION.SDK_INT>=26?Api26.builder(this):new Notification.Builder(this);
        Intent settings=new Intent(this,MainActivity.class).putExtra("controlCenterAction","controlCenterSettings")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent open=PendingIntent.getActivity(this,NOTIFICATION,settings,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return b.setSmallIcon(R.drawable.ic_prisme)
                .setContentTitle("PRISME · Centre de contrôle")
                .setContentText("Centre flottant disponible dans les applications")
                .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
                .setPriority(Notification.PRIORITY_LOW).setCategory(Notification.CATEGORY_SERVICE).build();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        reconcile();
        return START_STICKY;
    }
    private boolean interactive(){
        PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);
        return power==null||power.isInteractive();
    }
    private void reconcile(){
        if(!prefs.getBoolean("enabled",true)){stopSelf();return;}
        if(!Settings.canDrawOverlays(this)){closePanel();removeHandle();stopSelf();return;}
        if(!interactive()){closePanel();removeHandle();return;}
        if(panel!=null){panel.open();return;}
        removeHandle();
        showHandle();
    }
    private int type(){
        return Build.VERSION.SDK_INT>=26?Api26.windowType():WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
    }
    private void showHandle(){
        if(windows==null)return;
        Point size=new Point();windows.getDefaultDisplay().getRealSize(size);
        final int edge=prefs.getInt("edge",0),align=prefs.getInt("handlePosition",1);
        final int width=edge<=1?Math.min(size.x,112):Math.min(size.x,44);
        final int height=edge<=1?Math.min(size.y,44):Math.min(size.y,90);
        TextView view=new TextView(this);
        view.setGravity(Gravity.CENTER);
        view.setText(edge<=1?(prefs.getBoolean("compactHandle",true)?"━":"≡ Contrôles"):"⋮");
        view.setTextSize(edge<=1?15:23);
        view.setTextColor(prefs.getBoolean("customAccent",false)?
                0xff000000|(prefs.getInt("customColor",0x62d9cc)&0xffffff):0xff62d9cc);
        GradientDrawable background=new GradientDrawable();
        background.setColor(prefs.getBoolean("compactHandle",true)?0x9e162434:0xe81a2a3e);
        background.setCornerRadius(14);
        background.setStroke(1,0xaa62d9cc);
        view.setBackground(background);
        view.setContentDescription("Ouvrir le centre de contrôle PRISME");
        view.setOnTouchListener((v,event)->touch(event));
        int flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,height,type(),flags,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.LEFT|Gravity.TOP;
        if(edge<=1){
            p.x=align==0?8:align==2?Math.max(0,size.x-width-8):Math.max(0,(size.x-width)/2);
            p.y=edge==0?0:Math.max(0,size.y-height);
        }else{
            p.x=edge==2?0:Math.max(0,size.x-width);
            p.y=align==0?8:align==2?Math.max(0,size.y-height-8):Math.max(0,(size.y-height)/2);
        }
        p.setTitle("fr.dm100.prisme:ControlCenterHandle");
        try{windows.addView(view,p);handle=view;}
        catch(RuntimeException ignored){handle=null;}
    }
    private boolean touch(MotionEvent event){
        int opening=prefs.getInt("opening",3),edge=prefs.getInt("edge",0);
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                downX=event.getRawX();downY=event.getRawY();downAt=event.getEventTime();return true;
            case MotionEvent.ACTION_MOVE:
                if(opening==0||opening==3){
                    float dx=event.getRawX()-downX,dy=event.getRawY()-downY;
                    if(ControlCenterGesture.inwardDistance(edge,dx,dy)>=prefs.getInt("swipeDistance",36)
                            &&Math.abs(ControlCenterGesture.perpendicularDistance(edge,dx,dy))<Math.abs(ControlCenterGesture.inwardDistance(edge,dx,dy))){
                        lastTap=0;openPanel();
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if(Math.abs(event.getRawX()-downX)>18||Math.abs(event.getRawY()-downY)>18
                        ||event.getEventTime()-downAt>400)return true;
                if(opening>=2)openPanel();
                else if(opening==1){
                    if(lastTap>0&&event.getEventTime()-lastTap<=360){lastTap=0;openPanel();}
                    else lastTap=event.getEventTime();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                lastTap=0;return true;
            default:return true;
        }
    }
    private void openPanel(){
        if(panel!=null||windows==null||!Settings.canDrawOverlays(this))return;
        try{
            ControlCenterPanel next=new ControlCenterPanel(this,prefs,controller,new ControlCenterPanel.Listener(){
                @Override public void onCustomize(){launchPrisme("controlCenterSettings");}
                @Override public void onClosed(){closePanel();}
            });
            int flags=WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            WindowManager.LayoutParams p=new WindowManager.LayoutParams(-1,-1,type(),flags,PixelFormat.TRANSLUCENT);
            p.gravity=Gravity.TOP|Gravity.LEFT;
            p.setTitle("fr.dm100.prisme:ControlCenterPanel");
            panel=next;
            windows.addView(next,p);
            if(handle!=null)handle.setVisibility(View.GONE);
            next.open();
        }catch(RuntimeException ignored){closePanel();}
    }
    private void closePanel(){
        ControlCenterPanel previous=panel;
        panel=null;
        if(previous!=null){
            previous.close();
            if(previous.getParent()!=null)try{windows.removeView(previous);}catch(RuntimeException ignored){}
        }
        if(handle!=null)handle.setVisibility(View.VISIBLE);
    }
    private void removeHandle(){
        TextView previous=handle;handle=null;
        if(previous!=null&&previous.getParent()!=null)try{windows.removeView(previous);}catch(RuntimeException ignored){}
    }
    private void launchPrisme(String action){
        closePanel();
        if(action==null)return;
        Intent launch=new Intent(this,MainActivity.class).putExtra("controlCenterAction",action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try{startActivity(launch);}catch(RuntimeException ignored){}
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences shared,String key){
        handler.removeCallbacks(delayedRefresh);
        handler.postDelayed(delayedRefresh,60);
    }
    @Override public void onConfigurationChanged(Configuration config){
        super.onConfigurationChanged(config);
        closePanel();removeHandle();reconcile();
    }
    @Override public void onDestroy(){
        handler.removeCallbacksAndMessages(null);
        closePanel();removeHandle();
        if(registered)unregisterReceiver(screen);
        if(store!=null)store.unregisterOnSharedPreferenceChangeListener(this);
        if(apps!=null)apps.close();
        if(instance==this)instance=null;
        stopForeground(true);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
    private static final class Api26 {
        static void start(Context context,Intent intent){context.startForegroundService(intent);}
        static int windowType(){return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;}
        static Notification.Builder builder(Context context){
            NotificationManager m=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel c=new NotificationChannel(CHANNEL,"PRISME · Centre de contrôle",NotificationManager.IMPORTANCE_LOW);
            c.setSound(null,null);c.enableVibration(false);
            if(m!=null)m.createNotificationChannel(c);
            return new Notification.Builder(context,CHANNEL);
        }
    }
}

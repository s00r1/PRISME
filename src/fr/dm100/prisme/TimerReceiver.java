package fr.dm100.prisme;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class TimerReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        if(i==null)return;
        if("fr.dm100.prisme.TIMER".equals(i.getAction()))TimerEngine.finish(c,i.getLongExtra("token",-1));
        else if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())||Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction()))TimerEngine.schedule(c);
    }
    public static void dismissNotification(Context c){NotificationManager m=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(m!=null)m.cancel(410);}
    static void notifyFinished(Context c){
        NotificationManager m=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(m==null)return;
        Intent open=new Intent(c,MainActivity.class).putExtra("utilityAction","timer").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap=PendingIntent.getActivity(c,410,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?Api26.builder(c,m):new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_prisme).setContentTitle(I18n.t(c,"PRISME · Minuteur terminé")).setContentText(I18n.t(c,"Le temps est écoulé. Toucher pour ouvrir le minuteur."))
                .setCategory(Notification.CATEGORY_ALARM).setPriority(Notification.PRIORITY_HIGH).setDefaults(Notification.DEFAULT_ALL).setAutoCancel(true).setContentIntent(tap);
        try{m.notify(410,b.build());}catch(RuntimeException ignored){}
    }
    private static final class Api26 {
        static Notification.Builder builder(Context c,NotificationManager m){
            NotificationChannel channel=new NotificationChannel("prisme_timer",I18n.t(c,"Minuteurs PRISME"),NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);m.createNotificationChannel(channel);return new Notification.Builder(c,"prisme_timer");
        }
    }
}

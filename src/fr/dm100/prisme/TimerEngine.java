package fr.dm100.prisme;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

/** Alarm-backed countdown. Elapsed time survives sleep; wall time is used only after a reboot. */
public final class TimerEngine {
    private TimerEngine(){}
    private static SharedPreferences store(Context c){return c.getApplicationContext().getSharedPreferences("prisme_utilities",Context.MODE_PRIVATE);}
    private static int boot(Context c){try{return Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,-1);}catch(RuntimeException e){return -1;}}
    public static long deadline(Context c){return store(c).getLong("timerDeadline",0);}
    public static long remaining(Context c){
        SharedPreferences s=store(c);long wall=s.getLong("timerDeadline",0);if(wall==0)return s.getLong("timerPaused",0);
        long elapsed=s.getLong("timerElapsedDeadline",0);
        return Countdown.remaining(wall,elapsed,s.getInt("timerBoot",-2),boot(c),System.currentTimeMillis(),SystemClock.elapsedRealtime(),0);
    }
    public static boolean paused(Context c){return deadline(c)==0&&store(c).getLong("timerPaused",0)>0;}
    private static PendingIntent alarm(Context c,long token){return PendingIntent.getBroadcast(c,410,new Intent(c,TimerReceiver.class).setAction("fr.dm100.prisme.TIMER").putExtra("token",token),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    public static synchronized boolean start(Context c,long duration){
        if(!Countdown.validDuration(duration))return false;
        long token=store(c).getLong("timerToken",0)+1;
        store(c).edit().putLong("timerDeadline",System.currentTimeMillis()+duration).putLong("timerElapsedDeadline",SystemClock.elapsedRealtime()+duration)
                .putInt("timerBoot",boot(c)).putLong("timerPaused",0).putLong("timerToken",token).putLong("timerFinishedAt",0).commit();
        TimerReceiver.dismissNotification(c);return schedule(c);
    }
    public static synchronized boolean schedule(Context c){
        if(deadline(c)==0)return true;
        AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(a==null)return false;
        long left=remaining(c);if(left==0){finish(c,store(c).getLong("timerToken",0));return true;}
        store(c).edit().putLong("timerElapsedDeadline",SystemClock.elapsedRealtime()+left).putInt("timerBoot",boot(c)).commit();
        try{a.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+left,alarm(c,store(c).getLong("timerToken",0)));return true;}
        catch(RuntimeException ex){return false;}
    }
    public static synchronized void pause(Context c){long left=remaining(c);cancelAlarm(c);store(c).edit().putLong("timerDeadline",0).putLong("timerPaused",left).putLong("timerToken",store(c).getLong("timerToken",0)+1).commit();}
    public static synchronized boolean resume(Context c){long left=remaining(c);return left>0&&start(c,left);}
    private static void cancelAlarm(Context c){AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(a!=null)a.cancel(alarm(c,store(c).getLong("timerToken",0)));}
    public static synchronized void cancel(Context c){cancelAlarm(c);store(c).edit().putLong("timerDeadline",0).putLong("timerPaused",0).putLong("timerFinishedAt",0).putLong("timerToken",store(c).getLong("timerToken",0)+1).commit();TimerReceiver.dismissNotification(c);}
    public static synchronized boolean finish(Context c,long token){
        SharedPreferences s=store(c);if(token!=s.getLong("timerToken",0)||deadline(c)==0||remaining(c)>0)return false;
        s.edit().putLong("timerDeadline",0).putLong("timerPaused",0).putLong("timerFinishedAt",System.currentTimeMillis()).commit();
        TimerReceiver.notifyFinished(c);return true;
    }
    public static void check(Context c){if(deadline(c)>0&&remaining(c)==0)finish(c,store(c).getLong("timerToken",0));}
}

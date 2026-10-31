package fr.dm100.prisme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Public Android APIs only. Restricted system switches are explicit links, never simulated toggles. */
public final class ControlCenterController {
    public interface Host {
        void onPrismeAction(String action);
        void onCustomize();
    }
    public static final String[] CATALOG = {
        "wifi","bluetooth","sound","airplane","data","hotspot","location","rotateGlobal",
        "battery","display","volume","dnd","screenlamp","camera","alarms","timer",
        "stopwatch","calculator","notes","settings","apps","styles","clock","search",
        "music","vpn","accessibility","capture","volumepill","today","checklist","deskclock","device","musicPrevious","musicNext"
    };
    public static final class TileState {
        public final String id, title, subtitle;
        public final boolean active, available, direct, busy;
        TileState(String id,String title,String subtitle,boolean active,boolean available,boolean direct,boolean busy) {
            this.id=id;this.title=title;this.subtitle=subtitle;this.active=active;
            this.available=available;this.direct=direct;this.busy=busy;
        }
    }
    private final Activity activity;
    private final Host host;
    private final AudioManager audio;
    private final WifiManager wifi;
    private final NotificationManager notifications;
    private final Set<ComponentName> launchers=new HashSet<ComponentName>();
    public ControlCenterController(Activity activity,Host host) {
        this.activity=activity;this.host=host;
        audio=(AudioManager)activity.getSystemService(Activity.AUDIO_SERVICE);
        wifi=(WifiManager)activity.getApplicationContext().getSystemService(Activity.WIFI_SERVICE);
        notifications=(NotificationManager)activity.getSystemService(Activity.NOTIFICATION_SERVICE);
    }
    public void setApps(List<AppEntry> apps){
        launchers.clear();if(apps!=null)for(AppEntry app:apps)launchers.add(app.component);
    }
    public static String label(String id) {
        if(id==null)return "Raccourci";
        switch(id) {
            case "wifi":return "Wi-Fi";case "bluetooth":return "Bluetooth";case "sound":return "Sonnerie";
            case "airplane":return "Mode avion";case "data":return "Données mobiles";case "hotspot":return "Partage Internet";
            case "location":return "Localisation";case "rotation":case "rotateGlobal":return "Portrait / Paysage";case "battery":return "Batterie";
            case "display":return "Affichage";case "volume":return "Volume";case "dnd":return "Ne pas déranger";
            case "screenlamp":return "Lampe écran";case "camera":return "Appareil photo";case "alarms":return "Réveils";
            case "timer":return "Minuteur";case "stopwatch":return "Chronomètre";case "calculator":return "Calculatrice";
            case "notes":return "Carnet";case "settings":return "Réglages Android";case "apps":return "Applications";
            case "styles":return "Styles PRISME";case "clock":return "Cadran montre";case "search":return "Recherche";
            case "music":return "Lecture / pause";case "vpn":return "VPN";case "accessibility":return "Accessibilité";
            case "today":return "Aujourd’hui";case "checklist":return "Tâches";case "deskclock":return "Horloge de table";case "device":return "État montre";case "musicPrevious":return "Piste précédente";case "musicNext":return "Piste suivante";case "capture":return "Capture PRISME";case "volumepill":return "Pastille volume";default:return "Application";
        }
    }
    public static String symbol(String id) {
        if(id==null)return "+";
        switch(id) {
            case "wifi":return "Wi-Fi";case "bluetooth":return "BT";case "sound":return "SON";
            case "airplane":return "✈";case "data":return "4G";case "hotspot":return "AP";
            case "location":return "GPS";case "rotation":return "↻";case "battery":return "%";
            case "display":return "☀";case "volume":return "VOL";case "dnd":return "☾";
            case "screenlamp":return "◉";case "camera":return "PHOTO";case "alarms":return "AL";
            case "timer":return "MIN";case "stopwatch":return "CHRO";case "calculator":return "±";
            case "notes":return "NOTE";case "settings":return "⚙";case "apps":return "APP";
            case "styles":return "STYLE";case "clock":return "◷";case "search":return "⌕";
            case "music":return "▶Ⅱ";case "vpn":return "VPN";case "accessibility":return "A11Y";
            case "rotateGlobal":return "↻";case "today":return "JOUR";case "checklist":return "✓";case "deskclock":return "◷";case "device":return "ÉTAT";case "musicPrevious":return "|◀";case "musicNext":return "▶|";case "capture":return "IMG";case "volumepill":return "VOL";default:return "APP";
        }
    }
    private TileState tile(String id,String subtitle,boolean active,boolean available,boolean direct,boolean busy) {
        return new TileState(id,label(id),subtitle,active,available,direct,busy);
    }
    public TileState state(String id) {
        if(!ControlCenterPrefs.isValidTile(id))return tile(id,"Raccourci invalide",false,false,false,false);
        try {
            if(id.startsWith("app:")) {
                ComponentName component=ComponentName.unflattenFromString(id.substring(4));
                android.content.pm.ActivityInfo info=activity.getPackageManager().getActivityInfo(component,0);
                String name=info.loadLabel(activity.getPackageManager()).toString();
                boolean available=info.enabled&&info.exported&&info.applicationInfo.enabled&&launchers.contains(component);
                return new TileState(id,name,available?"Ouvrir":"Application indisponible",false,available,false,false);
            }
            if("today".equals(id)||"checklist".equals(id)||"deskclock".equals(id)||"device".equals(id))return tile(id,"Ouvrir",false,true,false,false);
            if("wifi".equals(id)) {
                if(wifi==null)return tile(id,"Non disponible",false,false,false,false);
                int status=wifi.getWifiState();boolean enabled=wifi.isWifiEnabled();
                boolean changing=status==WifiManager.WIFI_STATE_ENABLING||status==WifiManager.WIFI_STATE_DISABLING;
                ConnectivityManager cm=(ConnectivityManager)activity.getSystemService(Activity.CONNECTIVITY_SERVICE);
                NetworkInfo n=cm==null?null:cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                return tile(id,changing?"Changement…":enabled?(n!=null&&n.isConnected()?"Connecté":"Activé"):"Désactivé",enabled,true,Build.VERSION.SDK_INT<29,changing);
            }
            if("bluetooth".equals(id)) {
                BluetoothAdapter bt=BluetoothAdapter.getDefaultAdapter();
                if(bt==null)return tile(id,"Non disponible",false,false,false,false);
                int status=bt.getState();boolean changing=status==BluetoothAdapter.STATE_TURNING_ON||status==BluetoothAdapter.STATE_TURNING_OFF;
                return tile(id,changing?"Changement…":bt.isEnabled()?"Activé":"Désactivé",bt.isEnabled(),true,Build.VERSION.SDK_INT<31,changing);
            }
            if("sound".equals(id)) {
                if(audio==null)return tile(id,"Non disponible",false,false,false,false);
                int r=audio.getRingerMode();return tile(id,r==AudioManager.RINGER_MODE_NORMAL?"Sonnerie":r==AudioManager.RINGER_MODE_VIBRATE?"Vibreur":"Silencieux",r==AudioManager.RINGER_MODE_NORMAL,true,true,false);
            }
            if("airplane".equals(id))return tile(id,Settings.Global.getInt(activity.getContentResolver(),Settings.Global.AIRPLANE_MODE_ON,0)!=0?"Activé · réglages":"Réglages Android",Settings.Global.getInt(activity.getContentResolver(),Settings.Global.AIRPLANE_MODE_ON,0)!=0,canOpen(settingsIntent(id)),false,false);
            if("location".equals(id)) {
                boolean on=Settings.Secure.getInt(activity.getContentResolver(),Settings.Secure.LOCATION_MODE,0)!=0;
                return tile(id,on?"Activée · réglages":"Réglages Android",on,canOpen(settingsIntent(id)),false,false);
            }
            if("dnd".equals(id)) {
                boolean access=notifications!=null&&notifications.isNotificationPolicyAccessGranted();
                int filter=notifications==null?NotificationManager.INTERRUPTION_FILTER_UNKNOWN:notifications.getCurrentInterruptionFilter();
                boolean on=filter!=NotificationManager.INTERRUPTION_FILTER_UNKNOWN&&filter!=NotificationManager.INTERRUPTION_FILTER_ALL;
                return tile(id,access?(on?"Activé":"Désactivé"):"Autorisation Android",on,notifications!=null,access,false);
            }
            if("rotation".equals(id)||"rotateGlobal".equals(id))return tile(id,GlobalRotationService.label(activity),GlobalRotationService.isRunning(),true,true,false);
            if("timer".equals(id)){long left=TimerEngine.remaining(activity);return tile(id,left>0?(TimerEngine.paused(activity)?"Pause · ":"")+ExperienceController.duration(left):"Prêt",left>0,true,false,false);}
            if("musicPrevious".equals(id)||"musicNext".equals(id))return tile(id,"Lecteur audio actif",false,audio!=null,true,false);
            if("music".equals(id))return tile(id,audio!=null&&audio.isMusicActive()?"Audio en cours":"Commander la lecture",audio!=null&&audio.isMusicActive(),audio!=null,true,false);
            if("volumepill".equals(id))return tile(id,fr.dm100.prisme.volume.FloatingVolumeService.isShowing()?"Affichée":"Configurer",fr.dm100.prisme.volume.FloatingVolumeService.isShowing(),true,false,false);
            if("screenlamp".equals(id))return tile(id,"Écran blanc",false,true,true,false);
            if("camera".equals(id)||"alarms".equals(id)||"calculator".equals(id))return tile(id,"Ouvrir",false,canOpen(appIntent(id)),false,false);
            if(settingsIntent(id)!=null)return tile(id,"Réglages Android",false,canOpen(settingsIntent(id)),false,false);
            return tile(id,"capture".equals(id)?"Cette application uniquement":"Ouvrir",false,true,false,false);
        } catch(PackageManager.NameNotFoundException ex) { return new TileState(id,"Appli absente","À remplacer dans les réglages",false,false,false,false); }
        catch(RuntimeException ex) { return tile(id,"Réglage Android",false,canOpen(settingsIntent(id)),false,false); }
    }
    public void perform(String id) {
        TileState state=state(id);
        if(!state.available){toast(state.subtitle);return;}
        if(state.busy){toast("Le changement est en cours");return;}
        try {
            if(id.startsWith("app:")){host.onPrismeAction(id);return;}
            switch(id) {
                case "rotation":case "rotateGlobal":host.onPrismeAction("rotateGlobal");break;
                case "wifi":if(Build.VERSION.SDK_INT>=29){open(settingsIntent(id));break;}if(!wifi.setWifiEnabled(!wifi.isWifiEnabled()))toast("Android a refusé le changement Wi-Fi");break;
                case "bluetooth":{
                    if(Build.VERSION.SDK_INT>=31){open(settingsIntent(id));break;}
                    BluetoothAdapter bt=BluetoothAdapter.getDefaultAdapter();if(bt==null)break;
                    boolean accepted=bt.isEnabled()?bt.disable():bt.enable();if(!accepted)toast("Android a refusé le changement Bluetooth");break;
                }
                case "sound":{
                    int current=audio.getRingerMode();audio.setRingerMode(current==AudioManager.RINGER_MODE_NORMAL?AudioManager.RINGER_MODE_VIBRATE:AudioManager.RINGER_MODE_NORMAL);break;
                }
                case "dnd":
                    if(notifications!=null&&notifications.isNotificationPolicyAccessGranted())notifications.setInterruptionFilter(notifications.getCurrentInterruptionFilter()==NotificationManager.INTERRUPTION_FILTER_ALL?NotificationManager.INTERRUPTION_FILTER_PRIORITY:NotificationManager.INTERRUPTION_FILTER_ALL);
                    else host.onPrismeAction("dndPermission");break;
                case "musicPrevious":case "musicNext":{int key="musicPrevious".equals(id)?KeyEvent.KEYCODE_MEDIA_PREVIOUS:KeyEvent.KEYCODE_MEDIA_NEXT;audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));break;}
                case "music":
                    audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                    audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));break;
                case "camera":case "alarms":case "calculator":open(appIntent(id));break;
                case "volumepill":host.onPrismeAction("floatingVolumeSettings");break;
                case "today":case "checklist":case "deskclock":case "device":case "screenlamp":case "capture":case "timer":case "stopwatch":case "notes":case "apps":case "styles":case "clock":case "search":host.onPrismeAction(id);break;
                default:open(settingsIntent(id));break;
            }
        } catch(SecurityException ex) { toast("Android demande une autorisation ; ouverture des réglages");open(settingsIntent(id)); }
        catch(RuntimeException ex) { toast("Cette commande n’est pas disponible sur ce système"); }
    }
    public void longPress(String id) {
        if(id==null)return;
        if("rotation".equals(id)||"rotateGlobal".equals(id)){host.onPrismeAction("rotationSettings");return;}
        if("volumepill".equals(id)){host.onPrismeAction("floatingVolumeSettings");return;}
        if(id.startsWith("app:")){
            ComponentName c=ComponentName.unflattenFromString(id.substring(4));if(c!=null)open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+c.getPackageName())));return;
        }
        Intent i=settingsIntent(id);
        if(i!=null)open(i);else host.onCustomize();
    }
    private Intent settingsIntent(String id) {
        if(id==null)return null;
        switch(id) {
            case "wifi":return new Intent(Settings.ACTION_WIFI_SETTINGS);
            case "bluetooth":return new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            case "airplane":return new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            case "data":return new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
            case "hotspot":{
                Intent tether=new Intent("android.settings.TETHER_SETTINGS");
                return canOpen(tether)?tether:new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            }
            case "location":return new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            case "display":return new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            case "battery":return new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
            case "volume":case "sound":case "dnd":return new Intent(Settings.ACTION_SOUND_SETTINGS);
            case "settings":return new Intent(Settings.ACTION_SETTINGS);
            case "vpn":return new Intent(Settings.ACTION_VPN_SETTINGS);
            case "accessibility":return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            default:return null;
        }
    }
    private Intent appIntent(String id) {
        if("camera".equals(id)){
            Intent vendor=activity.getPackageManager().getLaunchIntentForPackage("com.mediatek.camera");
            return vendor!=null?vendor:new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        }
        if("alarms".equals(id))return new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        if("calculator".equals(id))return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR);
        return null;
    }
    private boolean canOpen(Intent i){try{return i!=null&&i.resolveActivity(activity.getPackageManager())!=null;}catch(RuntimeException ex){return false;}}
    private void open(Intent i) {
        if(!canOpen(i)) {
            if(i!=null&&"android.settings.TETHER_SETTINGS".equals(i.getAction()))i=new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            if(!canOpen(i)){toast("Cette page n’est pas disponible sur la montre");return;}
        }
        try{activity.startActivity(i);}catch(RuntimeException ex){toast("Android n’a pas pu ouvrir cette page");}
    }
    public int volume(){try{return audio==null?0:audio.getStreamVolume(AudioManager.STREAM_MUSIC);}catch(RuntimeException e){return 0;}}
    public int volumeMax(){try{return audio==null?15:Math.max(1,audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));}catch(RuntimeException e){return 15;}}
    public void setVolume(int value){try{if(audio!=null)audio.setStreamVolume(AudioManager.STREAM_MUSIC,Math.max(0,Math.min(volumeMax(),value)),0);}catch(RuntimeException e){toast("Le volume n’a pas pu être modifié");}}
    public int brightness(){
        float local=activity.getWindow().getAttributes().screenBrightness;
        if(local>=0)return Math.round(local*100);
        return Math.round(Settings.System.getInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128)*100f/255f);
    }
    public String brightnessLabel(){
        if(activity.getWindow().getAttributes().screenBrightness>=0)return "Luminosité PRISME";
        return Settings.System.canWrite(activity)?"Luminosité montre":"Luminosité PRISME";
    }
    public void setBrightness(int percent){
        int safe=Math.max(5,Math.min(100,percent));
        try {
            if(Settings.System.canWrite(activity)) {
                int previousMode=Settings.System.getInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                int previousValue=Settings.System.getInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128);
                boolean valueWritten=false,committed=false;
                try{
                    valueWritten=Settings.System.putInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.max(8,Math.round(safe*255f/100f)));
                    boolean modeWritten=valueWritten&&(previousMode==Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL||Settings.System.putInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL));
                    if(valueWritten&&modeWritten){
                        committed=true;WindowManager.LayoutParams p=activity.getWindow().getAttributes();p.screenBrightness=-1;activity.getWindow().setAttributes(p);return;
                    }
                }finally{
                    if(valueWritten&&!committed){
                        try{Settings.System.putInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,previousValue);}catch(RuntimeException ignored){}
                        try{Settings.System.putInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,previousMode);}catch(RuntimeException ignored){}
                    }
                }
            }
        } catch(RuntimeException ignored) { }
        WindowManager.LayoutParams p=activity.getWindow().getAttributes();p.screenBrightness=safe/100f;activity.getWindow().setAttributes(p);
    }
    public String statusText(){
        String time=new SimpleDateFormat("HH:mm",I18n.locale()).format(new Date());
        try{
            Intent b=activity.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(b!=null){int level=b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),max=b.getIntExtra(BatteryManager.EXTRA_SCALE,0);
                if(level>=0&&max>0)return time+"  ·  Batterie "+Math.round(level*100f/max)+" %";}
        }catch(RuntimeException ignored){}
        return time;
    }
    public void onHostPause(){
        // Intentional radio and global-setting changes remain; no background work is started.
    }
    private void toast(String text){I18n.makeToast(activity,text,Toast.LENGTH_SHORT).show();}
}

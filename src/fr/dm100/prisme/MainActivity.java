package fr.dm100.prisme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.graphics.Bitmap;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Native home screen and optional user-controlled global volume module. */
public final class MainActivity extends Activity implements DashboardView.Listener, ControlCenterController.Host {
    private static final int INK=0xffeaf4ff, MUTED=0xffa3b4c8, SURFACE=0xff192638, BG=0xff0b1220, ACCENT=0xff82ead2;
    private static final int EXPORT=71, IMPORT=72, CAPTURE=73;
    private DashboardPrefs prefs;
    private DashboardView board;
    private ExperienceController experience;
    private int activityLanguage;
    private FrameLayout mainLayer;
    private ControlCenterPrefs controlPrefs;
    private ControlCenterController controlController;
    private ControlCenterPanel controlPanel;
    private ControlCenterSettings controlSettings;
    private BatteryIndicatorView dashboardBattery;
    private MiniMusicPlayer dashboardPlayer;
    private WatchWidgetsOverlay watchWidgets;
    private MusicPrefs musicPrefs;
    private FloatingVolumePrefs floatingPrefs;
    private FloatingVolumeSettings floatingSettings;
    private View screenLamp;
    private float lampPreviousBrightness=-1;
    private String pendingCaptureName;
    private AppRepository repository;
    private final List<AppEntry> apps = new ArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean visible, registered;
    private long stopwatchStart, stopwatchAccumulated;
    private boolean stopwatchRunning;
    private long timerDeadline;
    private AlertDialog timerDialog, stopwatchDialog;
    private AlertDialog settingsDialog;
    private ScrollView settingsScroll;
    private LinearLayout settingsBody, expandedOptions;
    private Runnable shortcutPickerRefresh;
    private final List<Button> settingsTabs = new ArrayList<>();
    private final int[] settingsScrollY = new int[8];
    private int settingsTab;
    private boolean cancelSettingsRestore;
    private static final String[] SETTINGS_TABS = {"Écran", "Style", "Raccourcis", "Applications", "Outils", "Batterie", "Musique", "Profils"};
    private static final String[] SHORTCUT_DEFAULTS = {"apps", "styles", "search", "tools", "customize"};
    private static final String[] SHORTCUT_ACTIONS = {"apps", "styles", "search", "favorites", "tools", "notes", "timer", "focus", "stopwatch", "workspaces", "clock", "customize", "controlCenter", "controlCenterSettings", "floatingVolumeSettings", "today", "checklist", "deskclock", "presets", "device", "settingsSearch", "rotationSettings", "language"};
    private static final String[] SHORTCUT_NAMES = {"Toutes les applications", "Choisir un style PRISME", "Rechercher une application", "Filtrer les favoris", "Centre de contrôle", "Carnet local", "Minuteur", "Concentration", "Chronomètre", "Espaces de travail", "Cadran de la montre", "Réglages PRISME", "Centre de contrôle", "Personnaliser le centre", "Pastille de volume intégrée", "Aujourd’hui", "Liste de tâches", "Horloge de table", "Réglages rapides", "État de la montre", "Trouver un réglage", "Orientation globale", "Langue / Language"};
    private TextView timerDisplay, stopwatchDisplay;
    private final List<java.lang.ref.WeakReference<AlertDialog>> ownedDialogs=new ArrayList<>();
    private final Runnable mediaTick = new Runnable(){@Override public void run(){
        if(!visible)return;
        if(dashboardPlayer!=null)dashboardPlayer.update();
        main.postDelayed(this,2500);
    }};
    private final Runnable tick = new Runnable(){ public void run(){
        if(!visible)return;
        updateSnapshot();
        if(SenseEngine.apply(MainActivity.this,prefs))refreshBoard();
        checkTimer();
        main.postDelayed(this,60000-System.currentTimeMillis()%60000);
    }};
    private final Runnable timerTick = new Runnable(){ public void run(){
        if(!visible)return;
        updateTimerLabel(); checkTimer();
        if(timerDeadline>0)main.postDelayed(this,1000);
    }};
    private final Runnable stopwatchTick = new Runnable(){ public void run(){
        if(stopwatchDisplay!=null)stopwatchDisplay.setText(formatChrono(stopwatchElapsed()));
        if(visible && stopwatchDialog!=null && stopwatchDialog.isShowing() && stopwatchRunning)
            main.postDelayed(this,250);
    }};
    private final BroadcastReceiver changes = new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){
            String a=i.getAction();
            if(Intent.ACTION_PACKAGE_ADDED.equals(a)||Intent.ACTION_PACKAGE_REMOVED.equals(a)||Intent.ACTION_PACKAGE_CHANGED.equals(a))loadApps();
            else updateSnapshot();
        }
    };

    @Override protected void attachBaseContext(Context base){super.attachBaseContext(I18n.wrap(base));}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);I18n.init(this);activityLanguage=I18n.selected(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prefs=new DashboardPrefs(this);
        musicPrefs=new MusicPrefs(this);
        repository=new AppRepository(this);
        SafetyBackup.once(this,prefs);
        experience=new ExperienceController(this,prefs,apps,new ExperienceController.Host(){
            public AlertDialog panel(String title,View view,boolean fixed){OwnedBuilder b=new OwnedBuilder().setTitle(title).setNegativeButton("Fermer",null);if(fixed)b.setFixedView(view);else b.setView(view);return b.show();}
            public void action(String action){dismissOwnedDialogs();onAction(action);}
            public void app(AppEntry app){dismissOwnedDialogs();onApp(app);}
            public void settings(int tab){dismissOwnedDialogs();showSettings(tab);}
            public void changed(){profileImported();}
        });
        SharedPreferences utility=getSharedPreferences("prisme_utilities",MODE_PRIVATE);
        stopwatchAccumulated=utility.getLong("stopwatchAccumulated",0);
        stopwatchRunning=utility.getBoolean("stopwatchRunning",false);
        stopwatchStart=utility.getLong("stopwatchStart",0);
        int boot=currentBoot();
        if(boot<0||utility.getInt("stopwatchBoot",-2)!=boot||stopwatchStart>SystemClock.elapsedRealtime()){
            stopwatchRunning=false;stopwatchAccumulated=utility.getLong("stopwatchCheckpoint",stopwatchAccumulated);
        }
        timerDeadline=TimerEngine.deadline(this);TimerEngine.schedule(this);
        controlPrefs=prefs.getControlCenter();floatingPrefs=prefs.getFloatingVolume();
        controlController=new ControlCenterController(this,this);
        board=new DashboardView(this,prefs,this);
        mainLayer=new FrameLayout(this);
        mainLayer.addView(board,new FrameLayout.LayoutParams(-1,-1));
        watchWidgets=new WatchWidgetsOverlay(this,action->onAction(action));
        mainLayer.addView(watchWidgets,new FrameLayout.LayoutParams(-1,-1));
        controlPanel=new ControlCenterPanel(this,controlPrefs,controlController,new ControlCenterPanel.Listener(){
            public void onCustomize(){showControlCenterSettings();}
            public void onClosed(){board.requestFocus();}
        });
        mainLayer.addView(controlPanel,new FrameLayout.LayoutParams(-1,-1));
        setContentView(mainLayer);I18n.bind(mainLayer);
        watchWidgets.refresh();
        applyWindowPrefs();
        refreshDashboardOverlays();
        requestVolumeSettings(getIntent());requestUtilityAction(getIntent());requestControlCenterAction(getIntent());
        SharedPreferences uiTabs=getSharedPreferences("prisme_ui",MODE_PRIVATE);
        int oldTab=uiTabs.getInt("settingsTab",0);
        if(!uiTabs.getBoolean("settingsTabsV4",false)){
            if(oldTab==5)oldTab=7;
            uiTabs.edit().putBoolean("settingsTabsV4",true).putInt("settingsTab",oldTab).apply();
        }
        settingsTab=Math.max(0,Math.min(SETTINGS_TABS.length-1,oldTab));
        cancelSettingsRestore=getIntent()!=null&&(getIntent().hasCategory(Intent.CATEGORY_HOME)||getIntent().getBooleanExtra("volumeSettings",false));
        if(state!=null){
            pendingCaptureName=state.getString("pendingCaptureName");
            if(pendingCaptureFile()==null)pendingCaptureName=null;
            settingsTab=Math.max(0,Math.min(SETTINGS_TABS.length-1,state.getInt("settingsTab",settingsTab)));
            int[] savedScroll=state.getIntArray("settingsScrollY");
            if(savedScroll!=null)System.arraycopy(savedScroll,0,settingsScrollY,0,Math.min(savedScroll.length,settingsScrollY.length));
            if(state.getBoolean("settingsOpen",false))board.post(()->{
                Intent current=getIntent();
                if(!isFinishing()&&!cancelSettingsRestore&&(current==null||!current.hasCategory(Intent.CATEGORY_HOME)))showSettings();
            });
        }
    }
    @Override protected void onSaveInstanceState(Bundle state){
        captureSettingsScroll();
        state.putInt("settingsTab",settingsTab);state.putIntArray("settingsScrollY",settingsScrollY);
        state.putBoolean("settingsOpen",settingsDialog!=null&&settingsDialog.isShowing());
        state.putString("pendingCaptureName",pendingCaptureName);
        super.onSaveInstanceState(state);
    }
    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);setIntent(intent);
        requestVolumeSettings(intent);requestUtilityAction(intent);requestControlCenterAction(intent);
        if(intent!=null&&intent.hasCategory(Intent.CATEGORY_HOME)){
            MusicPlayback.prepareHomeTransition(this);
            cancelSettingsRestore=true;
            closeControlExtras();
            for(int n=ownedDialogs.size()-1;n>=0;n--){AlertDialog d=ownedDialogs.get(n).get();if(d!=null&&d.isShowing())d.dismiss();}
            ownedDialogs.clear();
        }
        refreshBoard();
    }
    @Override protected void onResume(){
        super.onResume();visible=true;if(experience!=null)experience.resume();timerDeadline=TimerEngine.deadline(this);
        SenseEngine.apply(this,prefs);board.onHostResume();if(watchWidgets!=null)watchWidgets.onHostResume();
        if(!registered){
            IntentFilter common=new IntentFilter();common.addAction(Intent.ACTION_TIME_CHANGED);common.addAction(Intent.ACTION_TIMEZONE_CHANGED);common.addAction(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(changes,common);
            IntentFilter packages=new IntentFilter();packages.addAction(Intent.ACTION_PACKAGE_ADDED);packages.addAction(Intent.ACTION_PACKAGE_REMOVED);packages.addAction(Intent.ACTION_PACKAGE_CHANGED);packages.addDataScheme("package");
            registerReceiver(changes,packages);registered=true;
        }
        main.removeCallbacks(tick);main.post(tick);
        refreshDashboardOverlays();main.removeCallbacks(mediaTick);main.post(mediaTick);
        main.removeCallbacks(timerTick);if(timerDeadline>0)main.post(timerTick);
        main.removeCallbacks(stopwatchTick);
        if(stopwatchRunning&&stopwatchDialog!=null&&stopwatchDialog.isShowing())main.post(stopwatchTick);
        loadApps();refreshBoard();
        if(GlobalRotationService.mode(this)>0&&!GlobalRotationService.isRunning())GlobalRotationService.apply(this,GlobalRotationService.mode(this));
        if(floatingPrefs!=null&&(fr.dm100.prisme.volume.FloatingVolumeService.isRunning()||floatingPrefs.mayResume(currentBoot())))fr.dm100.prisme.volume.FloatingVolumeService.refresh(this);
        ControlCenterService.refresh(this);
        EdgeDockService.refresh(this);
    }
    @Override protected void onPause(){
        main.removeCallbacks(mediaTick);
        visible=false;if(watchWidgets!=null)watchWidgets.onHostPause();if(experience!=null)experience.pause();closeScreenLamp();if(controlPanel!=null)controlPanel.onHostPause();if(controlController!=null)controlController.onHostPause();board.onHostPause();main.removeCallbacks(tick);main.removeCallbacks(timerTick);main.removeCallbacks(stopwatchTick);
        if(registered){unregisterReceiver(changes);registered=false;}
        saveUtilities();super.onPause();
    }
    @Override protected void onDestroy(){
        for(java.lang.ref.WeakReference<AlertDialog> ref:ownedDialogs){AlertDialog d=ref.get();if(d!=null&&d.isShowing())d.dismiss();}
        ownedDialogs.clear();if(experience!=null)experience.shutdown();closeControlExtras();repository.close();main.removeCallbacksAndMessages(null);super.onDestroy();
    }
    @Override public void onConfigurationChanged(Configuration c){
        super.onConfigurationChanged(c);board.refresh();if(watchWidgets!=null)watchWidgets.refresh();refreshDashboardOverlays();if(controlPanel!=null)controlPanel.refresh();if(controlSettings!=null&&controlSettings.isShowing())controlSettings.refit();if(floatingSettings!=null&&floatingSettings.isShowing())floatingSettings.refit();
        for(java.lang.ref.WeakReference<AlertDialog> ref:ownedDialogs){
            AlertDialog d=ref.get();if(d instanceof OwnedDialog&&d.isShowing())((OwnedDialog)d).fitWindow();
        }
    }
    @Override public void onBackPressed(){
        if(screenLamp!=null){closeScreenLamp();return;}
        if(controlPanel!=null&&controlPanel.isOpen()){controlPanel.close();return;}
        if(controlSettings!=null&&controlSettings.isShowing()){controlSettings.dismiss();return;}
        if(floatingSettings!=null&&floatingSettings.isShowing()){floatingSettings.dismiss();return;}
        if(prefs.getBoolean("favoritesOnly",false)||prefs.getInt("workspace",0)!=0){prefs.putBoolean("favoritesOnly",false);prefs.putInt("workspace",0);refreshBoard();}
        else originalClock();
    }
    private void loadApps(){repository.load(new AppRepository.Callback(){public void loaded(List<AppEntry> loaded){
        if(isFinishing())return;apps.clear();apps.addAll(loaded);refreshBoard();for(ThemePreview preview:stylePreviews)preview.refreshPreview();if(experience!=null)experience.refreshApps();
        if(settingsDialog!=null&&settingsDialog.isShowing()&&settingsTab==2)renderSettingsTab();
        if(controlSettings!=null&&controlSettings.isShowing())controlSettings.setApps(apps);
        if(shortcutPickerRefresh!=null)shortcutPickerRefresh.run();
        if(pendingStyleExport){pendingStyleExport=false;exportStyleGallery();}
    }});}
    private void refreshBoard(){
        I18n.refreshLabels(this);
        if(controlController!=null)controlController.setApps(apps);
        List<AppEntry> visibleApps=new ArrayList<>();int workspace=prefs.getInt("workspace",0);
        Set<String> selected=workspace>0?prefs.getSet("workspace"+workspace):Collections.<String>emptySet();
        for(AppEntry a:apps)if(workspace==0||selected.contains(a.id))visibleApps.add(a);
        board.setShortcutCatalog(apps);board.setApps(visibleApps);board.refresh();if(watchWidgets!=null)watchWidgets.refresh();updateSnapshot();applyWindowPrefs();
    }
    private void updateSnapshot(){
        if(board!=null)board.setSnapshot(DeviceSnapshot.read(this));
        if(dashboardBattery!=null){
            BatteryLevelReader.Reading now=BatteryLevelReader.read(this,null);
            dashboardBattery.update(now.level,now.charging);
        }
    }
    /** Widgets shared by all dashboard styles, beneath the control panel. */
    private void refreshDashboardOverlays(){
        if(mainLayer==null||controlPanel==null)return;
        if(dashboardBattery!=null){mainLayer.removeView(dashboardBattery);dashboardBattery=null;}
        if(dashboardPlayer!=null){mainLayer.removeView(dashboardPlayer);dashboardPlayer=null;}
        Point screenSize=screen();
        if(screenSize.x<=0||screenSize.y<=0)return;
        BatteryIndicatorPrefs bp=new BatteryIndicatorPrefs(this);
        if(bp.displayMode()==1){
            dashboardBattery=new BatteryIndicatorView(this,bp);
            dashboardBattery.setBackgroundHint(ThemeSpec.get(prefs.getInt("theme",0)).bg);
            BatteryLevelReader.Reading state=BatteryLevelReader.read(this,null);
            dashboardBattery.update(state.level,state.charging);
            int width=Math.min(screenSize.x,dashboardBattery.desiredWidth());
            int height=Math.min(screenSize.y,dashboardBattery.desiredHeight());
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(width,height,Gravity.LEFT|Gravity.TOP);
            lp.leftMargin=Math.round((screenSize.x-width)*bp.integer("x",100)/100f);
            lp.topMargin=Math.round((screenSize.y-height)*bp.integer("y",0)/100f);
            dashboardBattery.setAlpha(bp.integer("opacity",100)/100f);
            mainLayer.addView(dashboardBattery,Math.max(1,mainLayer.indexOfChild(controlPanel)),lp);
        }
        if(musicPrefs.flag("enabled",true)){
            dashboardPlayer=new MiniMusicPlayer(this,musicPrefs);
            int width=Math.min(screenSize.x,dashboardPlayer.desiredWidth());
            int height=Math.min(screenSize.y,dashboardPlayer.desiredHeight());
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(width,height,Gravity.LEFT|Gravity.TOP);
            lp.leftMargin=Math.round((screenSize.x-width)*musicPrefs.number("x",50)/100f);
            lp.topMargin=Math.round((screenSize.y-height)*musicPrefs.number("y",87)/100f);
            mainLayer.addView(dashboardPlayer,Math.max(1,mainLayer.indexOfChild(controlPanel)),lp);
            dashboardPlayer.update();
        }
    }
    private void applyWindowPrefs(){
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if(prefs.getBoolean("keepScreenOn",false))getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void haptic(){if(prefs.getBoolean("haptic",true)){try{Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator())v.vibrate(12);}catch(RuntimeException ignored){}}}
    @Override public void onApp(AppEntry app){
        haptic();
        Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if(launch(i)){prefs.putInt("launch:"+app.id,(int)(System.currentTimeMillis()/1000));board.refresh();}
    }
    @Override public void onAppLongPress(final AppEntry app){
        haptic();
        String[] choices={prefs.isFavorite(app.id)?"Retirer des favoris":"Ajouter aux favoris","Renommer sur le bureau","Choisir les espaces de travail","Masquer du bureau","Fiche Android de l’application","Ouvrir"};
        new OwnedBuilder().setTitle(label(app)).setItems(choices,(d,n)->{
            if(n==0){prefs.toggleSet("favorites",app.id);refreshBoard();}
            else if(n==1)rename(app);
            else if(n==2)assignWorkspace(app);
            else if(n==3){if(!prefs.isHidden(app.id))prefs.toggleSet("hidden",app.id);refreshBoard();toast("Application masquée · Réglages pour la retrouver");}
            else if(n==4)launch(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+app.packageName)));
            else onApp(app);
        }).setNegativeButton("Fermer",null).show();
    }
    @Override public void onAction(String action){
        if(action==null)return;
        if("language".equals(action)){showLanguagePicker();return;}
        if("studio".equals(action)){WatchStudio.open(this,prefs,()->{if(watchWidgets!=null)watchWidgets.refresh();},()->{refreshBoard();refreshDashboardOverlays();});return;}
        if("sense".equals(action)){SenseEngine.open(this,prefs,()->{SenseEngine.apply(this,prefs);refreshBoard();});return;}
        if("edgeDock".equals(action)){EdgeDockService.settings(this,()->EdgeDockService.refresh(this));return;}
        if("doctor".equals(action)){PrismeDoctor.show(this);return;}
        if("shortcutShelf".equals(action)){showShortcutShelf();return;}
        if("exportStyleGallery".equals(action)){exportStyleGallery();return;}
        if("rotationSettings".equals(action)){showRotationSettings();return;}
        if("rotateGlobal".equals(action)){toggleGlobalRotation();return;}
        if("batteryIndicatorSettings".equals(action)){showBatteryIndicatorSettings();return;}
        if("musicPlayerSettings".equals(action)){showMusicPlayerSettings();return;}
        if(action.startsWith("testColor:")){try{showTestColor(Integer.parseInt(action.substring(10)));}catch(NumberFormatException ignored){}return;}
        if("androidBattery".equals(action)){launch(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));return;}
        if("androidDisplay".equals(action)){launch(new Intent(Settings.ACTION_DISPLAY_SETTINGS));return;}
        if(experience!=null&&experience.open(action))return;
        if(action.startsWith("app:")){
            AppEntry app=shortcutApp(action);
            if(app!=null){dismissOwnedDialogs();onApp(app);return;}
            toast("Application indisponible · modifie ce raccourci");return;
        }
        if(action.startsWith("editShortcut:")){
            try{int slot=Integer.parseInt(action.substring(13));if(slot>=0&&slot<DashboardPrefs.SLOT_COUNT)showShortcutEditor(slot);}catch(NumberFormatException ignored){}
            return;
        }
        haptic();
        switch(action){
            case "styles":showStyles();break;
            case "customize":showSettings();break;
            case "shortcuts":showSettings(2);break;
            case "search":experience.search(false);break;
            case "apps":showApps(false);break;
            case "favorites":prefs.putBoolean("favoritesOnly",!prefs.getBoolean("favoritesOnly",false));refreshBoard();toast(prefs.getBoolean("favoritesOnly",false)?"Favoris uniquement":"Toutes les applications");break;
            case "home":case "clock":originalClock();break;
            case "tools":if(controlPrefs.getBoolean("enabled",true))openControlCenter(false);else showTools();break;
            case "controlCenter":openControlCenter(false);break;
            case "controlCenterSettings":showControlCenterSettings();break;
            case "floatingVolumeSettings":showFloatingVolumeSettings();break;
            case "notes":showNotes();break;
            case "timer":showTimer(false);break;
            case "focus":showTimer(true);break;
            case "stopwatch":showStopwatch();break;
            case "workspaces":showWorkspaces();break;
            default:showHelp();break;
        }
    }
    private String label(AppEntry app){String custom=prefs.getCustomLabel(app.id);return custom==null||custom.trim().isEmpty()?app.label:custom;}
    private void rename(AppEntry app){
        EditText input=edit(label(app));input.setSingleLine(true);input.setSelectAllOnFocus(true);
        dialog("Nom de l’icône",input,"Enregistrer",(d,w)->{String name=input.getText().toString().trim();prefs.setCustomLabel(app.id,name.length()>40?name.substring(0,40):name);refreshBoard();});
    }
    private void assignWorkspace(AppEntry app){
        String[] names={"Travail","Loisirs","Outils"};boolean[] checks=new boolean[3];
        for(int i=0;i<3;i++)checks[i]=prefs.getSet("workspace"+(i+1)).contains(app.id);
        new OwnedBuilder().setTitle("Espaces · "+label(app)).setMultiChoiceItems(names,checks,(d,n,checked)->{
            boolean before=prefs.getSet("workspace"+(n+1)).contains(app.id);if(before!=checked)prefs.toggleSet("workspace"+(n+1),app.id);
        }).setPositiveButton("Terminé",(d,w)->refreshBoard()).show();
    }
    private void showWorkspaces(){
        String[] names={"Tout le bureau","Travail","Loisirs","Outils"};
        new OwnedBuilder().setTitle("Espaces de travail").setSingleChoiceItems(names,prefs.getInt("workspace",0),(d,n)->{
            prefs.putInt("workspace",n);refreshBoard();d.dismiss();if(n>0&&prefs.getSet("workspace"+n).isEmpty())toast("Appui long sur une application pour l’ajouter à cet espace");
        }).setNegativeButton("Fermer",null).show();
    }

    private void showStyles(){
        stylePreviews.clear();
        ScrollView scroll=scroll();LinearLayout body=column();scroll.addView(body);
        body.addView(text(ThemeSpec.ALL.length+" univers, "+ThemeSpec.ALL.length+" façons d’utiliser ta montre",17,INK));
        body.addView(text("Le style règle sa disposition, ses couleurs et ses tailles conseillées. Les favoris et les notes sont conservés.",14,MUTED));
        addButton(body,"Exporter les 22 aperçus",()->{if(styleDialog!=null)styleDialog.dismiss();exportStyleGallery();});
        final View[] premiumHeader=new View[1],newHeader=new View[1];
        addButton(body,"Quotidien · Nova et Silence",()->{if(newHeader[0]!=null)scroll.smoothScrollTo(0,newHeader[0].getTop());});
        if(ThemeSpec.ALL.length>12)addButton(body,"Montres premium · "+Math.min(8,ThemeSpec.ALL.length-12)+" styles",()->{if(premiumHeader[0]!=null)scroll.smoothScrollTo(0,premiumHeader[0].getTop());});
        for(int row=0;row<(ThemeSpec.ALL.length+1)/2;row++){
            if(row*2==12){TextView premium=text("MONTRES PREMIUM · COLLECTION SIGNATURE",17,ACCENT);premium.setPadding(4,22,4,12);premiumHeader[0]=premium;body.addView(premium);}
            if(row*2==20){TextView heading=text("QUOTIDIEN · PRISME 5",18,ACCENT);newHeader[0]=heading;body.addView(heading);}
            LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
            for(int col=0;col<2;col++){
                int index=row*2+col;
                if(index>=ThemeSpec.ALL.length){line.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));continue;}
                final ThemeSpec t=ThemeSpec.ALL[index];
                LinearLayout card=column();card.setPadding(8,8,8,8);card.setBackgroundColor(t.surface);
                if(t.id==prefs.getInt("theme",0)){android.graphics.drawable.GradientDrawable selected=new android.graphics.drawable.GradientDrawable();selected.setColor(t.surface);selected.setCornerRadius(8);selected.setStroke(2,t.accent);card.setBackground(selected);card.setContentDescription(t.name+" · style actif");}
                card.addView(new ThemePreview(this,t),new LinearLayout.LayoutParams(-1,Math.max(100,Math.round((screen().x-64)*.375f))));
                TextView title=text(t.name,17,t.text);title.setTypeface(null,Typeface.BOLD);card.addView(title);
                card.addView(text(t.subtitle,12,t.muted));
                card.setOnClickListener(v->{applyTheme(t);if(styleDialog!=null)styleDialog.dismiss();});
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(3,4,3,4);line.addView(card,lp);
            }
            body.addView(line);
        }
        styleDialog=new OwnedBuilder().setTitle("Styles PRISME").setView(scroll)
                .setNegativeButton("Retour au bureau",null).setPositiveButton("Cadran montre",(d,w)->originalClock()).show();
    }
    private AlertDialog styleDialog;
    private final List<ThemePreview> stylePreviews=new ArrayList<>();
    private void applyTheme(ThemeSpec t){
        prefs.putInt("theme",t.id);prefs.putInt("columns",t.recommendedColumns);
        prefs.putInt("iconSize",t.recommendedIconSize);prefs.putInt("textSize",t.recommendedTextSize);
        prefs.putInt("font",0);prefs.putInt("accentIndex",0);prefs.putBoolean("favoritesOnly",false);
        refreshBoard();toast(t.name+" appliqué");
    }
    /** One settings window, a fixed category bar, and a scroll area for that category only. */
    private void showSettings(){showSettings(settingsTab);}
    private void showSettings(int tab){
        int safe=Math.max(0,Math.min(SETTINGS_TABS.length-1,tab));
        if(settingsDialog!=null&&settingsDialog.isShowing()){selectSettingsTab(safe);return;}
        settingsTab=safe;
        LinearLayout shell=column();shell.setPadding(0,0,0,0);
        LinearLayout tabs=column();tabs.setPadding(0,0,0,4);settingsTabs.clear();
        android.widget.HorizontalScrollView strip=new android.widget.HorizontalScrollView(this);strip.setHorizontalScrollBarEnabled(true);
        LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
        for(int index=0;index<SETTINGS_TABS.length;index++){
            final int chosen=index;Button tabButton=button(SETTINGS_TABS[index]);tabButton.setTextSize(TypedValue.COMPLEX_UNIT_PX,14);tabButton.setSingleLine(true);tabButton.setPadding(2,0,2,0);
            tabButton.setOnClickListener(v->selectSettingsTab(chosen));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(98,46);lp.setMargins(1,0,1,0);
            line.addView(tabButton,lp);settingsTabs.add(tabButton);
        }
        strip.addView(line);tabs.addView(strip,new LinearLayout.LayoutParams(-1,48));
        Button find=button("⌕ Trouver un réglage");find.setOnClickListener(v->{closeSettings();experience.search(true);});tabs.addView(find,new LinearLayout.LayoutParams(-1,44));
        shell.addView(tabs,new LinearLayout.LayoutParams(-1,-2));
        settingsScroll=scroll();settingsScroll.setFillViewport(false);
        settingsBody=column();settingsBody.setPadding(4,4,6,12);settingsScroll.addView(settingsBody);
        shell.addView(settingsScroll,new LinearLayout.LayoutParams(-1,0,1));
        settingsDialog=new OwnedBuilder().setTitle("Réglages PRISME").setFixedView(shell)
                .setNegativeButton("Fermer",null).setPositiveButton("Cadran montre",(d,w)->originalClock()).create();
        final AlertDialog opened=settingsDialog;
        settingsDialog.setOnDismissListener(d->{
            captureSettingsScroll();
            if(settingsDialog==opened){settingsDialog=null;settingsScroll=null;settingsBody=null;settingsTabs.clear();expandedOptions=null;}
        });
        settingsDialog.show();renderSettingsTab();
    }
    private void captureSettingsScroll(){if(settingsScroll!=null)settingsScrollY[settingsTab]=settingsScroll.getScrollY();}
    private void selectSettingsTab(int tab){
        if(tab<0||tab>=SETTINGS_TABS.length||settingsBody==null)return;
        captureSettingsScroll();settingsTab=tab;
        getSharedPreferences("prisme_ui",MODE_PRIVATE).edit().putInt("settingsTab",tab).apply();
        renderSettingsTab();
    }
    private void renderSettingsTab(){
        if(settingsBody==null)return;
        settingsBody.removeAllViews();expandedOptions=null;
        for(int i=0;i<settingsTabs.size();i++){
            Button tab=settingsTabs.get(i);boolean active=i==settingsTab;
            tab.setTextColor(active?BG:INK);tab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(active?ACCENT:SURFACE));
            if(active)tab.post(()->{
                if(tab.isAttachedToWindow()&&tab.getParent() instanceof View){
                    android.view.ViewParent parent=((View)tab.getParent()).getParent();
                    if(parent instanceof android.widget.HorizontalScrollView){
                        android.widget.HorizontalScrollView strip=(android.widget.HorizontalScrollView)parent;
                        strip.scrollTo(Math.max(0,tab.getLeft()-(strip.getWidth()-tab.getWidth())/2),0);
                    }
                }
            });
            tab.setSelected(active);tab.setContentDescription(SETTINGS_TABS[i]+(active?" · catégorie active":" · ouvrir la catégorie"));
        }
        LinearLayout b=settingsBody;
        switch(settingsTab){
            case 0:
                ViewportLayout layout=board.getViewportLayout();
                settingsIntro(b,"Taille et plein écran",layout==null?"Adapter le contenu de PRISME.":"Surface fournie par Android : "+Math.round(layout.surfaceWidth)+" × "+Math.round(layout.surfaceHeight)+" px. Ce chiffre ne mesure pas les zones physiques noires.");
                addButton(b,"Panneaux bord à bord et zoom 110 %",()->{
                    prefs.putInt("styleScale",110);prefs.putInt("contentInset",0);prefs.putInt("fitMode",0);prefs.putBoolean("volumeRail",false);
                    prefs.putBoolean("edgeCards",true);prefs.putBoolean("compactNavigation",true);
                    refreshBoard();renderSettingsTab();toast("Bord à bord appliqué · zoom 110 %");
                });
                slider(b,"Taille globale de PRISME (%)",75,140,prefs.getInt("styleScale",100),n->{prefs.putInt("styleScale",n);refreshBoard();});
                b.addView(text("75 % : plus d’éléments · 100 % : normal · 140 % : plus grand. Les grilles se réorganisent sans couper la navigation.",13,MUTED));
                toggle(b,"Étendre les panneaux décoratifs jusqu’aux bords","edgeCards",true);
                toggle(b,"Navigation compacte pour gagner de la place","compactNavigation",false);
                toggle(b,"Afficher aussi la barre des cinq raccourcis","styleDock",false);
                b.addView(text("Sans cette barre, maintiens Aujourd’hui ou ouvre Mes raccourcis. Les cinq emplacements personnalisés restent conservés.",13,MUTED));
                slider(b,"Marge intérieure (px)",0,24,prefs.getInt("contentInset",0),n->{prefs.putInt("contentInset",n);refreshBoard();});
                settingChoice(b,"Densité du style",new String[]{"Automatique","Compact","Grand confort"},prefs.getInt("fitMode",0),n->{prefs.putInt("fitMode",n);refreshBoard();});
                toggle(b,"Réserver une bande à droite pour le volume","volumeRail",false);
                b.addView(text("Laisse cette réserve désactivée : la pastille flottante n’a pas besoin de retirer une bande à PRISME.",13,MUTED));
                addButton(b,"Réglage standard à 100 %",()->{
                    prefs.putInt("styleScale",100);prefs.putInt("contentInset",0);prefs.putInt("fitMode",0);prefs.putBoolean("volumeRail",false);
                    prefs.putBoolean("edgeCards",false);prefs.putBoolean("compactNavigation",false);refreshBoard();renderSettingsTab();
                });
                settingsAction(b,"Diagnostic visuel de l’écran",()->onAction("displayTest"));
                addButton(b,"Voir le bureau",()->closeSettings());
                break;
            case 1:
                settingsIntro(b,"Style et lisibilité","Choisis un univers, puis ajuste ses icônes et ses couleurs.");
                settingsAction(b,"Langue / Language · "+I18n.NAMES[I18n.selected(this)],()->showLanguagePicker());
                settingsAction(b,"Choisir parmi les "+ThemeSpec.ALL.length+" styles",()->showStyles());
                slider(b,"Taille des icônes",52,100,prefs.getInt("iconSize",72),n->{prefs.putInt("iconSize",n);refreshBoard();});
                slider(b,"Taille des noms",12,24,prefs.getInt("textSize",16),n->{prefs.putInt("textSize",n);refreshBoard();});
                slider(b,"Colonnes",3,7,prefs.getInt("columns",5),n->{prefs.putInt("columns",n);refreshBoard();});
                slider(b,"Espacement",4,22,prefs.getInt("gap",12),n->{prefs.putInt("gap",n);refreshBoard();});
                settingChoice(b,"Forme des icônes",new String[]{"Selon le style","Rectangle arrondi","Cercle","Carré"},prefs.getInt("iconShape",0),n->{prefs.putInt("iconShape",n);refreshBoard();});
                settingChoice(b,"Police",new String[]{"Selon le style","Sans sérif","Monospace","Sérif"},prefs.getInt("font",0),n->{prefs.putInt("font",n);refreshBoard();});
                settingChoice(b,"Couleur d’accent",new String[]{"Selon le style","Cyan","Menthe","Orange","Violet","Rouge","Or"},prefs.getInt("accentIndex",0),n->{prefs.putInt("accentIndex",n);refreshBoard();});
                settingChoice(b,"Fond",new String[]{"Signature du style","Dégradé","Grille","Orbites","Particules fixes","Uni"},prefs.getInt("wallpaper",0),n->{prefs.putInt("wallpaper",n);refreshBoard();});
                toggle(b,"Afficher les noms des applications","labels",true);toggle(b,"Afficher l’heure","showClock",true);toggle(b,"Afficher la batterie","showBattery",true);
                break;
            case 2:
                settingsIntro(b,"Tes cinq raccourcis","Ils sont disponibles dans tous les styles. Un appui long sur un emplacement permet aussi de le modifier.");
                for(int i=0;i<DashboardPrefs.SLOT_COUNT;i++){
                    final int slot=i;String action=prefs.getShortcut(i,SHORTCUT_DEFAULTS[i]);
                    Button row=button((i+1)+" · "+shortcutName(action));row.setMinHeight(52);
                    row.setContentDescription("Modifier le raccourci "+(i+1)+" : "+shortcutName(action));
                    row.setOnClickListener(v->showShortcutEditor(slot));b.addView(row,new LinearLayout.LayoutParams(-1,56));
                }
                addButton(b,"Rétablir les cinq raccourcis",()->{
                    for(int i=0;i<DashboardPrefs.SLOT_COUNT;i++)prefs.setShortcut(i,null);
                    refreshBoard();renderSettingsTab();toast("Raccourcis d’origine rétablis");
                });
                break;
            case 3:
                settingsIntro(b,"Organiser les applications","Les applications restent installées. L’appui long sur une icône permet de la renommer et de choisir ses espaces.");
                toggle(b,"Favoris uniquement","favoritesOnly",false);toggle(b,"Afficher le dock de favoris","showDock",true);
                settingChoice(b,"Ordre des applications",new String[]{"A → Z","Z → A","Favoris d’abord","Récemment ouvertes depuis PRISME"},prefs.getInt("order",0),n->{prefs.putInt("order",n);refreshBoard();});
                settingsAction(b,"Voir toutes les applications",()->showApps(false));
                settingsAction(b,"Applications masquées",()->showHidden());settingsAction(b,"Espaces de travail",()->showWorkspaces());
                break;
            case 4:
                settingsIntro(b,"Confort et outils","Les utilitaires restent locaux et les réglages ci-dessous concernent PRISME.");
                toggle(b,"Transitions courtes","animations",true);toggle(b,"Vibration légère","haptic",true);
                toggle(b,"Garder l’écran allumé dans PRISME","keepScreenOn",false);
                b.addView(text("Cette option s’arrête lorsque tu quittes le bureau.",13,MUTED));
                settingsAction(b,"Ouvrir le centre de contrôle",()->openControlCenter(true));
                settingsAction(b,"Personnaliser le centre de contrôle",()->showControlCenterSettings());
                settingsAction(b,"Pastille de volume intégrée",()->showFloatingVolumeSettings());
                addButton(b,"Batterie · 24 styles et affichage global",()->selectSettingsTab(5));
                addButton(b,"Musique · lecteur et arrière-plan",()->selectSettingsTab(6));
                settingsAction(b,"Outils classiques",()->showTools());settingsAction(b,"Minuteur",()->showTimer(false));
                settingsAction(b,"Chronomètre",()->showStopwatch());settingsAction(b,"Carnet local",()->showNotes());
                settingsAction(b,"Aujourd’hui · favoris et récents",()->onAction("today"));
                settingsAction(b,"Liste de tâches",()->onAction("checklist"));settingsAction(b,"Horloge de table",()->onAction("deskclock"));
                settingsAction(b,"Réglages rapides de confort",()->onAction("presets"));settingsAction(b,"État de la montre",()->onAction("device"));
                settingsAction(b,"Orientation globale des applications",()->showRotationSettings());
                settingChoice(b,"Balayage vers le haut (hors listes)",new String[]{"Désactivé","Aujourd’hui","Recherche","Liste de tâches","Horloge de table","Studio","Sense","Dock","Doctor"},prefs.getInt("upGesture",1),n->prefs.putInt("upGesture",n));
                settingChoice(b,"Balayage vers le bas (hors listes)",new String[]{"Désactivé","Aujourd’hui","Recherche","Liste de tâches","Horloge de table","Studio","Sense","Dock","Doctor"},prefs.getInt("downGesture",0),n->prefs.putInt("downGesture",n));
                settingChoice(b,"Double appui sur une zone vide",new String[]{"Désactivé","Aujourd’hui","Centre de contrôle","Horloge de table","Recherche","Studio","Sense","Dock","Doctor"},prefs.getInt("blankDoubleTap",1),n->prefs.putInt("blankDoubleTap",n));
                toggle(b,"Garder l’horloge de table allumée","deskKeepAwake",false);
                settingsAction(b,"PRISME Studio · widgets tactiles",()->onAction("studio"));
                settingsAction(b,"PRISME Sense · styles automatiques",()->onAction("sense"));
                settingsAction(b,"Dock universel · raccourcis flottants",()->onAction("edgeDock"));
                settingsAction(b,"PRISME Doctor · diagnostic et RAM",()->onAction("doctor"));
                settingsAction(b,"Guide et gestes",()->showHelp());
                break;
            case 5:renderBatteryTab(b);break;
            case 6:renderMusicTab(b);break;
            default:
                settingsIntro(b,"Profils et retour à la montre","Les profils enregistrent le bureau, le centre de contrôle et l’apparence de la pastille. Son activation et sa relance au démarrage restent locales.");
                settingsAction(b,"Mes profils",()->showProfiles());settingsAction(b,"Exporter ma configuration",()->exportConfig());
                settingsAction(b,"Importer une configuration",()->importConfig());
                settingsAction(b,"Choisir l’accueil de la montre",()->showClockTargetChooser(false));
                b.addView(text("Le bouton Cadran montre ouvre cet accueil. Pour changer un cadran natif, effectue ensuite un appui long sur le cadran. Les styles PRISME se changent dans l’onglet Style.",13,MUTED));
                settingsAction(b,"Choisir le bureau Android par défaut",()->launch(new Intent(Settings.ACTION_HOME_SETTINGS)));
                settingsAction(b,"Affichage et luminosité Android",()->launch(new Intent(Settings.ACTION_DISPLAY_SETTINGS)));
                settingsAction(b,"Réinitialiser PRISME",()->new OwnedBuilder().setTitle("Réinitialiser ce bureau ?")
                        .setMessage("Le bureau, les raccourcis, favoris, noms, éléments masqués et le centre de contrôle seront remis à zéro. La pastille volume sera arrêtée et réinitialisée. Tes notes et tes profils enregistrés sont conservés.")
                        .setNegativeButton("Annuler",null).setPositiveButton("Réinitialiser",(d,w)->{resetDashboardSafely();}).show());
                break;
        }
        if(settingsScroll!=null){final ScrollView target=settingsScroll;final int y=settingsScrollY[settingsTab];target.post(()->{if(settingsScroll==target)target.scrollTo(0,y);});}
    }
    private void settingsIntro(LinearLayout b,String title,String explanation){
        TextView t=text(title,20,ACCENT);t.setTypeface(null,Typeface.BOLD);b.addView(t);b.addView(text(explanation,14,MUTED));
    }
    private void closeSettings(){if(settingsDialog!=null)settingsDialog.dismiss();}
    private void settingsAction(LinearLayout b,String label,Runnable action){addButton(b,label,()->{closeSettings();action.run();});}
    /** Inline expansion avoids another modal window over the settings on the watch. */
    private void settingChoice(LinearLayout b,String title,String[] options,int selected,IntChange change){
        b.addView(text(title,15,MUTED));final int[] chosen={Math.max(0,Math.min(options.length-1,selected))};
        Button current=button(options[chosen[0]]+"  ▾");b.addView(current,new LinearLayout.LayoutParams(-1,48));
        LinearLayout choices=column();choices.setPadding(8,2,8,4);choices.setVisibility(View.GONE);b.addView(choices);
        for(int i=0;i<options.length;i++){
            final int index=i;Button option=button(options[i]);option.setContentDescription(title+" : "+options[i]);
            option.setOnClickListener(v->{chosen[0]=index;change.change(index);current.setText(options[index]+"  ▾");choices.setVisibility(View.GONE);if(expandedOptions==choices)expandedOptions=null;});
            choices.addView(option,new LinearLayout.LayoutParams(-1,48));
        }
        current.setOnClickListener(v->{
            boolean open=choices.getVisibility()!=View.VISIBLE;
            if(expandedOptions!=null)expandedOptions.setVisibility(View.GONE);
            choices.setVisibility(open?View.VISIBLE:View.GONE);expandedOptions=open?choices:null;
        });
    }
    /** ComponentName equality accepts both package/.Activity and package/package.Activity. */
    private AppEntry shortcutApp(String action){
        if(action==null||!action.startsWith("app:")||!DashboardPrefs.isValidShortcutAction(action))return null;
        ComponentName component=ComponentName.unflattenFromString(action.substring(4));
        if(component==null)return null;
        for(AppEntry app:apps)if(app.component.equals(component))return app;
        return null;
    }
    private String shortcutName(String action){
        if("controlCenter".equals(action))return "Centre de contrôle";
        if("controlCenterSettings".equals(action))return "Personnaliser le centre";
        if("floatingVolumeSettings".equals(action))return "Pastille de volume intégrée";
        if("home".equals(action))return "Cadran de la montre";
        if("shortcuts".equals(action))return "Personnaliser les raccourcis";
        for(int i=0;i<SHORTCUT_ACTIONS.length;i++)if(SHORTCUT_ACTIONS[i].equals(action))return SHORTCUT_NAMES[i];
        if(action!=null&&action.startsWith("app:")){
            AppEntry app=shortcutApp(action);
            return app==null?"Application indisponible":label(app);
        }
        return "Applications";
    }
    private void showShortcutEditor(final int slot){
        if(slot<0||slot>=DashboardPrefs.SLOT_COUNT)return;
        closeSettings();
        ArrayList<String> choices=new ArrayList<>();choices.add("Choisir une application…");
        Collections.addAll(choices,SHORTCUT_NAMES);choices.add("Rétablir cet emplacement");
        new OwnedBuilder().setTitle("Raccourci "+(slot+1)+" · "+shortcutName(prefs.getShortcut(slot,SHORTCUT_DEFAULTS[slot])))
                .setItems(choices.toArray(new String[0]),(d,n)->{
                    if(n==0){showShortcutAppPicker(slot);return;}
                    if(n==choices.size()-1)prefs.setShortcut(slot,null);else prefs.setShortcut(slot,SHORTCUT_ACTIONS[n-1]);
                    refreshBoard();toast("Raccourci "+(slot+1)+" enregistré");
                }).setNegativeButton("Annuler",null).show();
    }
    private void showShortcutAppPicker(final int slot){
        LinearLayout body=column();body.setPadding(0,0,0,0);
        EditText query=edit("");query.setSingleLine(true);query.setHint("Rechercher une application");
        body.addView(query,new LinearLayout.LayoutParams(-1,48));
        ScrollView scroll=scroll();LinearLayout list=column();scroll.addView(list);
        body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        final AlertDialog picker=new OwnedBuilder().setTitle("Application du raccourci "+(slot+1)).setFixedView(body).setNegativeButton("Annuler",null).create();
        Runnable populate=()->{
            list.removeAllViews();String q=query.getText().toString().toLowerCase(I18n.locale()).trim();int count=0;
            for(AppEntry app:apps){
                if(!label(app).toLowerCase(I18n.locale()).contains(q))continue;
                Button row=button(label(app));row.setOnClickListener(v->{prefs.setShortcut(slot,"app:"+app.id);refreshBoard();picker.dismiss();toast("Raccourci enregistré : "+label(app));});
                list.addView(row,new LinearLayout.LayoutParams(-1,50));count++;
            }
            if(count==0)list.addView(text(apps.isEmpty()?"La liste se charge. Ferme puis réessaie dans un instant.":"Aucune application correspondante",15,MUTED));
        };
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){populate.run();}public void afterTextChanged(Editable e){}});
        shortcutPickerRefresh=populate;
        picker.setOnDismissListener(d->{if(shortcutPickerRefresh==populate)shortcutPickerRefresh=null;});
        populate.run();picker.show();
        picker.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    private void showHidden(){
        List<AppEntry> hidden=new ArrayList<>();for(AppEntry a:apps)if(prefs.isHidden(a.id))hidden.add(a);
        if(hidden.isEmpty()){toast("Aucune application masquée");return;}
        String[] names=new String[hidden.size()];boolean[] values=new boolean[hidden.size()];
        for(int i=0;i<names.length;i++){names[i]=label(hidden.get(i));values[i]=true;}
        new OwnedBuilder().setTitle("Décocher pour réafficher").setMultiChoiceItems(names,values,(d,n,checked)->{
            String id=hidden.get(n).id;if(prefs.isHidden(id)!=checked)prefs.toggleSet("hidden",id);refreshBoard();
        }).setPositiveButton("Terminé",null).show();
    }
    private void showApps(boolean focusSearch){
        LinearLayout body=column();EditText query=edit("");query.setSingleLine(true);query.setHint("Chercher une application…");body.addView(query);
        ScrollView scroll=scroll();LinearLayout list=column();scroll.addView(list);
        body.addView(scroll,new LinearLayout.LayoutParams(-1,Math.max(150,screen().y-170)));
        AlertDialog drawer=dialog("Applications",body,null,null);
        Runnable populate=()->{
            list.removeAllViews();String q=query.getText().toString().toLowerCase(I18n.locale()).trim();int count=0;
            for(final AppEntry a:apps){
                if(!SearchIndex.matches(label(a)+" "+a.packageName,q))continue;
                LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(4,3,4,3);
                ImageView icon=new ImageView(this);icon.setImageBitmap(a.icon);row.addView(icon,new LinearLayout.LayoutParams(42,42));
                TextView name=text(label(a)+(prefs.isFavorite(a.id)?"  ★":""),17,INK);I18n.userText(name);row.addView(name,new LinearLayout.LayoutParams(0,48,1));
                row.setOnClickListener(v->{drawer.dismiss();onApp(a);});row.setOnLongClickListener(v->{onAppLongPress(a);return true;});
                list.addView(row);count++;
            }
            if(count==0)list.addView(text("Aucune application correspondante",16,MUTED));
        };
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){populate.run();}public void afterTextChanged(Editable e){}});
        populate.run();if(focusSearch){query.requestFocus();drawer.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);}
    }
    private void openControlCenter(boolean preview){
        if(!preview&&!controlPrefs.getBoolean("enabled",true)){toast("Centre désactivé · réglages pour le réactiver");return;}
        dismissOwnedDialogs();controlPanel.open(preview);
    }
    private void showControlCenterSettings(){
        dismissOwnedDialogs();
        controlSettings=new ControlCenterSettings(this,controlPrefs,new ArrayList<AppEntry>(apps),new ControlCenterSettings.Listener(){
            public void onChanged(){board.refresh();if(controlPanel.isOpen())controlPanel.refresh();ControlCenterService.refresh(MainActivity.this);}
            public void onPreview(){if(controlSettings!=null)controlSettings.dismiss();openControlCenter(true);}
        });
        controlSettings.show();
    }
    private void closeControlExtras(){
        if(floatingSettings!=null&&floatingSettings.isShowing())floatingSettings.dismiss();
        if(controlController!=null)controlController.onHostPause();
        if(controlPanel!=null&&controlPanel.isOpen())controlPanel.close();
        if(controlSettings!=null&&controlSettings.isShowing())controlSettings.dismiss();
        closeScreenLamp();
    }
    @Override public void onCustomize(){showControlCenterSettings();}
    @Override public void onPrismeAction(String action){
        if(controlPanel!=null)controlPanel.close();
        if("floatingVolumeSettings".equals(action)){showFloatingVolumeSettings();return;}
        if("screenlamp".equals(action)){showScreenLamp();return;}
        if("capture".equals(action)){savePrismeCapture();return;}
        if("dndPermission".equals(action)){
            new OwnedBuilder().setTitle("Ne pas déranger").setMessage("Android demande ton autorisation pour que PRISME change ce mode. Tu choisis toi-même l’accès dans la page suivante. Sans cet accès, les réglages de son restent disponibles.")
                .setNegativeButton("Annuler",null).setPositiveButton("Ouvrir Android",(d,w)->launch(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))).show();return;
        }
        onAction(action);
    }
    private void resetDashboardSafely(){try{SafetyBackup.save(this,prefs);String note=prefs.getString("notes","");GlobalRotationService.release(this);getSharedPreferences("prisme_rotation",MODE_PRIVATE).edit().putBoolean("boot",false).apply();fr.dm100.prisme.volume.FloatingVolumeService.stop(this);prefs.reset();controlPrefs.resetAll();floatingPrefs.resetAll();ControlCenterService.refresh(this);prefs.putString("notes",note);refreshBoard();toast("Bureau réinitialisé · sauvegarde de sécurité disponible");}catch(Exception e){toast("Sauvegarde indisponible · remise à zéro annulée");}}
    private void importSafely(JSONObject data)throws Exception{SafetyBackup.save(this,prefs);prefs.importJSONObject(data);}
    private void requestUtilityAction(Intent intent){if(intent==null)return;String action=intent.getStringExtra("utilityAction");if(action!=null){intent.removeExtra("utilityAction");main.post(()->{if(!isFinishing()){dismissOwnedDialogs();onAction(action);}});}}
    private void showTestColor(int color){
        dismissOwnedDialogs();closeScreenLamp();TextView test=new TextView(this);test.setBackgroundColor(color);test.setTextColor(color==Color.WHITE?Color.BLACK:Color.WHITE);
        Point size=screen();test.setText("← 0                         "+(size.x-1)+" →\n\n"+size.x+" × "+size.y+"\n\nToucher pour fermer");test.setGravity(Gravity.CENTER);test.setTextSize(TypedValue.COMPLEX_UNIT_PX,20);
        screenLamp=test;lampPreviousBrightness=getWindow().getAttributes().screenBrightness;mainLayer.addView(test,new FrameLayout.LayoutParams(-1,-1));test.setOnClickListener(v->closeScreenLamp());
    }
    private void showScreenLamp(){
        closeScreenLamp();lampPreviousBrightness=getWindow().getAttributes().screenBrightness;
        WindowManager.LayoutParams lp=getWindow().getAttributes();lp.screenBrightness=1f;getWindow().setAttributes(lp);
        TextView lamp=text("LAMPE ÉCRAN\n\nToucher pour fermer",23,0xff101010);lamp.setGravity(Gravity.CENTER);
        lamp.setBackgroundColor(Color.WHITE);lamp.setContentDescription("Lampe écran. Toucher pour fermer.");
        lamp.setOnClickListener(v->closeScreenLamp());screenLamp=lamp;
        mainLayer.addView(lamp,new FrameLayout.LayoutParams(-1,-1));
    }
    private void closeScreenLamp(){
        if(screenLamp==null)return;
        mainLayer.removeView(screenLamp);screenLamp=null;
        WindowManager.LayoutParams lp=getWindow().getAttributes();lp.screenBrightness=lampPreviousBrightness;getWindow().setAttributes(lp);
    }
    private java.io.File pendingCaptureFile(){
        if(pendingCaptureName==null||pendingCaptureName.length()>100||!pendingCaptureName.matches("prisme_capture_[a-zA-Z0-9_.-]+\\.png"))return null;
        return new java.io.File(getCacheDir(),pendingCaptureName);
    }
    private void clearCaptureTemp(){
        java.io.File file=pendingCaptureFile();if(file!=null&&file.isFile())file.delete();pendingCaptureName=null;
    }
    private void savePrismeCapture(){
        mainLayer.post(()->{
            Bitmap image=null;
            try{
                if(mainLayer.getWidth()<1||mainLayer.getHeight()<1)return;
                clearCaptureTemp();
                java.io.File cached=java.io.File.createTempFile("prisme_capture_",".png",getCacheDir());pendingCaptureName=cached.getName();
                image=Bitmap.createBitmap(mainLayer.getWidth(),mainLayer.getHeight(),Bitmap.Config.ARGB_8888);
                mainLayer.draw(new Canvas(image));
                try(java.io.FileOutputStream out=new java.io.FileOutputStream(cached)){
                    if(!image.compress(Bitmap.CompressFormat.PNG,100,out))throw new java.io.IOException("Encodage de la capture refusé");
                }
                Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/png")
                    .putExtra(Intent.EXTRA_TITLE,"PRISME_"+System.currentTimeMillis()+".png");
                if(save.resolveActivity(getPackageManager())!=null)startActivityForResult(save,CAPTURE);
                else{
                    java.io.File folder=getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);if(folder==null)folder=getFilesDir();
                    java.io.File file=new java.io.File(folder,"PRISME_"+System.currentTimeMillis()+".png");
                    try(java.io.FileInputStream in=new java.io.FileInputStream(cached);java.io.FileOutputStream out=new java.io.FileOutputStream(file)){copyCapture(in,out);}
                    clearCaptureTemp();new OwnedBuilder().setTitle("Capture PRISME enregistrée").setMessage(file.getAbsolutePath()).setNegativeButton("Fermer",null).show();
                }
            }catch(Exception ex){clearCaptureTemp();toast("La capture n’a pas pu être enregistrée");}
            finally{if(image!=null)image.recycle();}
        });
    }
    private void copyCapture(InputStream in,OutputStream out)throws java.io.IOException{
        byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
    }
    private void requestControlCenterAction(final Intent requested){
        if(requested==null||requested.hasCategory(Intent.CATEGORY_HOME))return;
        final String action=requested.getStringExtra("controlCenterAction");
        if(action==null||action.length()==0)return;
        cancelSettingsRestore=true;
        requested.removeExtra("controlCenterAction");
        board.post(()->{
            if(!isFinishing()){
                if("controlCenterSettings".equals(action))showControlCenterSettings();
                else onPrismeAction(action);
            }
        });
    }
    private void requestVolumeSettings(final Intent requested){
        if(requested==null||!requested.getBooleanExtra("volumeSettings",false)||requested.hasCategory(Intent.CATEGORY_HOME))return;
        cancelSettingsRestore=true;
        board.post(()->{
            if(!isFinishing()&&getIntent()==requested&&requested.getBooleanExtra("volumeSettings",false)&&!requested.hasCategory(Intent.CATEGORY_HOME)){
                requested.removeExtra("volumeSettings");showFloatingVolumeSettings();
            }
        });
    }
    private void showBatteryIndicatorSettings(){showSettings(5);}
    private void showMusicPlayerSettings(){showSettings(6);}
    private void renderBatteryTab(LinearLayout b){
        final BatteryIndicatorPrefs battery=new BatteryIndicatorPrefs(this);
        settingsIntro(b,"Batterie · 24 styles",
                "Mesure réelle fournie par Android. Affichage au choix : Dashboard seulement, toutes les applis ou désactivé.");
        BatteryLevelReader.Reading state=BatteryLevelReader.read(this,null);
        b.addView(text("Charge lue : "+(state.level<0?"indisponible":state.level+" %")+" · "+state.source,14,ACCENT));
        BatteryIndicatorView preview=new BatteryIndicatorView(this,battery);
        preview.setBackgroundHint(ThemeSpec.get(prefs.getInt("theme",0)).bg);
        preview.update(state.level,state.charging);
        LinearLayout.LayoutParams previewLp=new LinearLayout.LayoutParams(preview.desiredWidth(),preview.desiredHeight());
        previewLp.setMargins(4,8,4,8);b.addView(preview,previewLp);
        final Runnable changed=()->{
            previewLp.width=preview.desiredWidth();previewLp.height=preview.desiredHeight();
            preview.setLayoutParams(previewLp);preview.invalidate();
            refreshDashboardOverlays();ControlCenterService.refresh(MainActivity.this);
        };
        section(b,"Sur quels écrans ?");
        settingChoice(b,"Portée de l'indicateur",new String[]{"Désactivé","Dashboard uniquement","Toutes les applications"},
                battery.displayMode(),n->{battery.setDisplayMode(n);changed.run();});
        b.addView(text("Le mode Dashboard ne demande aucune autorisation spéciale. Pour les autres applications, autorise la superposition Android.",14,MUTED));
        addButton(b,"Autoriser la superposition Android",()->{
            try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}
            catch(RuntimeException ex){toast("Permission indisponible sur cette montre");}
        });
        section(b,"Styles et couleurs");
        settingChoice(b,"Style de batterie",BatteryIndicatorPrefs.STYLES,battery.integer("style",0),
                n->{battery.putInt("style",n);changed.run();});
        settingChoice(b,"Palette de couleurs",BatteryIndicatorPrefs.PALETTES,battery.integer("palette",0),
                n->{battery.putInt("palette",n);changed.run();});
        slider(b,"Seuil batterie faible (%)",5,40,battery.integer("low",20),n->{battery.putInt("low",n);changed.run();});
        slider(b,"Seuil batterie moyenne (%)",45,90,battery.integer("high",65),n->{battery.putInt("high",n);changed.run();});
        section(b,"Taille, position et transparence");
        slider(b,"Taille (%)",35,200,battery.integer("size",60),n->{battery.putInt("size",n);changed.run();});
        slider(b,"Position horizontale (%)",0,100,battery.integer("x",100),n->{battery.putInt("x",n);changed.run();});
        slider(b,"Position verticale (%)",0,100,battery.integer("y",0),n->{battery.putInt("y",n);changed.run();});
        slider(b,"Opacité (%)",25,100,battery.integer("opacity",100),n->{battery.putInt("opacity",n);changed.run();});
        section(b,"Détails et diagnostic");
        batteryOption(b,"Afficher le pourcentage","percent",true,battery,changed);
        batteryOption(b,"Fond sombre derrière l'indicateur","backdrop",true,battery,changed);
        batteryOption(b,"Couleur différente pendant la recharge","chargingColor",true,battery,changed);
        batteryOption(b,"Contraste automatique sur les 22 styles PRISME","adaptiveContrast",false,battery,changed);
        batteryOption(b,"Zones vides translucides","transparentEmpty",true,battery,changed);
        settingChoice(b,"Contraste forcé",new String[]{"Automatique","Encre sombre","Encre claire"},battery.integer("contrastMode",0),n->{battery.putInt("contrastMode",n);changed.run();});
        batteryOption(b,"Afficher l'éclair de recharge","chargingMark",true,battery,changed);
        addButton(b,"Diagnostic : comparer les sources de batterie",()->{
            BatteryLevelReader.Reading result=BatteryLevelReader.read(this,null);
            new OwnedBuilder().setTitle("Mesure de batterie Android").setMessage(result.diagnostic())
                    .setNegativeButton("Fermer",null).show();
        });
        addButton(b,"Réinitialiser taille et position",()->{
            battery.putInt("x",100);battery.putInt("y",0);battery.putInt("size",60);
            changed.run();toast("Position batterie restaurée");
        });
    }
    private void batteryOption(LinearLayout b,String label,String key,boolean fallback,
                               BatteryIndicatorPrefs p,Runnable changed){
        CheckBox c=new CheckBox(this);c.setText(label);c.setTextColor(INK);
        c.setTextSize(TypedValue.COMPLEX_UNIT_PX,16);c.setMinHeight(44);
        c.setChecked(p.bool(key,fallback));
        c.setOnCheckedChangeListener((view,checked)->{p.putBoolean(key,checked);changed.run();});
        b.addView(c);
    }
    private void renderMusicTab(LinearLayout b){
        settingsIntro(b,"Musique · mini-lecteur",
                "Play / pause, piste suivante et précédente, sur les 22 styles de dashboard.");
        final Runnable changed=()->refreshDashboardOverlays();
        musicOption(b,"Afficher le mini-lecteur","enabled",true,changed);
        musicOption(b,"Préférer les transitions d’accueil sans réinitialisation des tâches","preserve",true,changed);
        b.addView(text("PRISME évite de réinitialiser les tâches au changement d’écran, sans lire les notifications. La continuité audio reste gérée par Spotify, Deezer ou le firmware. La reprise automatique avancée est désactivée dans ce correctif.",14,MUTED));
        MusicPlayback.State state=MusicPlayback.state(this);
        b.addView(text("État : "+state.title+" · "+(state.playing?"lecture":"pause / arrêt"),14,ACCENT));
        b.addView(text("Commandes multimédias Android, sans accès aux notifications. Le titre et l'artiste ne sont plus récupérés dans cette version pour éviter le blocage Play Protect.",13,MUTED));
        section(b,"12 styles de lecteur");
        settingChoice(b,"Design du mini-lecteur",MusicPrefs.STYLES,musicPrefs.number("style",0),
                n->{musicPrefs.putInt("style",n);changed.run();});
        section(b,"Taille et position");
        slider(b,"Taille (%)",50,180,musicPrefs.number("size",85),n->{musicPrefs.putInt("size",n);changed.run();});
        slider(b,"Position horizontale (%)",0,100,musicPrefs.number("x",50),n->{musicPrefs.putInt("x",n);changed.run();});
        slider(b,"Position verticale (%)",0,100,musicPrefs.number("y",87),n->{musicPrefs.putInt("y",n);changed.run();});
        slider(b,"Opacité (%)",30,100,musicPrefs.number("opacity",100),n->{musicPrefs.putInt("opacity",n);changed.run();});
        musicOption(b,"Afficher l’état audio du lecteur","title",true,changed);
        musicOption(b,"Afficher le fond du lecteur","background",true,changed);
        section(b,"Tester les commandes");
        LinearLayout row=new LinearLayout(this);
        for(final int command:new int[]{MusicPlayback.PREVIOUS,MusicPlayback.TOGGLE,MusicPlayback.NEXT}){
            Button btn=button(command==MusicPlayback.PREVIOUS?"⏮":command==MusicPlayback.NEXT?"⏭":"⏯");
            btn.setOnClickListener(v->MusicPlayback.command(this,command));
            row.addView(btn,new LinearLayout.LayoutParams(0,48,1));
        }
        b.addView(row);
        addButton(b,"Replacer le lecteur en bas au centre",()->{
            musicPrefs.putInt("x",50);musicPrefs.putInt("y",87);musicPrefs.putInt("size",85);
            changed.run();toast("Position musique restaurée");
        });
    }
    private void musicOption(LinearLayout b,String label,String key,boolean fallback,Runnable changed){
        CheckBox box=new CheckBox(this);box.setText(label);box.setTextColor(INK);
        box.setTextSize(TypedValue.COMPLEX_UNIT_PX,16);box.setMinHeight(48);
        box.setChecked(musicPrefs.flag(key,fallback));
        box.setOnCheckedChangeListener((view,value)->{musicPrefs.putBoolean(key,value);changed.run();});
        b.addView(box);
    }
    private void showFloatingVolumeSettings(){
        dismissOwnedDialogs();
        floatingSettings=new FloatingVolumeSettings(this,floatingPrefs,new FloatingVolumeSettings.Listener(){
            public void onChanged(){fr.dm100.prisme.volume.FloatingVolumeService.refresh(MainActivity.this);}
            public void onEnable(boolean enabled){
                if(enabled)fr.dm100.prisme.volume.FloatingVolumeService.begin(MainActivity.this);
                else fr.dm100.prisme.volume.FloatingVolumeService.stop(MainActivity.this);
            }
            public void onPositionReset(){floatingPrefs.putInt("xPercent",100);floatingPrefs.putInt("yPercent",55);fr.dm100.prisme.volume.FloatingVolumeService.refresh(MainActivity.this);}
        });
        floatingSettings.show();
    }
    @Override public void startActivity(Intent intent){
        if(intent!=null){String action=intent.getAction();
            if(Settings.ACTION_MANAGE_OVERLAY_PERMISSION.equals(action)||Settings.ACTION_MANAGE_WRITE_SETTINGS.equals(action)||Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS.equals(action))
                fr.dm100.prisme.volume.FloatingVolumeService.suspendForPermission(this);
        }
        super.startActivity(intent);
    }
    private void showTools(){
        ScrollView scroll=scroll();LinearLayout b=column();scroll.addView(b);DeviceSnapshot s=DeviceSnapshot.read(this);
        b.addView(text("CENTRE DE CONTRÔLE",21,INK));
        b.addView(text("Batterie "+s.batteryPct+" %"+(s.charging?" · en charge":"")+"\nMémoire utilisée "+s.memoryUsedPct+" % · "+String.format(I18n.locale(),"%.1f",s.freeStorageGb)+" Go libres",16,MUTED));
        section(b,"Volume des médias");AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        if(audio!=null)slider(b,"Volume",0,audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),audio.getStreamVolume(AudioManager.STREAM_MUSIC),n->{
            try{audio.setStreamVolume(AudioManager.STREAM_MUSIC,n,0);}catch(RuntimeException e){toast("Android refuse ce réglage audio");}
        });
        addButton(b,"Wi-Fi",()->launch(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        addButton(b,"Bluetooth",()->launch(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        addButton(b,"Batterie Android",()->launch(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY)));
        addButton(b,"Affichage et luminosité",()->launch(new Intent(Settings.ACTION_DISPLAY_SETTINGS)));
        addButton(b,"Tous les paramètres",()->launch(new Intent(Settings.ACTION_SETTINGS)));
        addButton(b,"Pastille de volume intégrée",()->showFloatingVolumeSettings());
        addButton(b,"Batterie : styles et emplacement",()->showBatteryIndicatorSettings());
        addButton(b,"Lecteur de musique : styles et contrôle",()->showMusicPlayerSettings());
        addButton(b,"Indicateur de batterie · 24 styles",()->showBatteryIndicatorSettings());
        addButton(b,"Minuteur / concentration",()->showTimer(false));
        addButton(b,"Chronomètre",()->showStopwatch());
        addButton(b,"Carnet local",()->showNotes());
        addButton(b,"Terminal installé",()->launchPackage("com.termux"));
        addButton(b,"Cadran d’origine",()->originalClock());
        dialog("Outils de la montre",scroll,null,null);
    }
    private void showNotes(){experience.notes();}
    private void showProfiles(){
        SharedPreferences profiles=getSharedPreferences("prisme_profiles",MODE_PRIVATE);
        List<String> names=new ArrayList<>(profiles.getAll().keySet());Collections.sort(names);
        List<String> actions=new ArrayList<>();actions.add("+ Enregistrer le bureau actuel");for(String n:names)actions.add(n);
        new OwnedBuilder().setTitle("Mes profils").setItems(actions.toArray(new String[0]),(d,n)->{
            if(n==0){EditText name=edit("");name.setSingleLine(true);name.setHint("Nom du profil");dialog("Enregistrer un profil",name,"Enregistrer",(di,wi)->{
                String key=name.getText().toString().trim();if(key.isEmpty())key="Mon bureau";if(key.length()>32)key=key.substring(0,32);
                if(profiles.getAll().size()>=12&&!profiles.contains(key)){toast("12 profils maximum");return;}
                try{profiles.edit().putString(key,prefs.exportJSONObject().toString()).apply();toast("Profil enregistré");}catch(Exception e){toast("Impossible d’enregistrer ce profil");}
            });}
            else{String key=names.get(n-1);new OwnedBuilder().setTitle(key).setItems(new String[]{"Appliquer","Supprimer ce profil"},(di,which)->{
                if(which==0){try{importSafely(new JSONObject(profiles.getString(key,"{}")));profileImported();toast("Profil appliqué");}catch(Exception e){toast("Profil invalide");}}
                else{profiles.edit().remove(key).apply();toast("Profil supprimé");}
            }).setNegativeButton("Fermer",null).show();}
        }).setNegativeButton("Fermer",null).show();
    }
    private void profileImported(){if(I18n.selected(this)!=activityLanguage){dismissOwnedDialogs();recreate();return;}SenseEngine.apply(this,prefs);refreshBoard();refreshDashboardOverlays();if(fr.dm100.prisme.volume.FloatingVolumeService.isRunning())fr.dm100.prisme.volume.FloatingVolumeService.refresh(this);ControlCenterService.refresh(this);EdgeDockService.refresh(this);}
    private void exportConfig(){saveLocalConfig();}
    private void exportWithAndroid(){
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"PRISME-configuration.json");
        try{startActivityForResult(i,EXPORT);}catch(RuntimeException e){saveLocalConfig();}
    }
    private java.io.File backupDirectory()throws java.io.IOException{
        java.io.File parent=getExternalFilesDir(null);if(parent==null)parent=getFilesDir();
        java.io.File directory=new java.io.File(parent,"sauvegardes");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new java.io.IOException("Dossier de sauvegarde indisponible");
        return directory;
    }
    private void saveLocalConfig(){
        java.io.File created=null;
        boolean reserved=false;
        try{
            String json=prefs.exportJSONObject().toString();byte[] bytes=json.getBytes("UTF-8");
            if(bytes.length>65536)throw new java.io.IOException("Configuration trop volumineuse");
            java.io.File directory=backupDirectory();
            String name="PRISME-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new java.util.Date());
            int suffix=0;
            do{created=new java.io.File(directory,name+(suffix==0?"":"-"+suffix)+".json");suffix++;}while(!created.createNewFile());
            reserved=true;
            try(OutputStream out=new java.io.FileOutputStream(created)){out.write(bytes);}
            reserved=false;
            showLocalConfigText("Sauvegarde enregistrée",created.getAbsolutePath(),json);
        }catch(Exception e){
            if(reserved&&created!=null&&created.isFile())created.delete();
            try{showLocalConfigText("Copie JSON disponible","Le fichier n’a pas pu être enregistré. Tu peux copier la configuration ci-dessous.",prefs.exportJSONObject().toString());}
            catch(Exception ignored){toast("Impossible d’exporter cette configuration");}
        }
    }
    private void showLocalConfigText(String title,String path,final String json){
        LinearLayout body=column();TextView location=text(path,15,ACCENT);location.setTextIsSelectable(true);body.addView(location);
        body.addView(text("Cette sauvegarde appartient à PRISME. Elle est accessible dans Importer une configuration.",14,MUTED));
        addButton(body,"Copier le JSON",()->{
            try{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                if(clipboard==null)throw new IllegalStateException();
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Configuration PRISME",json));toast("JSON copié");
            }catch(RuntimeException e){toast("Copie indisponible sur ce firmware");}
        });
        addButton(body,"Autre emplacement avec Android",()->exportWithAndroid());
        TextView source=text(json,14,INK);source.setTextIsSelectable(true);body.addView(source);
        ScrollView scroll=scroll();scroll.addView(body);dialog(title,scroll,null,null);
    }
    private void importConfig(){showLocalImports();}
    private void importWithAndroid(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        try{startActivityForResult(i,IMPORT);}catch(RuntimeException e){showLocalImports();}
    }
    private void showLocalImports(){
        java.io.File[] listed=null;
        try{listed=backupDirectory().listFiles(file->file.isFile()&&file.getName().toLowerCase(Locale.ROOT).endsWith(".json"));}catch(Exception ignored){}
        final java.io.File[] files=listed==null?new java.io.File[0]:listed;
        java.util.Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
        String[] choices=new String[files.length+2];choices[0]="Coller une configuration JSON";
        for(int n=0;n<files.length;n++)choices[n+1]=files[n].getName();
        choices[files.length+1]="Parcourir avec le sélecteur Android";
        new OwnedBuilder().setTitle(files.length==0?"Importer · aucune sauvegarde locale":"Importer une sauvegarde locale").setItems(choices,(d,n)->{
            if(n==0){showPasteConfig();return;}
            if(n==files.length+1){importWithAndroid();return;}
            try{
                java.io.File file=files[n-1];
                if(!file.getCanonicalFile().getParentFile().equals(backupDirectory().getCanonicalFile()))throw new java.io.IOException("Chemin invalide");
                try(InputStream in=new java.io.FileInputStream(file)){confirmImport(readBoundedConfig(in));}
            }catch(Exception e){toast("Sauvegarde invalide ou supérieure à 64 Ko");}
        }).setNegativeButton("Fermer",null).show();
    }
    private void showPasteConfig(){
        EditText input=edit("");input.setHint("Colle ici le JSON d’une configuration PRISME");input.setMinLines(6);input.setGravity(Gravity.TOP);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(65536)});
        dialog("Coller une configuration JSON",input,"Vérifier",(d,w)->{
            try{String json=input.getText().toString();if(json.getBytes("UTF-8").length>65536)throw new java.io.IOException("Trop volumineux");confirmImport(new JSONObject(json));}
            catch(Exception e){toast("JSON invalide ou supérieur à 64 Ko");}
        });
    }
    private JSONObject readBoundedConfig(InputStream in)throws Exception{
        if(in==null)throw new java.io.IOException("Fichier indisponible");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
        while((count=in.read(buffer))!=-1){if(bytes.size()+count>65536)throw new java.io.IOException("Trop volumineux");bytes.write(buffer,0,count);}
        return new JSONObject(new String(bytes.toByteArray(),"UTF-8"));
    }
    private void confirmImport(final JSONObject obj){
        new OwnedBuilder().setTitle("Importer ce bureau ?").setMessage("La configuration actuelle de PRISME sera remplacée par cette sauvegarde. Les applications de la montre restent installées.").setNegativeButton("Annuler",null).setPositiveButton("Importer",(d,w)->{
            try{importSafely(obj);profileImported();toast("Configuration importée");}catch(Exception e){toast("Configuration invalide · réglages conservés");}
        }).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==CAPTURE){
            if(result==RESULT_OK&&data!=null&&data.getData()!=null){
                java.io.File file=pendingCaptureFile();
                if(file==null||!file.isFile())toast("Capture interrompue. Relance la capture PRISME.");
                else try(InputStream in=new java.io.FileInputStream(file);OutputStream out=getContentResolver().openOutputStream(data.getData())){
                    if(out==null)throw new java.io.IOException();copyCapture(in,out);toast("Capture PRISME enregistrée");
                }catch(Exception ex){toast("La capture n’a pas pu être enregistrée");}
            }
            clearCaptureTemp();return;
        }
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        try{
            if(request==EXPORT){try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new Exception();out.write(prefs.exportJSONObject().toString().getBytes("UTF-8"));}toast("Configuration exportée");}
            else if(request==IMPORT){try(InputStream in=getContentResolver().openInputStream(uri)){confirmImport(readBoundedConfig(in));}}
        }catch(Exception e){toast("Impossible de lire ou d’enregistrer cette configuration");}
    }
    private void showTimer(boolean focus){
        LinearLayout b=column();b.addView(text(focus?"Une tâche à la fois":"Ton temps, même ailleurs",22,INK));
        b.addView(text("Alerte Android à la fin, même si tu ouvres une autre application. Un arrêt forcé de PRISME peut bloquer les alertes.",14,MUTED));
        timerDisplay=text("",42,ACCENT);timerDisplay.setGravity(Gravity.CENTER);b.addView(timerDisplay);
        LinearLayout row=new LinearLayout(this);int[] minutes=focus?new int[]{5,15,25,45}:new int[]{1,5,10,25};
        for(final int m:minutes){Button button=button(m+" min");button.setOnClickListener(v->startTimer(m*60000L));row.addView(button,new LinearLayout.LayoutParams(0,48,1));}b.addView(row);
        LinearLayout custom=new LinearLayout(this);EditText amount=edit("");amount.setSingleLine(true);amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);amount.setHint("Minutes · 1 à 1440");custom.addView(amount,new LinearLayout.LayoutParams(0,48,1));
        Button go=button("Démarrer");go.setOnClickListener(v->{try{long m=Long.parseLong(amount.getText().toString());if(m<1||m>1440)throw new NumberFormatException();startTimer(m*60000L);}catch(NumberFormatException e){toast("Choisis de 1 à 1440 minutes");}});custom.addView(go,new LinearLayout.LayoutParams(125,48));b.addView(custom);
        addButton(b,"Pause / reprendre",()->{if(TimerEngine.paused(this)){if(!TimerEngine.resume(this))toast("Impossible de programmer l’alerte");}else if(TimerEngine.deadline(this)>0)TimerEngine.pause(this);updateTimerLabel();board.refresh();});
        addButton(b,"Annuler le minuteur",()->{TimerEngine.cancel(this);timerDeadline=0;updateTimerLabel();board.refresh();});
        timerDialog=dialog(focus?"Concentration":"Minuteur",b,null,null);updateTimerLabel();main.removeCallbacks(timerTick);main.post(timerTick);
    }
    private void startTimer(long duration){if(!TimerEngine.start(this,duration))toast("L’alerte Android n’a pas pu être programmée");timerDeadline=TimerEngine.deadline(this);main.removeCallbacks(timerTick);main.post(timerTick);updateTimerLabel();board.refresh();}
    private void checkTimer(){TimerEngine.check(this);timerDeadline=TimerEngine.deadline(this);updateTimerLabel();}
    private void updateTimerLabel(){timerDeadline=TimerEngine.deadline(this);if(timerDisplay!=null)timerDisplay.setText(TimerEngine.remaining(this)>0?(TimerEngine.paused(this)?"Pause · ":"")+formatDuration(TimerEngine.remaining(this)):"Prêt");}
    private void showStopwatch(){
        LinearLayout b=column();stopwatchDisplay=text(formatChrono(stopwatchElapsed()),44,ACCENT);stopwatchDisplay.setGravity(Gravity.CENTER);b.addView(stopwatchDisplay);
        addButton(b,"Démarrer / pause",()->{if(stopwatchRunning){stopwatchAccumulated+=SystemClock.elapsedRealtime()-stopwatchStart;stopwatchRunning=false;}else{stopwatchStart=SystemClock.elapsedRealtime();stopwatchRunning=true;}saveUtilities();main.removeCallbacks(stopwatchTick);main.post(stopwatchTick);});
        addButton(b,"Remettre à zéro",()->{stopwatchAccumulated=0;stopwatchStart=SystemClock.elapsedRealtime();saveUtilities();stopwatchDisplay.setText(formatChrono(stopwatchElapsed()));});
        LinearLayout laps=column();
        Runnable renderLaps=()->{laps.removeAllViews();try{org.json.JSONArray list=new org.json.JSONArray(getSharedPreferences("prisme_utilities",MODE_PRIVATE).getString("laps","[]"));for(int i=list.length()-1;i>=0;i--)laps.addView(text("Tour "+(i+1)+" · "+formatChrono(list.getLong(i)),18,INK));}catch(Exception ignored){}};
        addButton(b,"Enregistrer un tour",()->{try{org.json.JSONArray list=new org.json.JSONArray(getSharedPreferences("prisme_utilities",MODE_PRIVATE).getString("laps","[]"));if(list.length()>=20){toast("20 tours maximum");return;}list.put(stopwatchElapsed());getSharedPreferences("prisme_utilities",MODE_PRIVATE).edit().putString("laps",list.toString()).apply();renderLaps.run();}catch(Exception ignored){}});
        addButton(b,"Effacer les tours",()->{getSharedPreferences("prisme_utilities",MODE_PRIVATE).edit().putString("laps","[]").apply();renderLaps.run();});b.addView(laps);renderLaps.run();
        stopwatchDialog=dialog("Chronomètre",b,null,null);main.post(stopwatchTick);
        stopwatchDialog.setOnDismissListener(d->main.removeCallbacks(stopwatchTick));
    }
    private static String formatChrono(long millis){long tenths=Math.max(0,millis)/100;return String.format(I18n.locale(),"%02d:%02d.%d",tenths/600,(tenths/10)%60,tenths%10);}
    private long stopwatchElapsed(){return stopwatchAccumulated+(stopwatchRunning?Math.max(0,SystemClock.elapsedRealtime()-stopwatchStart):0);}
    private int currentBoot(){try{return Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,-1);}catch(RuntimeException e){return -1;}}
    private void saveUtilities(){getSharedPreferences("prisme_utilities",MODE_PRIVATE).edit().putLong("stopwatchAccumulated",stopwatchAccumulated).putLong("stopwatchCheckpoint",stopwatchElapsed()).putLong("stopwatchStart",stopwatchStart).putBoolean("stopwatchRunning",stopwatchRunning).putInt("stopwatchBoot",currentBoot()).apply();}
    private static String formatDuration(long millis){long s=(millis+999)/1000;return String.format(I18n.locale(),"%02d:%02d",s/60,s%60);}
    /** Leave every PRISME panel before handing control to a real, installed HOME activity. */
    private void dismissOwnedDialogs(){
        closeControlExtras();
        for(int n=ownedDialogs.size()-1;n>=0;n--){
            AlertDialog d=ownedDialogs.get(n).get();if(d!=null&&d.isShowing())d.dismiss();
        }
        ownedDialogs.clear();styleDialog=null;
    }
    private List<android.content.pm.ResolveInfo> nativeHomes(){
        List<android.content.pm.ResolveInfo> homes=new ArrayList<>();
        Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        try{
            for(android.content.pm.ResolveInfo candidate:getPackageManager().queryIntentActivities(query,0)){
                android.content.pm.ActivityInfo a=candidate.activityInfo;
                if(a==null||!a.enabled||!a.exported||getPackageName().equals(a.packageName)||a.name.contains("FallbackHome"))continue;
                boolean duplicate=false;
                for(android.content.pm.ResolveInfo known:homes)if(known.activityInfo.packageName.equals(a.packageName)&&known.activityInfo.name.equals(a.name))duplicate=true;
                if(!duplicate)homes.add(candidate);
            }
        }catch(RuntimeException ignored){}
        return homes;
    }
    private ComponentName homeComponent(android.content.pm.ResolveInfo home){return new ComponentName(home.activityInfo.packageName,home.activityInfo.name);}
    private void originalClock(){
        dismissOwnedDialogs();
        List<android.content.pm.ResolveInfo> homes=nativeHomes();
        String saved=getSharedPreferences("prisme_ui",MODE_PRIVATE).getString("nativeClockComponent","");
        for(android.content.pm.ResolveInfo home:homes)if(homeComponent(home).flattenToString().equals(saved)){openNativeHome(homeComponent(home));return;}
        if(homes.size()==1){openNativeHome(homeComponent(homes.get(0)));return;}
        showClockTargetChooser(true);
    }
    private void showClockTargetChooser(boolean openAfterSelection){
        dismissOwnedDialogs();
        final List<android.content.pm.ResolveInfo> homes=nativeHomes();
        if(homes.isEmpty()){
            new OwnedBuilder().setTitle("Accueil de la montre indisponible")
                    .setMessage("Aucun autre accueil Android accessible n’a été trouvé. Tu peux toujours changer les styles PRISME. Le choix des cadrans natifs dépend du lanceur du fabricant.")
                    .setNegativeButton("Fermer",null).setPositiveButton("Styles PRISME",(d,w)->showStyles()).show();return;
        }
        String[] names=new String[homes.size()];
        for(int i=0;i<names.length;i++){
            android.content.pm.ResolveInfo home=homes.get(i);
            CharSequence appName=getPackageManager().getApplicationLabel(home.activityInfo.applicationInfo);
            names[i]=appName==null?"Accueil de la montre":appName.toString();
            if(names[i].trim().isEmpty())names[i]="Accueil de la montre";
        }
        new OwnedBuilder().setTitle("Choisir l’accueil de la montre").setItems(names,(d,n)->{
            ComponentName component=homeComponent(homes.get(n));
            getSharedPreferences("prisme_ui",MODE_PRIVATE).edit().putString("nativeClockComponent",component.flattenToString()).apply();
            if(openAfterSelection)openNativeHome(component);else toast("Accueil de la montre enregistré");
        }).setNegativeButton("Annuler",null).show();
    }
    private void openNativeHome(ComponentName component){
        // An explicit target avoids returning to PRISME if PRISME is the default HOME.
        MusicPlayback.prepareHomeTransition(this);
        dismissOwnedDialogs();
        Intent nativeHome=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if(!launch(nativeHome))showClockTargetChooser(false);
    }
    private void launchPackage(String pkg){Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i==null){toast("Application non installée");return;}launch(i);}
    private boolean launch(Intent i){try{startActivity(i);return true;}catch(ActivityNotFoundException|SecurityException e){toast("Cet écran n’est pas disponible sur ce firmware");return false;}catch(RuntimeException e){toast("Android n’a pas pu ouvrir cet écran");return false;}}
    private void showShortcutShelf(){
        LinearLayout body=column();body.addView(text("Un appui ouvre le raccourci. Un appui long permet de le modifier.",16,MUTED));
        final AlertDialog[] holder={null};
        for(int i=0;i<DashboardPrefs.SLOT_COUNT;i++){final int slot=i;final String action=prefs.getShortcut(slot,SHORTCUT_DEFAULTS[slot]);Button key=button((i+1)+" · "+shortcutName(action));
            key.setOnClickListener(v->{if(holder[0]!=null)holder[0].dismiss();onAction(action);});key.setOnLongClickListener(v->{if(holder[0]!=null)holder[0].dismiss();showShortcutEditor(slot);return true;});body.addView(key,new LinearLayout.LayoutParams(-1,60));}
        holder[0]=dialog("Mes raccourcis",body,null,null);
    }
    private boolean pendingStyleExport;
    private void exportStyleGallery(){
        if(apps.isEmpty()){pendingStyleExport=true;toast("Chargement des applications…");return;}
        java.io.File parent=getExternalFilesDir(null);if(parent==null)parent=getFilesDir();final java.io.File directory=new java.io.File(parent,"apercus-styles");
        if(!directory.isDirectory()&&!directory.mkdirs()){toast("Impossible de créer la galerie");return;}
        final TextView status=text("Préparation des aperçus…",20,INK);final AlertDialog progress=dialog("Galerie des styles",status,null,null);
        final org.json.JSONArray exported=new org.json.JSONArray();
        main.post(new Runnable(){int index;public void run(){
            if(isFinishing()||!progress.isShowing())return;
            try{Bitmap image=board.createStylePreview(index,640,480);java.io.File file=new java.io.File(directory,String.format(Locale.ROOT,"style_%02d.png",index));
                try(OutputStream output=new java.io.FileOutputStream(file)){if(!image.compress(Bitmap.CompressFormat.PNG,100,output))throw new java.io.IOException("PNG indisponible");}finally{image.recycle();}
                JSONObject item=new JSONObject(board.stylePreviewMetrics().toString());item.put("name",ThemeSpec.get(index).name);item.put("description",ThemeSpec.get(index).subtitle);item.put("file",file.getName());exported.put(item);index++;status.setText("Aperçu "+index+" / "+ThemeSpec.ALL.length);
                if(index<ThemeSpec.ALL.length){main.postDelayed(this,35);return;}
                JSONObject manifest=new JSONObject().put("version","5.1").put("generatedOnWatch",true).put("styles",exported);try(OutputStream output=new java.io.FileOutputStream(new java.io.File(directory,"STYLES.json"))){output.write(manifest.toString(2).getBytes("UTF-8"));}
                progress.dismiss();new OwnedBuilder().setTitle("Galerie exportée").setMessage("22 aperçus générés par PRISME.\n"+directory.getAbsolutePath()).setNegativeButton("Fermer",null).show();
            }catch(Exception error){progress.dismiss();toast("Impossible de terminer la galerie");}
        }});
    }
    private void showLanguagePicker(){
        new OwnedBuilder().setTitle("Langue / Language").setSingleChoiceItems(I18n.NAMES,I18n.selected(this),(dialog,which)->{
            prefs.putInt("language",which);I18n.init(this);if(GlobalRotationService.isRunning())GlobalRotationService.apply(this,GlobalRotationService.mode(this));
            dismissOwnedDialogs();recreate();
        }).setNegativeButton("Fermer",null).show();
    }
    private void toggleGlobalRotation(){
        if(!Settings.canDrawOverlays(this)){launch(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));return;}
        final int next=GlobalRotationService.oppositeMode(this);
        if(!GlobalRotationService.apply(this,next)){toast("Android a refusé la rotation globale");return;}
        main.postDelayed(()->{
            if(isFinishing()||GlobalRotationService.mode(this)!=next)return;
            if(!GlobalRotationService.isRunning())toast("La rotation n’a pas pu démarrer");
            else if(GlobalRotationService.matches(this,next))toast(next==1?"Portrait forcé":"Paysage forcé");
            else toast("L’application conserve son orientation");
        },900);
    }
    private void showRotationSettings(){
        LinearLayout body=column();body.addView(text("Choisis l’orientation pour toutes les applications. Libérer l’orientation rend la main à Android.",17,MUTED));
        final TextView status=text("Mode : "+GlobalRotationService.label(this),20,ACCENT);body.addView(status);
        String[] names={"Système · libérer l’orientation","Portrait forcé","Paysage forcé","Portrait inversé","Paysage inversé"};
        for(int i=0;i<names.length;i++){final int mode=i;addButton(body,names[i],()->{
            if(mode>0&&!Settings.canDrawOverlays(this)){launch(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));return;}
            if(GlobalRotationService.apply(this,mode)){status.setText("Mode : "+GlobalRotationService.label(this));toast(names[mode]);}else toast("Android a refusé la rotation globale");
        });}
        CheckBox boot=new CheckBox(this);boot.setText("Relancer l’orientation au redémarrage");boot.setTextColor(INK);boot.setTextSize(TypedValue.COMPLEX_UNIT_PX,17);boot.setMinHeight(48);
        boot.setChecked(getSharedPreferences("prisme_rotation",MODE_PRIVATE).getBoolean("boot",false));boot.setOnCheckedChangeListener((v,on)->getSharedPreferences("prisme_rotation",MODE_PRIVATE).edit().putBoolean("boot",on).apply());body.addView(boot);
        body.addView(text("La commande du centre alterne portrait et paysage. Un appui long ouvre ces options. La notification permet aussi de libérer l’orientation.",15,MUTED));dialog("Orientation globale",body,null,null);
    }
    private void showHelp(){
        ScrollView scroll=scroll();LinearLayout b=column();scroll.addView(b);
        b.addView(text("PRISME 5.4 · "+ThemeSpec.ALL.length+" bureaux pour ta montre",23,INK));
        b.addView(text("Nouveautés : Aujourd’hui rassemble favoris, récents et accès rapides. Recherche retrouve applications et commandes, même sans accents. La liste de tâches se coche et le carnet s’enregistre pendant la saisie. Le minuteur alerte aussi hors de PRISME. Nova et Silence sont accessibles dans Styles.\n\nDans Outils, personnalise le balayage vers le haut et le double appui sur une zone vide, puis retrouve l’horloge de table et les réglages rapides de confort. La pastille peut s’atténuer au repos. Trouver un réglage recherche les paramètres par mot-clé.",17,ACCENT));
        b.addView(text("Touche une icône pour ouvrir sa vraie application. Maintiens-la pour les favoris, le nom, les espaces ou le masquage.\n\nGlisse horizontalement pour les pages. Les dispositions en liste défilent aussi verticalement.\n\nLa barre du bas reste accessible dans chaque style : Cadrans, Styles, Aujourd’hui et Réglages. Le bouton Fermer des réglages ramène toujours au bureau.\n\nRéglages est organisé en six catégories. Écran règle le zoom global, les marges et l’adaptation ; Style règle les icônes et les couleurs. Raccourcis personnalise cinq emplacements, aussi modifiables par appui long.\n\nGlisse depuis le bord configuré pour ouvrir le centre de contrôle. Ses styles iOS et Android, ses boutons et ses gestes se personnalisent dans Outils. La pastille de volume intégrée s’active dans Outils, avec l’autorisation Android de superposition. Ses styles, formes, taille, transparence, position et mode déployable sont réglables ; sa notification permet de l’arrêter.\n\nLe bouton Cadran montre ouvre un accueil natif installé, sans changer le bureau Android par défaut. Pour changer un cadran natif, effectue ensuite un appui long sur son écran ; le sélecteur dépend du fabricant.\n\nLes compteurs indiquent des mesures réelles de la montre. Le terminal est une interface de lancement et d’outils locaux.\n\nLe bureau modifie l’accueil. Les applications et les menus Android gardent leurs propres interfaces.\n\nPour utiliser PRISME avec le bouton Accueil, choisis-le dans les bureaux Android disponibles. Certains firmwares de montre imposent leur propre accueil. Un bouton permet toujours de retrouver le cadran d’origine.\n\nApplication personnelle hors ligne, sans publicité ni compte. Aucun accès Internet, aux messages, aux contacts ou à l’historique des autres applications. Les favoris et les lancements depuis PRISME restent sur la montre.",16,INK));
        dialog("Guide PRISME",scroll,null,null);
    }

    private interface IntChange{void change(int value);}
    private void slider(LinearLayout b,String name,int min,int max,int initial,IntChange change){
        TextView title=text(name+" · "+initial,16,INK);b.addView(title);SeekBar seek=new SeekBar(this);seek.setMax(max-min);seek.setProgress(Math.max(0,Math.min(max-min,initial-min)));
        seek.setPadding(8,0,8,0);b.addView(seek,new LinearLayout.LayoutParams(-1,44));seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){change.change(min+s.getProgress());}public void onProgressChanged(SeekBar s,int value,boolean user){title.setText(name+" · "+(min+value));}
        });
    }
    private void select(LinearLayout b,String name,String[] values,int selected,IntChange change){
        b.addView(text(name,15,MUTED));Button button=button(values[Math.max(0,Math.min(values.length-1,selected))]);
        button.setOnClickListener(v->new OwnedBuilder().setTitle(name).setSingleChoiceItems(values,Math.max(0,Math.min(values.length-1,prefs.getInt(selectKey(name),selected))),(d,n)->{change.change(n);button.setText(values[n]);d.dismiss();}).setNegativeButton("Annuler",null).show());
        b.addView(button);
    }
    private String selectKey(String label){if(label.startsWith("Forme"))return "iconShape";if(label.startsWith("Police"))return "font";if(label.startsWith("Couleur"))return "accentIndex";if(label.startsWith("Fond"))return "wallpaper";return "order";}
    private void toggle(LinearLayout b,String label,String key,boolean defaultValue){CheckBox check=new CheckBox(this);check.setText(label);check.setTextColor(INK);check.setTextSize(TypedValue.COMPLEX_UNIT_PX,16);check.setMinHeight(44);check.setChecked(prefs.getBoolean(key,defaultValue));check.setOnCheckedChangeListener((v,on)->{prefs.putBoolean(key,on);refreshBoard();});b.addView(check);}
    private void section(LinearLayout b,String name){TextView t=text(name,20,ACCENT);t.setPadding(0,18,0,8);t.setTypeface(null,Typeface.BOLD);b.addView(t);}
    private void addButton(LinearLayout b,String name,Runnable action){Button button=button(name);button.setOnClickListener(v->action.run());b.addView(button,new LinearLayout.LayoutParams(-1,48));}
    private Button button(String name){Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setTextColor(INK);b.setTextSize(TypedValue.COMPLEX_UNIT_PX,16);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(SURFACE));b.setPadding(8,0,8,0);b.setMinHeight(44);return b;}
    private TextView text(String value,int px,int color){TextView t=new TextView(this);t.setText(value);t.setTextColor(color);t.setTextSize(TypedValue.COMPLEX_UNIT_PX,px);t.setPadding(2,5,2,5);return t;}
    private EditText edit(String value){EditText e=new EditText(this);e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);e.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);e.setText(value);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setTextSize(TypedValue.COMPLEX_UNIT_PX,18);e.setPadding(8,8,8,8);return e;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(10,6,10,10);l.setBackgroundColor(BG);return l;}
    private ScrollView scroll(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);return s;}
    private Point screen(){Point p=new Point();getWindowManager().getDefaultDisplay().getRealSize(p);return p;}
    private AlertDialog dialog(String title,View content,String positive,DialogInterface.OnClickListener action){
        AlertDialog.Builder builder=new OwnedBuilder().setTitle(title).setView(content).setNegativeButton("Fermer",null);
        if(positive!=null)builder.setPositiveButton(positive,action);AlertDialog d=builder.create();d.show();Point size=screen();d.getWindow().setLayout(size.x,size.y);
        for(int which:new int[]{AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_POSITIVE}){Button button=d.getButton(which);if(button!=null){button.setTextSize(TypedValue.COMPLEX_UNIT_PX,16);button.setMinHeight(44);}}
        return d;
    }
    private void toast(String value){I18n.makeToast(this,value,Toast.LENGTH_SHORT).show();}

    private final class OwnedBuilder extends AlertDialog.Builder{
        private CharSequence titleText, messageText, positiveText, negativeText;
        private View contentView;
        private boolean fixedContent;
        private CharSequence[] items;
        private int choiceMode, checkedItem=-1;
        private boolean[] checkedItems;
        private DialogInterface.OnClickListener itemListener, positiveListener, negativeListener;
        private DialogInterface.OnMultiChoiceClickListener multiListener;
        OwnedBuilder(){super(MainActivity.this);}
        @Override public OwnedBuilder setTitle(CharSequence title){titleText=title;return this;}
        @Override public OwnedBuilder setTitle(int title){return setTitle(getText(title));}
        @Override public OwnedBuilder setMessage(CharSequence message){messageText=message;return this;}
        @Override public OwnedBuilder setMessage(int message){return setMessage(getText(message));}
        @Override public OwnedBuilder setView(View view){contentView=view;fixedContent=false;return this;}
        OwnedBuilder setFixedView(View view){contentView=view;fixedContent=true;return this;}
        @Override public OwnedBuilder setView(int layout){return setView(getLayoutInflater().inflate(layout,null));}
        @Override public OwnedBuilder setItems(CharSequence[] values,DialogInterface.OnClickListener listener){
            items=values==null?new CharSequence[0]:values.clone();choiceMode=0;itemListener=listener;return this;
        }
        @Override public OwnedBuilder setItems(int values,DialogInterface.OnClickListener listener){return setItems(getResources().getTextArray(values),listener);}
        @Override public OwnedBuilder setSingleChoiceItems(CharSequence[] values,int selected,DialogInterface.OnClickListener listener){
            items=values==null?new CharSequence[0]:values.clone();choiceMode=1;checkedItem=selected;itemListener=listener;return this;
        }
        @Override public OwnedBuilder setSingleChoiceItems(int values,int selected,DialogInterface.OnClickListener listener){return setSingleChoiceItems(getResources().getTextArray(values),selected,listener);}
        @Override public OwnedBuilder setMultiChoiceItems(CharSequence[] values,boolean[] selected,DialogInterface.OnMultiChoiceClickListener listener){
            items=values==null?new CharSequence[0]:values.clone();choiceMode=2;checkedItems=new boolean[items.length];
            if(selected!=null)System.arraycopy(selected,0,checkedItems,0,Math.min(selected.length,checkedItems.length));
            multiListener=listener;return this;
        }
        @Override public OwnedBuilder setMultiChoiceItems(int values,boolean[] selected,DialogInterface.OnMultiChoiceClickListener listener){return setMultiChoiceItems(getResources().getTextArray(values),selected,listener);}
        @Override public OwnedBuilder setPositiveButton(CharSequence title,DialogInterface.OnClickListener listener){positiveText=title;positiveListener=listener;return this;}
        @Override public OwnedBuilder setPositiveButton(int title,DialogInterface.OnClickListener listener){return setPositiveButton(getText(title),listener);}
        @Override public OwnedBuilder setNegativeButton(CharSequence title,DialogInterface.OnClickListener listener){negativeText=title;negativeListener=listener;return this;}
        @Override public OwnedBuilder setNegativeButton(int title,DialogInterface.OnClickListener listener){return setNegativeButton(getText(title),listener);}
        @Override public AlertDialog create(){
            AlertDialog d=new OwnedDialog(this);
            for(int n=ownedDialogs.size()-1;n>=0;n--)if(ownedDialogs.get(n).get()==null)ownedDialogs.remove(n);
            ownedDialogs.add(new java.lang.ref.WeakReference<>(d));return d;
        }
        @Override public AlertDialog show(){AlertDialog dialog=create();dialog.show();return dialog;}
    }

    /** The firmware's AlertController constrains panels to a phone-size fragment.
     *  Keep Android Dialog lifecycle, but own every visible pixel and touch row. */
    private final class OwnedDialog extends AlertDialog{
        private final OwnedBuilder spec;
        private Button positiveButton,negativeButton;
        OwnedDialog(OwnedBuilder spec){
            super(MainActivity.this);
            this.spec=spec;
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            setCanceledOnTouchOutside(true);
        }
        private void fitWindow(){
            android.view.Window window=getWindow();if(window==null)return;
            Point size=screen();window.setLayout(size.x,size.y);
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.getDecorView().setSystemUiVisibility(5894);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));
            // AlertController sees an empty template before we replace it; restore editor focus.
            window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            window.getDecorView().setPadding(0,0,0,0);
            window.setGravity(Gravity.CENTER);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attrs=window.getAttributes();attrs.dimAmount=.62f;window.setAttributes(attrs);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            window.setTitle(spec.titleText==null?"PRISME":spec.titleText);
        }
        @Override protected void onCreate(Bundle state){
            super.onCreate(state);
            LinearLayout shell=new LinearLayout(MainActivity.this);shell.setOrientation(LinearLayout.VERTICAL);
            shell.setBackgroundColor(BG);shell.setPadding(12,10,12,10);
            if(spec.titleText!=null&&spec.titleText.length()>0){
                TextView heading=text(spec.titleText.toString(),22,INK);heading.setTypeface(null,Typeface.BOLD);
                heading.setPadding(2,2,2,12);shell.addView(heading,new LinearLayout.LayoutParams(-1,-2));
                View rule=new View(MainActivity.this);rule.setBackgroundColor(0xff2b4054);shell.addView(rule,new LinearLayout.LayoutParams(-1,1));
            }
            View content;
            if(spec.items!=null){
                LinearLayout rows=new LinearLayout(MainActivity.this);rows.setOrientation(LinearLayout.VERTICAL);
                final List<android.widget.RadioButton> radios=new ArrayList<>();
                for(int n=0;n<spec.items.length;n++){
                    final int index=n;
                    if(spec.choiceMode==2){
                        CheckBox choice=new CheckBox(MainActivity.this);configureChoice(choice,spec.items[n]);
                        choice.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT));choice.setChecked(spec.checkedItems[n]);
                        choice.setOnCheckedChangeListener((button,checked)->{spec.checkedItems[index]=checked;if(spec.multiListener!=null)spec.multiListener.onClick(OwnedDialog.this,index,checked);});
                        rows.addView(choice,new LinearLayout.LayoutParams(-1,-2));
                    }else if(spec.choiceMode==1){
                        android.widget.RadioButton choice=new android.widget.RadioButton(MainActivity.this);configureChoice(choice,spec.items[n]);
                        choice.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT));choice.setChecked(n==spec.checkedItem);radios.add(choice);
                        choice.setOnClickListener(v->{spec.checkedItem=index;for(int k=0;k<radios.size();k++)radios.get(k).setChecked(k==index);if(spec.itemListener!=null)spec.itemListener.onClick(OwnedDialog.this,index);});
                        rows.addView(choice,new LinearLayout.LayoutParams(-1,-2));
                    }else{
                        TextView choice=text(spec.items[n]==null?"":spec.items[n].toString(),17,INK);choice.setGravity(Gravity.CENTER_VERTICAL);
                        choice.setPadding(12,8,12,8);choice.setMinHeight(48);choice.setClickable(true);choice.setFocusable(true);
                        android.util.TypedValue highlight=new android.util.TypedValue();if(getTheme().resolveAttribute(android.R.attr.selectableItemBackground,highlight,true))choice.setBackgroundResource(highlight.resourceId);
                        choice.setOnClickListener(v->{try{if(spec.itemListener!=null)spec.itemListener.onClick(OwnedDialog.this,index);}finally{dismiss();}});
                        rows.addView(choice,new LinearLayout.LayoutParams(-1,-2));
                    }
                    if(n+1<spec.items.length){View rule=new View(MainActivity.this);rule.setBackgroundColor(0xff1b2d40);rows.addView(rule,new LinearLayout.LayoutParams(-1,1));}
                }
                content=scrollContent(rows);
            }else if(spec.contentView!=null){
                View supplied=spec.contentView;
                if(supplied.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)supplied.getParent()).removeView(supplied);
                content=spec.fixedContent||supplied instanceof ScrollView?supplied:scrollContent(supplied);
            }else{
                TextView message=text(spec.messageText==null?"":spec.messageText.toString(),17,INK);
                message.setPadding(8,12,8,12);content=scrollContent(message);
            }
            LinearLayout.LayoutParams contentParams=new LinearLayout.LayoutParams(-1,0,1);contentParams.topMargin=8;contentParams.bottomMargin=8;shell.addView(content,contentParams);
            if(spec.negativeText!=null||spec.positiveText!=null){
                LinearLayout bar=new LinearLayout(MainActivity.this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.END);
                if(spec.negativeText!=null){negativeButton=button(spec.negativeText.toString());negativeButton.setOnClickListener(v->{try{if(spec.negativeListener!=null)spec.negativeListener.onClick(OwnedDialog.this,BUTTON_NEGATIVE);}finally{dismiss();}});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,48,1);lp.rightMargin=spec.positiveText==null?0:6;bar.addView(negativeButton,lp);}
                if(spec.positiveText!=null){positiveButton=button(spec.positiveText.toString());positiveButton.setTextColor(ACCENT);positiveButton.setOnClickListener(v->{try{if(spec.positiveListener!=null)spec.positiveListener.onClick(OwnedDialog.this,BUTTON_POSITIVE);}finally{dismiss();}});bar.addView(positiveButton,new LinearLayout.LayoutParams(0,48,1));}
                shell.addView(bar,new LinearLayout.LayoutParams(-1,48));
            }
            setContentView(shell,new android.view.ViewGroup.LayoutParams(-1,-1));I18n.bind(shell);fitWindow();
        }
        private void configureChoice(android.widget.CompoundButton choice,CharSequence title){
            choice.setText(title);choice.setTextColor(INK);choice.setTextSize(TypedValue.COMPLEX_UNIT_PX,17);
            choice.setPadding(8,8,8,8);choice.setMinHeight(48);choice.setGravity(Gravity.CENTER_VERTICAL);
        }
        private ScrollView scrollContent(View content){
            ScrollView scroll=new ScrollView(MainActivity.this);scroll.setFillViewport(false);scroll.setClipToPadding(false);
            scroll.setPadding(0,0,4,0);scroll.setNestedScrollingEnabled(true);scroll.addView(content,new ScrollView.LayoutParams(-1,-2));return scroll;
        }
        @Override public void show(){fitWindow();super.show();fitWindow();}
        @Override public Button getButton(int which){return which==BUTTON_POSITIVE?positiveButton:which==BUTTON_NEGATIVE?negativeButton:null;}
    }

    /** Small, code-native previews; no screenshots or network images. */
    private static final class ThemePreview extends View {
        private final ThemeSpec theme;private final MainActivity owner;private Bitmap preview;
        ThemePreview(Context context,ThemeSpec theme){super(context);this.theme=theme;owner=(MainActivity)context;owner.stylePreviews.add(this);setContentDescription(theme.name);}
        void refreshPreview(){preview=null;invalidate();}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(preview==null)preview=owner.board.createStylePreview(theme.id);float scale=Math.min(getWidth()/320f,getHeight()/240f);float width=320*scale,height=240*scale;canvas.drawBitmap(preview,null,new RectF((getWidth()-width)/2,(getHeight()-height)/2,(getWidth()+width)/2,(getHeight()+height)/2),null);}
        @Override protected void onDetachedFromWindow(){preview=null;super.onDetachedFromWindow();}
    }
}

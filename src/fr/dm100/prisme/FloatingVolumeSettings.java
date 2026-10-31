package fr.dm100.prisme;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import fr.dm100.prisme.volume.FloatingVolumeService;

/** User-operated configuration for PRISME's global volume overlay. No permission is granted here. */
public final class FloatingVolumeSettings {
    public interface Listener {
        void onChanged();
        void onEnable(boolean enabled);
        void onPositionReset();
    }
    private static final int BG=0xff101925,SURFACE=0xff203044,INK=0xffeef5ff,MUTED=0xffa9bad0,
            ACCENT=0xff62d9cc,CAUTION=0xffffcf8c;
    private static final String LEGACY_PACKAGE="fr.dm100.volumepill";
    private static final String[] CATEGORIES={"Activation","Style","Forme","Position","Actions","Avancé"};
    private static final String[] STYLES={"Verre iOS","Android","Néon","Minimal","Titane","Clair"};
    private static final String[] COLOR_NAMES={"Menthe","Cyan","Bleu","Violet","Orange","Rose"};
    private static final int[] COLORS={0xff62d9cc,0xff59c9ef,0xff78a9ff,0xffbe9bff,0xffffb275,0xfff79fc2};
    private final Activity activity;
    private final FloatingVolumePrefs prefs;
    private final Listener listener;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final List<Button> tabs=new ArrayList<>();
    private final int[] scrollPositions=new int[CATEGORIES.length];
    private Dialog dialog;
    private ScrollView scroll;
    private LinearLayout body,expandedOptions;
    private TextView status,legacyWarning;
    private Button activationButton;
    private CheckBox activationCheck;
    private boolean bindingActivation,pendingActivation,windowFocused,registered;
    private int category,lastWidth,lastHeight;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChanges=(store,key)->{
        if(isShowing())main.post(()->{if(isShowing())updateStatus();});
    };
    private final Runnable statusTick=new Runnable(){public void run(){
        if(!isShowing()||!windowFocused)return;
        updateStatus();main.postDelayed(this,1000);
    }};

    public FloatingVolumeSettings(Activity activity,FloatingVolumePrefs prefs,Listener listener){
        if(activity==null||prefs==null)throw new IllegalArgumentException("Activité et réglages nécessaires");
        this.activity=activity;this.prefs=prefs;this.listener=listener;
    }
    public boolean isShowing(){return dialog!=null&&dialog.isShowing();}
    public void show(){
        if(activity.isFinishing()||activity.isDestroyed())return;
        if(isShowing()){refit();return;}
        pendingActivation=false;windowFocused=false;main.removeCallbacks(statusTick);
        if(registered){prefs.unregisterListener(preferenceChanges);registered=false;}
        lastWidth=lastHeight=0;
        final Dialog opened=new Dialog(activity){
            @Override public void onWindowFocusChanged(boolean focused){
                super.onWindowFocusChanged(focused);if(FloatingVolumeSettings.this.dialog!=this)return;
                windowFocused=focused;main.removeCallbacks(statusTick);
                if(focused){checkRequestedActivation();updateStatus();updateLegacyWarning();if(isShowing())main.post(statusTick);}
            }
        };
        dialog=opened;opened.requestWindowFeature(Window.FEATURE_NO_TITLE);opened.setCanceledOnTouchOutside(false);
        LinearLayout shell=column();shell.setPadding(10,8,10,8);
        TextView title=text("Pastille volume PRISME",22,INK);title.setTypeface(null,Typeface.BOLD);shell.addView(title);
        shell.addView(text("Les boutons de volume peuvent suivre toutes tes applications.",13,MUTED));
        tabs.clear();
        for(int row=0;row<2;row++){
            LinearLayout line=new LinearLayout(activity);
            for(int col=0;col<3;col++){
                final int index=row*3+col;Button tab=button(CATEGORIES[index]);tab.setTextSize(TypedValue.COMPLEX_UNIT_PX,15);
                tab.setOnClickListener(v->selectCategory(index));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,44,1);lp.setMargins(2,1,2,1);line.addView(tab,lp);tabs.add(tab);
            }
            shell.addView(line,new LinearLayout.LayoutParams(-1,46));
        }
        scroll=new ScrollView(activity);scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.setPadding(0,4,2,4);
        body=column();body.setPadding(2,4,4,12);scroll.addView(body,new ScrollView.LayoutParams(-1,-2));shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout footer=new LinearLayout(activity);
        Button close=button("Fermer");close.setOnClickListener(v->dismiss());activationButton=button("Activer");activationButton.setTextColor(ACCENT);
        activationButton.setOnClickListener(v->{
            if(pendingActivation&&!canOverlay()){openOverlayPermission(true);return;}
            requestEnabled(!prefs.getBoolean("enabled",false));
        });
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,48,1);left.rightMargin=6;footer.addView(close,left);
        footer.addView(activationButton,new LinearLayout.LayoutParams(0,48,1));shell.addView(footer,new LinearLayout.LayoutParams(-1,48));
        opened.setContentView(shell,new android.view.ViewGroup.LayoutParams(-1,-1));
        opened.setOnDismissListener(d->{
            if(dialog!=opened)return;
            captureScroll();pendingActivation=false;windowFocused=false;main.removeCallbacks(statusTick);
            if(registered){prefs.unregisterListener(preferenceChanges);registered=false;}
            Window w=opened.getWindow();
            if(w!=null&&layoutListener!=null&&w.getDecorView().getViewTreeObserver().isAlive())w.getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
            if(dialog==opened){dialog=null;scroll=null;body=null;tabs.clear();status=null;legacyWarning=null;activationCheck=null;activationButton=null;expandedOptions=null;layoutListener=null;}
        });
        opened.show();I18n.bind(opened.getWindow().getDecorView());fitWindow(true);layoutListener=()->fitWindow(false);
        opened.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
        prefs.registerListener(preferenceChanges);registered=true;render();updateStatus();
    }
    public void dismiss(){pendingActivation=false;main.removeCallbacks(statusTick);if(dialog!=null)dialog.dismiss();}
    public void refit(){fitWindow(true);updateStatus();}
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener;
    private void fitWindow(boolean force){
        if(dialog==null||dialog.getWindow()==null)return;
        Point size=new Point();activity.getWindowManager().getDefaultDisplay().getRealSize(size);
        if(!force&&size.x==lastWidth&&size.y==lastHeight)return;
        lastWidth=size.x;lastHeight=size.y;Window w=dialog.getWindow();
        w.setLayout(size.x,size.y);w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));
        w.getDecorView().setPadding(0,0,0,0);w.getDecorView().setSystemUiVisibility(5894);w.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setGravity(Gravity.CENTER);
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);WindowManager.LayoutParams a=w.getAttributes();a.dimAmount=.6f;w.setAttributes(a);
    }
    private void captureScroll(){if(scroll!=null)scrollPositions[category]=scroll.getScrollY();}
    private void selectCategory(int next){if(next<0||next>=CATEGORIES.length)return;captureScroll();category=next;render();}
    private void render(){
        if(body==null)return;
        body.removeAllViews();expandedOptions=null;status=null;activationCheck=null;legacyWarning=null;
        for(int i=0;i<tabs.size();i++){
            boolean selected=i==category;Button tab=tabs.get(i);tab.setSelected(selected);tab.setTextColor(selected?BG:INK);
            tab.setBackgroundTintList(ColorStateList.valueOf(selected?ACCENT:SURFACE));tab.setContentDescription(CATEGORIES[i]+(selected?" · catégorie active":" · ouvrir la catégorie"));
        }
        switch(category){
            case 0:activationSettings();break;
            case 1:styleSettings();break;
            case 2:shapeSettings();break;
            case 3:positionSettings();break;
            case 4:actionSettings();break;
            default:advancedSettings();break;
        }
        updateStatus();
        final ScrollView target=scroll;final int y=scrollPositions[category];
        if(target!=null)target.post(()->{if(scroll==target)target.scrollTo(0,y);});
    }
    private void activationSettings(){
        intro("Une vraie pastille globale","Elle appartient à PRISME et peut rester visible dans les autres applications, avec ton autorisation Android.");
        status=text("",16,ACCENT);body.addView(status);
        activationCheck=check("Afficher la pastille",prefs.getBoolean("enabled",false));
        activationCheck.setOnCheckedChangeListener((v,on)->{if(!bindingActivation)requestEnabled(on);});body.addView(activationCheck);
        if(pendingActivation&&!canOverlay()){
            body.addView(text("Activation en attente : Android doit d’abord autoriser PRISME à afficher des éléments au-dessus des autres applications. Active cette autorisation dans Android, puis reviens ici.",14,CAUTION));
            action("Ouvrir l’autorisation Android",()->openOverlayPermission(true));
            action("Annuler cette activation",()->{pendingActivation=false;render();});
        }
        body.addView(text("Cette autorisation est propre à PRISME : celle de l’ancienne application Pastille Volume ne se transfère pas. Aucune lecture d’écran ni autorisation d’accessibilité n’est demandée.",14,MUTED));
        toggle("Relancer après redémarrage de la montre","boot",false);
        body.addView(text("Décoché au départ. La reprise nécessite une pastille activée et une autorisation toujours accordée. Un arrêt volontaire l’empêche de repartir jusqu’à une nouvelle activation.",13,MUTED));
        body.addView(text("Une notification PRISME reste présente pendant le fonctionnement. Son bouton Arrêter retire la pastille. Fermer cet écran de réglages laisse la pastille active.",14,INK));
        legacyWarning=text("",14,CAUTION);body.addView(legacyWarning);updateLegacyWarning();
        if(legacyInstalled())action("Ouvrir l’ancienne Pastille Volume",()->{
            Intent launch=activity.getPackageManager().getLaunchIntentForPackage(LEGACY_PACKAGE);
            if(launch==null){toast("L’ancienne application n’est pas accessible");return;}open(launch);
        });
    }
    private void requestEnabled(boolean enabled){
        if(!enabled){pendingActivation=false;if(listener!=null)listener.onEnable(false);updateStatus();if(category==0)render();return;}
        if(!canOverlay()){
            pendingActivation=true;category=0;scrollPositions[0]=0;render();return;
        }
        pendingActivation=false;if(listener!=null)listener.onEnable(true);updateStatus();
    }
    private void openOverlayPermission(boolean activateOnReturn){
        if(activateOnReturn)pendingActivation=true;
        Intent intent=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+activity.getPackageName()));
        if(!open(intent))toast("Dans Android, autorise PRISME à s’afficher au-dessus des autres applications, puis reviens ici.");
    }
    private void checkRequestedActivation(){
        if(!pendingActivation||!isShowing()||!canOverlay())return;
        pendingActivation=false;if(listener!=null)listener.onEnable(true);if(category==0)render();
    }
    private boolean canOverlay(){try{return Settings.canDrawOverlays(activity);}catch(RuntimeException ignored){return false;}}
    private void updateStatus(){
        boolean enabled=prefs.getBoolean("enabled",false),allowed=canOverlay();
        if(activationButton!=null)activationButton.setText(pendingActivation&&!allowed?"Autoriser dans Android":enabled?"Arrêter":"Activer");
        if(activationCheck!=null){bindingActivation=true;activationCheck.setChecked(enabled);bindingActivation=false;}
        if(status!=null){
            String message;
            if(!allowed)message=pendingActivation?"En attente de ton autorisation Android":"Autorisation de superposition manquante";
            else if(FloatingVolumeService.isShowing())message="Pastille affichée au-dessus des applications";
            else if(enabled)message=FloatingVolumeService.isRunning()?"Service actif · affichage en attente":"Activation demandée · service en attente";
            else message="Pastille arrêtée";
            String error=FloatingVolumeService.lastError();
            if(error!=null&&!error.trim().isEmpty())message+="\nDernier signalement : "+error;
            status.setText(message);
        }
    }
    private void styleSettings(){
        intro("Six styles, à ton goût","Le style change le dessin de la pastille. Sa forme et sa disposition se règlent séparément.");
        for(int row=0;row<3;row++){
            LinearLayout line=new LinearLayout(activity);
            for(int col=0;col<2;col++){
                final int index=row*2+col;Button style=button((prefs.getInt("style",0)==index?"✓ ":"")+STYLES[index]);
                style.setOnClickListener(v->{prefs.putInt("style",index);changed();captureScroll();render();});
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,48,1);lp.setMargins(2,1,2,1);line.addView(style,lp);
            }
            body.addView(line);
        }
        section("Couleur");
        for(int row=0;row<2;row++){
            LinearLayout line=new LinearLayout(activity);
            for(int col=0;col<3;col++){
                final int index=row*3+col;boolean selected=!prefs.getBoolean("customAccent",false)&&prefs.getInt("accent",0)==index;
                Button color=button((selected?"✓ ":"")+COLOR_NAMES[index]);color.setTextColor(COLORS[index]);
                color.setOnClickListener(v->{prefs.putInt("accent",index);prefs.putBoolean("customAccent",false);changed();captureScroll();render();});
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,46,1);lp.setMargins(2,1,2,1);line.addView(color,lp);
            }
            body.addView(line);
        }
        colorEditor();toggle("Atténuer la pastille quand elle est au repos","idleFade",false);slider("Atténuation au repos (%)","idleOpacity",20,90,40);slider("Délai avant atténuation (secondes)","idleDelay",2,60,8);body.addView(text("Au toucher, la pastille retrouve immédiatement son opacité habituelle. Le pourcentage au repos multiplie cette opacité.",13,MUTED));slider("Opacité habituelle (%)","opacity",15,100,90);slider("Opacité déployée (%)","expandedOpacity",15,100,95);
    }
    private void colorEditor(){
        CheckBox custom=check("Utiliser une couleur personnalisée",prefs.getBoolean("customAccent",false));body.addView(custom);
        LinearLayout editor=column();editor.setPadding(6,2,6,8);body.addView(editor);
        EditText hex=new EditText(activity);hex.setSingleLine(true);hex.setSelectAllOnFocus(true);
        hex.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        hex.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);
        hex.setTextColor(INK);hex.setHintTextColor(MUTED);hex.setTextSize(TypedValue.COMPLEX_UNIT_PX,18);
        hex.setText(String.format(Locale.US,"#%06X",prefs.getInt("customColor",0x62d9cc)));hex.setHint("#RRGGBB");editor.addView(hex,new LinearLayout.LayoutParams(-1,48));
        View swatch=new View(activity);editor.addView(swatch,new LinearLayout.LayoutParams(-1,28));TextView valid=text("",13,MUTED);editor.addView(valid);
        Button apply=button("Appliquer cette couleur");editor.addView(apply,new LinearLayout.LayoutParams(-1,48));
        Runnable validate=()->{Integer value=parseColor(hex.getText().toString());apply.setEnabled(value!=null);
            swatch.setBackgroundColor(value==null?SURFACE:0xff000000|value);valid.setText(value==null?"Entre six chiffres, par exemple #62D9CC.":String.format(Locale.US,"Aperçu : #%06X",value));valid.setTextColor(value==null?CAUTION:MUTED);
        };
        hex.addTextChangedListener(new Watcher(validate));validate.run();editor.setVisibility(custom.isChecked()?View.VISIBLE:View.GONE);
        custom.setOnCheckedChangeListener((v,on)->{prefs.putBoolean("customAccent",on);editor.setVisibility(on?View.VISIBLE:View.GONE);changed();});
        apply.setOnClickListener(v->{Integer value=parseColor(hex.getText().toString());if(value==null){toast("Couleur invalide : utilise #RRGGBB");return;}prefs.putInt("customColor",value);prefs.putBoolean("customAccent",true);custom.setChecked(true);changed();toast("Couleur enregistrée");});
    }
    private void shapeSettings(){
        intro("Forme et disposition","Ces deux choix sont indépendants du style visuel.");
        choice("Forme",new String[]{"Capsule","Cercle","Carré arrondi","Losange"},prefs.getInt("shape",0),n->changeInt("shape",n));
        choice("Disposition",new String[]{"Verticale","Horizontale","Bouton compact déployable"},prefs.getInt("layout",0),n->changeInt("layout",n));
        slider("Taille des boutons (px)","size",40,96,56);
        body.addView(text("La taille utilise les pixels réels de l’écran. Cercle et losange concernent les boutons : ils ne sont pas étirés en une longue ellipse.",13,MUTED));
        toggle("Replier automatiquement le bouton déployable","autoCollapse",true);
        slider("Délai avant repli (secondes)","collapseDelay",1,15,4);
        body.addView(text("Avec le bouton déployable, un appui ouvre les commandes. Le repli automatique concerne cette disposition uniquement.",13,MUTED));
    }
    private void positionSettings(){
        intro("Placer la pastille","Tu peux la déplacer directement en la faisant glisser, sauf si sa position est verrouillée.");
        slider("Position horizontale (%)","xPercent",0,100,100);slider("Position verticale (%)","yPercent",0,100,55);
        body.addView(text("0 correspond à gauche ou en haut ; 100 à droite ou en bas. La position tient compte de la taille de la pastille et des limites de l’écran.",13,MUTED));
        toggle("Ranger au bord après un déplacement","snap",true);toggle("Verrouiller la position","locked",false);
        body.addView(text("Le verrouillage empêche le déplacement ; les boutons de volume restent utilisables.",13,MUTED));
        action("Replacer au bord droit",()->{
            if(listener!=null)listener.onPositionReset();captureScroll();render();
        });
    }
    private void actionSettings(){
        intro("Choisir ce que règlent les boutons","Le volume réel change lorsque tu utilises + et −.");
        choice("Volume contrôlé",new String[]{"Médias / musique","Sonnerie","Alarme","Appel"},prefs.getInt("stream",0),n->changeInt("stream",n));
        body.addView(text("Appel concerne une conversation. Le mode Ne pas déranger peut limiter les changements de sonnerie.",13,MUTED));
        toggle("Afficher le niveau du volume","showLevel",true);toggle("Afficher une commande muet","showMute",false);toggle("Vibration légère à l’appui","haptic",false);
        section("Appui prolongé");slider("Délai avant répétition (ms)","repeatDelay",250,900,450);slider("Intervalle de répétition (ms)","repeatInterval",70,300,140);
        body.addView(text("Maintenir + ou − répète le changement de volume. Un intervalle plus court rend la répétition plus rapide.",13,MUTED));
    }
    private void advancedSettings(){
        intro("Rester disponible","Le service utilise une notification avec Arrêter. Il ne lit pas le contenu des autres applications.");
        action("Gérer l’autorisation de superposition",()->openOverlayPermission(false));
        action("Optimisation de batterie Android",()->open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        body.addView(text("Dans la liste Android, choisis PRISME puis l’option qui évite son optimisation si nécessaire. L’autorisation ne remplace pas le nettoyeur propre à la montre.",14,MUTED));
        action("Ouvrir les réglages de la montre",()->open(new Intent(Settings.ACTION_SETTINGS)));
        body.addView(text("Sur la DM100, cherche Plus > Background Cleaner > liste des applications. Exclus PRISME du nettoyage si cette liste existe. Garde les autres choix du nettoyeur. Les noms dépendent du firmware.",14,MUTED));
        body.addView(text("Si Android bloque une confirmation parce qu’une fenêtre est superposée, arrête temporairement la pastille avec Arrêter, puis termine la confirmation.",13,CAUTION));
        final TextView diagnostics=text("",13,MUTED);diagnostics.setTypeface(Typeface.MONOSPACE);diagnostics.setVisibility(View.GONE);
        action("Afficher le diagnostic local",()->{diagnostics.setText(FloatingVolumeService.diagnostics(activity));diagnostics.setVisibility(View.VISIBLE);});body.addView(diagnostics);
        action("Rétablir l’apparence de la pastille",()->{prefs.resetAppearance();changed();captureScroll();render();toast("Apparence rétablie · activation et démarrage conservés");});
        final LinearLayout confirm=column();confirm.setPadding(6,6,6,6);confirm.setVisibility(View.GONE);
        action("Réinitialiser toute la pastille",()->confirm.setVisibility(confirm.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
        confirm.addView(text("La pastille sera arrêtée, sa relance au démarrage désactivée et ses réglages remis à l’origine. Les styles et les autres données de PRISME sont conservés.",14,CAUTION));
        Button reset=button("Confirmer la réinitialisation"),cancel=button("Annuler");
        reset.setOnClickListener(v->{pendingActivation=false;if(listener!=null)listener.onEnable(false);prefs.resetAll();changed();captureScroll();render();toast("Pastille réinitialisée et arrêtée");});
        cancel.setOnClickListener(v->confirm.setVisibility(View.GONE));confirm.addView(reset,new LinearLayout.LayoutParams(-1,48));confirm.addView(cancel,new LinearLayout.LayoutParams(-1,48));body.addView(confirm);
    }
    private boolean legacyInstalled(){try{activity.getPackageManager().getPackageInfo(LEGACY_PACKAGE,0);return true;}catch(PackageManager.NameNotFoundException|RuntimeException error){return false;}}
    private boolean legacyRunning(){
        try{
            ActivityManager manager=(ActivityManager)activity.getSystemService(Activity.ACTIVITY_SERVICE);
            if(manager!=null)for(ActivityManager.RunningServiceInfo service:manager.getRunningServices(80))if(service.service!=null&&LEGACY_PACKAGE.equals(service.service.getPackageName()))return true;
        }catch(RuntimeException ignored){}
        return false;
    }
    private void updateLegacyWarning(){
        if(legacyWarning==null)return;
        if(!legacyInstalled()){legacyWarning.setText("");legacyWarning.setVisibility(View.GONE);return;}
        legacyWarning.setVisibility(View.VISIBLE);
        legacyWarning.setText(legacyRunning()?"L’ancienne Pastille Volume est encore active. Arrête-la depuis son application ou sa notification pour éviter deux pastilles en même temps. Elle ne sera pas désinstallée automatiquement."
                :"L’ancienne Pastille Volume est encore installée. Si elle est aussi activée, deux pastilles peuvent apparaître. Tu pourras l’arrêter ou la désinstaller toi-même après tes essais de PRISME.");
    }
    private boolean open(Intent intent){
        try{activity.startActivity(intent);return true;}
        catch(ActivityNotFoundException|SecurityException error){toast("Cette page n’est pas disponible sur ce firmware");return false;}
        catch(RuntimeException error){toast("Android n’a pas pu ouvrir cet écran");return false;}
    }
    private void changed(){if(listener!=null)listener.onChanged();updateStatus();}
    private void changeInt(String key,int value){prefs.putInt(key,value);changed();}
    private void intro(String title,String description){TextView heading=text(title,20,ACCENT);heading.setTypeface(null,Typeface.BOLD);body.addView(heading);body.addView(text(description,14,MUTED));}
    private void section(String title){TextView heading=text(title,18,ACCENT);heading.setPadding(2,16,2,6);body.addView(heading);}
    private void action(String label,Runnable action){Button b=button(label);b.setOnClickListener(v->action.run());body.addView(b,new LinearLayout.LayoutParams(-1,50));}
    private void slider(String title,String key,int min,int max,int fallback){
        int value=prefs.getInt(key,fallback);TextView label=text(title+" · "+value,16,INK);body.addView(label);
        SeekBar seek=new SeekBar(activity);seek.setMax(max-min);seek.setProgress(Math.max(0,Math.min(max-min,value-min)));seek.setPadding(8,0,8,0);body.addView(seek,new LinearLayout.LayoutParams(-1,46));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar bar){}
            public void onProgressChanged(SeekBar bar,int progress,boolean user){label.setText(title+" · "+(min+progress));}
            public void onStopTrackingTouch(SeekBar bar){changeInt(key,min+bar.getProgress());}
        });
    }
    private void toggle(String title,String key,boolean fallback){CheckBox box=check(title,prefs.getBoolean(key,fallback));box.setOnCheckedChangeListener((v,on)->{prefs.putBoolean(key,on);changed();});body.addView(box);}
    private void choice(String title,String[] values,int selected,IntChoice change){
        body.addView(text(title,15,MUTED));Button current=button(values[Math.max(0,Math.min(values.length-1,selected))]+"  ▾");body.addView(current,new LinearLayout.LayoutParams(-1,48));
        LinearLayout choices=column();choices.setPadding(8,2,8,4);choices.setVisibility(View.GONE);body.addView(choices);
        for(int i=0;i<values.length;i++){
            final int index=i;Button option=button(values[i]);option.setContentDescription(title+" : "+values[i]);
            option.setOnClickListener(v->{change.change(index);current.setText(values[index]+"  ▾");choices.setVisibility(View.GONE);if(expandedOptions==choices)expandedOptions=null;});choices.addView(option,new LinearLayout.LayoutParams(-1,48));
        }
        current.setOnClickListener(v->{boolean visible=choices.getVisibility()!=View.VISIBLE;if(expandedOptions!=null)expandedOptions.setVisibility(View.GONE);choices.setVisibility(visible?View.VISIBLE:View.GONE);expandedOptions=visible?choices:null;});
    }
    private interface IntChoice{void change(int value);}
    private static Integer parseColor(String value){String s=value==null?"":value.trim();if(!s.matches("#?[0-9a-fA-F]{6}"))return null;try{return Integer.parseInt(s.startsWith("#")?s.substring(1):s,16);}catch(NumberFormatException error){return null;}}
    private CheckBox check(String title,boolean selected){CheckBox box=new CheckBox(activity);box.setText(title);box.setTextColor(INK);box.setTextSize(TypedValue.COMPLEX_UNIT_PX,16);box.setButtonTintList(ColorStateList.valueOf(ACCENT));box.setMinHeight(46);box.setChecked(selected);return box;}
    private Button button(String label){Button b=new Button(activity);b.setText(label);b.setAllCaps(false);b.setTextColor(INK);b.setTextSize(TypedValue.COMPLEX_UNIT_PX,16);b.setPadding(6,0,6,0);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(44);b.setBackgroundTintList(ColorStateList.valueOf(SURFACE));return b;}
    private TextView text(String value,int size,int color){TextView t=new TextView(activity);t.setText(value);t.setTextColor(color);t.setTextSize(TypedValue.COMPLEX_UNIT_PX,size);t.setPadding(2,5,2,5);return t;}
    private LinearLayout column(){LinearLayout layout=new LinearLayout(activity);layout.setOrientation(LinearLayout.VERTICAL);layout.setBackgroundColor(BG);return layout;}
    private void toast(String value){I18n.makeToast(activity,value,Toast.LENGTH_SHORT).show();}
    private static final class Watcher implements TextWatcher{
        private final Runnable changed;Watcher(Runnable changed){this.changed=changed;}
        public void beforeTextChanged(CharSequence text,int start,int count,int after){}
        public void onTextChanged(CharSequence text,int start,int before,int count){changed.run();}
        public void afterTextChanged(Editable text){}
    }
}

package fr.dm100.prisme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Local daily tools; all windows use the same full-screen shell as PRISME. */
public final class ExperienceController {
    public interface Host {
        AlertDialog panel(String title, View content, boolean fixed);
        void action(String action);
        void app(AppEntry app);
        void settings(int tab);
        void changed();
    }
    private static final int BG=0xff0b1220,INK=0xffeef5ff,MUTED=0xffa3b6c9,ACCENT=0xff82ead2,SURFACE=0xff192638;
    private final Activity activity;private final DashboardPrefs prefs;private final Host host;private final List<AppEntry> apps;
    private final ChecklistStore tasks;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Runnable live,searchRefresh;
    public void resume(){if(live!=null){handler.removeCallbacks(live);handler.post(live);}if(searchRefresh!=null)searchRefresh.run();}
    public void refreshApps(){if(searchRefresh!=null)searchRefresh.run();}
    public void shutdown(){pause();live=null;searchRefresh=null;}
    private static final String[][] ACTIONS={
        {"weather","Météo et prévisions","ville temperature vent pluie soleil meteo"},{"widgetsSettings","Widgets des 22 styles","horloge musique meteo ajouter retirer personnaliser widget"},
        {"v6Home","Accueil modulaire","pages blocs theme v6"},{"multiTimers","Minuteurs nommés","alarme compte rebours"},{"mediaPanel","Musique","lecteur titre piste"},{"v6Tools","Outils PRISME 6","studio sense doctor secours"},
        {"rotationSettings","Orientation globale","portrait paysage forcer tourner ecran"},{"language","Langue / Language","arabe anglais allemand chinois turc russe francais"},{"shortcutShelf","Mes raccourcis","raccourcis favoris commandes rapides"},{"today","Aujourd’hui","accueil quotidien favoris recents"},{"checklist","Liste de tâches","courses rappels liste cocher"},
        {"notes","Carnet local","notes texte idee"},{"timer","Minuteur","alarme compte a rebours"},{"focus","Concentration","pomodoro travail repos"},
        {"stopwatch","Chronomètre","temps tours sport"},{"deskclock","Horloge de table","nuit veille heure"},
        {"controlCenter","Centre de contrôle","wifi bluetooth luminosite son"},{"floatingVolumeSettings","Pastille de volume","bouton flottant audio opacite"},
         {"batteryIndicatorSettings","Indicateur batterie","24 styles batterie couleurs seuils position taille"},
         {"musicPlayerSettings","Lecteur musique","Spotify Deezer YouTube titres piste suivante lecteur musique"},
        {"styles","Styles PRISME","apparence theme cadran"},{"presets","Réglages rapides","lecture discret confort preset"},
        {"device","État de la montre","batterie temperature stockage memoire"},{"displayTest","Test visuel de l’écran","bandes noir largeur hauteur mire bord"},
        {"settingsSearch","Trouver un réglage","parametres rechercher"},{"shortcuts","Raccourcis des styles","personnaliser boutons"},
        {"clock","Cadran de la montre","retour accueil origine"}};
    private static final String[][] SETTINGS={
        {"language","Langue / Language","arabe anglais allemand chinois turc russe francais"},{"rotationSettings","Orientation globale","portrait paysage rotation forcer tourner"},{"0","Taille globale, zoom et marges","ecran taille plein bord largeur densite"},
        {"1","Styles, icônes et lisibilité","police couleur noms colonnes espacement"},
        {"2","Raccourcis des styles","personnaliser boutons applications"},
        {"3","Applications, favoris et espaces","masque trier recent"},
        {"4","Gestes, minuteur et outils","glisser double appui horloge taches notes"},
        {"7","Sauvegardes et profils","importer exporter restaurer retour cadran"},
        {"controlCenterSettings","Centre de contrôle","transparence opacite ios android geste wifi bluetooth"},
        {"floatingVolumeSettings","Pastille de volume","forme taille cercle position opacite audio"}};
    public ExperienceController(Activity a,DashboardPrefs p,List<AppEntry> entries,Host h){activity=a;prefs=p;apps=entries;host=h;tasks=p.getChecklist();}
    public boolean open(String action){
        switch(action){case "today":today();return true;case "checklist":checklist();return true;case "deskclock":deskClock();return true;
            case "device":device();return true;case "displayTest":displayTest();return true;case "presets":presets();return true;
            case "settingsSearch":search(true);return true;default:return false;}
    }
    private static void setText(TextView view,String value){if(!value.contentEquals(view.getText()))view.setText(value);}
    public void pause(){handler.removeCallbacksAndMessages(null);}
    private LinearLayout column(){LinearLayout b=new LinearLayout(activity);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(6,6,6,10);return b;}
    private TextView text(String value,int size,int color){TextView t=new TextView(activity);t.setText(value);t.setTextColor(color);t.setTextSize(TypedValue.COMPLEX_UNIT_PX,size);t.setPadding(8,5,8,5);return t;}
    private TextView heading(String value){TextView t=text(value,18,ACCENT);t.setTypeface(null,Typeface.BOLD);t.setPadding(8,16,8,7);return t;}
    private GradientDrawable surface(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(14);d.setStroke(1,0xff2a4053);return d;}
    private Button button(String title,Runnable action){Button b=new Button(activity);b.setText(title);b.setAllCaps(false);b.setTextColor(INK);b.setTextSize(TypedValue.COMPLEX_UNIT_PX,17);b.setBackground(surface(SURFACE));b.setMinHeight(48);b.setPadding(9,5,9,5);b.setOnClickListener(v->action.run());return b;}
    private void add(LinearLayout root,String title,Runnable action){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,52);p.setMargins(0,3,0,3);root.addView(button(title,action),p);}
    private void actions(LinearLayout root,String[] titles,String[] ids){LinearLayout row=new LinearLayout(activity);for(int i=0;i<titles.length;i++){final String id=ids[i];LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,54,1);p.setMargins(3,3,3,3);row.addView(button(titles[i],()->host.action(id)),p);}root.addView(row);}
    private String label(AppEntry a){String custom=prefs.getCustomLabel(a.id);return custom.isEmpty()?a.label:custom;}
    private void appRow(LinearLayout list,AppEntry a){
        LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(10,6,10,6);row.setBackground(surface(SURFACE));
        ImageView icon=new ImageView(activity);icon.setImageBitmap(a.icon);row.addView(icon,new LinearLayout.LayoutParams(38,38));
        TextView title=text(label(a)+(prefs.isFavorite(a.id)?"  ★":""),17,INK);I18n.userText(title);row.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        row.setFocusable(true);row.setContentDescription("Ouvrir "+label(a));row.setOnClickListener(v->host.app(a));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,54);p.setMargins(0,3,0,3);list.addView(row,p);
    }
    public void today(){
        LinearLayout b=column();DeviceSnapshot s=DeviceSnapshot.read(activity);
        LinearLayout hero=column();hero.setOrientation(LinearLayout.HORIZONTAL);hero.setGravity(Gravity.CENTER_VERTICAL);hero.setBackground(surface(0xff17353f));
        TextView time=text(new java.text.SimpleDateFormat("HH:mm",I18n.locale()).format(new Date()),46,INK);time.setTypeface(null,Typeface.BOLD);time.setGravity(Gravity.CENTER_VERTICAL);hero.addView(time,new LinearLayout.LayoutParams(175,100));
        LinearLayout details=column();details.addView(text(new java.text.SimpleDateFormat("EEEE d MMMM",I18n.locale()).format(new Date()),18,MUTED));
        details.addView(text("Batterie "+s.batteryPct+" %"+(s.charging?" · en charge":""),16,ACCENT));details.addView(text(tasks.remaining()+" tâche(s) à faire",16,INK));hero.addView(details,new LinearLayout.LayoutParams(0,-2,1));b.addView(hero);
        actions(b,new String[]{"⌕ Rechercher","☷ Tâches","◷ Minuteur"},new String[]{"search","checklist","timer"});
        actions(b,new String[]{"Contrôles","Carnet","Horloge"},new String[]{"controlCenter","notes","deskclock"});
        add(b,"Mes raccourcis",()->host.action("shortcutShelf"));
        TextView countdown=text("",18,ACCENT);b.addView(countdown);
        b.addView(heading("FAVORIS"));int count=0;for(AppEntry a:apps)if(!prefs.isHidden(a.id)&&prefs.isFavorite(a.id)){appRow(b,a);if(++count==6)break;}
        if(count==0)b.addView(text("Appui long sur une application pour l’ajouter aux favoris.",15,MUTED));
        List<AppEntry> recent=new ArrayList<>(apps);Collections.sort(recent,(a,c)->Integer.compare(prefs.getInt("launch:"+c.id,0),prefs.getInt("launch:"+a.id,0)));
        b.addView(heading("RÉCEMMENT OUVERTES DEPUIS PRISME"));count=0;
        for(AppEntry a:recent)if(!prefs.isHidden(a.id)&&prefs.getInt("launch:"+a.id,0)>0){appRow(b,a);if(++count==5)break;}
        if(count==0)b.addView(text("Tes prochaines ouvertures apparaîtront ici.",15,MUTED));
        add(b,"Confort, lecture et mode discret",()->host.action("presets"));add(b,"État de la montre",()->host.action("device"));
        AlertDialog d=host.panel("Aujourd’hui",b,false);
        live=new Runnable(){public void run(){if(!d.isShowing())return;long left=TimerEngine.remaining(activity);countdown.setVisibility(left>0?View.VISIBLE:View.GONE);setText(countdown,left>0?(TimerEngine.paused(activity)?"En pause · ":"Minuteur · ")+duration(left):"Prêt pour ta journée");setText(time,new java.text.SimpleDateFormat("HH:mm",I18n.locale()).format(new Date()));handler.postDelayed(this,1000);}};
        handler.post(live);d.setOnDismissListener(x->{pause();live=null;});
    }
    public void search(boolean settingsOnly){
        LinearLayout shell=column();EditText query=new EditText(activity);query.setSingleLine(true);query.setTextColor(INK);query.setHintTextColor(MUTED);query.setTextSize(TypedValue.COMPLEX_UNIT_PX,18);
        query.setHint(settingsOnly?"Un réglage : volume, taille, gestes…":"Applications, outils et commandes…");
        query.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);
        shell.addView(query,new LinearLayout.LayoutParams(-1,48));ScrollView scroller=new ScrollView(activity);LinearLayout results=column();scroller.addView(results);shell.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
        AlertDialog d=host.panel(settingsOnly?"Trouver un réglage":"Recherche PRISME",shell,true);
        Runnable populate=()->{
            results.removeAllViews();String q=query.getText().toString();int count=0;
            if(settingsOnly){for(String[] setting:SETTINGS)if(SearchIndex.matches(I18n.t(activity,setting[1]+" "+setting[2])+" "+setting[1]+" "+setting[2],q)){add(results,setting[1],()->{d.dismiss();try{host.settings(Integer.parseInt(setting[0]));}catch(NumberFormatException e){host.action(setting[0]);}});count++;}}
            else {
                results.addView(heading("COMMANDES"));for(String[] a:ACTIONS)if(SearchIndex.matches(I18n.t(activity,a[1]+" "+a[2])+" "+a[1]+" "+a[2],q)){add(results,a[1],()->{d.dismiss();host.action(a[0]);});count++;}
                results.addView(heading("APPLICATIONS"));for(AppEntry a:apps)if(!prefs.isHidden(a.id)&&SearchIndex.matches(label(a)+" "+a.packageName,q)){appRow(results,a);count++;}
            }
            if(count==0)results.addView(text("Aucun résultat. Essaie un autre mot.",17,MUTED));scroller.scrollTo(0,0);
        };
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){populate.run();}public void afterTextChanged(Editable e){}});searchRefresh=populate;d.setOnDismissListener(x->{if(searchRefresh==populate)searchRefresh=null;});populate.run();
    }
    public void notes(){
        LinearLayout b=column();TextView status=text("",14,ACCENT);EditText input=new EditText(activity);input.setTextColor(INK);input.setHintTextColor(MUTED);input.setGravity(Gravity.TOP);
        input.setTextSize(TypedValue.COMPLEX_UNIT_PX,20);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2000)});
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);input.setHint("Une idée à garder…");input.setText(prefs.getString("notes",""));
        b.addView(status);b.addView(input,new LinearLayout.LayoutParams(-1,0,1));status.setText("Enregistré sur la montre · "+input.length()+" / 2000");
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int c){prefs.putString("notes",s.toString());status.setText("Enregistré · "+s.length()+" / 2000");}public void afterTextChanged(Editable e){}});
        add(b,"Copier le carnet",()->{ClipboardManager clip=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);if(clip!=null){clip.setPrimaryClip(ClipData.newPlainText("Carnet PRISME",input.getText()));I18n.makeToast(activity,"Carnet copié",Toast.LENGTH_SHORT).show();}});
        host.panel("Carnet · sauvegarde automatique",b,true);
    }
    private void checklist(){
        LinearLayout shell=column();LinearLayout entry=new LinearLayout(activity);EditText input=new EditText(activity);input.setSingleLine(true);input.setTextColor(INK);input.setHintTextColor(MUTED);input.setHint("Une nouvelle tâche…");input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});input.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);
        entry.addView(input,new LinearLayout.LayoutParams(0,52,1));Button add=button("Ajouter",()->{});entry.addView(add,new LinearLayout.LayoutParams(100,52));shell.addView(entry);
        TextView summary=text("",15,ACCENT);shell.addView(summary);ScrollView scroll=new ScrollView(activity);LinearLayout rows=column();scroll.addView(rows);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Runnable populate=()->{
            rows.removeAllViews();JSONArray items=tasks.read();summary.setText(tasks.remaining()+" à faire · "+items.length()+" / 40 · appui long pour supprimer");
            if(items.length()==0)rows.addView(text("Ta liste est prête. Ajoute une course, une idée ou une tâche.",18,MUTED));
            for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);String id=item.optString("id");CheckBox check=new CheckBox(activity);check.setText(item.optString("text"));I18n.userText(check);check.setChecked(item.optBoolean("done"));check.setTextColor(item.optBoolean("done")?MUTED:INK);check.setTextSize(TypedValue.COMPLEX_UNIT_PX,19);check.setMinHeight(54);
                check.setOnCheckedChangeListener((v,on)->{tasks.toggle(id);check.setTextColor(on?MUTED:INK);summary.setText(tasks.remaining()+" à faire · "+tasks.read().length()+" / 40 · appui long pour supprimer");});
                check.setOnLongClickListener(v->{LinearLayout confirm=column();confirm.addView(text(item.optString("text"),19,INK));AlertDialog dialog=host.panel("Supprimer cette tâche ?",confirm,false);add(confirm,"Supprimer",()->{tasks.remove(id);dialog.dismiss();rows.removeView(check);summary.setText(tasks.remaining()+" à faire · "+tasks.read().length()+" / 40 · appui long pour supprimer");});return true;});rows.addView(check,new LinearLayout.LayoutParams(-1,-2));}
        };
        add.setOnClickListener(v->{if(tasks.add(input.getText().toString())){input.setText("");populate.run();}else I18n.makeToast(activity,"Saisis une tâche · 40 maximum",Toast.LENGTH_SHORT).show();});
        add(shell,"Retirer les tâches terminées",()->{tasks.clearDone();populate.run();});populate.run();host.panel("Ma liste de tâches",shell,true);
    }
    private void checklistRefresh(LinearLayout rows,TextView summary){rows.removeAllViews();summary.setText(tasks.remaining()+" à faire");JSONArray list=tasks.read();for(int i=0;i<list.length();i++){JSONObject item=list.optJSONObject(i);CheckBox c=new CheckBox(activity);c.setText(item.optString("text"));I18n.userText(c);c.setChecked(item.optBoolean("done"));c.setTextColor(INK);c.setTextSize(TypedValue.COMPLEX_UNIT_PX,19);c.setMinHeight(52);String id=item.optString("id");c.setOnCheckedChangeListener((v,on)->{tasks.toggle(id);summary.setText(tasks.remaining()+" à faire");});rows.addView(c);}}
    private void presets(){
        LinearLayout b=column();b.addView(text("Un réglage de confort se rétablit en un geste. Tes favoris et tes raccourcis sont conservés.",17,MUTED));
        add(b,"Lecture · grandes icônes et textes",()->preset(0));add(b,"Discret · fond sombre, sans animations",()->preset(1));add(b,"Nova · tableau de bord moderne",()->preset(2));
        add(b,"Annuler le dernier réglage rapide",()->{String saved=activity.getSharedPreferences("prisme_experience",0).getString("beforePreset","");try{JSONObject values=new JSONObject(saved);for(String key:new String[]{"iconSize","textSize","columns","styleScale","theme","wallpaper"})prefs.putInt(key,values.getInt(key));for(String key:new String[]{"animations","haptic","keepScreenOn"})prefs.putBoolean(key,values.getBoolean(key));host.changed();I18n.makeToast(activity,"Réglage précédent rétabli",Toast.LENGTH_SHORT).show();}catch(Exception e){I18n.makeToast(activity,"Aucun réglage précédent disponible",Toast.LENGTH_SHORT).show();}});host.panel("Réglages rapides",b,false);
    }
    private void preset(int type){
        try{JSONObject values=new JSONObject();for(String key:new String[]{"iconSize","textSize","columns","styleScale","theme","wallpaper"})values.put(key,prefs.exportJSONObject().getJSONObject("settings").getInt(key));for(String key:new String[]{"animations","haptic","keepScreenOn"})values.put(key,prefs.exportJSONObject().getJSONObject("settings").getBoolean(key));activity.getSharedPreferences("prisme_experience",0).edit().putString("beforePreset",values.toString()).commit();}catch(Exception e){return;}
        if(type==0){prefs.putInt("iconSize",84);prefs.putInt("textSize",20);prefs.putInt("columns",4);prefs.putInt("styleScale",110);}
        else if(type==1){prefs.putInt("theme",21);prefs.putInt("wallpaper",5);prefs.putBoolean("animations",false);prefs.putBoolean("haptic",false);prefs.putBoolean("keepScreenOn",false);}
        else{prefs.putInt("theme",20);prefs.putInt("wallpaper",0);prefs.putInt("columns",4);prefs.putInt("iconSize",72);prefs.putInt("textSize",17);}
        host.changed();I18n.makeToast(activity,"Réglage appliqué · Annuler reste disponible",Toast.LENGTH_SHORT).show();
    }
    private void deskClock(){
        LinearLayout b=column();b.setGravity(Gravity.CENTER);b.setBackgroundColor(Color.BLACK);TextView time=text("",90,INK),date=text("",21,MUTED),hint=text("Double appui sur l’heure pour fermer",15,ACCENT);time.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));time.setGravity(Gravity.CENTER);date.setGravity(Gravity.CENTER);
        b.addView(time);b.addView(date);b.addView(hint);AlertDialog d=host.panel("Horloge de table",b,false);
        final long[] tap={0};time.setOnClickListener(v->{long now=SystemClock.uptimeMillis();if(now-tap[0]<350)d.dismiss();tap[0]=now;});
        boolean keep=prefs.getBoolean("deskKeepAwake",false);if(keep)d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        live=new Runnable(){public void run(){if(!d.isShowing())return;setText(time,new java.text.SimpleDateFormat("HH:mm",I18n.locale()).format(new Date()));setText(date,new java.text.SimpleDateFormat("EEEE d MMMM",I18n.locale()).format(new Date()));handler.postDelayed(this,1000);}};handler.post(live);d.setOnDismissListener(x->{pause();live=null;});
    }
    private void device(){
        DeviceSnapshot s=DeviceSnapshot.read(activity);LinearLayout b=column();Intent battery=activity.registerReceiver(null,new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        b.addView(text("Batterie "+s.batteryPct+" %"+(s.charging?" · en charge":""),27,INK));
        if(battery!=null)b.addView(text(String.format(I18n.locale(),"Température batterie : %.1f °C",battery.getIntExtra("temperature",0)/10f),18,MUTED));
        b.addView(text(String.format(I18n.locale(),"Stockage libre : %.1f Go\nMémoire utilisée : %d %%",s.freeStorageGb,s.memoryUsedPct),20,ACCENT));
        actions(b,new String[]{"Batterie Android","Affichage Android"},new String[]{"androidBattery","androidDisplay"});
        add(b,"Vérifier les accès de PRISME",()->{LinearLayout access=column();access.addView(text("Affichage par-dessus : "+(Settings.canDrawOverlays(activity)?"autorisé":"à autoriser pour la pastille")+"\nLuminosité système : "+(Settings.System.canWrite(activity)?"autorisée":"réglage limité à PRISME"),18,INK));host.panel("Accès de PRISME",access,false);});
        add(b,"Test visuel des bords de l’écran",()->host.action("displayTest"));host.panel("État de la montre",b,false);
    }
    private void displayTest(){
        LinearLayout b=column();Point size=new Point();activity.getWindowManager().getDefaultDisplay().getRealSize(size);
        b.addView(text("Surface fournie par Android : "+size.x+" × "+size.y+" pixels",21,INK));
        b.addView(text("La mire utilise cette surface entière. Elle ne permet pas à elle seule de déterminer si une bande physique est affichable.",17,MUTED));
        for(int color:new int[]{Color.WHITE,Color.RED,Color.GREEN,Color.BLUE})add(b,color==Color.WHITE?"Mire blanche bord à bord":color==Color.RED?"Mire rouge":color==Color.GREEN?"Mire verte":"Mire bleue",()->host.action("testColor:"+color));
        add(b,"Ouvrir les réglages d’affichage Android",()->host.action("androidDisplay"));host.panel("Diagnostic de l’affichage",b,false);
    }
    public static String duration(long millis){long sec=Math.max(0,(millis+999)/1000);return String.format(I18n.locale(),"%02d:%02d",sec/60,sec%60);}
}

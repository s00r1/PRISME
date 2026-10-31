package fr.dm100.prisme;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Local settings for the control panel. Does not grant permissions or change hardware itself. */
public final class ControlCenterSettings {
    public interface Listener {
        void onChanged();
        void onPreview();
    }
    private static final int BG=0xff101925, SURFACE=0xff203044, INK=0xffeef5ff,
            MUTED=0xffa9bad0, ACCENT=0xff62d9cc, CAUTION=0xffffcf8c;
    private static final int MAX_TILES=ControlCenterPrefs.MAX_TILES;
    private static final String[] CATEGORIES={"Style", "Aspect", "Contenu", "Disposition", "Gestes", "Commandes"};
    private static final String[] COLOR_NAMES={"Menthe", "Cyan", "Bleu", "Violet", "Orange", "Rose"};
    private static final int[] COLORS={0xff62d9cc,0xff59c9ef,0xff78a9ff,0xffbe9bff,0xffffb275,0xfff79fc2};
    private final Activity activity;
    private final ControlCenterPrefs prefs;
    private final List<AppEntry> apps=new ArrayList<>();
    private final Listener listener;
    private final int[] scrollPositions=new int[CATEGORIES.length];
    private final List<Button> tabs=new ArrayList<>();
    private Dialog dialog;
    private ScrollView scroller;
    private LinearLayout body, expandedOptions;
    private TextView brightnessAccessLabel, dndAccessLabel;
    private Runnable appPickerRefresh;
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener;
    private int category, contentMode, lastWidth, lastHeight;

    public ControlCenterSettings(Activity activity,ControlCenterPrefs prefs,List<AppEntry> apps,Listener listener){
        if(activity==null||prefs==null)throw new IllegalArgumentException("Activité et réglages nécessaires");
        this.activity=activity;this.prefs=prefs;this.listener=listener;
        setApps(apps);
    }
    /** Replace the app snapshot without losing a search already typed in the open picker. */
    public void setApps(List<AppEntry> entries){
        List<AppEntry> next=entries==null?Collections.<AppEntry>emptyList():new ArrayList<>(entries);
        apps.clear();for(AppEntry entry:next)if(entry!=null)apps.add(entry);
        if(isShowing()&&category==2){
            if(contentMode==2&&appPickerRefresh!=null)appPickerRefresh.run();
            else if(contentMode==0){captureScroll();render();}
        }
    }
    public boolean isShowing(){return dialog!=null&&dialog.isShowing();}
    public void show(){
        if(activity.isFinishing()||activity.isDestroyed())return;
        if(isShowing()){refit();return;}
        final Dialog opened=new Dialog(activity){
            @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)updatePermissionLabels();}
        };
        dialog=opened;lastWidth=lastHeight=0;
        opened.requestWindowFeature(Window.FEATURE_NO_TITLE);opened.setCanceledOnTouchOutside(false);
        LinearLayout shell=column();shell.setPadding(10,8,10,8);
        TextView title=text("Centre de contrôle",22,INK);title.setTypeface(null,Typeface.BOLD);shell.addView(title);
        shell.addView(text("Un panneau à ta mesure, indépendant du style du bureau.",13,MUTED));
        tabs.clear();
        for(int row=0;row<2;row++){
            LinearLayout line=new LinearLayout(activity);line.setOrientation(LinearLayout.HORIZONTAL);
            for(int col=0;col<3;col++){
                final int index=row*3+col;Button tab=button(CATEGORIES[index]);tab.setTextSize(TypedValue.COMPLEX_UNIT_PX,15);
                tab.setOnClickListener(v->selectCategory(index));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,44,1);lp.setMargins(2,1,2,1);line.addView(tab,lp);tabs.add(tab);
            }
            shell.addView(line,new LinearLayout.LayoutParams(-1,46));
        }
        scroller=new ScrollView(activity);scroller.setFillViewport(false);scroller.setClipToPadding(false);scroller.setPadding(0,4,2,4);
        body=column();body.setPadding(2,4,4,12);scroller.addView(body,new ScrollView.LayoutParams(-1,-2));
        shell.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout footer=new LinearLayout(activity);footer.setOrientation(LinearLayout.HORIZONTAL);
        Button close=button("Fermer"), preview=button("Aperçu");preview.setTextColor(ACCENT);
        close.setOnClickListener(v->dismiss());preview.setOnClickListener(v->{dismiss();if(listener!=null)listener.onPreview();});
        LinearLayout.LayoutParams first=new LinearLayout.LayoutParams(0,48,1);first.rightMargin=6;footer.addView(close,first);
        footer.addView(preview,new LinearLayout.LayoutParams(0,48,1));shell.addView(footer,new LinearLayout.LayoutParams(-1,48));
        opened.setContentView(shell,new android.view.ViewGroup.LayoutParams(-1,-1));
        opened.setOnDismissListener(d->{
            captureScroll();
            Window w=opened.getWindow();
            if(w!=null&&layoutListener!=null&&w.getDecorView().getViewTreeObserver().isAlive())w.getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
            if(dialog==opened){dialog=null;scroller=null;body=null;tabs.clear();expandedOptions=null;brightnessAccessLabel=null;dndAccessLabel=null;layoutListener=null;appPickerRefresh=null;}
        });
        opened.show();I18n.bind(opened.getWindow().getDecorView());fitWindow(true);
        layoutListener=()->fitWindow(false);
        opened.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
        render();
    }
    public void dismiss(){if(dialog!=null)dialog.dismiss();}
    /** Called by the host after a rotation; the current page and its scroll are retained. */
    public void refit(){fitWindow(true);}
    private void fitWindow(boolean force){
        if(dialog==null||dialog.getWindow()==null)return;
        Point size=new Point();activity.getWindowManager().getDefaultDisplay().getRealSize(size);
        if(!force&&size.x==lastWidth&&size.y==lastHeight)return;
        lastWidth=size.x;lastHeight=size.y;
        Window window=dialog.getWindow();window.setLayout(size.x,size.y);
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));window.getDecorView().setPadding(0,0,0,0);window.getDecorView().setSystemUiVisibility(5894);
        window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setGravity(Gravity.CENTER);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        WindowManager.LayoutParams attributes=window.getAttributes();attributes.dimAmount=.6f;window.setAttributes(attributes);
    }
    private void captureScroll(){if(scroller!=null&&contentMode==0)scrollPositions[category]=scroller.getScrollY();}
    private void selectCategory(int next){
        if(next<0||next>=CATEGORIES.length)return;
        captureScroll();category=next;contentMode=0;render();
    }
    private void render(){
        if(body==null)return;
        body.removeAllViews();expandedOptions=null;brightnessAccessLabel=null;dndAccessLabel=null;appPickerRefresh=null;
        for(int i=0;i<tabs.size();i++){
            boolean selected=i==category;Button tab=tabs.get(i);tab.setSelected(selected);
            tab.setTextColor(selected?BG:INK);tab.setBackgroundTintList(ColorStateList.valueOf(selected?ACCENT:SURFACE));
            tab.setContentDescription(CATEGORIES[i]+(selected?" · catégorie active":" · ouvrir la catégorie"));
        }
        switch(category){
            case 0:styleSettings();break;
            case 1:appearanceSettings();break;
            case 2:contentSettings();break;
            case 3:layoutSettings();break;
            case 4:gestureSettings();break;
            default:commandHelp();break;
        }
        final ScrollView target=scroller;final int y=contentMode==0?scrollPositions[category]:0;
        if(target!=null)target.post(()->{if(scroller==target)target.scrollTo(0,y);});
    }
    private void styleSettings(){
        intro("Deux ambiances","Le style du centre de contrôle se choisit séparément des 22 styles PRISME.");
        choice("Présentation",new String[]{"iOS · cartes arrondies","Android · tuiles rapides"},prefs.getInt("style",0),n->changeInt("style",n));
        toggle("Fond sombre","dark",true);
        body.addView(text("Couleur principale",16,INK));
        for(int row=0;row<2;row++){
            LinearLayout line=new LinearLayout(activity);
            for(int col=0;col<3;col++){
                final int index=row*3+col;Button color=button(COLOR_NAMES[index]);color.setTextColor(COLORS[index]);
                boolean selected=!prefs.getBoolean("customAccent",false)&&prefs.getInt("accent",0)==index;
                if(selected)color.setText("✓ "+COLOR_NAMES[index]);
                color.setOnClickListener(v->{prefs.putInt("accent",index);prefs.putBoolean("customAccent",false);changed();captureScroll();render();});
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,46,1);lp.setMargins(2,1,2,1);line.addView(color,lp);
            }
            body.addView(line);
        }
        CheckBox custom=check("Utiliser ma couleur personnalisée",prefs.getBoolean("customAccent",false));body.addView(custom);
        LinearLayout editor=column();editor.setPadding(6,2,6,8);body.addView(editor);
        EditText hex=new EditText(activity);hex.setSingleLine(true);hex.setSelectAllOnFocus(true);
        hex.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        hex.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);
        hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});hex.setTextColor(INK);hex.setHintTextColor(MUTED);hex.setTextSize(TypedValue.COMPLEX_UNIT_PX,18);
        hex.setText(String.format(Locale.US,"#%06X",prefs.getInt("customColor",0x62d9cc)));hex.setHint("#RRGGBB");
        editor.addView(hex,new LinearLayout.LayoutParams(-1,48));
        View sample=new View(activity);editor.addView(sample,new LinearLayout.LayoutParams(-1,28));
        TextView validity=text("",13,MUTED);editor.addView(validity);
        Button apply=button("Appliquer cette couleur");editor.addView(apply,new LinearLayout.LayoutParams(-1,48));
        Runnable validate=()->{
            Integer value=parseColor(hex.getText().toString());apply.setEnabled(value!=null);
            if(value==null){sample.setBackgroundColor(SURFACE);validity.setText("Entre six chiffres, par exemple #62D9CC.");validity.setTextColor(CAUTION);}
            else{sample.setBackgroundColor(0xff000000|value);validity.setText(String.format(Locale.US,"Aperçu : #%06X",value));validity.setTextColor(MUTED);}
        };
        hex.addTextChangedListener(new SimpleWatcher(validate));validate.run();
        editor.setVisibility(custom.isChecked()?View.VISIBLE:View.GONE);
        custom.setOnCheckedChangeListener((v,selected)->{
            prefs.putBoolean("customAccent",selected);editor.setVisibility(selected?View.VISIBLE:View.GONE);changed();
        });
        apply.setOnClickListener(v->{Integer value=parseColor(hex.getText().toString());if(value==null){toast("Couleur invalide : utilise #RRGGBB");return;}prefs.putInt("customColor",value);prefs.putBoolean("customAccent",true);custom.setChecked(true);changed();toast("Couleur du centre enregistrée");});
        action("Rétablir uniquement l’aspect",()->{prefs.resetAppearance();changed();captureScroll();render();toast("Aspect rétabli · commandes et gestes conservés");});
    }
    private void appearanceSettings(){
        intro("Lisibilité du panneau","Ces réglages ne modifient pas les icônes ou le zoom du bureau PRISME.");
        slider("Opacité du panneau (%)","opacity",20,100,68);
        slider("Opacité des tuiles (%)","tileOpacity",0,100,28);
        slider("Assombrir l’arrière-plan (%)","scrimOpacity",0,60,8);
        body.addView(text("Moins d’opacité = plus de transparence. Le texte et les icônes gardent leur lisibilité. 100 % rend le fond opaque.",13,MUTED));slider("Taille générale (%)","scale",80,125,100);
        slider("Arrondi des cartes","radius",0,36,24);toggle("Afficher les noms des commandes","labels",true);
        toggle("Afficher l’heure et l’état de la montre","status",true);
        action("Rétablir uniquement l’aspect",()->{prefs.resetAppearance();changed();captureScroll();render();toast("Aspect rétabli · commandes et gestes conservés");});
    }
    private void contentSettings(){
        if(contentMode!=0){addTileChooser(contentMode==2);return;}
        final List<String> tiles=prefs.getTiles();
        intro("Mes commandes · "+tiles.size()+" / "+MAX_TILES,"Monte, descends ou retire une commande. Chaque commande ne figure qu’une fois.");
        LinearLayout addRow=new LinearLayout(activity);
        Button addCommand=button("+ Commande"),addApp=button("+ Application");
        addCommand.setEnabled(tiles.size()<MAX_TILES);addApp.setEnabled(tiles.size()<MAX_TILES);
        addCommand.setOnClickListener(v->{captureScroll();contentMode=1;render();});addApp.setOnClickListener(v->{captureScroll();contentMode=2;render();});
        LinearLayout.LayoutParams addLeft=new LinearLayout.LayoutParams(0,48,1);addLeft.rightMargin=4;addRow.addView(addCommand,addLeft);addRow.addView(addApp,new LinearLayout.LayoutParams(0,48,1));body.addView(addRow);
        if(tiles.size()>=MAX_TILES)body.addView(text("Retire une commande avant d’en ajouter une autre.",13,CAUTION));
        if(tiles.isEmpty())body.addView(text("Aucune tuile. Ajoute une commande ou une application. Les curseurs ci-dessous restent indépendants.",15,MUTED));
        for(int i=0;i<tiles.size();i++){
            final int index=i;final String id=tiles.get(i);String name=tileName(id);
            LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,3,0,3);
            TextView title=text((i+1)+" · "+name,16,INK);row.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            Button up=button("↑"),down=button("↓"),remove=button("×");
            up.setContentDescription("Monter "+name);down.setContentDescription("Descendre "+name);remove.setContentDescription("Retirer "+name);
            up.setEnabled(i>0);down.setEnabled(i+1<tiles.size());
            up.setOnClickListener(v->moveTile(index,-1));down.setOnClickListener(v->moveTile(index,1));
            remove.setOnClickListener(v->{List<String> next=new ArrayList<>(prefs.getTiles());if(index<next.size()&&next.get(index).equals(id)){next.remove(index);saveTiles(next);}});
            row.addView(up,new LinearLayout.LayoutParams(44,48));row.addView(down,new LinearLayout.LayoutParams(44,48));row.addView(remove,new LinearLayout.LayoutParams(44,48));
            body.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
        section("Curseurs indépendants");toggle("Curseur du volume des médias","volume",true);toggle("Curseur de luminosité","brightness",true);
        body.addView(text("La luminosité reste locale à PRISME sans l’autorisation Android décrite dans Commandes.",13,MUTED));
    }
    private void addTileChooser(boolean applications){
        intro(applications?"Ajouter une application":"Ajouter une commande","Choisis une entrée pour l’ajouter à la fin de ta liste.");
        action("← Retour à mes commandes",()->{contentMode=0;render();});
        EditText search=new EditText(activity);search.setSingleLine(true);search.setTextColor(INK);search.setHintTextColor(MUTED);
        search.setHint(applications?"Rechercher une application":"Rechercher une commande");search.setTextSize(TypedValue.COMPLEX_UNIT_PX,17);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);
        body.addView(search,new LinearLayout.LayoutParams(-1,48));
        LinearLayout results=column();results.setPadding(0,2,0,6);body.addView(results);
        Runnable populate=()->{
            results.removeAllViews();String query=search.getText().toString().trim().toLowerCase(I18n.locale());int count=0;
            if(applications){
                for(AppEntry app:apps){
                    if(!app.label.toLowerCase(I18n.locale()).contains(query))continue;
                    addCandidate(results,"app:"+app.id,app.label);count++;
                }
            }else for(String id:ControlCenterController.CATALOG){
                String label=tileName(id);if(!label.toLowerCase(I18n.locale()).contains(query))continue;
                addCandidate(results,id,label);count++;
            }
            if(count==0)results.addView(text(applications&&apps.isEmpty()?"La liste des applications n’est pas encore chargée. Ferme puis réessaie.":"Aucun résultat",15,MUTED));
        };
        if(applications)appPickerRefresh=populate;
        search.addTextChangedListener(new SimpleWatcher(populate));populate.run();
    }
    private void addCandidate(LinearLayout list,String id,String name){
        boolean selected=containsTile(prefs.getTiles(),id);Button choice=button((selected?"✓ ":"+ ")+name);choice.setEnabled(!selected);
        choice.setContentDescription((selected?"Déjà ajouté : ":"Ajouter ")+name);
        choice.setOnClickListener(v->{List<String> next=new ArrayList<>(prefs.getTiles());if(next.size()>=MAX_TILES){toast("Maximum 24 commandes");return;}if(!containsTile(next,id)){next.add(id);saveTiles(next);}});
        list.addView(choice,new LinearLayout.LayoutParams(-1,50));
    }
    private void moveTile(int index,int step){
        List<String> next=new ArrayList<>(prefs.getTiles());int target=index+step;
        if(index<0||index>=next.size()||target<0||target>=next.size())return;
        Collections.swap(next,index,target);saveTiles(next);
    }
    private void saveTiles(List<String> next){
        try{prefs.setTiles(next);captureScroll();contentMode=0;changed();render();}
        catch(IllegalArgumentException error){toast("Cette liste de commandes n’est pas valide");}
    }
    private boolean containsTile(List<String> tiles,String wanted){
        for(String id:tiles){
            if(id.equals(wanted))return true;
            if(id.startsWith("app:")&&wanted.startsWith("app:")){
                ComponentName one=ComponentName.unflattenFromString(id.substring(4)),two=ComponentName.unflattenFromString(wanted.substring(4));
                if(one!=null&&one.equals(two))return true;
            }
        }
        return false;
    }
    private String tileName(String id){
        if(id!=null&&id.startsWith("app:")){
            ComponentName component=ComponentName.unflattenFromString(id.substring(4));
            for(AppEntry app:apps)if(app.component.equals(component))return app.label;
            return "Application indisponible";
        }
        return ControlCenterController.label(id);
    }
    private void layoutSettings(){
        intro("Placement du panneau","Le bord de la zone de geste se choisit séparément dans Gestes.");
        choice("Position du panneau",new String[]{"En haut","En bas","À gauche","À droite"},prefs.getInt("position",0),n->changeInt("position",n));
        slider("Largeur disponible (%)","width",65,100,100);slider("Hauteur disponible (%)","height",55,100,90);
        slider("Colonnes des commandes","columns",2,5,4);
        choice("Emplacement de la poignée",new String[]{"Gauche / début","Centre","Droite / fin"},prefs.getInt("handlePosition",1),n->changeInt("handlePosition",n));
    }
    private void gestureSettings(){
        intro("Ouverture et fermeture","La zone de geste agit dans PRISME. Elle ne surveille pas les autres applications.");
        toggle("Activer le centre de contrôle","enabled",true);
        toggle("Poignée discrète dans les styles","compactHandle",true);
        choice("Bord de la zone de geste",new String[]{"Haut","Bas","Gauche","Droite"},prefs.getInt("edge",0),n->changeInt("edge",n));
        choice("Ouvrir le panneau",new String[]{"Glisser depuis le bord","Double appui sur la zone","Bouton seulement","Glisser ou bouton"},prefs.getInt("opening",3),n->changeInt("opening",n));
        choice("Fermer le panneau",new String[]{"Glisser ou toucher à l’extérieur","Glisser","Toucher à l’extérieur","Bouton seulement"},prefs.getInt("closing",0),n->changeInt("closing",n));
        slider("Zone d’ouverture depuis le bord (px)","edgeSize",16,96,64);
        slider("Distance pour ouvrir (px)","swipeDistance",24,140,36);
        slider("Distance pour fermer (px)","closeDistance",24,120,28);
        body.addView(text("Glisse depuis la zone du bord ou touche Contrôles. Pour fermer, glisse depuis l’en-tête ou la barre Fermer en bas. ×, Retour et Fermer restent toujours disponibles ; la liste garde son défilement.",13,MUTED));
    }
    private void commandHelp(){
        intro("Des commandes réelles","Certaines commandes changent un état ; d’autres ouvrent les réglages Android correspondants.");
        paragraph("Wi-Fi et Bluetooth","Les boutons peuvent agir directement lorsque le système le permet. Leur état doit refléter celui d’Android.");
        paragraph("Avion, données mobiles, partage et position","Ces commandes ouvrent les vrais réglages. Elles ne prétendent pas activer une fonction protégée.");
        paragraph("Volume","Le curseur règle le volume des médias et de la musique.");
        paragraph("Luminosité","Sans autorisation spéciale, le curseur ne modifie que la luminosité de PRISME. Pour agir sur celle de la montre, l’autorisation doit être accordée dans Android.");
        brightnessAccessLabel=text("",14,ACCENT);body.addView(brightnessAccessLabel);
        action("Ouvrir l’autorisation de luminosité Android",()->openSettings(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+activity.getPackageName()))));
        paragraph("Ne pas déranger","L’accès doit être autorisé dans Android avant de commander ce mode. Sans cet accès, la commande ouvre la page prévue par Android.");
        dndAccessLabel=text("",14,ACCENT);body.addView(dndAccessLabel);
        action("Ouvrir l’autorisation Ne pas déranger",()->openSettings(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));
        paragraph("Lampe et capture","La lampe éclaire l’écran de PRISME sans utiliser la caméra. La capture enregistre seulement PRISME, jamais l’écran des autres applications.");
        updatePermissionLabels();
        final LinearLayout confirmation=column();confirmation.setPadding(6,8,6,8);confirmation.setVisibility(View.GONE);
        action("Rétablir tout le centre de contrôle",()->confirmation.setVisibility(confirmation.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
        confirmation.addView(text("Cela remet aussi les commandes et les gestes à leur état d’origine. Les styles et les données du bureau PRISME restent conservés.",14,CAUTION));
        Button reset=button("Confirmer la remise à l’origine"),cancel=button("Annuler");
        reset.setOnClickListener(v->{prefs.resetAll();changed();captureScroll();render();toast("Centre de contrôle rétabli");});
        cancel.setOnClickListener(v->confirmation.setVisibility(View.GONE));confirmation.addView(reset,new LinearLayout.LayoutParams(-1,48));confirmation.addView(cancel,new LinearLayout.LayoutParams(-1,48));body.addView(confirmation);
    }
    private void updatePermissionLabels(){
        if(brightnessAccessLabel!=null){
            boolean permitted=false;try{permitted=Settings.System.canWrite(activity);}catch(RuntimeException ignored){}
            brightnessAccessLabel.setText(permitted?"Luminosité globale : autorisation Android accordée":"Luminosité locale à PRISME · autorisation globale absente");
        }
        if(dndAccessLabel!=null){
            boolean permitted=false;try{NotificationManager manager=(NotificationManager)activity.getSystemService(Activity.NOTIFICATION_SERVICE);permitted=manager!=null&&manager.isNotificationPolicyAccessGranted();}catch(RuntimeException ignored){}
            dndAccessLabel.setText(permitted?"Ne pas déranger : accès Android accordé":"Ne pas déranger : accès Android à autoriser");
        }
    }
    private void openSettings(Intent intent){
        try{activity.startActivity(intent);}
        catch(ActivityNotFoundException|SecurityException error){toast("Cette page n’est pas disponible sur ce firmware");}
        catch(RuntimeException error){toast("Android n’a pas pu ouvrir ce réglage");}
    }
    private void changed(){if(listener!=null)listener.onChanged();}
    private void changeInt(String key,int value){prefs.putInt(key,value);changed();}
    private void intro(String title,String description){TextView heading=text(title,20,ACCENT);heading.setTypeface(null,Typeface.BOLD);body.addView(heading);body.addView(text(description,14,MUTED));}
    private void section(String title){TextView heading=text(title,18,ACCENT);heading.setPadding(2,16,2,6);body.addView(heading);}
    private void paragraph(String title,String description){section(title);body.addView(text(description,14,INK));}
    private void action(String label,Runnable action){Button b=button(label);b.setOnClickListener(v->action.run());body.addView(b,new LinearLayout.LayoutParams(-1,50));}
    private void slider(String title,String key,int min,int max,int fallback){
        int value=prefs.getInt(key,fallback);TextView label=text(title+" · "+value,16,INK);body.addView(label);
        SeekBar seek=new SeekBar(activity);seek.setMax(max-min);seek.setProgress(Math.max(0,Math.min(max-min,value-min)));seek.setPadding(8,0,8,0);
        body.addView(seek,new LinearLayout.LayoutParams(-1,46));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar bar){}
            public void onProgressChanged(SeekBar bar,int progress,boolean user){label.setText(title+" · "+(min+progress));}
            public void onStopTrackingTouch(SeekBar bar){changeInt(key,min+bar.getProgress());}
        });
    }
    private void toggle(String title,String key,boolean fallback){CheckBox box=check(title,prefs.getBoolean(key,fallback));box.setOnCheckedChangeListener((v,on)->{prefs.putBoolean(key,on);changed();});body.addView(box);}
    private void choice(String title,String[] values,int selected,IntChoice changed){
        body.addView(text(title,15,MUTED));Button current=button(values[Math.max(0,Math.min(values.length-1,selected))]+"  ▾");body.addView(current,new LinearLayout.LayoutParams(-1,48));
        LinearLayout choices=column();choices.setPadding(8,2,8,4);choices.setVisibility(View.GONE);body.addView(choices);
        for(int i=0;i<values.length;i++){
            final int index=i;Button option=button(values[i]);option.setContentDescription(title+" : "+values[i]);
            option.setOnClickListener(v->{changed.change(index);current.setText(values[index]+"  ▾");choices.setVisibility(View.GONE);if(expandedOptions==choices)expandedOptions=null;});choices.addView(option,new LinearLayout.LayoutParams(-1,48));
        }
        current.setOnClickListener(v->{boolean open=choices.getVisibility()!=View.VISIBLE;if(expandedOptions!=null)expandedOptions.setVisibility(View.GONE);choices.setVisibility(open?View.VISIBLE:View.GONE);expandedOptions=open?choices:null;});
    }
    private interface IntChoice{void change(int value);}
    private static Integer parseColor(String value){
        String trimmed=value==null?"":value.trim();if(!trimmed.matches("#?[0-9a-fA-F]{6}"))return null;
        try{return Integer.parseInt(trimmed.startsWith("#")?trimmed.substring(1):trimmed,16);}catch(NumberFormatException error){return null;}
    }
    private CheckBox check(String title,boolean selected){CheckBox box=new CheckBox(activity);box.setText(title);box.setTextColor(INK);box.setTextSize(TypedValue.COMPLEX_UNIT_PX,16);box.setButtonTintList(ColorStateList.valueOf(ACCENT));box.setMinHeight(46);box.setChecked(selected);return box;}
    private Button button(String label){Button b=new Button(activity);b.setText(label);b.setAllCaps(false);b.setTextColor(INK);b.setTextSize(TypedValue.COMPLEX_UNIT_PX,16);b.setPadding(6,0,6,0);b.setMinimumWidth(0);b.setMinWidth(0);b.setMinHeight(44);b.setBackgroundTintList(ColorStateList.valueOf(SURFACE));return b;}
    private TextView text(String value,int size,int color){TextView view=new TextView(activity);view.setText(value);view.setTextColor(color);view.setTextSize(TypedValue.COMPLEX_UNIT_PX,size);view.setPadding(2,5,2,5);return view;}
    private LinearLayout column(){LinearLayout layout=new LinearLayout(activity);layout.setOrientation(LinearLayout.VERTICAL);layout.setBackgroundColor(BG);return layout;}
    private void toast(String value){I18n.makeToast(activity,value,Toast.LENGTH_SHORT).show();}
    private static final class SimpleWatcher implements TextWatcher{
        private final Runnable changed;SimpleWatcher(Runnable changed){this.changed=changed;}
        public void beforeTextChanged(CharSequence s,int start,int count,int after){}
        public void onTextChanged(CharSequence s,int start,int before,int count){changed.run();}
        public void afterTextChanged(Editable text){}
    }
}

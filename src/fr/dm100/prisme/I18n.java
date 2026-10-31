package fr.dm100.prisme;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.*;
import java.util.*;

/** Bundled offline UI catalogs. User-entered text and exported JSON are never translated. */
public final class I18n {
    public static final String[] NAMES={"Français","English","Deutsch","中文","Türkçe","Русский","العربية"};
    private static final String[] CODES={"fr","en","de","zh","tr","ru","ar"};
    private static final Locale[] LOCALES={Locale.FRANCE,Locale.ENGLISH,Locale.GERMAN,Locale.SIMPLIFIED_CHINESE,new Locale("tr"),new Locale("ru"),new Locale("ar")};
    private static final int BOUND=0x7f0f0001,TEXT=0x7f0f0002,DESC=0x7f0f0003,HINT=0x7f0f0004,USER_TEXT=0x7f0f0005;
    private static volatile int language=-1;
    private static final android.util.LruCache<String,android.text.StaticLayout> layouts=new android.util.LruCache<>(256);
    private static Map<String,String> catalog=Collections.emptyMap();
    private static Map<Character,List<String>> fragments=Collections.emptyMap();
    private static final Set<String> names=new HashSet<>(Arrays.asList(NAMES));
    private static Set<String> userLabels=Collections.emptySet();
    private I18n(){}
    public static int selected(Context context){try{return Math.max(0,Math.min(6,context.getSharedPreferences("prisme_dashboard",0).getInt("language",0)));}catch(RuntimeException ex){return 0;}}
    public static Locale locale(){return LOCALES[Math.max(0,language)];}
    public static boolean rtl(){return language==6;}
    public static Context wrap(Context base){int chosen=selected(base);Configuration config=new Configuration(base.getResources().getConfiguration());config.setLocale(LOCALES[chosen]);config.setLayoutDirection(LOCALES[chosen]);return base.createConfigurationContext(config);}
    public static synchronized void init(Context context){
        int chosen=selected(context);if(chosen==language)return;language=chosen;layouts.evictAll();Locale.setDefault(LOCALES[chosen]);
        HashMap<String,String> next=new HashMap<>();HashMap<Character,List<String>> index=new HashMap<>();
        if(chosen!=0){
            int resource=context.getResources().getIdentifier("lang_"+CODES[chosen],"raw",context.getPackageName());
            if(resource!=0)try(InputStream in=context.getResources().openRawResource(resource);Reader reader=new InputStreamReader(in,"UTF-8")){
                StringBuilder data=new StringBuilder();char[] buffer=new char[4096];int n;while((n=reader.read(buffer))!=-1)data.append(buffer,0,n);
                JSONObject object=new JSONObject(data.toString());Iterator<String> keys=object.keys();while(keys.hasNext()){String key=keys.next();next.put(key,edges(key,object.getString(key)));}
            }catch(Exception ignored){}
            for(String key:next.keySet())if(key.length()>=3&&key.length()<=100&&!names.contains(key)&&!key.trim().equals("PRISME")){
                Character start=key.charAt(0);List<String> bucket=index.get(start);if(bucket==null){bucket=new ArrayList<>();index.put(start,bucket);}bucket.add(key);
            }
            for(List<String> bucket:index.values())Collections.sort(bucket,(a,b)->Integer.compare(b.length(),a.length()));
        }
        catalog=next;fragments=index;refreshLabels(context);
    }
    public static void refreshLabels(Context context){HashSet<String> labels=new HashSet<>();for(Map.Entry<String,?> e:context.getSharedPreferences("prisme_dashboard",0).getAll().entrySet())if(e.getKey().startsWith("customLabel:")&&e.getValue() instanceof String)labels.add((String)e.getValue());labels.addAll(context.getSharedPreferences("prisme_profiles",0).getAll().keySet());userLabels=labels;}
    private static String edges(String source,String translated){int left=0,right=source.length();while(left<right&&Character.isWhitespace(source.charAt(left)))left++;while(right>left&&Character.isWhitespace(source.charAt(right-1)))right--;return source.substring(0,left)+translated.trim()+source.substring(right);}
    public static String t(Context context,CharSequence sequence){init(context);return translate(sequence==null?"":sequence.toString());}
    public static String translate(String source){
        if(language<=0||source==null||source.isEmpty()||source.startsWith("{")||source.startsWith("/")||source.startsWith("C:\\")||names.contains(source)||userLabels.contains(source))return source;
        String exact=catalog.get(source);if(exact!=null)return exact;
        StringBuilder out=new StringBuilder();
        for(int i=0;i<source.length();){List<String> candidates=fragments.get(source.charAt(i));String chosen=null;
            if(candidates!=null)for(String key:candidates){int end=i+key.length();if(!source.startsWith(key,i))continue;
                if(Character.isLetter(key.charAt(0))&&i>0&&Character.isLetter(source.charAt(i-1)))continue;
                if(Character.isLetter(key.charAt(key.length()-1))&&end<source.length()&&Character.isLetter(source.charAt(end)))continue;
                chosen=key;break;}
            if(chosen==null){out.append(source.charAt(i));i++;}else{out.append(catalog.get(chosen));i+=chosen.length();}
        }
        return out.toString();
    }
    public static void drawText(android.graphics.Canvas canvas,String value,float x,float baseline,android.graphics.Paint paint){drawUserText(canvas,translate(value==null?"":value),x,baseline,paint);}
    public static void drawUserText(android.graphics.Canvas canvas,String value,float x,float baseline,android.graphics.Paint paint){
        String text=value==null?"":value;if(!rtl()){canvas.drawText(text,x,baseline,paint);return;}
        int width=Math.max(1,(int)Math.ceil(paint.measureText(text))+4);
        String key=text+"|"+paint.getTextSize()+"|"+paint.getColor()+"|"+System.identityHashCode(paint.getTypeface());
        android.text.StaticLayout layout=layouts.get(key);
        if(layout==null){android.text.TextPaint tp=new android.text.TextPaint(paint);tp.setTextAlign(android.graphics.Paint.Align.LEFT);layout=new android.text.StaticLayout(text,tp,width,android.text.Layout.Alignment.ALIGN_NORMAL,1f,0f,false);layouts.put(key,layout);}
        float left=paint.getTextAlign()==android.graphics.Paint.Align.CENTER?x-width/2f:paint.getTextAlign()==android.graphics.Paint.Align.RIGHT?x-width:x;
        canvas.save();canvas.translate(left,baseline-layout.getLineBaseline(0));layout.draw(canvas);canvas.restore();
    }
    public static Toast makeToast(Context context,CharSequence message,int duration){return Toast.makeText(context,t(context,message),duration);}
    /** Translate native windows, including late-created rows, without ever changing editor contents. */
    public static void bind(final View root){
        if(root==null||Boolean.TRUE.equals(root.getTag(BOUND)))return;root.setTag(BOUND,true);init(root.getContext());
        root.setLayoutDirection(rtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        final ViewTreeObserver.OnPreDrawListener listener=()->{localize(root);return true;};root.getViewTreeObserver().addOnPreDrawListener(listener);
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){if(v.getViewTreeObserver().isAlive())v.getViewTreeObserver().removeOnPreDrawListener(listener);}});
        localize(root);
    }
    public static void userText(View view){view.setTag(USER_TEXT,true);}
    private static void localize(View view){
        if(Boolean.TRUE.equals(view.getTag(USER_TEXT)))return;
        if(view instanceof TextView){TextView text=(TextView)view;
            if(!(view instanceof EditText)){String raw=text.getText().toString();if(!raw.equals(view.getTag(TEXT))){String translated=translate(raw);view.setTag(TEXT,translated);if(!raw.equals(translated))text.setText(translated);}}
            CharSequence hint=text.getHint();if(hint!=null&&!hint.toString().equals(view.getTag(HINT))){String value=translate(hint.toString());view.setTag(HINT,value);if(!value.contentEquals(hint))text.setHint(value);}
        }
        CharSequence description=view.getContentDescription();if(description!=null&&!description.toString().equals(view.getTag(DESC))){String value=translate(description.toString());view.setTag(DESC,value);if(!value.contentEquals(description))view.setContentDescription(value);}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)localize(group.getChildAt(i));}
    }
}

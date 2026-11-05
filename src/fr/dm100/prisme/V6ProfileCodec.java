package fr.dm100.prisme;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Validated, offline V6 profile modules. Never import arbitrary preference keys. */
public final class V6ProfileCodec {
    private V6ProfileCodec() {}
    private static final String[] GROUPS={"battery","music","studio","sense","widgets","edge"};
    private static final String[] STORES={"prisme_battery_overlay","prisme_music","prisme_studio","prisme_sense","prisme_widgets","prisme_edge"};
    private static final String[][] INTS={
        {"style","palette","size","opacity","x","y","low","high","scope","contrastMode"},
        {"style","size","x","y","opacity"},
        {"clockX","clockY","tasksX","tasksY","timerX","timerY","infoX","infoY","size"},
        {"dayTheme","nightTheme","lowTheme","chargeTheme","dayStart","nightStart","lowThreshold"},
        {"size","opacity"},
        {"size","opacity","position"}};
    private static final String[][] BOOLS={
        {"enabled","percent","backdrop","chargingColor","chargingMark","adaptiveContrast","transparentEmpty"},
        {"enabled","title","background","preserve"},
        {"enabled"},
        {"enabled","batteryRule","chargeRule","powerSaver"},
        {"showClock","showTasks","showTimer","showDevice"},
        {}};
    private static int limit(int group,String key,boolean upper){
        if(key.equals("style"))return upper?(group==0?23:11):0;
        if(key.equals("palette"))return upper?11:0;
        if(key.equals("scope"))return upper?2:0;
        if(key.equals("contrastMode"))return upper?2:0;
        if(key.equals("low"))return upper?40:5;
        if(key.equals("high"))return upper?90:45;
        if(key.equals("lowThreshold"))return upper?40:5;
        if(key.equals("dayTheme")||key.equals("nightTheme")||key.equals("lowTheme")||key.equals("chargeTheme"))return upper?21:0;
        if(key.equals("dayStart")||key.equals("nightStart"))return upper?23:0;
        if(key.equals("position"))return upper?3:0;
        if(key.equals("opacity"))return upper?100:(group==0?25:group==1?30:20);
        if(key.equals("size"))return group==5?(upper?90:30):group==1?(upper?180:50):group==0?(upper?200:35):(upper?200:30);
        return upper?100:0;
    }
    static final class Prepared {
        final Map<String,Map<String,Object>> groups=new HashMap<>();
    }
    public static JSONObject exportJSONObject(Context context) throws JSONException {
        JSONObject result=new JSONObject();
        for(int group=0;group<GROUPS.length;group++){
            SharedPreferences prefs=context.getSharedPreferences(STORES[group],Context.MODE_PRIVATE);
            JSONObject values=new JSONObject();
            for(String key:INTS[group]){
                Object value=prefs.getAll().get(key);
                if(value instanceof Integer){
                    int n=(Integer)value;
                    if(n>=limit(group,key,false)&&n<=limit(group,key,true))values.put(key,n);
                }
            }
            for(String key:BOOLS[group]){
                Object value=prefs.getAll().get(key);
                if(value instanceof Boolean)values.put(key,(Boolean)value);
            }
            result.put(GROUPS[group],values);
        }
        SharedPreferences home=context.getSharedPreferences("prisme_home",0);
        JSONObject h=new JSONObject().put("layout",new HomeStore(context).read());
        h.put("enabled",new HomeStore(context).enabled());
        result.put("home",h);
        result.put("dashboardWidgets",new WidgetStore(context).read());
        android.content.SharedPreferences weather=context.getSharedPreferences("prisme_weather",0);
        result.put("weather",new JSONObject().put("fahrenheit",PrefValues.flag(weather,"fahrenheit",false)).put("interval",Math.max(15,Math.min(180,PrefValues.number(weather,"interval",45)))));
        return result;
    }
    public static Prepared prepareImport(JSONObject root) throws JSONException {
        if(root==null||root.toString().length()>65536)throw new JSONException("Paramètres V6 invalides");
        Prepared result=new Prepared();
        Iterator<String> top=root.keys();
        while(top.hasNext()){
            String key=top.next();boolean known=false;
            for(String g:GROUPS)if(g.equals(key))known=true;
            if("home".equals(key)||"dashboardWidgets".equals(key)||"weather".equals(key))known=true;
            if(!known)throw new JSONException("Module V6 non reconnu : "+key);
        }
        for(int group=0;group<GROUPS.length;group++){
            JSONObject values=root.getJSONObject(GROUPS[group]);
            Map<String,Object> validated=new HashMap<>();
            Iterator<String> keys=values.keys();
            while(keys.hasNext()){
                String key=keys.next();boolean matched=false;
                for(String accepted:INTS[group])if(accepted.equals(key)){
                    Object value=values.get(key);
                    if(!(value instanceof Number))throw new JSONException("Nombre attendu : "+key);
                    double number=((Number)value).doubleValue();int n=((Number)value).intValue();
                    if(!Double.isFinite(number)||number!=n||n<limit(group,key,false)||n>limit(group,key,true))
                        throw new JSONException("Nombre V6 hors limites : "+key);
                    validated.put(key,n);matched=true;break;
                }
                if(!matched)for(String accepted:BOOLS[group])if(accepted.equals(key)){
                    Object value=values.get(key);
                    if(!(value instanceof Boolean))throw new JSONException("Booléen attendu : "+key);
                    if(!(group==3&&"enabled".equals(key)))validated.put(key,value);matched=true;break;
                }
                if(!matched)throw new JSONException("Option V6 inconnue : "+key);
            }
            result.groups.put(STORES[group],validated);
        }
        if(root.has("dashboardWidgets")){Map<String,Object> values=new HashMap<>();values.put("layout",DashboardWidgets.validate(root.getJSONObject("dashboardWidgets")).toString());result.groups.put("prisme_widget_engine",values);}
        if(root.has("weather")){JSONObject weather=root.getJSONObject("weather");DashboardWidgets.keys(weather,"fahrenheit","interval");Map<String,Object> values=new HashMap<>();if(weather.has("fahrenheit")){if(!(weather.get("fahrenheit") instanceof Boolean))throw new JSONException("Unité météo invalide");values.put("fahrenheit",weather.getBoolean("fahrenheit"));}if(weather.has("interval"))values.put("interval",HomeLayout.integer(weather,"interval",15,180));result.groups.put("prisme_weather",values);}
        if(root.has("home")){
            JSONObject home=root.getJSONObject("home");
            if(home.length()!=2||!(home.get("enabled") instanceof Boolean))throw new JSONException("Accueil invalide");
            Map<String,Object> values=new HashMap<>();
            // Keep active desktop/card mode local; importing a layout must not replace the selected desktop.
            values.put("layout",HomeLayout.validate(home.getJSONObject("layout")).toString());
            result.groups.put("prisme_home",values);
        }
        return result;
    }
    public static void applyPrepared(Context context,Prepared validated){
        for(Map.Entry<String,Map<String,Object>> group:validated.groups.entrySet()){
            SharedPreferences.Editor edit=context.getSharedPreferences(group.getKey(),Context.MODE_PRIVATE).edit();
            for(Map.Entry<String,Object> entry:group.getValue().entrySet()){
                if(entry.getValue() instanceof Boolean)edit.putBoolean(entry.getKey(),(Boolean)entry.getValue());
                else if(entry.getValue() instanceof String)edit.putString(entry.getKey(),(String)entry.getValue());
                else edit.putInt(entry.getKey(),(Integer)entry.getValue());
            }
            edit.apply();
        }
    }
}

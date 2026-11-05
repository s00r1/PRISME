package fr.dm100.prisme.qa;
import android.app.*;import android.content.*;import android.os.*;import org.json.*;import java.lang.reflect.*;
/** Controlled visual QA: retain the user's full profile privately and restore it afterwards. */
public final class VisualFixture extends Instrumentation {
 Bundle args;Context c;Class<?> prefsClass;Object prefs;
 @Override public void onCreate(Bundle a){super.onCreate(a);args=a;start();}
 Object call(String name,String method,Class<?>[] types,Object... values)throws Exception{return Class.forName("fr.dm100.prisme."+name,true,c.getClassLoader()).getMethod(method,types).invoke(null,values);}
 @Override public void onStart(){Bundle out=new Bundle();try{
 c=getTargetContext();prefsClass=Class.forName("fr.dm100.prisme.DashboardPrefs",true,c.getClassLoader());prefs=prefsClass.getConstructor(Context.class).newInstance(c);SharedPreferences saved=c.getSharedPreferences("prisme_visual_qa",0);String action=args.getString("action","state");
 if(action.equals("save")){
  if(saved.contains("profile"))throw new IllegalStateException("Existing visual snapshot must be restored first");
  JSONObject profile=(JSONObject)prefsClass.getMethod("exportJSONObject").invoke(prefs);saved.edit().putString("profile",profile.toString()).putBoolean("sense",c.getSharedPreferences("prisme_sense",0).getBoolean("enabled",false)).putBoolean("overlays",c.getSharedPreferences("prisme_overlay_policy",0).getBoolean("suspended",false)).commit();out.putString("snapshot","saved privately");
 }else if(action.equals("style")){
  if(!saved.contains("profile"))throw new IllegalStateException("No safety snapshot");int id=Integer.parseInt(args.getString("id"));if(id<0||id>21)throw new IllegalArgumentException();
  call("OverlayPolicy","suspend",new Class[]{Context.class},c);
  Class<?> themes=Class.forName("fr.dm100.prisme.ThemeSpec",true,c.getClassLoader());Object t=((Object[])themes.getField("ALL").get(null))[id];String[] keys={"theme","columns","iconSize","textSize"};int[] values={id,t.getClass().getField("recommendedColumns").getInt(t),t.getClass().getField("recommendedIconSize").getInt(t),t.getClass().getField("recommendedTextSize").getInt(t)};
  for(int i=0;i<keys.length;i++)prefsClass.getMethod("putInt",String.class,int.class).invoke(prefs,keys[i],values[i]);
  c.getSharedPreferences("prisme_home",0).edit().putBoolean("enabled",false).commit();c.getSharedPreferences("prisme_sense",0).edit().putBoolean("enabled",false).commit();c.getSharedPreferences("prisme_music",0).edit().putBoolean("enabled",false).commit();out.putString("style",(String)t.getClass().getField("name").get(t));
 }else if(action.equals("restore")){
  String profile=saved.getString("profile","");if(profile.isEmpty())throw new IllegalStateException("No visual snapshot");prefsClass.getMethod("importJSONObject",JSONObject.class).invoke(prefs,new JSONObject(profile));
  c.getSharedPreferences("prisme_home",0).edit().putBoolean("enabled",false).commit();boolean suspended=saved.getBoolean("overlays",false);c.getSharedPreferences("prisme_sense",0).edit().putBoolean("enabled",saved.getBoolean("sense",false)).commit();saved.edit().clear().commit();call("OverlayPolicy",suspended?"suspend":"resume",new Class[]{Context.class},c);out.putString("snapshot","restored; selected desktop active");
 }
 JSONObject current=(JSONObject)prefsClass.getMethod("exportJSONObject").invoke(prefs);out.putInt("selectedStyle",current.getJSONObject("settings").getInt("theme"));out.putBoolean("cardsEnabled",c.getSharedPreferences("prisme_home",0).getBoolean("enabled",false));out.putString("result","PASS");
 }catch(Throwable e){out.putString("result","FAIL");out.putString("failure",e.toString());}finish(Activity.RESULT_OK,out);}
}

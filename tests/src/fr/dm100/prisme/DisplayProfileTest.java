package fr.dm100.prisme;
import org.json.JSONObject;
import org.json.JSONException;
public final class DisplayProfileTest {
    static int n;
    static void check(boolean ok,String why){n++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        DashboardPrefs source=new DashboardPrefs(new DashboardPrefsTest.MemoryContext());
        source.putInt("theme",12);source.putString("notes","Conserver cette note");
        source.getFloatingVolume().setEnabled(true);source.getFloatingVolume().putBoolean("boot",true);
        JSONObject legacy=source.exportJSONObject();legacy.put("schemaVersion",4);legacy.remove("organizer");
        legacy.getJSONObject("settings").remove("edgeCards");legacy.getJSONObject("settings").remove("compactNavigation");
        JSONObject cc=legacy.getJSONObject("controlCenter").getJSONObject("settings");
        for(String key:new String[]{"tileOpacity","scrimOpacity","closeDistance"})cc.remove(key);
        DashboardPrefs target=new DashboardPrefs(new DashboardPrefsTest.MemoryContext());target.importJSONObject(legacy);
        check(target.getInt("theme",0)==12&&target.getString("notes","").equals("Conserver cette note"),"legacy display profile keeps personal content");
        check(target.getControlCenter().getInt("tileOpacity",-1)==28&&target.getControlCenter().getInt("closeDistance",-1)==28,"legacy centre receives new independent controls");
        check(!target.getFloatingVolume().getBoolean("enabled",true),"import does not activate the global overlay");
        target.putBoolean("edgeCards",false);target.putBoolean("compactNavigation",true);target.putInt("styleScale",125);
        target.getControlCenter().putInt("tileOpacity",0);target.getControlCenter().putInt("scrimOpacity",0);target.getControlCenter().putInt("closeDistance",24);
        JSONObject saved=target.exportJSONObject();check(saved.getInt("schemaVersion")==6,"new display schema is explicit");
        DashboardPrefs restored=new DashboardPrefs(new DashboardPrefsTest.MemoryContext());restored.importJSONObject(saved);
        check(!restored.getBoolean("edgeCards",true)&&restored.getBoolean("compactNavigation",false)&&restored.getInt("styleScale",0)==125,"global size and edge options survive backup");
        check(restored.getControlCenter().getInt("tileOpacity",-1)==0&&restored.getControlCenter().getInt("scrimOpacity",-1)==0,"fully transparent tile and scrim choices survive");
        String before=restored.exportJSONObject().toString();saved.getJSONObject("controlCenter").getJSONObject("settings").put("closeDistance",121);
        try{restored.importJSONObject(saved);throw new AssertionError("bad closing distance accepted");}catch(JSONException expected){}
        check(before.equals(restored.exportJSONObject().toString()),"invalid new gesture leaves all settings untouched");
        System.out.println("DisplayProfileTest: "+n+" assertions passed.");
    }
}

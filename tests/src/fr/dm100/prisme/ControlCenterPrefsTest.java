package fr.dm100.prisme;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Configuration and backup safety checks; no device services or permissions are involved. */
public final class ControlCenterPrefsTest {
    private static int checks;
    private static final String[] KEYS = {"style", "opacity", "scale", "columns", "position", "width", "height", "edge", "opening", "closing", "edgeSize", "swipeDistance", "handlePosition", "radius", "accent", "customColor", "tileOpacity", "scrimOpacity", "closeDistance"};
    private static final int[] DEFAULTS = {0,68,100,4,0,100,90,0,3,0,64,36,1,24,0,0x62D9CC,28,8,28};
    private static final int[] MIN = {0,20,80,2,0,65,55,0,0,0,16,24,0,0,0,0,0,0,24};
    private static final int[] MAX = {1,100,125,5,3,100,100,3,3,3,96,140,2,36,5,0xFFFFFF,100,60,120};
    private static final String[] CATALOG = {"wifi","bluetooth","sound","airplane","data","hotspot","location","rotation","battery","display","volume","dnd","screenlamp","camera","alarms","timer","stopwatch","calculator","notes","settings","apps","styles","clock","search","music","vpn","accessibility","capture","volumepill"};

    public static void main(String[] args) throws Exception {
        defaultsAndBounds();
        tileOrderValidationAndDefensiveCopies();
        exactRoundTripAndResets();
        validationNeverWrites();
        combinedProfilesValidateBeforeEitherWrite();
        oldProfilesKeepTheCurrentCenter();
        legacyRotationAliasesKeepTheFirstPosition();
        System.out.println("PASS: " + checks + " control-centre assertions across 7 scenarios.");
    }

    private static void defaultsAndBounds() throws Exception {
        DashboardPrefsTest.MemoryContext context = new DashboardPrefsTest.MemoryContext();
        ControlCenterPrefs p = new ControlCenterPrefs(context);
        for (int i=0;i<KEYS.length;i++) {
            check(p.getInt(KEYS[i],-999)==DEFAULTS[i], KEYS[i]+" has its central default");
            p.putInt(KEYS[i],Integer.MIN_VALUE);check(p.getInt(KEYS[i],-999)==MIN[i],KEYS[i]+" clamps minimum");
            p.putInt(KEYS[i],Integer.MAX_VALUE);check(p.getInt(KEYS[i],-999)==MAX[i],KEYS[i]+" clamps maximum");
        }
        p.resetAll();
        for (String key:new String[]{"enabled","dark","labels","status","volume","brightness","media"}) check(p.getBoolean(key,false),key+" defaults enabled");
        JSONObject legacy=p.exportJSONObject();legacy.getJSONObject("settings").remove("media");
        ControlCenterPrefs restored=new ControlCenterPrefs(new DashboardPrefsTest.MemoryContext());
        restored.importJSONObject(legacy);
        check(restored.getBoolean("media",false),"Older center backups gain the media module without losing their tiles");
        check(restored.getTiles().equals(p.getTiles()),"Adding the media module preserves the exact previous tile order");
        check(!p.getBoolean("customAccent",true),"Custom color is initially opt-in");
        check(p.getTiles().equals(Arrays.asList("wifi","bluetooth","sound","airplane","location","rotateGlobal","screenlamp","settings","timer","clock")),"Default tile order is exact");
        p.putInt("unknown",42);p.putBoolean("unknownFlag",true);
        check(!p.exportJSONObject().getJSONObject("settings").has("unknown"),"Unknown settings cannot enter backups");
        check(p.getInt("unknown",17)==17 && !p.getBoolean("unknownFlag",false),"Unknown keys preserve caller fallback only");
        context.store("prisme_control_center").edit().putString("opacity","bad type").putString("dark","bad type").apply();
        check(p.getInt("opacity",1)==68 && p.getBoolean("dark",false),"Corrupt stored primitive types use safe central defaults");
        context.store("prisme_control_center").edit().putString("tiles",null).apply();
        check(p.getTiles().size()==10,"Null stored tile JSON falls back safely");
        context.store("prisme_control_center").edit().putString("tiles","[1]").apply();
        check(p.getTiles().size()==10,"Malformed stored tile values fall back safely");
    }

    private static void tileOrderValidationAndDefensiveCopies() throws Exception {
        DashboardPrefsTest.MemoryContext context = new DashboardPrefsTest.MemoryContext();ControlCenterPrefs p = new ControlCenterPrefs(context);
        for (String tile:CATALOG) check(ControlCenterPrefs.isValidTile(tile),"Catalogue tile "+tile);
        p.setTiles(Arrays.asList("notes","wifi","notes","app:fr.demo/.Main","app:fr.demo/fr.demo.Main","settings"));
        check(p.getTiles().equals(Arrays.asList("notes","wifi","app:fr.demo/fr.demo.Main","settings")),"First occurrence wins, including relative/full component aliases");
        List<String> copy=p.getTiles();copy.clear();
        check(p.getTiles().size()==4,"Returned order is defensive");
        List<String> maximum=new ArrayList<String>(Arrays.asList(CATALOG).subList(0,24));p.setTiles(maximum);
        check(p.getTiles().size()==24,"Twenty-four unique tiles are accepted");
        List<String> tooMany=new ArrayList<String>(maximum);tooMany.add("music");rejectSet(p,context,tooMany);
        for (List<String> invalid:Arrays.asList(Arrays.asList("wifi","unlisted"),Arrays.asList("wifi",null),Arrays.asList("app:fr.demo/.Bad/Extra")))rejectSet(p,context,invalid);
        rejectSet(p,context,null);
        for (String tile:new String[]{"torch","home","controlCenter","https://example.com","app:bad","app:fr.demo/.Bad?x=1","app:fr..demo/.Main"})check(!ControlCenterPrefs.isValidTile(tile),"Reject unsafe or unknown tile "+tile);
        p.setTiles(new ArrayList<String>());check(p.getTiles().isEmpty(),"Explicit empty tile order is preserved");
    }

    private static void exactRoundTripAndResets() throws Exception {
        DashboardPrefsTest.MemoryContext context = new DashboardPrefsTest.MemoryContext();ControlCenterPrefs p = new ControlCenterPrefs(context);
        for(int i=0;i<KEYS.length;i++)p.putInt(KEYS[i],MAX[i]);
        for(String key:new String[]{"enabled","dark","labels","status","volume","brightness","media"})p.putBoolean(key,false);
        p.putBoolean("customAccent",true);p.setTiles(Arrays.asList("capture","notes","app:fr.demo/.Main","wifi"));
        JSONObject exported=p.exportJSONObject();ControlCenterPrefs q=new ControlCenterPrefs(new DashboardPrefsTest.MemoryContext());q.importJSONObject(new JSONObject(exported.toString()));
        check(q.exportJSONObject().toString().equals(exported.toString()),"All centre preferences and tile order round-trip through real JSON");
        Map<String,?> before=context.store("prisme_control_center").getAll();ControlCenterPrefs.validateJSONObject(exported);
        check(before.equals(context.store("prisme_control_center").getAll()),"Validation has no side effects");
        List<String> tiles=p.getTiles();p.resetAppearance();
        check(!p.getBoolean("enabled",true),"Appearance reset preserves disabled state");
        check(p.getTiles().equals(tiles),"Appearance reset preserves custom tile order");
        for(String key:new String[]{"edge","opening","closing","edgeSize","swipeDistance","handlePosition","closeDistance"})check(p.getInt(key,-1)==MAX[Arrays.asList(KEYS).indexOf(key)],"Appearance reset preserves interaction "+key);
        for(String key:new String[]{"style","opacity","scale","columns","position","width","height","radius","accent","customColor","tileOpacity","scrimOpacity"})check(p.getInt(key,-1)==DEFAULTS[Arrays.asList(KEYS).indexOf(key)],"Appearance reset restores "+key);
        check(!p.getBoolean("customAccent",true),"Appearance reset returns to palette color");
        p.resetAll();
        for(int i=0;i<KEYS.length;i++)check(p.getInt(KEYS[i],-1)==DEFAULTS[i],"Full reset restores "+KEYS[i]);
        check(p.getBoolean("enabled",false) && p.getTiles().size()==10,"Full reset restores activation and default tiles");
    }

    private static void validationNeverWrites() throws Exception {
        DashboardPrefsTest.MemoryContext context=new DashboardPrefsTest.MemoryContext();ControlCenterPrefs p=new ControlCenterPrefs(context);
        p.putInt("style",1);p.putInt("customColor",0xA3B4C5);p.setTiles(Arrays.asList("notes","clock"));
        JSONObject original=p.exportJSONObject();
        for(int i=0;i<KEYS.length;i++){
            JSONObject bad=copy(original);bad.getJSONObject("settings").put(KEYS[i],MIN[i]-1);rejectImport(p,context,bad);
            bad=copy(original);bad.getJSONObject("settings").put(KEYS[i],MAX[i]+1);rejectImport(p,context,bad);
        }
        JSONObject bad=copy(original);bad.getJSONObject("settings").put("style",0.5);rejectImport(p,context,bad);
        bad=copy(original);bad.getJSONObject("settings").put("enabled","true");rejectImport(p,context,bad);
        bad=copy(original);bad.getJSONObject("settings").put("customAccent",1);rejectImport(p,context,bad);
        bad=copy(original);bad.getJSONObject("settings").put("unknown",true);rejectImport(p,context,bad);
        bad=copy(original);bad.put("unknown",false);rejectImport(p,context,bad);
        bad=copy(original);bad.remove("settings");rejectImport(p,context,bad);
        bad=copy(original);bad.remove("tiles");rejectImport(p,context,bad);
        for(Object order:new Object[]{true,JSONObject.NULL,new JSONArray().put(7),new JSONArray().put("torch"),new JSONArray().put("app:fr.demo/.A/Other")}){
            bad=copy(original);bad.put("tiles",order);rejectImport(p,context,bad);
        }
        JSONArray tooMany=new JSONArray();for(int i=0;i<25;i++)tooMany.put("wifi");
        bad=copy(original);bad.put("tiles",tooMany);rejectImport(p,context,bad);
        bad=copy(original);bad.put("tiles",new JSONArray().put(repeat(65537)));rejectImport(p,context,bad);
        rejectImport(p,context,null);
        JSONObject duplicates=copy(original);duplicates.put("tiles",new JSONArray().put("wifi").put("notes").put("wifi"));
        p.importJSONObject(duplicates);check(p.getTiles().equals(Arrays.asList("wifi","notes")),"Valid duplicate JSON is normalized without reordering");
    }

    private static void combinedProfilesValidateBeforeEitherWrite() throws Exception {
        DashboardPrefsTest.MemoryContext context=new DashboardPrefsTest.MemoryContext();DashboardPrefs dash=new DashboardPrefs(context);ControlCenterPrefs center=dash.getControlCenter();
        dash.putInt("theme",7);dash.putString("notes","Mon bureau");dash.setShortcut(0,"controlCenter");dash.setShortcut(1,"controlCenterSettings");
        center.putInt("style",1);center.putInt("opacity",88);center.setTiles(Arrays.asList("notes","camera"));
        JSONObject original=dash.exportJSONObject();
        check(original.getInt("schemaVersion")==7 && original.has("controlCenter"),"Dashboard schema4 includes centre configuration");
        JSONObject bad=copy(original);bad.getJSONObject("settings").put("theme",15);bad.getJSONObject("controlCenter").getJSONObject("settings").put("opacity",19);rejectCombined(dash,context,bad);
        bad=copy(original);bad.getJSONObject("settings").put("styleScale",141);bad.getJSONObject("controlCenter").getJSONObject("settings").put("opacity",90);rejectCombined(dash,context,bad);
        bad=copy(original);bad.getJSONObject("controlCenter").put("tiles",new JSONArray().put("bad-action"));rejectCombined(dash,context,bad);
        bad=copy(original);bad.remove("controlCenter");rejectCombined(dash,context,bad);
        JSONObject valid=copy(original);valid.getJSONObject("settings").put("theme",15);valid.getJSONObject("controlCenter").getJSONObject("settings").put("opacity",90);
        valid.getJSONObject("controlCenter").getJSONObject("settings").put("customAccent",true).put("customColor",0x102030);
        dash.importJSONObject(valid);
        check(dash.getInt("theme",0)==15 && center.getInt("opacity",0)==90,"Valid combined profile updates both stores");
        check(center.getBoolean("customAccent",false) && center.getInt("customColor",0)==0x102030,"Free custom RGB color is part of profile restore");
        check(dash.getShortcut(0,"apps").equals("controlCenter") && dash.getShortcut(1,"apps").equals("controlCenterSettings"),"Both centre actions can be assigned to existing shortcut slots");
        DashboardPrefs reopened=new DashboardPrefs(context);
        check(reopened.getControlCenter().getInt("opacity",0)==90 && reopened.getInt("theme",0)==15,"Both configurations survive a new prefs instance");
    }

    private static void oldProfilesKeepTheCurrentCenter() throws Exception {
        DashboardPrefsTest.MemoryContext context=new DashboardPrefsTest.MemoryContext();DashboardPrefs dash=new DashboardPrefs(context);ControlCenterPrefs center=dash.getControlCenter();
        center.putInt("style",1);center.putInt("height",71);center.putBoolean("dark",false);center.setTiles(Arrays.asList("music","wifi","timer"));
        Map<String,?> before=context.store("prisme_control_center").getAll();
        for(int schema:new int[]{1,2}){
            JSONObject legacy=new JSONObject().put("schemaVersion",schema).put("settings",new JSONObject().put("theme",schema));
            if(schema==2)legacy.put("shortcuts",new JSONObject().put("0","clock"));
            dash.importJSONObject(legacy);
            check(dash.getInt("theme",0)==schema,"Legacy schema"+schema+" still applies dashboard settings");
            check(before.equals(context.store("prisme_control_center").getAll()),"Legacy schema"+schema+" preserves the existing centre exactly");
        }
        JSONObject unknown=dash.exportJSONObject();unknown.put("schemaVersion",2);rejectCombined(dash,context,unknown);
    }

    private static void legacyRotationAliasesKeepTheFirstPosition() throws Exception {
        DashboardPrefsTest.MemoryContext context = new DashboardPrefsTest.MemoryContext();
        SharedPreferences store = context.store("prisme_control_center");
        String legacy = new JSONArray().put("wifi").put("rotation").put("notes").put("rotateGlobal").put("clock").toString();
        store.edit().putString("tiles", legacy).putInt("opacity", 83).apply();
        Map<String,?> before = store.getAll();
        ControlCenterPrefs p = new ControlCenterPrefs(context);
        List<String> expected = Arrays.asList("wifi", "rotateGlobal", "notes", "clock");
        check(p.getTiles().equals(expected), "Existing rotation becomes forced rotation at its original first position");
        check(p.getTiles().equals(expected) && before.equals(store.getAll()), "Repeated reads normalize without any storage writes");
        check(new ControlCenterPrefs(context).getTiles().equals(expected), "Normalization survives reopening without destructive migration");
        check(p.getInt("opacity", 0) == 83, "Rotation migration preserves unrelated panel appearance");
        JSONObject exported = p.exportJSONObject();
        check(exported.getJSONArray("tiles").toString().equals(new JSONArray(expected).toString()), "Exports use only the canonical forced-rotation action");
        p.setTiles(Arrays.asList("notes", "rotateGlobal", "wifi", "rotation", "clock"));
        check(p.getTiles().equals(Arrays.asList("notes", "rotateGlobal", "wifi", "clock")), "New ID first keeps its position when an old alias follows");
        JSONObject imported = p.exportJSONObject();
        imported.put("tiles", new JSONArray().put("timer").put("rotation").put("wifi").put("rotateGlobal").put("notes"));
        Map<String,?> untouched = store.getAll(); ControlCenterPrefs.validateJSONObject(imported);
        check(untouched.equals(store.getAll()), "Legacy profile validation does not write migration state");
        p.importJSONObject(imported);
        check(p.getTiles().equals(Arrays.asList("timer", "rotateGlobal", "wifi", "notes")), "Old profile IDs normalize before duplicate removal on import");
        check(ControlCenterPrefs.isValidTile("rotation") && ControlCenterPrefs.isValidTile("rotateGlobal"), "Both profile generations remain accepted");
        p.setTiles(Arrays.asList("notes", "clock"));
        check(!p.getTiles().contains("rotateGlobal"), "An intentionally removed rotation button is not reinserted");
    }

    private static void rejectCombined(DashboardPrefs p,DashboardPrefsTest.MemoryContext context,JSONObject json)throws Exception{
        Map<String,?> beforeDash=context.store("prisme_dashboard").getAll(),beforeCenter=context.store("prisme_control_center").getAll();
        try{p.importJSONObject(json);throw new AssertionError("Invalid combined profile accepted");}catch(JSONException expected){}
        check(beforeDash.equals(context.store("prisme_dashboard").getAll()),"Invalid combined import leaves dashboard untouched");
        check(beforeCenter.equals(context.store("prisme_control_center").getAll()),"Invalid combined import leaves centre untouched");
    }
    private static void rejectImport(ControlCenterPrefs p,DashboardPrefsTest.MemoryContext context,JSONObject json)throws Exception{
        Map<String,?> before=context.store("prisme_control_center").getAll();
        try{p.importJSONObject(json);throw new AssertionError("Invalid centre JSON accepted");}catch(JSONException expected){}
        check(before.equals(context.store("prisme_control_center").getAll()),"Invalid centre import is non-destructive");
    }
    private static void rejectSet(ControlCenterPrefs p,DashboardPrefsTest.MemoryContext context,List<String> tiles){
        Map<String,?> before=context.store("prisme_control_center").getAll();
        try{p.setTiles(tiles);throw new AssertionError("Invalid tile list accepted");}catch(IllegalArgumentException expected){}
        check(before.equals(context.store("prisme_control_center").getAll()),"Invalid tile edit is non-destructive");
    }
    private static JSONObject copy(JSONObject json)throws Exception{return new JSONObject(json.toString());}
    private static String repeat(int length){char[] text=new char[length];Arrays.fill(text,'x');return new String(text);}
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
}

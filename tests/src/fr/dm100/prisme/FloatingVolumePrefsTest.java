package fr.dm100.prisme;

import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.Map;

/** Portable appearance must never become permission to run a global overlay. */
public final class FloatingVolumePrefsTest {
    private static int checks;
    private static final String[] KEYS = {"style","shape","layout","size","opacity","expandedOpacity","accent","customColor","stream","xPercent","yPercent","collapseDelay","repeatDelay","repeatInterval"};
    private static final int[] DEFAULTS = {0,0,0,56,90,95,0,0x62D9CC,0,100,55,4,450,140};
    private static final int[] MIN = {0,0,0,40,15,15,0,0,0,0,0,1,250,70};
    private static final int[] MAX = {5,3,2,96,100,100,5,0xFFFFFF,3,100,100,15,900,300};

    public static void main(String[] args) throws Exception {
        defaultsBoundsAndCorruption();
        activationHasOnlyLocalScope();
        appearanceRoundTripAndResets();
        invalidImportsNeverWrite();
        schemaFourValidatesAllThreeStores();
        oldProfilesPreserveFloatingExactly();
        observersCanBeDetached();
        System.out.println("PASS: "+checks+" floating-volume assertions across 7 scenarios.");
    }

    private static void defaultsBoundsAndCorruption() throws Exception {
        DashboardPrefsTest.MemoryContext context=new DashboardPrefsTest.MemoryContext();FloatingVolumePrefs p=new FloatingVolumePrefs(context);
        for(int i=0;i<KEYS.length;i++){
            check(p.getInt(KEYS[i],-999)==DEFAULTS[i],KEYS[i]+" defaults centrally");
            p.putInt(KEYS[i],Integer.MIN_VALUE);check(p.getInt(KEYS[i],-1)==MIN[i],KEYS[i]+" minimum");
            p.putInt(KEYS[i],Integer.MAX_VALUE);check(p.getInt(KEYS[i],-1)==MAX[i],KEYS[i]+" maximum");
        }
        p.resetAll();
        for(String key:new String[]{"snap","autoCollapse","showLevel"})check(p.getBoolean(key,false),key+" initially true");
        for(String key:new String[]{"customAccent","locked","showMute","haptic","enabled","boot"})check(!p.getBoolean(key,true),key+" initially false");
        check(p.getInt("activeBoot",42)==-2,"No installed session marker exists initially");
        check(!p.mayResume(0)&&!p.mayResume(-1),"Fresh installation cannot resume an overlay");
        p.putInt("unknown",7);p.putBoolean("unknownFlag",true);
        check(p.getInt("unknown",19)==19&&!p.getBoolean("unknownFlag",false),"Unknown keys do not persist");
        context.store("prisme_floating_volume").edit().putString("enabled","true").putString("boot","true").putString("activeBoot","42").putString("size","wrong").apply();
        check(!p.getBoolean("enabled",true)&&!p.getBoolean("boot",true)&&p.getInt("activeBoot",42)==-2,"Corrupted operational types fail closed");
        check(p.getInt("size",90)==56&&!p.mayResume(42),"Corrupt appearance falls back without activating anything");
    }

    private static void activationHasOnlyLocalScope() throws Exception {
        FloatingVolumePrefs p=new FloatingVolumePrefs(new DashboardPrefsTest.MemoryContext());
        p.setEnabled(true);check(!p.mayResume(42),"Enabled alone cannot invent a manual session");
        p.markActiveBoot(42);check(p.mayResume(42)&&!p.mayResume(43),"Manual session resumes only during its actual boot");
        check(!p.mayResume(-1),"Unknown current boot never matches a stored session marker");
        p.markActiveBoot(-1);check(!p.mayResume(-1)&&!p.mayResume(42),"Unknown marker does not grant persistent resume");
        p.putBoolean("boot",true);check(p.mayResume(-1)&&p.mayResume(43),"Explicit boot opt-in works even without a readable counter");
        p.setEnabled(false);check(!p.mayResume(43)&&p.getInt("activeBoot",9)==-2,"Voluntary stop revokes the activation marker");
        check(p.getBoolean("boot",false),"Voluntary stop retains the separate boot preference without enabling it");
        p.putBoolean("enabled",true);p.putBoolean("boot",false);p.markActiveBoot(0);check(p.mayResume(0),"Boot zero is a valid manual session");
        p.putBoolean("enabled",false);check(!p.mayResume(0)&&p.getInt("activeBoot",0)==-2,"Generic disabling follows the same stop semantics");
        p.markActiveBoot(Integer.MIN_VALUE);check(p.getInt("activeBoot",0)==-2,"Invalid negative marker clamps to the inactive sentinel");
        p.resetAll();check(!p.getBoolean("enabled",true)&&!p.getBoolean("boot",true)&&p.getInt("activeBoot",0)==-2,"Full reset revokes runtime activation");
    }

    private static void appearanceRoundTripAndResets() throws Exception {
        FloatingVolumePrefs p=new FloatingVolumePrefs(new DashboardPrefsTest.MemoryContext());
        p.setEnabled(true);p.putBoolean("boot",true);p.markActiveBoot(42);
        for(int i=0;i<KEYS.length;i++)p.putInt(KEYS[i],MAX[i]);
        p.putBoolean("customAccent",true);p.putBoolean("locked",true);p.putBoolean("snap",false);
        JSONObject appearance=p.exportJSONObject(),settings=appearance.getJSONObject("settings");
        for(String runtime:new String[]{"enabled","boot","activeBoot"})check(!appearance.has(runtime)&&!settings.has(runtime),runtime+" never enters appearance profiles");
        FloatingVolumePrefs fresh=new FloatingVolumePrefs(new DashboardPrefsTest.MemoryContext());fresh.importJSONObject(copy(appearance));
        check(fresh.exportJSONObject().toString().equals(appearance.toString()),"Every appearance option round-trips");
        check(!fresh.getBoolean("enabled",true)&&!fresh.getBoolean("boot",true)&&!fresh.mayResume(42),"Applying an enabled device's style never activates a fresh installation");
        appearance.getJSONObject("settings").put("style",2);p.importJSONObject(appearance);
        check(p.getInt("style",0)==2&&p.mayResume(42)&&p.getBoolean("boot",false),"Import preserves a locally enabled session and boot consent");
        p.resetAppearance();
        for(int i=0;i<KEYS.length;i++)check(p.getInt(KEYS[i],-1)==DEFAULTS[i],"Appearance reset restores "+KEYS[i]);
        check(p.mayResume(42)&&p.getBoolean("enabled",false)&&p.getBoolean("boot",false),"Appearance reset does not change operational consent");
        p.resetAll();check(!p.getBoolean("enabled",true)&&!p.getBoolean("boot",true)&&p.getInt("activeBoot",0)==-2,"Reset all is a local stop, not a portable style reset");
    }

    private static void invalidImportsNeverWrite() throws Exception {
        DashboardPrefsTest.MemoryContext context=new DashboardPrefsTest.MemoryContext();FloatingVolumePrefs p=new FloatingVolumePrefs(context);
        p.setEnabled(true);p.putBoolean("boot",true);p.markActiveBoot(42);p.putInt("style",3);p.putInt("size",72);
        JSONObject original=p.exportJSONObject();
        for(int i=0;i<KEYS.length;i++){
            JSONObject bad=copy(original);bad.getJSONObject("settings").put(KEYS[i],MIN[i]-1);reject(p,context,bad);
            bad=copy(original);bad.getJSONObject("settings").put(KEYS[i],MAX[i]+1);reject(p,context,bad);
        }
        for(String key:new String[]{"enabled","boot","activeBoot"}){
            JSONObject bad=copy(original);bad.getJSONObject("settings").put(key,true);reject(p,context,bad);
            bad=copy(original);bad.put(key,true);reject(p,context,bad);
        }
        JSONObject bad=copy(original);bad.getJSONObject("settings").put("snap","true");reject(p,context,bad);
        bad=copy(original);bad.getJSONObject("settings").put("size",56.5);reject(p,context,bad);
        bad=copy(original);bad.getJSONObject("settings").put("unknown",false);reject(p,context,bad);
        bad=copy(original);bad.put("unknownRoot",true);reject(p,context,bad);
        bad=copy(original);bad.remove("settings");reject(p,context,bad);reject(p,context,null);
        Map<String,?> before=context.store("prisme_floating_volume").getAll();FloatingVolumePrefs.validateJSONObject(original);
        check(before.equals(context.store("prisme_floating_volume").getAll()),"Validation alone never writes");
    }

    private static void schemaFourValidatesAllThreeStores() throws Exception {
        DashboardPrefsTest.MemoryContext context=new DashboardPrefsTest.MemoryContext();DashboardPrefs dash=new DashboardPrefs(context);
        ControlCenterPrefs cc=dash.getControlCenter();FloatingVolumePrefs p=dash.getFloatingVolume();
        dash.putInt("theme",7);dash.putString("notes","Mon bureau");cc.putInt("style",1);cc.setTiles(Arrays.asList("wifi","volumepill"));
        p.setEnabled(true);p.putBoolean("boot",true);p.markActiveBoot(42);p.putInt("style",3);p.putInt("size",72);
        JSONObject original=dash.exportJSONObject();
        check(original.getInt("schemaVersion")==6&&original.has("floatingVolume"),"Schema4 includes floating appearance");
        JSONObject bad=copy(original);bad.getJSONObject("settings").put("theme",15);bad.getJSONObject("controlCenter").getJSONObject("settings").put("opacity",90);bad.getJSONObject("floatingVolume").getJSONObject("settings").put("style",6);rejectCombined(dash,context,bad);
        bad=copy(original);bad.getJSONObject("settings").put("styleScale",141);bad.getJSONObject("floatingVolume").getJSONObject("settings").put("size",64);rejectCombined(dash,context,bad);
        bad=copy(original);bad.getJSONObject("controlCenter").getJSONObject("settings").put("opacity",19);bad.getJSONObject("floatingVolume").getJSONObject("settings").put("size",64);rejectCombined(dash,context,bad);
        bad=copy(original);bad.getJSONObject("floatingVolume").getJSONObject("settings").put("enabled",true);rejectCombined(dash,context,bad);
        bad=copy(original);bad.remove("floatingVolume");rejectCombined(dash,context,bad);
        JSONObject valid=copy(original);valid.getJSONObject("settings").put("theme",15);valid.getJSONObject("controlCenter").getJSONObject("settings").put("opacity",90);valid.getJSONObject("floatingVolume").getJSONObject("settings").put("size",64);
        dash.importJSONObject(valid);
        check(dash.getInt("theme",0)==15&&cc.getInt("opacity",0)==90&&p.getInt("size",0)==64,"Valid schema4 applies all three appearances");
        check(p.getBoolean("enabled",false)&&p.getBoolean("boot",false)&&p.getInt("activeBoot",0)==42,"Valid combined import preserves local runtime state");
        DashboardPrefs clean=new DashboardPrefs(new DashboardPrefsTest.MemoryContext());clean.importJSONObject(valid);
        check(clean.getFloatingVolume().getInt("size",0)==64&&!clean.getFloatingVolume().getBoolean("enabled",true)&&!clean.getFloatingVolume().getBoolean("boot",true),"Portable schema4 never imports overlay activation");
        dash.setShortcut(0,"floatingVolumeSettings");check(dash.getShortcut(0,"apps").equals("floatingVolumeSettings"),"Integrated-volume settings can be assigned to a dock slot");
        check(ControlCenterPrefs.isValidTile("volumepill"),"Integrated-volume tile is part of the shared catalogue");
    }

    private static void oldProfilesPreserveFloatingExactly() throws Exception {
        DashboardPrefsTest.MemoryContext context=new DashboardPrefsTest.MemoryContext();DashboardPrefs dash=new DashboardPrefs(context);FloatingVolumePrefs p=dash.getFloatingVolume();
        p.setEnabled(true);p.putBoolean("boot",true);p.markActiveBoot(42);p.putInt("style",5);p.putInt("yPercent",77);p.putBoolean("snap",false);
        Map<String,?> before=context.store("prisme_floating_volume").getAll();
        for(int schema:new int[]{1,2,3}){
            JSONObject old=new JSONObject().put("schemaVersion",schema).put("settings",new JSONObject().put("theme",schema));
            if(schema>=2)old.put("shortcuts",new JSONObject().put("0","clock"));
            if(schema==3)old.put("controlCenter",dash.getControlCenter().exportJSONObject());
            dash.importJSONObject(old);
            check(dash.getInt("theme",0)==schema,"Old schema"+schema+" applies the dashboard");
            check(before.equals(context.store("prisme_floating_volume").getAll()),"Old schema"+schema+" preserves every floating field exactly");
        }
        JSONObject future=dash.exportJSONObject();future.put("schemaVersion",3);rejectCombined(dash,context,future);
    }

    private static void observersCanBeDetached() {
        DashboardPrefsTest.MemoryContext context=new DashboardPrefsTest.MemoryContext();FloatingVolumePrefs p=new FloatingVolumePrefs(context);
        final int[] calls={0};SharedPreferences.OnSharedPreferenceChangeListener listener=(store,key)->calls[0]++;
        p.registerListener(listener);p.putInt("size",64);check(calls[0]>0,"Live appearance changes notify registered observers");
        int previous=calls[0];p.unregisterListener(listener);p.putInt("size",72);check(calls[0]==previous,"Unregistered service observers are detached");
        p.registerListener(listener);p.setEnabled(true);p.markActiveBoot(42);previous=calls[0];p.resetAll();
        check(calls[0]>previous&&!p.mayResume(42),"Reset all notifies a running service of the stop");
        p.unregisterListener(listener);
    }

    private static void rejectCombined(DashboardPrefs p,DashboardPrefsTest.MemoryContext context,JSONObject json)throws Exception{
        Map<String,?> dash=context.store("prisme_dashboard").getAll(),cc=context.store("prisme_control_center").getAll(),volume=context.store("prisme_floating_volume").getAll();
        try{p.importJSONObject(json);throw new AssertionError("Invalid combined profile accepted");}catch(JSONException expected){}
        check(dash.equals(context.store("prisme_dashboard").getAll()),"Invalid profile preserves dashboard");
        check(cc.equals(context.store("prisme_control_center").getAll()),"Invalid profile preserves centre");
        check(volume.equals(context.store("prisme_floating_volume").getAll()),"Invalid profile preserves floating appearance and runtime");
    }
    private static void reject(FloatingVolumePrefs p,DashboardPrefsTest.MemoryContext context,JSONObject json)throws Exception{
        Map<String,?> before=context.store("prisme_floating_volume").getAll();
        try{p.importJSONObject(json);throw new AssertionError("Invalid volume appearance accepted");}catch(JSONException expected){}
        check(before.equals(context.store("prisme_floating_volume").getAll()),"Invalid floating import is entirely non-destructive");
    }
    private static JSONObject copy(JSONObject json)throws Exception{return new JSONObject(json.toString());}
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
}

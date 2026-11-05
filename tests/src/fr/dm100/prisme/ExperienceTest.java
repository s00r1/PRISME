package fr.dm100.prisme;
import org.json.*;
import java.util.Arrays;
public final class ExperienceTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        check(SearchIndex.matches("État de la montre", "etat montre"),"Accents and multiword matching");
        check(SearchIndex.matches("Chronomètre", "CHRONOMETRE"),"Case-independent search");
        check(!SearchIndex.matches("Volume", "volume bluetooth"),"Every search word must match");
        DashboardPrefsTest.MemoryContext context=new DashboardPrefsTest.MemoryContext();DashboardPrefs p=new DashboardPrefs(context);ChecklistStore c=p.getChecklist();
        check(!c.add("  "),"Blank task rejected");check(c.add("  Acheter du pain  "),"New task accepted");
        String id=c.read().getJSONObject(0).getString("id");check(c.read().getJSONObject(0).getString("text").equals("Acheter du pain"),"Task text cleaned");
        c.toggle(id);check(c.remaining()==0,"Completion state persisted");check(new ChecklistStore(context).remaining()==0,"State survives store recreation");
        c.toggle(id);check(c.remaining()==1,"Completion can be undone");
        JSONArray defensive=c.read();defensive.getJSONObject(0).put("text","Unwanted change");check(!c.read().getJSONObject(0).getString("text").equals("Unwanted change"),"Reads are defensive");
        JSONObject exported=p.exportJSONObject();DashboardPrefs restored=new DashboardPrefs(new DashboardPrefsTest.MemoryContext());restored.importJSONObject(exported);
        check(restored.getChecklist().remaining()==1,"Tasks included in complete profile");
        for(String action:new String[]{"today","checklist","deskclock","presets","device","displayTest","settingsSearch","rotationSettings","language"})check(DashboardPrefs.isValidShortcutAction(action),"New action whitelisted: "+action);
        p.putInt("language",6);p.putInt("upGesture",4);p.putInt("blankDoubleTap",3);p.putBoolean("deskKeepAwake",true);
        p.getFloatingVolume().putBoolean("idleFade",true);p.getFloatingVolume().putInt("idleDelay",20);p.getFloatingVolume().putInt("idleOpacity",30);
        p.getControlCenter().setTiles(Arrays.asList("today","checklist","musicPrevious","musicNext","device","deskclock"));
        JSONObject modern=p.exportJSONObject();restored.importJSONObject(modern);
        check(restored.getInt("language",0)==6,"Language selection survives profiles");
        check(restored.getInt("upGesture",0)==4&&restored.getInt("blankDoubleTap",0)==3,"Gesture choices survive export/import");
        check(restored.getFloatingVolume().getBoolean("idleFade",false)&&restored.getFloatingVolume().getInt("idleDelay",0)==20,"Idle appearance portable");
        check(restored.getControlCenter().getTiles().size()==6,"New centre tiles portable");
        JSONObject legacy=new JSONObject(modern.toString());legacy.put("schemaVersion",5);legacy.remove("organizer");legacy.remove("v6");legacy.getJSONObject("settings").remove("language");restored.importJSONObject(legacy);
        check(restored.getInt("language",0)==6,"Old profiles preserve the chosen UI language");
        check(restored.getChecklist().remaining()==1,"Old profiles preserve new personal task data");
        String before=restored.exportJSONObject().toString();
        for(JSONArray invalid:new JSONArray[]{new JSONArray().put(new JSONObject().put("id","x").put("text","bad").put("done","false")),new JSONArray().put(c.read().getJSONObject(0)).put(c.read().getJSONObject(0))}){
            JSONObject bad=new JSONObject(modern.toString());bad.put("organizer",invalid);bad.getJSONObject("settings").put("theme",21);
            try{restored.importJSONObject(bad);throw new AssertionError("Invalid task accepted");}catch(JSONException expected){}
            check(before.equals(restored.exportJSONObject().toString()),"Invalid organizer leaves all profile families unchanged");
        }
        for(int i=1;i<40;i++)check(c.add("Tâche "+i),"Task accepted below limit");check(!c.add("41e"),"Limit enforced");
        c.toggle(id);c.clearDone();check(c.read().length()==39,"Only completed tasks removed");
        c.remove(c.read().getJSONObject(0).getString("id"));check(c.read().length()==38,"Deletion targets only one stable id");
        System.out.println("ExperienceTest: "+checks+" assertions passed.");
    }
}

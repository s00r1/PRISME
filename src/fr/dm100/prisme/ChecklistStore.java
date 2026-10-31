package fr.dm100.prisme;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.UUID;
import java.util.HashSet;

/** A bounded local list, independent of timer runtime state and backed up with profiles. */
public final class ChecklistStore {
    public static final int LIMIT = 40;
    private final SharedPreferences store;
    public ChecklistStore(Context context) { store=context.getApplicationContext().getSharedPreferences("prisme_checklist",Context.MODE_PRIVATE); }
    public JSONArray read() {
        try { return validate(new JSONArray(store.getString("items","[]"))); }
        catch (JSONException | ClassCastException ex) { return new JSONArray(); }
    }
    public static JSONArray validate(JSONArray source) throws JSONException {
        if(source==null||source.length()>LIMIT)throw new JSONException("40 tâches maximum");
        JSONArray clean=new JSONArray();HashSet<String> ids=new HashSet<>();
        for(int i=0;i<source.length();i++){
            JSONObject item=source.getJSONObject(i);
            if(item.length()!=3||!item.has("id")||!item.has("text")||!item.has("done"))throw new JSONException("Tâche invalide");
            Object id=item.get("id"),text=item.get("text"),done=item.get("done");
            if(!(id instanceof String)||((String)id).length()<1||((String)id).length()>64||!ids.add((String)id)
                    ||!(text instanceof String)||((String)text).trim().isEmpty()||((String)text).length()>120||!(done instanceof Boolean))throw new JSONException("Tâche invalide");
            clean.put(new JSONObject().put("id",id).put("text",((String)text).trim()).put("done",done));
        }
        return clean;
    }
    public void replace(JSONArray items) throws JSONException { store.edit().putString("items",validate(items).toString()).apply(); }
    public boolean add(String text) {
        String clean=text==null?"":text.trim();JSONArray items=read();
        if(clean.isEmpty()||clean.length()>120||items.length()>=LIMIT)return false;
        try {items.put(new JSONObject().put("id",UUID.randomUUID().toString()).put("text",clean).put("done",false));replace(items);return true;}
        catch(JSONException ex){return false;}
    }
    public void toggle(String id){JSONArray items=read();try{for(int i=0;i<items.length();i++){JSONObject x=items.getJSONObject(i);if(x.getString("id").equals(id))x.put("done",!x.getBoolean("done"));}replace(items);}catch(JSONException ignored){}}
    public void remove(String id){JSONArray out=new JSONArray(),items=read();try{for(int i=0;i<items.length();i++)if(!items.getJSONObject(i).getString("id").equals(id))out.put(items.get(i));replace(out);}catch(JSONException ignored){}}
    public void clearDone(){JSONArray out=new JSONArray(),items=read();try{for(int i=0;i<items.length();i++)if(!items.getJSONObject(i).getBoolean("done"))out.put(items.get(i));replace(out);}catch(JSONException ignored){}}
    public int remaining(){int result=0;JSONArray items=read();for(int i=0;i<items.length();i++)if(!items.optJSONObject(i).optBoolean("done"))result++;return result;}
}

package fr.dm100.prisme;

import android.content.Context;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Keep ten automatic recovery points; manual exports are never removed. */
public final class SafetyBackup {
    private SafetyBackup(){}
    public static void once(Context c,DashboardPrefs prefs) {
        if(c.getSharedPreferences("prisme_experience",0).getInt("backupVersion",0)>=15)return;
        try{save(c,prefs);c.getSharedPreferences("prisme_experience",0).edit().putInt("backupVersion",15).apply();}catch(Exception ignored){}
    }
    public static synchronized void save(Context c,DashboardPrefs prefs)throws Exception {
        File parent=c.getExternalFilesDir(null);if(parent==null)parent=c.getFilesDir();File dir=new File(parent,"sauvegardes");
        if(!dir.isDirectory()&&!dir.mkdirs())throw new java.io.IOException("Dossier de sauvegarde indisponible");
        byte[] data=prefs.exportJSONObject().toString().getBytes(StandardCharsets.UTF_8);
        File target=new File(dir,"PRISME-securite-"+System.currentTimeMillis()+"-"+java.util.UUID.randomUUID().toString().substring(0,6)+".json");
        AtomicFile atomic=new AtomicFile(target);FileOutputStream stream=null;
        try{stream=atomic.startWrite();stream.write(data);atomic.finishWrite(stream);}catch(Exception ex){if(stream!=null)atomic.failWrite(stream);throw ex;}
        File[] files=dir.listFiles((d,name)->name.startsWith("PRISME-securite-")&&name.endsWith(".json"));
        if(files!=null){Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));for(int i=10;i<files.length;i++)if(files[i].isFile())files[i].delete();}
    }
}

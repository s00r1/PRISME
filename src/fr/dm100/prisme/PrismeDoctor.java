package fr.dm100.prisme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Point;
import android.os.Build;
import android.os.Debug;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.WindowManager;

/** On-device read-only diagnostics; no logcat dump, serial numbers, or remote telemetry. */
public final class PrismeDoctor {
    private PrismeDoctor(){}
    public static String report(Context context){
        StringBuilder out=new StringBuilder("PRISME Doctor · V6\n\n");
        out.append("Montre : ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        out.append("Android : ").append(Build.VERSION.RELEASE).append(" · API ").append(Build.VERSION.SDK_INT).append("\n");
        WindowManager window=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        if(window!=null){
            Point p=new Point();window.getDefaultDisplay().getRealSize(p);
            out.append("Écran : ").append(p.x).append(" × ").append(p.y).append(" px");
            out.append(p.x!=p.y?" · rectangle":" · format non validé").append("\n");
            if(!((p.x==640&&p.y==480)||(p.x==480&&p.y==640)))out.append("Matériel non certifié : vérifier les marges, gestes et rotation.\n");
        }
        out.append("Superposition : ").append(Settings.canDrawOverlays(context)?"autorisée":"non autorisée").append("\n");
        out.append("Centre de contrôle : ").append(ControlCenterService.isShowing()?"visible":"masqué / inactif").append("\n");
        out.append("Volume flottant : ").append(fr.dm100.prisme.volume.FloatingVolumeService.isRunning()?"service actif":"service arrêté").append("\n");
        out.append("Rotation globale : ").append(GlobalRotationService.isRunning()?"active":"inactive").append("\n");
        out.append("Dock : ").append(context.getSharedPreferences("prisme_edge",0).getBoolean("enabled",false)?"demandé":"désactivé").append("\n");
        int media=MediaAccess.accessStatus(context);
        out.append("Informations musicales : ").append(media==MediaAccess.ACCESS_READY?"autorisées":media==MediaAccess.ACCESS_DISABLED?"désactivées dans PRISME":"autorisation Android manquante").append("\n");
        MusicPlayback.State music=MusicPlayback.state(context);
        out.append("Session musicale : ").append(music.hasSession?"accessible":"indisponible").append("\n");
        out.append("Pochette locale : ").append(music.artworkLoading?"chargement":music.artwork!=null?"disponible":"non disponible").append("\n");
        out.append("\n").append(BatteryLevelReader.read(context,null).diagnostic()).append("\n");
        DeviceSnapshot d=DeviceSnapshot.read(context);
        out.append("\nRAM système utilisée : ").append(d.memoryUsedPct<0?"inconnue":d.memoryUsedPct+" %").append("\n");
        out.append("Stockage libre : ").append(String.format(java.util.Locale.US,"%.2f Go",d.freeStorageGb)).append("\n");
        Debug.MemoryInfo mem=new Debug.MemoryInfo();Debug.getMemoryInfo(mem);
        out.append("Mémoire PRISME (PSS) : ").append(mem.getTotalPss()).append(" Ko\n");
        out.append("Temps depuis démarrage : ").append(SystemClock.elapsedRealtime()/60000).append(" min\n");
        out.append("\nDonnées collectées uniquement pour cet affichage. Aucun envoi réseau.");
        return out.toString();
    }
    public static void show(Activity a){
        new V6Ui.Builder(a).setTitle("◈ PRISME Doctor").setMessage(report(a))
            .setPositiveButton("Fermer",null).show();
    }
}

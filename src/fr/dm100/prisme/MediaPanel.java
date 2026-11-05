package fr.dm100.prisme;

import android.app.*;
import android.content.*;
import android.media.session.*;
import android.provider.Settings;
import android.widget.*;
import java.util.*;

public final class MediaPanel {
  private MediaPanel() {}

  public static void open(Activity a) {
    LinearLayout root = V6Ui.column(a);
    MusicPlayback.State state = MusicPlayback.state(a);
    root.addView(V6Ui.text(a, state.title + "\n" + state.artist, 21));
    V6Ui.add(root, "Piste précédente", () -> MusicPlayback.command(a, 1));
    V6Ui.add(root, "Lecture / pause", () -> MusicPlayback.command(a, 0));
    V6Ui.add(root, "Piste suivante", () -> MusicPlayback.command(a, 2));
    List<android.media.session.MediaController> sessions = MediaAccess.sessions(a);
    for (android.media.session.MediaController m : sessions)
      V6Ui.add(
          root,
          "Lecteur · " + m.getPackageName(),
          () -> {
            a.getSharedPreferences("prisme_media_access", 0)
                .edit()
                .putString("player", m.getPackageName())
                .apply();
            Toast.makeText(a, "Lecteur sélectionné", 0).show();
          });
    V6Ui.add(
        root,
        "Accès multimédia enrichi (facultatif)",
        () ->
            new V6Ui.Builder(a)
                .setTitle("Autoriser les informations musicales ?")
                .setMessage(
                    "Android propose un accès aux notifications. Il est sensible : PRISME utilisera"
                        + " uniquement les sessions multimédias pour les titres et commandes. Aucun"
                        + " message n’est lu ni conservé. Vous pouvez rester en mode simple.")
                .setPositiveButton(
                    "Ouvrir Android",
                    (d, w) -> {
                      a.getSharedPreferences("prisme_media_access", 0)
                          .edit()
                          .putBoolean("enabled", true)
                          .apply();
                      try {
                        a.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                      } catch (RuntimeException e) {
                        Toast.makeText(a, "Réglage indisponible", 0).show();
                      }
                    })
                .setNegativeButton("Annuler", null)
                .show());
    V6Ui.add(
        root,
        "Revenir aux commandes simples",
        () -> {
          a.getSharedPreferences("prisme_media_access", 0)
              .edit()
              .putBoolean("enabled", false)
              .remove("player")
              .apply();
          Toast.makeText(
                  a,
                  "Accès PRISME désactivé. Révocation Android dans les réglages de notifications.",
                  1)
              .show();
        });
    V6Ui.dialog(a, "Musique · PRISME 6", root);
  }
}

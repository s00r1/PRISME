package fr.dm100.prisme;

import android.app.*;
import android.os.*;
import android.widget.*;
import org.json.*;

public final class MultiTimerPanel {
  private MultiTimerPanel() {}

  public static void open(Activity a) {
    final AlertDialog[] owner = new AlertDialog[1];
    LinearLayout root = V6Ui.column(a);
    LinearLayout list = V6Ui.column(a);
    root.addView(list);
    V6Ui.add(
        root,
        "Ajouter un minuteur",
        () -> {
          LinearLayout form = V6Ui.column(a);
          EditText name = new EditText(a), minutes = new EditText(a);
          name.setTextColor(V6Ui.INK);
          name.setHint("Nom du minuteur");
          name.setText("Mon minuteur");
          minutes.setTextColor(V6Ui.INK);
          minutes.setInputType(2);
          minutes.setText("5");
          form.addView(name);
          form.addView(V6Ui.text(a, "Durée en minutes (1 à 1440)", 16));
          form.addView(minutes);
          new V6Ui.Builder(a)
              .setTitle("Nouveau minuteur")
              .setView(form)
              .setPositiveButton(
                  "Créer",
                  (d, w) -> {
                    try {
                      MultiTimers.create(
                          a,
                          name.getText().toString(),
                          Math.multiplyExact(Long.parseLong(minutes.getText().toString()), 60000L));
                      if (owner[0] != null) owner[0].dismiss();
                      open(a);
                    } catch (Exception e) {
                      Toast.makeText(a, e.getMessage(), 1).show();
                    }
                  })
              .setNegativeButton("Annuler", null)
              .show();
        });
    AlertDialog dialog = V6Ui.dialog(a, "Minuteurs · PRISME 6", root);
    owner[0] = dialog;
    Handler h = new Handler(Looper.getMainLooper());
    Runnable tick =
        new Runnable() {
          public void run() {
            if (!dialog.isShowing()) return;
            MultiTimers.check(a);
            list.removeAllViews();
            for (int i = 0; i < 8; i++) {
              final int id = i;
              JSONObject t = MultiTimers.read(a, id);
              if (!t.has("name")) continue;
              long left = MultiTimers.remaining(a, t);
              V6Ui.add(
                  list,
                  t.optString("name")
                      + " · "
                      + (t.optBoolean("finished")
                          ? "Terminé"
                          : ExperienceController.duration(left)),
                  () ->
                      new V6Ui.Builder(a)
                          .setTitle(t.optString("name"))
                          .setItems(
                              new String[] {
                                "Démarrer / reprendre", "Pause", "Recommencer", "Supprimer"
                              },
                              (d, n) -> {
                                if (n == 0 || n == 2) {
                                  if (!MultiTimers.start(a, id, n == 2))
                                    Toast.makeText(
                                            a,
                                            "Alarme non programmée : vérifier les réglages Android",
                                            1)
                                        .show();
                                } else if (n == 1) MultiTimers.pause(a, id);
                                else MultiTimers.remove(a, id);
                              })
                          .show());
            }
            h.postDelayed(this, 1000);
          }
        };
    dialog.setOnDismissListener(d -> h.removeCallbacksAndMessages(null));
    h.post(tick);
  }
}

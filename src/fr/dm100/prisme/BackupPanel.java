package fr.dm100.prisme;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Private vault UI; expensive password derivation never runs on the UI thread. */
public final class BackupPanel {
  private BackupPanel() {}

  private static final ExecutorService worker = Executors.newSingleThreadExecutor();

  public static void open(
      Activity a,
      DashboardPrefs prefs,
      Runnable changed,
      java.util.function.Consumer<File> export) {
    LinearLayout root = V6Ui.column(a);
    root.addView(
        V6Ui.text(
            a,
            "Les sauvegardes automatiques restent dans l’espace privé. L’export chiffré nécessite"
                + " un mot de passe de 10 caractères minimum, indispensable pour le récupérer.",
            16));
    V6Ui.add(
        root,
        "Créer une sauvegarde chiffrée",
        () ->
            password(
                a,
                "Mot de passe de sauvegarde",
                pass -> {
                  try {
                    JSONObject profile = prefs.exportJSONObject();
                    worker.execute(
                        () -> {
                          try {
                            JSONObject vault = BackupVault.seal(profile, pass);
                            File dir = new File(a.getFilesDir(), "sauvegardes");
                            if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException();
                            File out =
                                new File(
                                    dir, "PRISME-chiffre-" + System.currentTimeMillis() + ".vault");
                            try (FileOutputStream stream = new FileOutputStream(out)) {
                              stream.write(vault.toString().getBytes(StandardCharsets.UTF_8));
                              stream.getFD().sync();
                            }
                            a.runOnUiThread(
                                () -> {
                                  if (!a.isFinishing() && !a.isDestroyed())
                                    Toast.makeText(
                                            a, "Sauvegarde chiffrée créée · " + out.getName(), 1)
                                        .show();
                                });
                          } catch (Exception e) {
                            a.runOnUiThread(
                                () -> Toast.makeText(a, "Création impossible", 1).show());
                          } finally {
                            Arrays.fill(pass, '\0');
                          }
                        });
                  } catch (Exception e) {
                    Arrays.fill(pass, '\0');
                  }
                }));
    File dir = new File(a.getFilesDir(), "sauvegardes");
    File[] files = dir.listFiles((d, n) -> n.startsWith("PRISME-chiffre-") && n.endsWith(".vault"));
    if (files != null) {
      Arrays.sort(files, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
      for (File file : files)
        V6Ui.add(
            root,
            file.getName(),
            () ->
                password(
                    a,
                    "Déverrouiller la sauvegarde",
                    pass ->
                        worker.execute(
                            () -> {
                              try {
                                if (file.length() > 100000) throw new IOException();
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                try (InputStream in = new FileInputStream(file)) {
                                  byte[] buf = new byte[4096];
                                  int n;
                                  while ((n = in.read(buf)) != -1) {
                                    if (out.size() + n > 100000) throw new IOException();
                                    out.write(buf, 0, n);
                                  }
                                }
                                JSONObject profile =
                                    BackupVault.open(
                                        new JSONObject(
                                            new String(out.toByteArray(), StandardCharsets.UTF_8)),
                                        pass);
                                a.runOnUiThread(
                                    () -> {
                                      if (a.isFinishing() || a.isDestroyed()) return;
                                      new V6Ui.Builder(a)
                                          .setTitle("Restaurer cette sauvegarde ?")
                                          .setMessage(
                                              "La configuration sera remplacée. Les minuteurs en"
                                                  + " cours ne sont pas importés.")
                                          .setPositiveButton(
                                              "Restaurer",
                                              (d, w) -> {
                                                try {
                                                  SafetyBackup.save(a, prefs);
                                                  prefs.importJSONObject(profile);
                                                  changed.run();
                                                  Toast.makeText(a, "Restauration terminée", 0)
                                                      .show();
                                                } catch (Exception e) {
                                                  Toast.makeText(a, "Restauration refusée", 1)
                                                      .show();
                                                }
                                              })
                                          .setNegativeButton("Annuler", null)
                                          .show();
                                    });
                              } catch (Exception e) {
                                a.runOnUiThread(
                                    () ->
                                        Toast.makeText(
                                                a, "Mot de passe incorrect ou fichier altéré", 1)
                                            .show());
                              } finally {
                                Arrays.fill(pass, '\0');
                              }
                            })));
    }
    if (files != null)
      for (File file : files)
        V6Ui.add(root, "Exporter · " + file.getName(), () -> export.accept(file));
    V6Ui.dialog(a, "Sauvegardes chiffrées", root);
  }

  public static void importVault(
      Activity a, JSONObject vault, DashboardPrefs prefs, Runnable changed) {
    password(
        a,
        "Déverrouiller la sauvegarde",
        pass ->
            worker.execute(
                () -> {
                  try {
                    JSONObject profile = BackupVault.open(vault, pass);
                    a.runOnUiThread(
                        () -> {
                          if (a.isFinishing() || a.isDestroyed()) return;
                          new V6Ui.Builder(a)
                              .setTitle("Restaurer cette sauvegarde ?")
                              .setMessage(
                                  "Un point de récupération sera créé avant le remplacement de la"
                                      + " configuration.")
                              .setPositiveButton(
                                  "Restaurer",
                                  (d, w) -> {
                                    try {
                                      SafetyBackup.save(a, prefs);
                                      prefs.importJSONObject(profile);
                                      changed.run();
                                      Toast.makeText(a, "Restauration terminée", 0).show();
                                    } catch (Exception e) {
                                      Toast.makeText(a, "Restauration refusée", 1).show();
                                    }
                                  })
                              .setNegativeButton("Annuler", null)
                              .show();
                        });
                  } catch (Exception e) {
                    a.runOnUiThread(
                        () ->
                            Toast.makeText(a, "Mot de passe incorrect ou fichier altéré", 1)
                                .show());
                  } finally {
                    Arrays.fill(pass, '\0');
                  }
                }));
  }

  private interface Password {
    void accept(char[] pass);
  }

  private static void password(Activity a, String title, Password action) {
    EditText e = new EditText(a);
    e.setInputType(129);
    e.setHint("10 caractères minimum");
    new V6Ui.Builder(a)
        .setTitle(title)
        .setView(e)
        .setPositiveButton(
            "Continuer",
            (d, w) -> {
              char[] pass = new char[e.length()];
              e.getText().getChars(0, e.length(), pass, 0);
              e.getText().clear();
              if (pass.length < 10) {
                Arrays.fill(pass, '\0');
                Toast.makeText(a, "10 caractères minimum", 0).show();
                return;
              }
              action.accept(pass);
            })
        .setNegativeButton("Annuler", (d, w) -> e.getText().clear())
        .show();
  }
}

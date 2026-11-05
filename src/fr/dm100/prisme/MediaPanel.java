package fr.dm100.prisme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

/** Live media details with explicit local consent and Android authorization state. */
public final class MediaPanel {
  private MediaPanel() {}

  public static String title(MusicPlayback.State state) {
    if (!state.accessEnabled) return "Titres et pochettes à activer";
    if (!state.accessGranted) return "Autorisation Android requise";
    return state.title;
  }

  public static String detail(MusicPlayback.State state) {
    if (!state.accessEnabled) return "Musique → Activer titres et pochettes";
    if (!state.accessGranted) return "Autoriser PRISME dans les réglages Android";
    if (!state.hasSession) return "Ouvrir un lecteur et lancer un titre";
    if (!state.artist.isEmpty()) return state.artist;
    return state.playing ? "Lecture en cours" : "En pause";
  }

  public static void launch(Context context) {
    try {
      context.startActivity(
          new Intent(context, MainActivity.class)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
              .putExtra("utilityAction", "mediaPanel"));
    } catch (RuntimeException error) {
      Toast.makeText(context, "Ouvrir Musique depuis les outils PRISME", Toast.LENGTH_LONG).show();
    }
  }

  public static void open(Activity activity) {
    LinearLayout root = V6Ui.column(activity);
    LinearLayout hero = new LinearLayout(activity);
    hero.setGravity(Gravity.CENTER_VERTICAL);
    hero.setPadding(6, 8, 6, 8);
    MediaArtworkView art = new MediaArtworkView(activity);
    art.colors(V6Ui.SURFACE, V6Ui.ACCENT);
    hero.addView(art, new LinearLayout.LayoutParams(100, 100));
    LinearLayout labels = V6Ui.column(activity);
    labels.setPadding(14, 0, 0, 0);
    TextView title = V6Ui.text(activity, "", 23), artist = V6Ui.text(activity, "", 17);
    I18n.userText(title);
    I18n.userText(artist);
    title.setMaxLines(3);
    title.setEllipsize(TextUtils.TruncateAt.END);
    artist.setMaxLines(2);
    artist.setEllipsize(TextUtils.TruncateAt.END);
    labels.addView(title);
    labels.addView(artist);
    hero.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
    root.addView(hero);
    TextView info = V6Ui.text(activity, "", 14);
    I18n.userText(info);
    info.setPadding(6, 0, 6, 8);
    root.addView(info);
    LinearLayout controls = new LinearLayout(activity);
    Button[] transport = new Button[3];
    String[] names = {"⏮", "▶", "⏭"};
    String[] descriptions = {"Piste précédente", "Lecture ou pause", "Piste suivante"};
    for (int i = 0; i < transport.length; i++) {
      final int command =
          i == 0 ? MusicPlayback.PREVIOUS : i == 2 ? MusicPlayback.NEXT : MusicPlayback.TOGGLE;
      Button button =
          V6Ui.button(activity, names[i], () -> MusicPlayback.command(activity, command));
      button.setContentDescription(descriptions[i]);
      transport[i] = button;
      controls.addView(button, new LinearLayout.LayoutParams(0, 52, 1));
    }
    root.addView(controls);
    Button access = V6Ui.button(activity, "Titres et pochettes", () -> permissions(activity));
    root.addView(access, new LinearLayout.LayoutParams(-1, 52));
    LinearLayout readers = V6Ui.column(activity);
    root.addView(readers);
    V6Ui.add(
        root,
        "Choisir automatiquement le lecteur en cours",
        () -> {
          activity.getSharedPreferences("prisme_media_access", 0).edit().remove("player").apply();
          Toast.makeText(activity, "Sélection automatique du lecteur", Toast.LENGTH_SHORT).show();
        });
    V6Ui.add(
        root,
        "Revenir aux commandes simples",
        () -> {
          activity
              .getSharedPreferences("prisme_media_access", 0)
              .edit()
              .putBoolean("enabled", false)
              .remove("player")
              .apply();
          Toast.makeText(
                  activity,
                  "Titres et pochettes désactivés. Révocation Android dans les réglages de"
                      + " notifications.",
                  Toast.LENGTH_LONG)
              .show();
        });
    AlertDialog[] owner = new AlertDialog[1];
    Handler handler = new Handler(Looper.getMainLooper());
    String[] previousReaders = {null};
    Runnable update =
        () -> {
          MusicPlayback.State state = MusicPlayback.state(activity);
          set(title, title(state));
          set(artist, detail(state));
          art.artwork(state.artwork);
          String details =
              state.hasSession
                  ? "Lecteur · " + state.packageName
                  : "Commandes Android disponibles sans titre ni pochette.";
          if (state.hasSession) {
            if (!state.album.isEmpty()) details += "\n" + state.album;
            details +=
                state.artworkLoading
                    ? "\nChargement de la pochette…"
                    : state.artwork == null
                        ? "\nLe lecteur ne fournit pas de pochette locale."
                        : "";
          }
          set(info, details);
          set(transport[1], state.playing ? "Ⅱ" : "▶");
          transport[1].setContentDescription(
              state.playing ? "Mettre en pause" : "Lancer la lecture");
          set(
              access,
              !state.accessEnabled
                  ? "Activer titres et pochettes"
                  : !state.accessGranted
                      ? "Autoriser PRISME dans Android"
                      : "Titres et pochettes · autorisés");
          List<MediaController> sessions = MediaAccess.sessions(activity);
          StringBuilder signature = new StringBuilder();
          for (MediaController controller : sessions)
            signature.append(controller.getPackageName()).append('\n');
          String wanted =
              PrefValues.text(
                  activity.getSharedPreferences("prisme_media_access", 0), "player", "");
          signature.append(wanted);
          if (!signature.toString().equals(previousReaders[0])) {
            previousReaders[0] = signature.toString();
            readers.removeAllViews();
            if (!sessions.isEmpty())
              readers.addView(V6Ui.text(activity, "LECTEURS ACCESSIBLES", 14));
            java.util.Set<String> shown = new java.util.HashSet<>();
            for (MediaController controller : sessions) {
              String pkg = controller.getPackageName();
              if (!shown.add(pkg)) continue;
              String label = pkg;
              try {
                label =
                    activity
                        .getPackageManager()
                        .getApplicationLabel(
                            activity.getPackageManager().getApplicationInfo(pkg, 0))
                        .toString();
              } catch (Exception ignored) {
              }
              V6Ui.add(
                  readers,
                  (pkg.equals(wanted) ? "✓ " : "") + label,
                  () -> {
                    activity
                        .getSharedPreferences("prisme_media_access", 0)
                        .edit()
                        .putString("player", pkg)
                        .apply();
                  });
            }
          }
        };
    Runnable tick =
        new Runnable() {
          public void run() {
            if (activity.isFinishing()
                || activity.isDestroyed()
                || owner[0] == null
                || !owner[0].isShowing()) return;
            if (root.hasWindowFocus()) update.run();
            handler.postDelayed(this, 1000);
          }
        };
    root.addOnAttachStateChangeListener(
        new View.OnAttachStateChangeListener() {
          public void onViewAttachedToWindow(View view) {}

          public void onViewDetachedFromWindow(View view) {
            handler.removeCallbacksAndMessages(null);
          }
        });
    update.run();
    owner[0] = V6Ui.dialog(activity, "Musique · titre et pochette", root);
    handler.post(tick);
  }

  private static void set(TextView view, String text) {
    if (!text.contentEquals(view.getText())) view.setText(text);
  }

  /** Android requires notification-listener access; no notification messages are consumed. */
  public static void permissions(Activity activity) {
    new V6Ui.Builder(activity)
        .setTitle("Titres et pochettes")
        .setMessage(
            "Android exige son accès aux notifications pour partager les sessions multimédias. Cet"
                + " accès est sensible. PRISME utilise uniquement le titre, l’artiste, la pochette"
                + " locale et les commandes du lecteur ; aucun message n’est lu ni conservé. Deux"
                + " choix sont nécessaires : activer cette fonction dans PRISME, puis autoriser"
                + " PRISME dans Android.")
        .setPositiveButton(
            "Activer et ouvrir Android",
            (dialog, which) -> {
              activity
                  .getSharedPreferences("prisme_media_access", 0)
                  .edit()
                  .putBoolean("enabled", true)
                  .apply();
              try {
                activity.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
              } catch (RuntimeException error) {
                Toast.makeText(
                        activity, "Réglage Android indisponible sur ce firmware", Toast.LENGTH_LONG)
                    .show();
              }
            })
        .setNegativeButton("Rester en mode simple", null)
        .show();
  }
}

package fr.dm100.prisme;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;
import org.json.*;

/** Native, accessible 4x4 modular home. Editor updates are validated before being saved. */
public final class HomeDeck extends LinearLayout {
  public interface Host {
    void action(String action);
  }

  private final Activity a;
  private final Host host;
  private final HomeStore store;
  private final LinearLayout header, nav;
  private final FrameLayout grid;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private int page;
  private boolean running, editing;
  private final List<Runnable> updates = new ArrayList<>();
  private static final int[][] PALETTES = {
    {0xff0d1421, 0xff1c2c3e, 0xff8aebd7, 0xffeff7ff},
    {0xff112b40, 0xdd25475b, 0xff95dfff, 0xfff1fcff},
    {0xffede7da, 0xfffaf6ed, 0xff775436, 0xff29251e},
    {0xff121917, 0xff202d27, 0xffe4cf92, 0xffeff1dc},
    {0xff100b26, 0xff22163e, 0xffee8aff, 0xfff6f1ff},
    {0xff050708, 0xff111719, 0xffb7e4de, 0xfff2ffff}
  };
  private final Runnable tick =
      new Runnable() {
        public void run() {
          if (!running) return;
          MultiTimers.check(a);
          for (Runnable r : updates) r.run();
          handler.postDelayed(
              this, TimerEngine.deadline(a) > 0 || MultiTimers.active(a) ? 1000 : 10000);
        }
      };

  public HomeDeck(Activity a, Host host) {
    super(a);
    this.a = a;
    this.host = host;
    store = new HomeStore(a);
    setOrientation(VERTICAL);
    header = new LinearLayout(a);
    header.setGravity(Gravity.CENTER_VERTICAL);
    addView(header, new LayoutParams(-1, 45));
    grid = new FrameLayout(a);
    grid.setPadding(4, 0, 4, 0);
    addView(grid, new LayoutParams(-1, 0, 1));
    nav = new LinearLayout(a);
    addView(nav, new LayoutParams(-1, 47));
    setPadding(8, 2, 8, 4);
    grid.addOnLayoutChangeListener(
        (v, l, t, r, b, ol, ot, or, ob) -> {
          if (r - l != or - ol || b - t != ob - ot) post(this::refresh);
        });
  }

  public void resume() {
    running = true;
    handler.removeCallbacks(tick);
    refresh();
    handler.post(tick);
  }

  public void pause() {
    running = false;
    handler.removeCallbacks(tick);
  }

  private Button button(String label, Runnable action, int surface, int accent, int ink) {
    Button b = V6Ui.button(a, label, action);
    b.setTextColor(ink);
    b.setBackground(V6Ui.surface(surface, accent));
    b.setContentDescription(label);
    return b;
  }

  public void refresh() {
    try {
      JSONObject layout = store.read();
      JSONArray pages = layout.getJSONArray("pages");
      page = Math.min(Math.max(0, page), pages.length() - 1);
      JSONObject current = pages.getJSONObject(page);
      int[] colors = PALETTES[layout.getInt("theme")];
      setBackgroundColor(colors[0]);
      header.removeAllViews();
      nav.removeAllViews();
      grid.removeAllViews();
      updates.clear();
      Button prev =
          button(
              "‹",
              () -> {
                page = (page + pages.length() - 1) % pages.length();
                refresh();
              },
              colors[1],
              colors[1],
              colors[3]);
      header.addView(prev, new LayoutParams(48, -1));
      TextView title =
          V6Ui.text(
              a,
              "◈ "
                  + I18n.t(a, current.getString("name"))
                  + "   "
                  + (page + 1)
                  + "/"
                  + pages.length(),
              19);
      I18n.userText(title);
      title.setTextColor(colors[3]);
      title.setTypeface(null, Typeface.BOLD);
      header.addView(title, new LayoutParams(0, -1, 1));
      header.addView(
          button(
              "›",
              () -> {
                page = (page + 1) % pages.length();
                refresh();
              },
              colors[1],
              colors[1],
              colors[3]),
          new LayoutParams(48, -1));
      JSONArray cards = current.getJSONArray("cards");
      int width = Math.max(1, grid.getWidth()), height = Math.max(1, grid.getHeight());
      int scale = layout.getBoolean("largeText") ? 22 : 17;
      for (int i = 0; i < cards.length(); i++) {
        final int index = i;
        JSONObject c = cards.getJSONObject(i);
        String kind = c.getString("kind");
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(10, 5, 10, 5);
        card.setBackground(V6Ui.surface(colors[1], colors[2]));
        card.setClipChildren(true);
        TextView name = V6Ui.text(a, title(kind), 13);
        name.setTextColor(colors[2]);
        card.addView(name, new LayoutParams(-1, -2));
        TextView value = V6Ui.text(a, "", kind.equals("clock") ? Math.min(40, scale + 15) : scale);
        value.setTextColor(colors[3]);
        value.setMaxLines(c.getInt("h") >= 2 ? 3 : 1);
        value.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(value, new LayoutParams(-1, 0, 1));
        Runnable update =
            () -> {
              String text = value(kind);
              if (!text.contentEquals(value.getText())) value.setText(text);
            };
        updates.add(update);
        update.run();
        card.setContentDescription(title(kind));
        card.setFocusable(true);
        card.setOnClickListener(v -> host.action(action(kind)));
        card.setOnLongClickListener(
            v -> {
              editCard(index);
              return true;
            });
        if (editing) {
          final float[] down = new float[2];
          final int baseX = c.getInt("x"),
              baseY = c.getInt("y"),
              cw = c.getInt("w"),
              ch = c.getInt("h");
          card.setOnTouchListener(
              (v, e) -> {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                  down[0] = e.getRawX();
                  down[1] = e.getRawY();
                  return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                  v.setTranslationX(e.getRawX() - down[0]);
                  v.setTranslationY(e.getRawY() - down[1]);
                  return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                  try {
                    JSONObject next = store.read();
                    JSONObject moved =
                        next.getJSONArray("pages")
                            .getJSONObject(page)
                            .getJSONArray("cards")
                            .getJSONObject(index);
                    moved.put(
                        "x",
                        Math.max(
                            0,
                            Math.min(
                                4 - cw,
                                baseX
                                    + Math.round(
                                        (I18n.rtl() ? -4 : 4)
                                            * v.getTranslationX()
                                            / Math.max(1, grid.getWidth())))));
                    moved.put(
                        "y",
                        Math.max(
                            0,
                            Math.min(
                                4 - ch,
                                baseY
                                    + Math.round(
                                        4 * v.getTranslationY() / Math.max(1, grid.getHeight())))));
                    save(next);
                  } catch (Exception x) {
                  }
                  v.setTranslationX(0);
                  v.setTranslationY(0);
                  return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                  v.setTranslationX(0);
                  v.setTranslationY(0);
                  return true;
                }
                return true;
              });
        }

        if (kind.equals("music") && c.getInt("h") >= 2) {
          LinearLayout controls = new LinearLayout(a);
          String[] labels = {"⏮", "▶ / Ⅱ", "⏭"};
          for (int k = 0; k < 3; k++) {
            final int cmd = k;
            controls.addView(
                button(
                    labels[k],
                    () -> MusicPlayback.command(a, cmd == 0 ? 1 : cmd == 2 ? 2 : 0),
                    colors[1],
                    colors[1],
                    colors[3]),
                new LayoutParams(0, 42, 1));
          }
          card.addView(controls);
        }
        FrameLayout.LayoutParams lp =
            new FrameLayout.LayoutParams(
                Math.max(1, width * c.getInt("w") / 4 - 6),
                Math.max(1, height * c.getInt("h") / 4 - 6));
        lp.leftMargin =
            width * (I18n.rtl() ? 4 - c.getInt("x") - c.getInt("w") : c.getInt("x")) / 4 + 3;
        lp.topMargin = height * c.getInt("y") / 4 + 3;
        grid.addView(card, lp);
      }
      String[] names = {"Applications", "Outils", editing ? "Terminer" : "Modifier", "Réglages"},
          actions = {"apps", "v6Tools", "v6Edit", "customize"};
      for (int i = 0; i < names.length; i++) {
        final String action = actions[i];
        LayoutParams lp = new LayoutParams(0, -1, 1);
        lp.setMargins(2, 2, 2, 0);
        nav.addView(
            button(
                names[i],
                () -> {
                  if (action.equals("v6Edit")) {
                    if (editing) {
                      editing = false;
                      refresh();
                    } else editor();
                  } else host.action(action);
                },
                colors[1],
                colors[1],
                colors[3]),
            lp);
      }
      I18n.bind(this);
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
  }

  private String title(String k) {
    switch (k) {
      case "clock":
        return "HORLOGE";
      case "tasks":
        return "TÂCHES";
      case "timer":
        return "MINUTEUR";
      case "music":
        return "MUSIQUE";
      case "notes":
        return "CARNET";
      case "favorites":
        return "FAVORIS";
      default:
        return "MONTRE";
    }
  }

  private String action(String k) {
    switch (k) {
      case "clock":
        return "deskclock";
      case "tasks":
        return "checklist";
      case "timer":
        return "multiTimers";
      case "music":
        return "mediaPanel";
      case "notes":
        return "notes";
      case "favorites":
        return "v6Favorites";
      default:
        return "doctor";
    }
  }

  private String value(String k) {
    switch (k) {
      case "clock":
        return new SimpleDateFormat("HH:mm", I18n.locale()).format(new Date());
      case "tasks":
        return new ChecklistStore(a).remaining() + " à faire";
      case "timer":
        long left = MultiTimers.active(a) ? MultiTimers.nextRemaining(a) : TimerEngine.remaining(a);
        return left > 0 ? ExperienceController.duration(left) : "Ouvrir les minuteurs";
      case "music":
        return MusicPlayback.state(a).title;
      case "notes":
        return "Ouvrir le carnet";
      case "favorites":
        return "Ouvrir mes applications";
      default:
        BatteryLevelReader.Reading b = BatteryLevelReader.read(a, null);
        return (b.level < 0 ? "—" : b.level + " %") + (b.charging ? " · en charge" : "");
    }
  }

  private void save(JSONObject json) {
    try {
      store.save(json);
      refresh();
    } catch (JSONException e) {
      Toast.makeText(a, e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  public void editor() {
    LinearLayout root = V6Ui.column(a);
    root.addView(
        V6Ui.text(
            a,
            "Appui long sur un bloc pour le déplacer ou changer sa taille. La grille refuse les"
                + " chevauchements.",
            16));
    final AlertDialog[] editDialog = new AlertDialog[1];
    V6Ui.add(
        root,
        "Déplacer les blocs au doigt",
        () -> {
          editing = true;
          if (editDialog[0] != null) editDialog[0].dismiss();
          refresh();
        });
    V6Ui.add(root, "Ajouter un bloc", () -> chooseAdd());
    V6Ui.add(
        root,
        "Nommer la page",
        () -> {
          EditText e = new EditText(a);
          e.setText("Ma page");
          new V6Ui.Builder(a)
              .setTitle("Nom de page")
              .setView(e)
              .setPositiveButton(
                  "Enregistrer",
                  (d, w) -> {
                    try {
                      JSONObject o = store.read();
                      o.getJSONArray("pages")
                          .getJSONObject(page)
                          .put("name", e.getText().toString());
                      save(o);
                    } catch (Exception x) {
                    }
                  })
              .setNegativeButton("Annuler", null)
              .show();
        });
    V6Ui.add(
        root,
        "Ajouter une page",
        () -> {
          try {
            JSONObject o = store.read();
            o.getJSONArray("pages")
                .put(
                    new JSONObject()
                        .put("name", "Page " + (o.getJSONArray("pages").length() + 1))
                        .put("cards", new JSONArray()));
            save(o);
          } catch (Exception e) {
          }
        });
    V6Ui.add(
        root,
        "Supprimer cette page",
        () ->
            new V6Ui.Builder(a)
                .setTitle("Supprimer la page ?")
                .setMessage("Les notes et tâches restent conservées.")
                .setPositiveButton(
                    "Supprimer",
                    (d, w) -> {
                      try {
                        JSONObject o = store.read();
                        o.getJSONArray("pages").remove(page);
                        save(o);
                      } catch (Exception e) {
                      }
                    })
                .setNegativeButton("Annuler", null)
                .show());
    V6Ui.add(
        root,
        "Thème visuel",
        () ->
            new V6Ui.Builder(a)
                .setTitle("Thèmes PRISME")
                .setItems(
                    HomeLayout.THEMES,
                    (d, n) -> {
                      try {
                        JSONObject o = store.read();
                        o.put("theme", n);
                        save(o);
                      } catch (Exception e) {
                      }
                    })
                .show());
    V6Ui.add(
        root,
        "Basculer les grands textes",
        () -> {
          try {
            JSONObject o = store.read();
            o.put("largeText", !o.getBoolean("largeText"));
            save(o);
          } catch (Exception e) {
          }
        });
    V6Ui.add(
        root,
        "Annuler la dernière modification",
        () -> {
          store.undo(false);
          refresh();
        });
    V6Ui.add(
        root,
        "Rétablir",
        () -> {
          store.undo(true);
          refresh();
        });
    V6Ui.add(
        root,
        "Revenir au bureau classique",
        () -> {
          store.enabled(false);
          host.action("v6Refresh");
        });
    editDialog[0] = V6Ui.dialog(a, "PRISME 6 · Composer mon accueil", root);
  }

  private void chooseAdd() {
    new V6Ui.Builder(a)
        .setTitle("Ajouter un bloc")
        .setItems(
            new String[] {
              "Horloge", "Tâches", "Minuteur", "Musique", "Carnet", "Favoris", "Montre"
            },
            (d, n) -> {
              try {
                JSONObject o = store.read();
                JSONArray cards = o.getJSONArray("pages").getJSONObject(page).getJSONArray("cards");
                for (int y = 0; y < 4; y++)
                  for (int x = 0; x < 4; x++) {
                    JSONObject test = new JSONObject(o.toString());
                    test.getJSONArray("pages")
                        .getJSONObject(page)
                        .getJSONArray("cards")
                        .put(new HomeLayout.Card(HomeLayout.KINDS[n], x, y, 1, 1).json());
                    try {
                      HomeLayout.validate(test);
                      save(test);
                      return;
                    } catch (JSONException occupied) {
                    }
                  }
                Toast.makeText(a, "Aucune case disponible", 0).show();
              } catch (Exception e) {
              }
            })
        .show();
  }

  private void editCard(int index) {
    try {
      JSONObject o = store.read(),
          card =
              o.getJSONArray("pages")
                  .getJSONObject(page)
                  .getJSONArray("cards")
                  .getJSONObject(index);
      LinearLayout root = V6Ui.column(a);
      String[] keys = {"x", "y", "w", "h"},
          labels = {"Colonne (0 à 3)", "Ligne (0 à 3)", "Largeur (1 à 4)", "Hauteur (1 à 4)"};
      EditText[] fields = new EditText[4];
      for (int i = 0; i < 4; i++) {
        root.addView(V6Ui.text(a, labels[i], 16));
        EditText e = new EditText(a);
        e.setTextColor(V6Ui.INK);
        e.setInputType(2);
        e.setText("" + card.getInt(keys[i]));
        fields[i] = e;
        root.addView(e);
      }
      V6Ui.add(
          root,
          "Appliquer",
          () -> {
            try {
              for (int i = 0; i < 4; i++)
                card.put(keys[i], Integer.parseInt(fields[i].getText().toString()));
              save(o);
            } catch (Exception e) {
              Toast.makeText(a, "Valeurs invalides", 0).show();
            }
          });
      V6Ui.add(
          root,
          "Supprimer le bloc",
          () -> {
            try {
              o.getJSONArray("pages").getJSONObject(page).getJSONArray("cards").remove(index);
              save(o);
            } catch (Exception e) {
            }
          });
      V6Ui.dialog(a, title(card.getString("kind")), root);
    } catch (JSONException e) {
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    pause();
    super.onDetachedFromWindow();
  }
}

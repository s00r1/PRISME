package fr.dm100.prisme;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Regression coverage for edits from an already-open editor after reorder or removal. */
public final class WidgetStoreMutationTest {
  private static int assertions;

  private static void check(boolean condition, String message) {
    assertions++;
    if (!condition) throw new AssertionError(message);
  }

  private interface Action {
    void run() throws Exception;
  }

  private static void reject(Action operation) throws Exception {
    try {
      operation.run();
      throw new AssertionError("Invalid widget mutation accepted");
    } catch (JSONException expected) {
      assertions++;
    }
  }

  public static void main(String[] args) throws Exception {
    DashboardPrefsTest.MemoryContext context = new DashboardPrefsTest.MemoryContext();
    WidgetStore store = new WidgetStore(context);
    String weather = "weather", music = "music", tasks = "tasks";
    int originalMusic = WidgetStore.indexOf(store.read(), music);
    store.move(music, -1);
    check(
        WidgetStore.indexOf(store.read(), music) == originalMusic - 1,
        "Move resolves the current ID");
    store.set(music, "title", "Ma musique");
    check(
        store.find(music).getString("title").equals("Ma musique"),
        "Editor retains identity after reorder");
    check(store.find(weather).getString("title").isEmpty(), "Neighbour remains unchanged");
    store.remove(weather);
    store.set(music, "accent", 3);
    check(
        store.find(music).getInt("accent") == 3,
        "Editor retains identity after another widget is removed");
    check(store.find(tasks).getInt("accent") == 0, "Removal does not redirect later edits");
    String beforeMissing = store.read().toString();
    reject(() -> store.set(weather, "title", "Removed"));
    reject(() -> store.move(weather, 1));
    reject(() -> store.remove(weather));
    check(store.read().toString().equals(beforeMissing), "Missing ID mutations are atomic");
    store.move(music, -1);
    check(
        store.read().toString().equals(beforeMissing), "Moving above the first widget is a no-op");
    store.set(music, "title", "Avant");
    store.set(music, "title", "Après");
    store.set(music, "title", "Après");
    check(store.undo(), "Last edit can be undone");
    check(
        store.find(music).getString("title").equals("Avant"),
        "No-op does not consume undo history");
    check(store.undo(), "Undo can be redone");
    check(store.find(music).getString("title").equals("Après"), "Redo restores the changed value");
    String beforeInvalid = store.read().toString();
    reject(() -> store.set(music, "title", "First\nSecond"));
    reject(() -> store.set(music, "type", 17));
    reject(() -> store.set(music, "id", 32));
    reject(() -> store.set(music, "timezone", 7));
    check(store.read().toString().equals(beforeInvalid), "Invalid edits preserve stored layout");
    JSONArray widgets = store.read().getJSONArray("widgets");
    JSONObject last = widgets.getJSONObject(widgets.length() - 1);
    String lastId = last.getString("id");
    store.move(lastId, 1);
    check(store.read().toString().equals(beforeInvalid), "Moving below the last widget is a no-op");
    context.store("prisme_widget_engine").edit().putString("layout", "broken").apply();
    check(
        store.read().toString().equals(DashboardWidgets.defaults().toString()),
        "Corrupt stored layout recovers to validated defaults");
    System.out.println("WidgetStoreMutationTest: " + assertions + " assertions passed.");
  }
}

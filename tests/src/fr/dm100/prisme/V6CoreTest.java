package fr.dm100.prisme;

import java.util.*;
import org.json.*;

public final class V6CoreTest {
  static int count;

  static void check(boolean ok, String why) {
    count++;
    if (!ok) throw new AssertionError(why);
  }

  interface Action {
    void run() throws Exception;
  }

  static void reject(Action a, String why) throws Exception {
    try {
      a.run();
      throw new AssertionError("accepted " + why);
    } catch (JSONException
        | java.security.GeneralSecurityException
        | IllegalArgumentException expected) {
      count++;
    }
  }

  public static void main(String[] args) throws Exception {
    check(ExternalActions.allowed("multiTimers"), "open timer screen");
    check(
        !ExternalActions.allowed("safeMode")
            && !ExternalActions.allowed("v6Home")
            && !ExternalActions.allowed("app:bad")
            && !ExternalActions.allowed(null),
        "public commands cannot mutate settings");
    check(UtilityMath.calculate("0,1", "0.2", 0).equals("0.3"), "decimal arithmetic");
    check(UtilityMath.calculate("9", "3", 3).equals("3"), "division");
    check(Math.abs(UtilityMath.convert(32, 2, 1, 0)) < 0.0001, "F to C");
    check(Math.abs(UtilityMath.convert(1, 0, 2, 0) - 1000) < 0.0001, "km to m");
    check(UtilityMath.convert(1, 3, 3, 2) == 24, "day to hour");
    check(Math.abs(UtilityMath.convert(273.15, 2, 2, 0)) < 0.0001, "K to C");
    reject(() -> UtilityMath.calculate("1e999999999", "1", 0), "unbounded exponent");
    try {
      UtilityMath.calculate("1", "0", 3);
      throw new AssertionError("division by zero");
    } catch (ArithmeticException expected) {
      count++;
    }
    check(Countdown.remaining(2000,5000,-1,-1,1500,10,0)==500,"unknown boot uses wall clock");
    JSONObject defaults = HomeLayout.defaults();
    HomeLayout.validate(defaults);
    check(defaults.getJSONArray("pages").length() == 3, "default pages");
    for (int x = -1; x < 6; x++)
      for (int y = -1; y < 6; y++)
        for (int w = 0; w < 6; w++)
          for (int h = 0; h < 6; h++) {
            JSONObject test = HomeLayout.defaults();
            JSONArray cards = new JSONArray().put(new HomeLayout.Card("clock", x, y, w, h).json());
            test.getJSONArray("pages").getJSONObject(0).put("cards", cards);
            boolean valid =
                x >= 0 && x < 4 && y >= 0 && y < 4 && w >= 1 && h >= 1 && x + w <= 4 && y + h <= 4;
            try {
              HomeLayout.validate(test);
              check(valid, "layout boundary");
            } catch (JSONException e) {
              check(!valid, "valid layout rejected");
            }
          }
    JSONObject overlap = HomeLayout.defaults();
    overlap
        .getJSONArray("pages")
        .getJSONObject(0)
        .getJSONArray("cards")
        .put(new HomeLayout.Card("device", 0, 0, 1, 1).json());
    reject(() -> HomeLayout.validate(overlap), "overlap");
    JSONObject unknown = HomeLayout.defaults().put("script", "bad");
    reject(() -> HomeLayout.validate(unknown), "unknown field");
    JSONObject bad = HomeLayout.defaults();
    bad.getJSONArray("pages").getJSONObject(0).put("name", "\n");
    reject(() -> HomeLayout.validate(bad), "control character");
    DashboardPrefsTest.MemoryContext c = new DashboardPrefsTest.MemoryContext();
    HomeStore home = new HomeStore(c);
    String before = home.read().toString();
    JSONObject edit = home.read().put("theme", 3);
    home.save(edit);
    check(home.read().getInt("theme") == 3, "saved theme");
    check(home.undo(false), "undo");
    check(home.read().toString().equals(before), "exact undo");
    check(home.undo(true) && home.read().getInt("theme") == 3, "redo");
    DashboardPrefs prefs = new DashboardPrefs(c);
    prefs.putString("notes", "Keep my note");
    new MusicPrefs(c).putInt("style", 4);
    new BatteryIndicatorPrefs(c).putInt("style", 22);
    JSONObject profile = prefs.exportJSONObject();
    DashboardPrefsTest.MemoryContext other = new DashboardPrefsTest.MemoryContext();
    new DashboardPrefs(other).importJSONObject(profile);
    check(new HomeStore(other).read().getInt("theme") == 3, "home round trip");
    check(new MusicPrefs(other).number("style", 0) == 4, "music round trip");
    check(new BatteryIndicatorPrefs(other).integer("style", 0) == 22, "battery round trip");
    JSONObject hostile = new JSONObject(profile.toString());
    hostile.getJSONObject("v6").getJSONObject("home").getJSONObject("layout").put("theme", 6);
    String snapshot = prefs.exportJSONObject().toString();
    reject(() -> prefs.importJSONObject(hostile), "invalid module");
    check(snapshot.equals(prefs.exportJSONObject().toString()), "validation before any write");
    c.store("prisme_home")
        .edit()
        .putLong("longTest", 98765432101L)
        .putFloat("floatTest", 1.5f)
        .putStringSet("setTest", new HashSet<>(Arrays.asList("a", "b")))
        .apply();
    ProfileJournal.begin(c);
    c.store("prisme_dashboard").edit().putString("notes", "interrupted").apply();
    c.store("prisme_home").edit().clear().apply();
    check(ProfileJournal.recover(c), "interruption recovered");
    check(prefs.getString("notes", "").equals("Keep my note"), "note recovered");
    check(
        c.store("prisme_home").getLong("longTest", 0) == 98765432101L, "long primitive recovered");
    check(
        c.store("prisme_home").getStringSet("setTest", Collections.emptySet()).size() == 2,
        "set recovered");
    check(!ProfileJournal.recover(c), "recovery idempotent");
    ProfileJournal.begin(c);
    c.store("prisme_dashboard").edit().putString("notes", "committed").apply();
    ProfileJournal.finish(c);
    check(
        !ProfileJournal.recover(c) && prefs.getString("notes", "").equals("committed"),
        "committed import kept");
    check(
        Countdown.remaining(2000, 5000, 3, 3, 9000000, 4000, 0) == 1000,
        "wall clock change ignored");
    check(Countdown.remaining(2000, 5000, 3, 4, 1500, 10, 0) == 500, "reboot uses wall clock");
    check(Countdown.remaining(0, 0, 3, 3, 0, 0, 450) == 450, "subsecond pause");
    check(
        Countdown.validDuration(1)
            && !Countdown.validDuration(0)
            && !Countdown.validDuration(86400001L),
        "duration bounds");
    reject(() -> Countdown.validateName("\n"), "timer name");
    char[] password = "correct horse battery".toCharArray();
    JSONObject vault = BackupVault.seal(profile, password);
    check(
        BackupVault.open(vault, password).toString().equals(profile.toString()),
        "encrypted round trip");
    reject(() -> BackupVault.open(vault, "wrong password".toCharArray()), "wrong password");
    JSONObject changed = new JSONObject(vault.toString());
    String payload = changed.getString("data");
    changed.put("data", (payload.charAt(0) == 'A' ? "B" : "A") + payload.substring(1));
    reject(() -> BackupVault.open(changed, password), "tampering");
    reject(() -> BackupVault.seal(profile, "short".toCharArray()), "weak password");
    JSONObject second = BackupVault.seal(profile, password);
    check(!vault.getString("data").equals(second.getString("data")), "fresh salt and nonce");
    Arrays.fill(password, '\0');
    System.out.println("V6CoreTest: " + count + " assertions passed.");
  }
}

package fr.dm100.prisme;

import org.json.*;

public final class WeatherWidgetTest {
  static int count;

  static void check(boolean ok, String why) {
    count++;
    if (!ok) throw new AssertionError(why);
  }

  interface Action {
    void run() throws Exception;
  }

  static void reject(Action a) throws Exception {
    try {
      a.run();
      throw new AssertionError("invalid accepted");
    } catch (JSONException expected) {
      count++;
    }
  }

  public static void main(String[] args) throws Exception {
    JSONObject forecast =
        new JSONObject()
            .put(
                "current",
                new JSONObject()
                    .put("temperature_2m", 12.5)
                    .put("weather_code", 3)
                    .put("is_day", 1));
    WeatherData weather = new WeatherData(forecast, "Test city", 48, 2, 1000);
    check(weather.temperature(false) == 12.5, "Celsius");
    check(Math.abs(weather.temperature(true) - 54.5) < .001, "Fahrenheit");
    check(weather.stale(1000 + 91 * 60000L), "stale timestamp");
    check(weather.stale(500), "clock rollback marked stale");
    check(!weather.stale(2000), "fresh timestamp");
    check(WeatherData.cached(weather.cache()).city.equals("Test city"), "weather cache round trip");
    reject(() -> new WeatherData(forecast, "Test", 91, 2, 1000));
    JSONObject malformed = new JSONObject().put("current", new JSONObject().put("weather_code", 0));
    reject(() -> new WeatherData(malformed, "Test", 0, 0, 1000));
    check(
        WeatherData.condition(-1).equals("Conditions inconnues"),
        "missing condition not fabricated");
    check(
        WeatherData.condition(40).equals("Conditions inconnues"),
        "unknown WMO condition not invented");
    check(WeatherData.condition(67).contains("verglaçante"), "freezing rain distinct");
    check(WeatherData.condition(99).contains("grêle"), "hail distinct");
    JSONObject config = DashboardWidgets.defaults();
    DashboardWidgets.validate(config);
    for (int w : new int[] {320, 480, 640, 720, 854})
      for (int h : new int[] {320, 480, 640})
        for (int requested : new int[] {0, 144, 176, 208, 999}) {
          DashboardWidgets.Bounds area = DashboardWidgets.rail(w, h, requested);
          check(area.width >= 0 && area.width <= w, "widget rail width bound");
          check(area.height >= 0 && area.height <= h, "widget rail height bound");
          if (w >= h) check(w - area.width >= 320, "desktop keeps minimum width");
        }
    JSONObject bad = new JSONObject(config.toString());
    bad.getJSONArray("widgets").getJSONObject(0).put("type", "javascript:bad");
    reject(() -> DashboardWidgets.validate(bad));
    JSONObject duplicate = new JSONObject(config.toString());
    duplicate.getJSONArray("widgets").put(duplicate.getJSONArray("widgets").get(0));
    reject(() -> DashboardWidgets.validate(duplicate));
    JSONObject tooWide = new JSONObject(config.toString()).put("width", 500);
    reject(() -> DashboardWidgets.validate(tooWide));
    DashboardPrefsTest.MemoryContext c = new DashboardPrefsTest.MemoryContext();
    new WidgetStore(c).save(config);
    DashboardPrefs p = new DashboardPrefs(c);
    JSONObject export = p.exportJSONObject();
    check(export.getJSONObject("v6").has("dashboardWidgets"), "widgets included in profile");
    DashboardPrefsTest.MemoryContext dest = new DashboardPrefsTest.MemoryContext();
    dest.store("prisme_weather_consent").edit().putBoolean("enabled", false).apply();
    dest.store("prisme_weather").edit().putString("city", "Private city").apply();
    new DashboardPrefs(dest).importJSONObject(export);
    check(
        !dest.store("prisme_weather_consent").getBoolean("enabled", false),
        "profile cannot grant weather consent");
    check(
        dest.store("prisme_weather").getString("city", "").equals("Private city"),
        "profile cannot change chosen location");
    JSONObject hostile = new JSONObject(export.toString());
    hostile.getJSONObject("v6").getJSONObject("weather").put("endpoint", "http://evil.example");
    reject(() -> p.importJSONObject(hostile));
    System.out.println("WeatherWidgetTest: " + count + " assertions passed.");
  }
}

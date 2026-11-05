package fr.dm100.prisme.qa;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.json.JSONArray;
import org.json.JSONObject;

public final class WeatherValidation extends Instrumentation {
  Context c;
  int checks;
  Map<String, Map<String, ?>> saved = new HashMap<>();
  String[] names = {"prisme_weather", "prisme_weather_cache", "prisme_weather_consent"};

  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  Class<?> type(String name) throws Exception {
    return Class.forName("fr.dm100.prisme." + name, true, c.getClassLoader());
  }

  void check(boolean ok, String why) {
    checks++;
    if (!ok) throw new AssertionError(why);
  }

  @Override
  public void onStart() {
    c = getTargetContext();
    Bundle out = new Bundle();
    try {
      for (String name : names)
        saved.put(name, new HashMap<>(c.getSharedPreferences(name, 0).getAll()));
      c.getSharedPreferences("prisme_weather_consent", 0)
          .edit()
          .putBoolean("enabled", true)
          .commit();
      Class<?> repo = type("WeatherRepository"), citiesInterface = type("WeatherRepository$Cities");
      CountDownLatch search = new CountDownLatch(1);
      JSONArray[] found = {null};
      String[] failure = {null};
      Object searchCallback =
          Proxy.newProxyInstance(
              c.getClassLoader(),
              new Class[] {citiesInterface},
              (p, m, args) -> {
                if (m.getName().equals("complete")) {
                  found[0] = (JSONArray) args[0];
                  failure[0] = (String) args[1];
                  search.countDown();
                }
                return null;
              });
      repo.getMethod("search", Context.class, String.class, citiesInterface)
          .invoke(null, c, "Paris", searchCallback);
      check(search.await(35, TimeUnit.SECONDS), "city lookup timeout");
      check(found[0] != null && found[0].length() > 0, "city lookup: " + failure[0]);
      JSONObject city = found[0].getJSONObject(0);
      repo.getMethod("select", Context.class, JSONObject.class).invoke(null, c, city);

      Class<?> callback = type("WeatherRepository$Callback");
      Class<?> dataType = type("WeatherData");
      CountDownLatch forecasts = new CountDownLatch(2);
      Object[] received = {null, null};
      String[] failures = {null, null};
      AtomicIntegerArray calls = new AtomicIntegerArray(2);
      Object[] updates = new Object[2];
      for (int i = 0; i < updates.length; i++) {
        final int index = i;
        updates[i] =
            Proxy.newProxyInstance(
                c.getClassLoader(),
                new Class[] {callback},
                (p, m, args) -> {
                  if (m.getName().equals("complete")) {
                    received[index] = args[0];
                    failures[index] = (String) args[1];
                    // A duplicate callback must never satisfy the other request's latch slot.
                    if (calls.incrementAndGet(index) == 1) forecasts.countDown();
                  }
                  return null;
                });
      }
      // One main-loop turn guarantees both callers register before any posted network result.
      Throwable[] launchFailure = {null};
      runOnMainSync(
          () -> {
            try {
              repo.getMethod("refresh", Context.class, boolean.class, callback)
                  .invoke(null, c, true, updates[0]);
              repo.getMethod("refresh", Context.class, boolean.class, callback)
                  .invoke(null, c, true, updates[1]);
            } catch (Exception error) {
              launchFailure[0] = error;
            }
          });
      if (launchFailure[0] != null) throw new RuntimeException(launchFailure[0]);
      check(forecasts.await(40, TimeUnit.SECONDS), "both forecast callbacks timeout");
      // Drain the delivery turn before inspecting callback counts and the persistent cache.
      runOnMainSync(() -> {});
      check(received[0] != null, "forecast: " + failures[0]);
      check(received[1] != null, "concurrent forecast: " + failures[1]);
      check(calls.get(0) == 1 && calls.get(1) == 1, "each forecast caller completes exactly once");
      check(
          failures[0] == null && failures[1] == null,
          "both concurrent callers receive a successful result");
      long fetched = dataType.getField("fetchedAt").getLong(received[0]);
      check(
          fetched > 0 && fetched == dataType.getField("fetchedAt").getLong(received[1]),
          "concurrent callers share the same fetchedAt");
      double degrees =
          (Double) dataType.getMethod("temperature", boolean.class).invoke(received[0], false);
      check(Double.isFinite(degrees) && degrees >= -100 && degrees <= 70, "real temperature range");
      double secondDegrees =
          (Double) dataType.getMethod("temperature", boolean.class).invoke(received[1], false);
      check(
          Double.compare(degrees, secondDegrees) == 0,
          "concurrent callers receive the same real temperature");
      Object cached = repo.getMethod("cached", Context.class).invoke(null, c);
      check(cached != null, "persistent cache");
      check(
          dataType.getField("fetchedAt").getLong(cached) == fetched,
          "cache contains the completed shared forecast");
      repo.getMethod("disable", Context.class).invoke(null, c);
      check(!(Boolean) repo.getMethod("allowed", Context.class).invoke(null, c), "disable consent");
      check(
          repo.getMethod("cached", Context.class).invoke(null, c) != null,
          "offline cache preserved");
      out.putString("result", "PASS");
      out.putString("network", "real HTTPS geocoding and shared concurrent forecast on Android25");
    } catch (Throwable e) {
      out.putString("result", "FAIL");
      out.putString("failure", e.toString());
    } finally {
      // Invalidate an unfinished request before restoring preferences after a timeout/failure.
      try {
        type("WeatherRepository").getMethod("disable", Context.class).invoke(null, c);
      } catch (Exception ignored) {
      }
      for (String name : names) {
        SharedPreferences.Editor edit = c.getSharedPreferences(name, 0).edit().clear();
        Map<String, ?> values = saved.get(name);
        if (values != null)
          for (Map.Entry<String, ?> e : values.entrySet()) {
            Object value = e.getValue();
            if (value instanceof String) edit.putString(e.getKey(), (String) value);
            else if (value instanceof Boolean) edit.putBoolean(e.getKey(), (Boolean) value);
            else if (value instanceof Integer) edit.putInt(e.getKey(), (Integer) value);
            else if (value instanceof Long) edit.putLong(e.getKey(), (Long) value);
          }
        edit.commit();
      }
      out.putString("preferences", "restored");
      out.putInt("assertions", checks);
    }
    finish(Activity.RESULT_OK, out);
  }
}

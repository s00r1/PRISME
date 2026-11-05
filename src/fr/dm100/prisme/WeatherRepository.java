package fr.dm100.prisme;

import android.content.*;
import android.os.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.*;

/** Foreground-only HTTPS weather requests, explicit consent, bounded data and local cache. */
public final class WeatherRepository {
  public interface Callback {
    void complete(WeatherData data, String error);
  }

  public interface Cities {
    void complete(JSONArray cities, String error);
  }

  private static final ExecutorService worker = Executors.newSingleThreadExecutor();
  private static final Handler main = new Handler(Looper.getMainLooper());
  private static final AtomicInteger generation = new AtomicInteger();
  private static final AtomicInteger searchSequence = new AtomicInteger();
  private static boolean busy;
  private static int busyGeneration = -1;
  private static final Map<Integer, List<Callback>> waiting = new HashMap<>();
  private static long attemptAt;

  private WeatherRepository() {}

  public static SharedPreferences options(Context c) {
    return c.getSharedPreferences("prisme_weather", 0);
  }

  public static boolean allowed(Context c) {
    return PrefValues.flag(c.getSharedPreferences("prisme_weather_consent", 0), "enabled", false);
  }

  public static String city(Context c) {
    return PrefValues.text(options(c), "city", "");
  }

  public static boolean fahrenheit(Context c) {
    return PrefValues.flag(options(c), "fahrenheit", false);
  }

  public static WeatherData cached(Context c) {
    try {
      return WeatherData.cached(
          new JSONObject(
              PrefValues.text(c.getSharedPreferences("prisme_weather_cache", 0), "data", "")));
    } catch (Exception e) {
      return null;
    }
  }

  public static String status(Context c) {
    return PrefValues.text(c.getSharedPreferences("prisme_weather_cache", 0), "error", "");
  }

  private static JSONObject get(String url) throws Exception {
    URL address = new URL(url);
    if (!"https".equals(address.getProtocol())
        || !("api.open-meteo.com".equals(address.getHost())
            || "geocoding-api.open-meteo.com".equals(address.getHost())))
      throw new IOException("Hôte météo refusé");
    HttpURLConnection connection = (HttpURLConnection) address.openConnection();
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(15000);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestProperty("User-Agent", "PRISME/6 (weather; non-commercial)");
    try {
      if (connection.getResponseCode() != 200)
        throw new IOException("HTTP " + connection.getResponseCode());
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (InputStream in = connection.getInputStream()) {
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
          if (bytes.size() + n > 262144) throw new IOException("Réponse trop grande");
          bytes.write(buf, 0, n);
        }
      }
      return new JSONObject(new String(bytes.toByteArray(), "UTF-8"));
    } finally {
      connection.disconnect();
    }
  }

  public static void search(Context context, String query, Cities callback) {
    Context c = context.getApplicationContext();
    final int request = generation.get(), search = searchSequence.incrementAndGet();
    String q = query == null ? "" : query.trim();
    if (!allowed(c)) {
      callback.complete(new JSONArray(), "Activer la météo connectée");
      return;
    }
    if (q.length() < 2 || q.length() > 80) {
      callback.complete(new JSONArray(), "Ville : 2 à 80 caractères");
      return;
    }
    worker.execute(
        () -> {
          JSONArray cities = new JSONArray();
          String error = null;
          try {
            if (request != generation.get() || search != searchSequence.get() || !allowed(c))
              throw new CancellationException();
            JSONObject result =
                get(
                    "https://geocoding-api.open-meteo.com/v1/search?count=5&language=fr&format=json&name="
                        + URLEncoder.encode(q, "UTF-8"));
            JSONArray found = result.optJSONArray("results");
            if (found != null)
              for (int i = 0; i < Math.min(5, found.length()); i++) {
                JSONObject x = found.getJSONObject(i);
                double lat = x.getDouble("latitude"), lon = x.getDouble("longitude");
                if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180)
                  cities.put(
                      new JSONObject()
                          .put("name", x.getString("name"))
                          .put("region", x.optString("admin1"))
                          .put("country", x.optString("country"))
                          .put("latitude", lat)
                          .put("longitude", lon)
                          .put("timezone", x.optString("timezone", "UTC")));
              }
          } catch (Exception e) {
            error = "Recherche indisponible · vérifier la connexion";
          }
          JSONArray result = cities;
          String failure = error;
          main.post(
              () -> {
                if (request != generation.get() || search != searchSequence.get() || !allowed(c)) {
                  callback.complete(
                      new JSONArray(),
                      allowed(c) ? "Recherche remplacée · relancer" : "Météo connectée désactivée");
                } else callback.complete(result, failure);
              });
        });
  }

  public static void select(Context c, JSONObject city) throws JSONException {
    if (!allowed(c)) throw new JSONException("Météo connectée désactivée");
    double lat = city.getDouble("latitude"), lon = city.getDouble("longitude");
    if (!Double.isFinite(lat)
        || lat < -90
        || lat > 90
        || !Double.isFinite(lon)
        || lon < -180
        || lon > 180) throw new JSONException("Ville invalide");
    generation.incrementAndGet();
    options(c)
        .edit()
        .putString("city", city.getString("name") + " · " + city.optString("country"))
        .putString("lat", Double.toString(lat))
        .putString("lon", Double.toString(lon))
        .putString("timezone", city.optString("timezone", "UTC"))
        .commit();
    c.getSharedPreferences("prisme_weather_cache", 0).edit().clear().commit();
    synchronized (WeatherRepository.class) {
      attemptAt = 0;
    }
  }

  public static void refresh(Context context, boolean force, Callback callback) {
    Context c = context.getApplicationContext();
    WeatherData cache = cached(c);
    if (!allowed(c) || city(c).isEmpty()) {
      callback.complete(cache, allowed(c) ? "Choisir une ville" : "Météo connectée désactivée");
      return;
    }
    long now = System.currentTimeMillis();
    final int request = generation.get();
    int interval = Math.max(15, Math.min(180, PrefValues.number(options(c), "interval", 45)));
    synchronized (WeatherRepository.class) {
      if (busy && busyGeneration == request) {
        waiting.get(request).add(callback);
        return;
      }
      if (!force
          && cache != null
          && now >= cache.fetchedAt
          && now - cache.fetchedAt < interval * 60000L) {
        callback.complete(cache, null);
        return;
      }
      if (now >= attemptAt && now - attemptAt < (force ? 60000 : 300000)) {
        long seconds = ((force ? 60000 : 300000) - (now - attemptAt) + 999) / 1000;
        callback.complete(cache, "Actualisation limitée · réessayer dans " + seconds + " s");
        return;
      }
      attemptAt = now;
      busy = true;
      busyGeneration = request;
      List<Callback> callbacks = new ArrayList<>();
      callbacks.add(callback);
      waiting.put(request, callbacks);
    }
    final String selectedCity = city(c);
    final String lat = PrefValues.text(options(c), "lat", ""),
        lon = PrefValues.text(options(c), "lon", "");
    worker.execute(
        () -> {
          WeatherData result = null;
          String error = null;
          try {
            if (request != generation.get() || !allowed(c) || !selectedCity.equals(city(c)))
              throw new CancellationException();
            double latitude = Double.parseDouble(lat), longitude = Double.parseDouble(lon);
            if (!Double.isFinite(latitude)
                || latitude < -90
                || latitude > 90
                || !Double.isFinite(longitude)
                || longitude < -180
                || longitude > 180) throw new IOException("Position invalide");
            String url =
                "https://api.open-meteo.com/v1/forecast?latitude="
                    + latitude
                    + "&longitude="
                    + longitude
                    + "&timezone=auto&timeformat=unixtime&forecast_days=5&current=temperature_2m,apparent_temperature,relative_humidity_2m,is_day,precipitation,weather_code,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,precipitation_probability,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max,uv_index_max";
            result =
                new WeatherData(
                    get(url), selectedCity, latitude, longitude, System.currentTimeMillis());
          } catch (Exception e) {
            error = "Actualisation impossible · cache conservé";
          }
          WeatherData received = result;
          String failure = error;
          main.post(
              () -> {
                List<Callback> callbacks;
                synchronized (WeatherRepository.class) {
                  if (busyGeneration == request) busy = false;
                  callbacks = waiting.remove(request);
                }
                if (request != generation.get() || !allowed(c) || !selectedCity.equals(city(c))) {
                  if (callbacks != null)
                    for (Callback pending : callbacks)
                      pending.complete(
                          cached(c),
                          allowed(c)
                              ? "Ville modifiée · actualiser"
                              : "Météo connectée désactivée");
                  return;
                }
                if (received != null)
                  try {
                    c.getSharedPreferences("prisme_weather_cache", 0)
                        .edit()
                        .putString("data", received.cache().toString())
                        .putString("error", "")
                        .apply();
                  } catch (JSONException ignored) {
                  }
                else
                  c.getSharedPreferences("prisme_weather_cache", 0)
                      .edit()
                      .putString("error", failure)
                      .apply();
                if (callbacks != null)
                  for (Callback pending : callbacks)
                    pending.complete(received != null ? received : cached(c), failure);
              });
        });
  }

  public static void disable(Context c) {
    generation.incrementAndGet();
    synchronized (WeatherRepository.class) {
      attemptAt = 0;
    }
    c.getSharedPreferences("prisme_weather_consent", 0)
        .edit()
        .putBoolean("enabled", false)
        .commit();
  }
}

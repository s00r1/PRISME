package fr.dm100.prisme;

import org.json.*;

/** Validated Open-Meteo response. Missing measurements stay unknown, never fabricated. */
public final class WeatherData {
  public final JSONObject raw;
  public final long fetchedAt;
  public final String city;
  public final double latitude, longitude;

  public WeatherData(JSONObject response, String city, double lat, double lon, long fetched)
      throws JSONException {
    if (response == null
        || response.toString().length() > 262144
        || !Double.isFinite(lat)
        || lat < -90
        || lat > 90
        || !Double.isFinite(lon)
        || lon < -180
        || lon > 180
        || city == null
        || city.length() > 120) throw new JSONException("Données météo invalides");
    JSONObject current = response.getJSONObject("current");
    double temp = current.optDouble("temperature_2m", Double.NaN);
    if (!Double.isFinite(temp) || temp < -100 || temp > 70)
      throw new JSONException("Température indisponible");
    int code = current.getInt("weather_code");
    if (code < 0 || code > 99) throw new JSONException("Condition météo invalide");
    raw = new JSONObject(response.toString());
    this.city = city;
    latitude = lat;
    longitude = lon;
    fetchedAt = fetched;
  }

  public double temperature(boolean fahrenheit) {
    double n = raw.optJSONObject("current").optDouble("temperature_2m", Double.NaN);
    return fahrenheit ? n * 9 / 5 + 32 : n;
  }

  public int code() {
    return raw.optJSONObject("current").optInt("weather_code", -1);
  }

  public boolean day() {
    return raw.optJSONObject("current").optInt("is_day", 1) == 1;
  }

  public boolean stale(long now) {
    return now < fetchedAt || now - fetchedAt > 90 * 60000L;
  }

  public String temperatureText(boolean fahrenheit) {
    return String.format(
        java.util.Locale.getDefault(), "%.0f°%s", temperature(fahrenheit), fahrenheit ? "F" : "C");
  }

  public static String condition(int code) {
    switch (code) {
      case 0:
        return "Ciel dégagé";
      case 1:
      case 2:
        return "Éclaircies";
      case 3:
        return "Couvert";
      case 45:
      case 48:
        return "Brouillard";
      case 51:
      case 53:
      case 55:
        return "Bruine";
      case 56:
      case 57:
        return "Bruine verglaçante";
      case 61:
      case 63:
      case 65:
        return "Pluie";
      case 66:
      case 67:
        return "Pluie verglaçante";
      case 71:
      case 73:
      case 75:
      case 77:
        return "Neige";
      case 80:
      case 81:
      case 82:
        return "Averses";
      case 85:
      case 86:
        return "Averses de neige";
      case 95:
        return "Orage";
      case 96:
      case 99:
        return "Orage et grêle";
      default:
        return "Conditions inconnues";
    }
  }

  public JSONObject cache() throws JSONException {
    return new JSONObject()
        .put("response", raw)
        .put("city", city)
        .put("latitude", latitude)
        .put("longitude", longitude)
        .put("fetchedAt", fetchedAt);
  }

  public static WeatherData cached(JSONObject o) throws JSONException {
    return new WeatherData(
        o.getJSONObject("response"),
        o.getString("city"),
        o.getDouble("latitude"),
        o.getDouble("longitude"),
        o.getLong("fetchedAt"));
  }
}

package fr.dm100.prisme;

import android.content.SharedPreferences;

public final class PrefValues {
  private PrefValues() {}

  public static int number(SharedPreferences p, String key, int fallback) {
    try {
      return p.getInt(key, fallback);
    } catch (ClassCastException e) {
      return fallback;
    }
  }

  public static boolean flag(SharedPreferences p, String key, boolean fallback) {
    try {
      return p.getBoolean(key, fallback);
    } catch (ClassCastException e) {
      return fallback;
    }
  }

  public static String text(SharedPreferences p, String key, String fallback) {
    try {
      return p.getString(key, fallback);
    } catch (ClassCastException e) {
      return fallback;
    }
  }
}

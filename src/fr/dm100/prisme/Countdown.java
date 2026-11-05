package fr.dm100.prisme;

import org.json.*;

/** Countdown arithmetic independent of Android, monotonic within a boot. */
public final class Countdown {
  private Countdown() {}

  public static long remaining(
      long wall,
      long elapsed,
      int savedBoot,
      int boot,
      long nowWall,
      long nowElapsed,
      long paused) {
    if (wall == 0) return Math.max(0, paused);
    return Math.max(
        0,
        elapsed > 0 && savedBoot >= 0 && savedBoot == boot ? elapsed - nowElapsed : wall - nowWall);
  }

  public static void validateName(String name) throws JSONException {
    if (name == null || name.trim().isEmpty() || name.length() > 32)
      throw new JSONException("Nom du minuteur invalide");
    for (char c : name.toCharArray())
      if (Character.isISOControl(c)) throw new JSONException("Nom du minuteur invalide");
  }

  public static boolean validDuration(long ms) {
    return ms > 0 && ms <= 86400000L;
  }
}

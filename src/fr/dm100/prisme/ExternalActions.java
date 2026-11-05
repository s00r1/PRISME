package fr.dm100.prisme;

import java.util.*;

/** Public launcher entry points may open screens, never execute configuration changes. */
public final class ExternalActions {
  private ExternalActions() {}

  private static final Set<String> OPEN =
      new HashSet<>(
          Arrays.asList(
              "notes",
              "timer",
              "multiTimers",
              "mediaPanel",
              "musicPlayerSettings",
              "batteryIndicatorSettings",
              "rotationSettings",
              "controlCenter",
              "controlCenterSettings",
              "floatingVolumeSettings",
              "doctor",
              "search",
              "today",
              "apps",
              "customize",
              "v6Tools",
              "studio",
              "sense",
              "edgeDock",
              "stopwatch",
              "checklist",
              "deskclock"));

  public static boolean allowed(String value) {
    return value != null && OPEN.contains(value);
  }
}

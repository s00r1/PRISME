package fr.dm100.prisme;

import java.math.*;

/** Offline arithmetic with explicit operators, never interpreted code. */
public final class UtilityMath {
  private UtilityMath() {}

  public static String calculate(String left, String right, int operation) {
    if (!left.matches("[-+]?\\d{1,30}(?:[.,]\\d{0,20})?")
        || !right.matches("[-+]?\\d{1,30}(?:[.,]\\d{0,20})?"))
      throw new IllegalArgumentException("Nombre trop long");
    BigDecimal a = new BigDecimal(left.replace(',', '.')),
        b = new BigDecimal(right.replace(',', '.'));
    BigDecimal r;
    switch (operation) {
      case 0:
        r = a.add(b);
        break;
      case 1:
        r = a.subtract(b);
        break;
      case 2:
        r = a.multiply(b);
        break;
      case 3:
        r = a.divide(b, MathContext.DECIMAL64);
        break;
      default:
        throw new IllegalArgumentException("Opération invalide");
    }
    return r.stripTrailingZeros().toPlainString();
  }

  public static final String[][] UNITS = {
    {"m", "cm", "km", "pouces", "pieds"},
    {"kg", "g", "livres"},
    {"°C", "°F", "K"},
    {"minutes", "secondes", "heures", "jours"}
  };
  private static final double[][] FACTORS = {
    {1, .01, 1000, .0254, .3048}, {1, .001, .45359237}, {1, 1, 1}, {60, 1, 3600, 86400}
  };

  public static double convert(double value, int category, int from, int to) {
    if (!Double.isFinite(value)
        || category < 0
        || category >= 4
        || from < 0
        || to < 0
        || from >= UNITS[category].length
        || to >= UNITS[category].length) throw new IllegalArgumentException("Conversion invalide");
    double result;
    if (category == 2) {
      double c = from == 0 ? value : from == 1 ? (value - 32) * 5 / 9 : value - 273.15;
      result = to == 0 ? c : to == 1 ? c * 9 / 5 + 32 : c + 273.15;
    } else result = value * FACTORS[category][from] / FACTORS[category][to];
    if (!Double.isFinite(result)) throw new IllegalArgumentException("Nombre hors limites");
    return result;
  }
}

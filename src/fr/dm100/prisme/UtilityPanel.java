package fr.dm100.prisme;

import android.app.*;
import android.view.*;
import android.widget.*;

public final class UtilityPanel {
  private UtilityPanel() {}

  static EditText input(Activity a, String value) {
    EditText e = new EditText(a);
    e.setText(value);
    e.setTextColor(V6Ui.INK);
    e.setInputType(0x3002);
    e.setSingleLine();
    e.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(64)});
    return e;
  }

  static Spinner spinner(Activity a, String[] values) {
    Spinner s = new Spinner(a);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(a, android.R.layout.simple_spinner_item, values);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    s.setAdapter(adapter);
    return s;
  }

  public static void calculator(Activity a) {
    LinearLayout root = V6Ui.column(a);
    EditText left = input(a, "0"), right = input(a, "0");
    Spinner operation = spinner(a, new String[] {"+", "−", "×", "÷"});
    TextView result = V6Ui.text(a, "", 28);
    root.addView(left);
    root.addView(operation);
    root.addView(right);
    root.addView(result);
    V6Ui.add(
        root,
        "Calculer",
        () -> {
          try {
            result.setText(
                UtilityMath.calculate(
                    left.getText().toString(),
                    right.getText().toString(),
                    operation.getSelectedItemPosition()));
          } catch (Exception e) {
            result.setText("Calcul impossible : vérifier les nombres et la division par zéro");
          }
        });
    V6Ui.dialog(a, "Calculatrice", root);
  }

  public static void converter(Activity a) {
    LinearLayout root = V6Ui.column(a);
    EditText value = input(a, "1");
    Spinner category = spinner(a, new String[] {"Longueur", "Masse", "Température", "Durée"}),
        from = spinner(a, UtilityMath.UNITS[0]),
        to = spinner(a, UtilityMath.UNITS[0]);
    root.addView(category);
    root.addView(value);
    root.addView(V6Ui.text(a, "De", 16));
    root.addView(from);
    root.addView(V6Ui.text(a, "Vers", 16));
    root.addView(to);
    TextView result = V6Ui.text(a, "", 26);
    root.addView(result);
    category.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          public void onItemSelected(android.widget.AdapterView<?> p, View v, int i, long id) {
            for (Spinner s : new Spinner[] {from, to}) {
              ArrayAdapter<String> x =
                  new ArrayAdapter<>(a, android.R.layout.simple_spinner_item, UtilityMath.UNITS[i]);
              x.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
              s.setAdapter(x);
            }
          }

          public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
    V6Ui.add(
        root,
        "Convertir",
        () -> {
          try {
            double answer =
                UtilityMath.convert(
                    Double.parseDouble(value.getText().toString().replace(',', '.')),
                    category.getSelectedItemPosition(),
                    from.getSelectedItemPosition(),
                    to.getSelectedItemPosition());
            result.setText(String.format(I18n.locale(), "%.6g", answer));
          } catch (Exception e) {
            result.setText("Valeur invalide");
          }
        });
    V6Ui.dialog(a, "Convertisseur hors ligne", root);
  }
}

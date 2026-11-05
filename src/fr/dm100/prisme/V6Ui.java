package fr.dm100.prisme;

import android.app.*;
import android.content.*;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

/** Shared native surfaces for the V6 modules, fitted to the actual display. */
public final class V6Ui {
  private V6Ui() {}

  private static final java.util.List<java.lang.ref.WeakReference<AlertDialog>> dialogs =
      new java.util.ArrayList<>();

  public static final class Builder extends AlertDialog.Builder {
    private final Context owner;
    private final AlertDialog.Builder delegate;

    public Builder(Context c) {
      super(c);
      owner = c;
      delegate = c instanceof MainActivity ? ((MainActivity) c).v6Builder() : null;
    }

    @Override
    public AlertDialog.Builder setTitle(CharSequence title) {
      if (delegate != null) delegate.setTitle(title);
      else super.setTitle(title);
      return this;
    }

    @Override
    public AlertDialog.Builder setMessage(CharSequence message) {
      if (delegate != null) delegate.setMessage(message);
      else super.setMessage(message);
      return this;
    }

    @Override
    public AlertDialog.Builder setView(View view) {
      if (delegate != null) delegate.setView(view);
      else super.setView(view);
      return this;
    }

    @Override
    public AlertDialog.Builder setItems(
        CharSequence[] values, DialogInterface.OnClickListener click) {
      if (delegate != null) delegate.setItems(values, click);
      else super.setItems(values, click);
      return this;
    }

    @Override
    public AlertDialog.Builder setPositiveButton(
        CharSequence title, DialogInterface.OnClickListener click) {
      if (delegate != null) delegate.setPositiveButton(title, click);
      else super.setPositiveButton(title, click);
      return this;
    }

    @Override
    public AlertDialog.Builder setNegativeButton(
        CharSequence title, DialogInterface.OnClickListener click) {
      if (delegate != null) delegate.setNegativeButton(title, click);
      else super.setNegativeButton(title, click);
      return this;
    }

    @Override public AlertDialog.Builder setMultiChoiceItems(CharSequence[] values,boolean[] checked,DialogInterface.OnMultiChoiceClickListener click){if(delegate!=null)delegate.setMultiChoiceItems(values,checked,click);else super.setMultiChoiceItems(values,checked,click);return this;}
    @Override
    public AlertDialog create() {
      AlertDialog d = delegate == null ? super.create() : delegate.create();
      if (owner instanceof Activity) d.setOwnerActivity((Activity) owner);
      dialogs.add(new java.lang.ref.WeakReference<>(d));
      if (delegate == null)
        d.setOnShowListener(
            x -> {
              if (owner instanceof Activity) {
                android.graphics.Point p = new android.graphics.Point();
                ((Activity) owner).getWindowManager().getDefaultDisplay().getSize(p);
                if (d.getWindow() != null) {
                  d.getWindow().setLayout(Math.max(240, p.x - 20), Math.max(240, p.y - 20));
                  I18n.bind(d.getWindow().getDecorView());
                }
              }
            });
      return d;
    }
  }

  public static void closeAll(Activity a) {
    for (java.lang.ref.WeakReference<AlertDialog> ref : new java.util.ArrayList<>(dialogs)) {
      AlertDialog d = ref.get();
      if (d != null && d.getOwnerActivity() == a) d.dismiss();
      else if (d != null && d.getContext() == a) d.dismiss();
    }
    dialogs.removeIf(ref -> ref.get() == null || !ref.get().isShowing());
  }

  public static final int BG = 0xff0d1421,
      INK = 0xffeff7ff,
      ACCENT = 0xff8aebd7,
      SURFACE = 0xff1c2c3e;

  public static LinearLayout column(Context c) {
    LinearLayout v = new LinearLayout(c);
    v.setOrientation(1);
    v.setPadding(10, 8, 10, 8);
    v.setBackgroundColor(BG);
    return v;
  }

  public static TextView text(Context c, String s, int size) {
    TextView t = new TextView(c);
    t.setText(s);
    t.setTextColor(INK);
    t.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    t.setPadding(8, 6, 8, 6);
    return t;
  }

  public static GradientDrawable surface(int color, int accent) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(14);
    d.setStroke(1, accent);
    return d;
  }

  public static Button button(Context c, String title, Runnable action) {
    Button b = new Button(c);
    b.setAllCaps(false);
    b.setText(title);
    b.setTextColor(INK);
    b.setTextSize(TypedValue.COMPLEX_UNIT_PX, 16);
    b.setMinHeight(44);
    b.setBackground(surface(SURFACE, 0xff385268));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  public static void add(LinearLayout root, String title, Runnable action) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 48);
    p.setMargins(0, 3, 0, 3);
    root.addView(button(root.getContext(), title, action), p);
  }

  public static AlertDialog dialog(Activity a, String title, View content) {
    ScrollView s = new ScrollView(a);
    s.addView(content);
    AlertDialog d =
        new Builder(a).setTitle(title).setView(s).setPositiveButton("Fermer", null).create();
    if (!(a instanceof MainActivity))
      d.setOnShowListener(
          x -> {
            Window w = d.getWindow();
            if (w != null) {
              android.graphics.Point p = new android.graphics.Point();
              a.getWindowManager().getDefaultDisplay().getSize(p);
              w.setLayout(Math.max(240, p.x - 20), Math.max(240, p.y - 20));
              I18n.bind(w.getDecorView());
            }
          });
    d.show();
    return d;
  }
}

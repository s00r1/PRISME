package fr.dm100.prisme;

import android.content.*;
import android.graphics.*;
import android.view.View;

/** Weather pictograms drawn locally; no icon downloads and no emoji font dependency. */
public final class WeatherIcon extends View {
  private final Paint p = new Paint(3);
  private int code;
  private boolean day = true;
  private final Path cloud = new Path();

  public WeatherIcon(Context c) {
    super(c);
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);
  }

  public void weather(int code, boolean day) {
    this.code = code;
    this.day = day;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float s = Math.min(getWidth(), getHeight()) / 100f;
    canvas.save();
    canvas.translate((getWidth() - 100 * s) / 2, (getHeight() - 100 * s) / 2);
    canvas.scale(s, s);
    p.setStyle(Paint.Style.FILL);
    if (code <= 2) {
      p.setColor(day ? 0xffffd173 : 0xffb6ceff);
      if (day) {
        canvas.drawCircle(40, 36, 20, p);
        p.setStrokeWidth(3);
        p.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 8; i++) {
          double a = i * Math.PI / 4;
          canvas.drawLine(
              40 + (float) Math.cos(a) * 25,
              36 + (float) Math.sin(a) * 25,
              40 + (float) Math.cos(a) * 30,
              36 + (float) Math.sin(a) * 30,
              p);
        }
      } else {
        cloud.reset();
        cloud.moveTo(44, 16);
        cloud.cubicTo(12, 8, 10, 65, 48, 56);
        cloud.cubicTo(24, 49, 24, 26, 44, 16);
        canvas.drawPath(cloud, p);
      }
    }
    if (code > 0) {
      p.setColor(code >= 95 ? 0xffa4b6d1 : 0xffdbe8f4);
      canvas.drawCircle(48, 52, 20, p);
      canvas.drawCircle(67, 47, 23, p);
      canvas.drawCircle(82, 58, 14, p);
      canvas.drawRoundRect(29, 52, 94, 73, 14, 14, p);
    }
    if (code >= 51 && code < 70 || code >= 80 && code < 83 || code >= 95) {
      p.setColor(0xff67baff);
      p.setStrokeWidth(5);
      p.setStrokeCap(Paint.Cap.ROUND);
      for (int i = 0; i < 3; i++) canvas.drawLine(42 + i * 18, 80, 38 + i * 18, 90, p);
    }
    if (code >= 71 && code <= 77 || code >= 85 && code <= 86) {
      p.setColor(0xffd2f5ff);
      for (int i = 0; i < 3; i++) canvas.drawCircle(42 + i * 18, 85, 3, p);
    }
    if (code >= 95) {
      p.setColor(0xffffd477);
      cloud.reset();
      cloud.moveTo(66, 66);
      cloud.lineTo(52, 83);
      cloud.lineTo(63, 83);
      cloud.lineTo(54, 98);
      cloud.lineTo(79, 76);
      cloud.lineTo(67, 76);
      cloud.close();
      canvas.drawPath(cloud, p);
    }
    if (code == 45 || code == 48) {
      p.setStrokeWidth(4);
      p.setColor(0xffb9d0df);
      canvas.drawLine(22, 82, 90, 82, p);
      canvas.drawLine(31, 91, 80, 91, p);
    }
    canvas.restore();
  }
}

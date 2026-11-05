package fr.dm100.prisme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/** Small analogue clock with a readable face and a spoken time in the chosen zone. */
public final class ClockFace extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private int accent = 0xff9fe9dc, ink = 0xffeef6ff;
  private TimeZone zone = TimeZone.getDefault();
  private Calendar time = Calendar.getInstance(zone);
  private boolean seconds;

  public ClockFace(Context context) {
    super(context);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
  }

  public void configure(int accent, int ink, String timezone, boolean seconds) {
    this.accent = accent;
    this.ink = ink;
    this.zone = timezone == null ? TimeZone.getDefault() : TimeZone.getTimeZone(timezone);
    this.seconds = seconds;
    time = Calendar.getInstance(zone);
    refreshTime();
  }

  public void refreshTime() {
    time.setTimeInMillis(System.currentTimeMillis());
    SimpleDateFormat format = new SimpleDateFormat(seconds ? "HH:mm:ss" : "HH:mm", I18n.locale());
    format.setTimeZone(zone);
    setContentDescription(format.format(new Date(time.getTimeInMillis())) + " · " + zone.getID());
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float radius =
        Math.min(
                getWidth() - getPaddingLeft() - getPaddingRight(),
                getHeight() - getPaddingTop() - getPaddingBottom())
            * .44f;
    if (radius <= 0) return;
    float x = (getWidth() + getPaddingLeft() - getPaddingRight()) / 2f,
        y = (getHeight() + getPaddingTop() - getPaddingBottom()) / 2f;
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(ink);
    paint.setStrokeWidth(1);
    paint.setStrokeCap(Paint.Cap.ROUND);
    canvas.drawCircle(x, y, radius, paint);
    for (int i = 0; i < 12; i++) {
      double angle = i * Math.PI / 6;
      paint.setStrokeWidth(i % 3 == 0 ? Math.max(2, radius * .055f) : 1);
      canvas.drawLine(
          x + (float) Math.sin(angle) * radius * .82f,
          y - (float) Math.cos(angle) * radius * .82f,
          x + (float) Math.sin(angle) * radius * .94f,
          y - (float) Math.cos(angle) * radius * .94f,
          paint);
    }
    hand(
        canvas,
        x,
        y,
        (time.get(Calendar.HOUR) + time.get(Calendar.MINUTE) / 60f) * 30,
        radius * .5f,
        Math.max(2, radius * .08f),
        ink);
    hand(
        canvas,
        x,
        y,
        (time.get(Calendar.MINUTE) + (seconds ? time.get(Calendar.SECOND) / 60f : 0)) * 6,
        radius * .74f,
        Math.max(2, radius * .055f),
        ink);
    if (seconds)
      hand(
          canvas,
          x,
          y,
          time.get(Calendar.SECOND) * 6,
          radius * .83f,
          Math.max(1, radius * .025f),
          accent);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(accent);
    canvas.drawCircle(x, y, Math.max(2, radius * .075f), paint);
  }

  private void hand(
      Canvas canvas, float x, float y, float degrees, float length, float width, int color) {
    double angle = Math.toRadians(degrees);
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(color);
    paint.setStrokeWidth(width);
    canvas.drawLine(
        x, y, x + (float) Math.sin(angle) * length, y - (float) Math.cos(angle) * length, paint);
  }
}

package fr.dm100.prisme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Real local media artwork, or an explicitly neutral music placeholder. */
public final class MediaArtworkView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private final Path clip = new Path();
  private final RectF frame = new RectF();
  private Bitmap bitmap;
  private int background = 0xff213448, foreground = 0xff8de7d2;

  public MediaArtworkView(Context context) {
    super(context);
    setContentDescription("Pochette indisponible");
  }

  public void colors(int background, int foreground) {
    this.background = background;
    this.foreground = foreground;
    invalidate();
  }

  public void artwork(Bitmap value) {
    if (value != null && value.isRecycled()) value = null;
    if (bitmap == value) return;
    bitmap = value;
    setContentDescription(
        value == null ? "Pochette non fournie par le lecteur" : "Pochette du titre en cours");
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float width = getWidth(), height = getHeight();
    if (width < 1 || height < 1) return;
    float radius = Math.min(width, height) * .15f;
    frame.set(0, 0, width, height);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(background);
    canvas.drawRoundRect(frame, radius, radius, paint);
    Bitmap image = bitmap;
    if (image != null && !image.isRecycled() && image.getWidth() > 0 && image.getHeight() > 0) {
      float scale = Math.max(width / image.getWidth(), height / image.getHeight());
      float left = (width - image.getWidth() * scale) / 2;
      float top = (height - image.getHeight() * scale) / 2;
      clip.reset();
      clip.addRoundRect(frame, radius, radius, Path.Direction.CW);
      int saved = canvas.save();
      canvas.clipPath(clip);
      canvas.drawBitmap(
          image,
          null,
          new RectF(left, top, left + image.getWidth() * scale, top + image.getHeight() * scale),
          paint);
      canvas.restoreToCount(saved);
    } else {
      float unit = Math.min(width, height), x = width * .5f, y = height * .5f;
      paint.setColor(foreground);
      paint.setAlpha(170);
      paint.setStrokeWidth(Math.max(2, unit * .045f));
      paint.setStrokeCap(Paint.Cap.ROUND);
      canvas.drawLine(x - unit * .1f, y + unit * .14f, x - unit * .1f, y - unit * .2f, paint);
      canvas.drawLine(x + unit * .16f, y + unit * .07f, x + unit * .16f, y - unit * .26f, paint);
      canvas.drawLine(x - unit * .1f, y - unit * .2f, x + unit * .16f, y - unit * .26f, paint);
      canvas.drawOval(
          new RectF(x - unit * .27f, y + unit * .09f, x - unit * .08f, y + unit * .22f), paint);
      canvas.drawOval(new RectF(x, y + unit * .02f, x + unit * .18f, y + unit * .15f), paint);
      paint.setAlpha(255);
    }
  }
}

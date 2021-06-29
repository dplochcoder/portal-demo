package graphics;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import com.google.auto.value.AutoValue;
import geom.OrientedRectangle;
import geom.Rectangle;

@AutoValue
public abstract class ImageRenderer implements Renderer {
  public abstract Image image();
  public abstract OrientedRectangle rect();

  @Override
  public void render(Graphics2D g2d, Rectangle occlusion) {
    AffineTransform tx = g2d.getTransform();
    Rectangle r = rect().rectangle();
    g2d.transform(AffineTransform.getRotateInstance(rect().angle().radians(),
        rect().rectangle().center().x(), r.center().y()));
    g2d.drawImage(image(), (int) r.x1(), (int) r.y1(), (int) r.x2(), (int) r.y2(), 0, 0,
        image().getWidth(null), image().getHeight(null), null);
    g2d.setTransform(tx);
  }

  public static ImageRenderer create(Image image, OrientedRectangle rect) {
    return new AutoValue_ImageRenderer(image, rect);
  }
}
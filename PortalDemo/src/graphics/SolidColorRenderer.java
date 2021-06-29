package graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import com.google.auto.value.AutoValue;
import geom.Rectangle;

@AutoValue
public abstract class SolidColorRenderer implements Renderer {
  public abstract Color color();

  @Override
  public void render(Graphics2D g2d, Rectangle occlusion) {
    g2d.setColor(color());
    g2d.fillRect((int) (occlusion.center().x() - occlusion.width() / 2),
        (int) (occlusion.center().y() - occlusion.height() / 2), (int) occlusion.width() + 1,
        (int) occlusion.height() + 1);
  }

  public static SolidColorRenderer create(Color color) {
    return new AutoValue_SolidColorRenderer(color);
  }
}

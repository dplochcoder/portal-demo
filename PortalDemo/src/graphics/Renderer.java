package graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import geom.Rectangle;

public interface Renderer {
  void render(Graphics2D g2d, Rectangle occlusion);
  
  static Renderer forColor(Color color) {
    return SolidColorRenderer.create(color);
  }
}
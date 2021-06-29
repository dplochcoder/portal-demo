package geom;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class Rectangle {
  public abstract Point center();
  public abstract double width();
  public abstract double height();
  
  public double x1() {
    return center().x() - width() / 2;
  }

  public double x2() {
    return center().x() + width() / 2;
  }
  
  public double y1() {
    return center().y() - height() / 2;
  }

  public double y2() {
    return center().y() + height() / 2;
  }
  
  @Memoized
  public Polygon asPolygon() {
    return Polygon.builder()
        .addPoint(center().translate(-width()/2, -height()/2))
        .addPoint(center().translate(-width()/2, height()/2))
        .addPoint(center().translate(width()/2, height()/2))
        .addPoint(center().translate(width()/2, -height()/2))
        .build();
  }
  
  public ImmutableList<Point> points() {
    return asPolygon().points();
  }
  
  public static Rectangle create(Point center, double width, double height) {
    Preconditions.checkArgument(width > 0, "width: %d", width);
    Preconditions.checkArgument(height > 0, "height: %d", height);
    return new AutoValue_Rectangle(center, width, height);
  }
}
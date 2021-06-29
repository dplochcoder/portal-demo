package geom;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

@AutoValue
public abstract class Circle {
  public abstract Point center();
  public abstract double radius();
  
  public Polygon asPolygon(int resolution) {
    Polygon.Builder polyBuilder = Polygon.builder();
    for (int i = 0; i < resolution; i++) {
      double theta = (2 * Math.PI * i) / resolution;
      polyBuilder.addPoint(center().translate(RadialVector.create(Angle.ofRadians(theta), radius())));
    }
    return polyBuilder.build();
  }
  
  public static Circle create(Point center, double radius) {
    Preconditions.checkArgument(radius > 0, "radius");
    return new AutoValue_Circle(center, radius);
  }
}

package geom;

import java.awt.geom.AffineTransform;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Point {
  private static final Point ORIGIN = Point.create(0, 0);
  
  public static Point origin() {
    return ORIGIN;
  }
  
  public abstract double x();
  public abstract double y();
  
  public static double distSquared(Point p1, Point p2) {
    double dx = p1.x() - p2.x();
    double dy = p1.y() - p2.y();
    return dx*dx + dy*dy;
  }
  
  public static double dist(Point p1, Point p2) {
    return Math.sqrt(distSquared(p1, p2));
  }
  
  public Point translate(double dx, double dy) {
    return Point.create(x() + dx, y() + dy);
  }
  
  public Point translate(RadialVector radialVector) {
    return translate(radialVector.dx(), radialVector.dy());
  }
  
  public Point translate(Vector vector) {
    return translate(vector.dx(), vector.dy());
  }
  
  public Point transform(AffineTransform tx) {
    double[] out = new double[2];
    tx.transform(new double[] {x(), y()}, 0, out, 0, 1);
    return create(out[0], out[1]);
  }
  
  public static Point create(double x, double y) {
    return new AutoValue_Point(x, y);
  }
}
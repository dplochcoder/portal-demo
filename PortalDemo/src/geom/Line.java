package geom;

import java.util.Optional;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

// a*x + b*y = c
@AutoValue
public abstract class Line {
  private static final double MIN_DELTA = 1e-6;
  private static final double MIN_DELTA_SQUARED = 1e-12;

  public abstract double a();

  public abstract double b();

  public abstract double c();

  public static Optional<Point> intersection(Line l1, Line l2) {
    if (Math.abs(l1.b()) < MIN_DELTA && Math.abs(l2.b()) < MIN_DELTA) {
      // Parallel-ish vertical lines.
      return Optional.empty();
    } else if (Math.abs(l1.b()) < MIN_DELTA) {
      Line tmp = l1;
      l1 = l2;
      l2 = tmp;
    }

    double det = l2.a() * l1.b() - l1.a() * l2.b();
    if (Math.abs(det) < MIN_DELTA) {
      // Parallel.
      return Optional.empty();
    }

    return Optional.of(Point.create((l1.b() * l2.c() - l2.b() * l1.c()) / det,
        (l2.a() * l1.c() - l1.a() * l2.c()) / det));
  }
  
  public static double distanceSquared(Line line, Point p) {
    if (Math.abs(line.a()) < MIN_DELTA) {
      return Math.abs(line.c() / line.b() - p.y());
    }
    if (Math.abs(line.b()) < MIN_DELTA) {
      return Math.abs(line.c() / line.a() - p.x());
    }

    Angle angle = Angle.from(Point.origin(), Point.create(-line.a(), line.b()));
    Line perp = RadialVector.create(Angle.add(angle, Angle.ofRadians(Math.PI / 2)), 1.0).lineFrom(p);
    return Point.distSquared(p, Line.intersection(line, perp).get());
  }
  
  public static Line create(double a, double b, double c) {
    return new AutoValue_Line(a, b, c);
  }
  
  public static boolean tooClose(Point p1, Point p2) {
    return Point.distSquared(p1, p2) < MIN_DELTA_SQUARED;
  }

  public static Line from(Point p1, Point p2) {
    Preconditions.checkArgument(!tooClose(p1, p2), "points too close");
    if (Math.abs(p1.x()) < MIN_DELTA && Math.abs(p1.y()) <= MIN_DELTA) {
      Point tmp = p1;
      p1 = p2;
      p2 = tmp;
    }
    
    double dx = p1.x() - p2.x();
    if (Math.abs(dx) < MIN_DELTA) {
      return create(1, 0, (p1.x() + p2.x()) / 2.0);
    }

    double dy = p1.y() - p2.y();
    if (Math.abs(dy) < MIN_DELTA) {
      return create(0, 1, (p1.y() + p2.y()) / 2.0);
    }

    double det = p2.x() * p1.y() - p1.x() * p2.y();
    if (Math.abs(det) < MIN_DELTA) {
      return create(p1.y(), -p1.x(), 0);
    }

    // General line not through (0, 0).
    return create(dy, -dx, det);
  }
}

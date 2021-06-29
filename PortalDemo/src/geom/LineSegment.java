package geom;

import java.util.Optional;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Preconditions;

@AutoValue
public abstract class LineSegment {
  public abstract Point p1();
  public abstract Point p2();
  
  @Memoized
  public double lengthSquared() {
    return Point.distSquared(p1(), p2());
  }
  
  @Memoized
  public double length() {
    return Math.sqrt(lengthSquared());
  }
  
  public Point midPoint() {
    return Point.create((p1().x() + p2().x()) / 2.0, (p1().y() + p2().y()) / 2.0);
  }
  
  // 'Flat' lines (vertical or horizontal) are likely not to match exactly, so we allow some wiggle room.
  private static final double CONTAINMENT_DELTA = 1e-7;
  
  // Assuming `p` is on `line()`, determine if it's on the segment.
  public final boolean containsLinePoint(Point p) {
    if (p1().x() < p2().x()) {
      if (p.x() + CONTAINMENT_DELTA < p1().x() || p.x() - CONTAINMENT_DELTA > p2().x()) {
        return false;
      }
    } else if (p.x() + CONTAINMENT_DELTA < p2().x() || p.x() - CONTAINMENT_DELTA > p1().x()) {
      return false;
    }
    
    if (p1().y() < p2().y()) {
      if (p.y() + CONTAINMENT_DELTA < p1().y() || p.y() - CONTAINMENT_DELTA > p2().y()) {
        return false;
      }
    } else if (p.y() + CONTAINMENT_DELTA < p2().y() || p.y() - CONTAINMENT_DELTA > p1().y()) {
      return false;
    }
    
    return true;
  }
  
  private LineSegment reversed = null;
  public final LineSegment reversed() {
    if (reversed == null) {
      reversed = create(p2(), p1());
      reversed.reversed = this;
    }
    return reversed;
  }
  
  public static Optional<Point> intersection(LineSegment s1, LineSegment s2) {
    return Line.intersection(s1.line(), s2.line())
        .filter(s1::containsLinePoint)
        .filter(s2::containsLinePoint);
  }
  
  @Memoized
  public Line line() {
    return Line.from(p1(), p2());
  }
  
  public static LineSegment create(Point p1, Point p2) {
    return new AutoValue_LineSegment(p1, p2);
  }
}
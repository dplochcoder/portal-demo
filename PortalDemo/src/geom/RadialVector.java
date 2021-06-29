package geom;

import java.util.Comparator;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Preconditions;

@AutoValue
public abstract class RadialVector {
  public static Comparator<RadialVector> raycastSort(Comparator<Angle> angleComparator) {
    return Comparator.<RadialVector, Angle>comparing(r -> r.angle(), angleComparator)
        .thenComparing(r -> -r.magnitudeSquared());
  }

  public abstract Angle angle();

  public abstract double magnitudeSquared();
  
  private RadialVector normalized = null;
  public final RadialVector normalized() {
    if (normalized == null) {
      normalized = createSquared(angle(), 1.0);
      normalized.normalized = normalized;
    }
    return normalized;
  }

  private Double magnitude = null;
  public double magnitude() { 
    if (magnitude == null) {
      magnitude = Math.sqrt(magnitudeSquared());
    }
    return magnitude;
  }
  
  @Memoized
  public double dx() { 
    return angle().cos() * magnitude();
  }

  @Memoized
  public double dy() { 
    return angle().sin() * magnitude();
  }
  
  public RadialVector withAngle(Angle angle) {
    RadialVector v = createSquared(angle, magnitudeSquared());
    if (magnitude != null) {
      v.magnitude = magnitude;
    }
    return v;
  }
  
  public RadialVector rotate(Angle angle) {
    return withAngle(Angle.add(angle(), angle));
  }
  
  public Line lineFrom(Point origin) {
    return Line.from(origin, origin.translate(normalized()));
  }
  
  public static RadialVector from(Point p1, Point p2) {
    return createSquared(Angle.from(p1, p2), Point.distSquared(p1, p2));
  }

  public static RadialVector create(Angle angle, double magnitude) {
    Preconditions.checkArgument(magnitude > 0, "magnitude: %d", magnitude);

    RadialVector rv = createSquared(angle, magnitude*magnitude);
    rv.magnitude = magnitude;
    return rv;
  }
  
  public static RadialVector createSquared(Angle angle, double magnitudeSquared) {
    Preconditions.checkArgument(magnitudeSquared >= 0, "negative magnitude: %d", magnitudeSquared);

    return new AutoValue_RadialVector(angle, magnitudeSquared);
  }
}

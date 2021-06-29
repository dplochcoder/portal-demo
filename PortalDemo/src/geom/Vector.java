package geom;

import java.util.Collection;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;

@AutoValue
public abstract class Vector {
  private static final Vector ZERO = create(0, 0);
  
  public static Vector zero() { return ZERO; }
  
  public abstract double dx();
  public abstract double dy();
  
  @Memoized
  public double magnitudeSquared() {
    return dx()*dx() + dy()*dy();
  }

  @Memoized
  public double magnitude() {
    return Math.sqrt(magnitudeSquared());
  }
  
  private Vector normalized = null;
  public Vector normalized() {
    if (normalized == null) {
      if (magnitudeSquared() == 0.0) {
        normalized = zero();
      } else {
        normalized = create(dx() / magnitude(), dy() / magnitude());
      }
    }
    return normalized;
  }
  
  @Memoized
  public RadialVector toRadial() {
    return RadialVector.from(Point.origin(), Point.create(dx(), dy()));
  }
  
  public Vector multiply(double multiplier) {
    return create(dx() * multiplier, dy() * multiplier);
  }
  
  public static Vector add(Vector v1, Vector v2) {
    return create(v1.dx() + v2.dx(), v1.dy() + v2.dy());
  }
  
  public static Vector sum(Collection<Vector> vectors) {
    return vectors.stream().reduce(zero(), Vector::add);
  }
  
  public static Vector create(double dx, double dy) {
    return new AutoValue_Vector(dx, dy);
  }
}

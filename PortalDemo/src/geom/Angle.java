package geom;

import java.util.Comparator;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.Comparators;

@AutoValue
public abstract class Angle {
  private static final Angle ORIGIN = Angle.ofRadians(0.0);
  
  public static Angle counterClockwiseDiff(Angle a1, Angle a2) {
    if (a1.radians() > a2.radians()) {
      return Angle.ofRadians(a1.radians() - a2.radians());
    } else {
      return Angle.ofRadians(2*Math.PI - (a2.radians() - a1.radians()));
    }
  }
  
  @AutoValue
  public abstract static class Range {
    public abstract Angle start();
    public abstract Angle end();
    
    public boolean contains(Angle a) {
      return counterClockwiseDiff(a, start()).radians() < counterClockwiseDiff(end(), start()).radians(); 
    }
    
    public Comparator<Angle> comparator() {
      return Comparator.comparing(a -> counterClockwiseDiff(a, start()).radians());
    }
    
    public static Range createAcuteCounterClockwise(Angle a1, Angle a2) {
      if (Angle.ofRadians(a2.radians() - a1.radians()).radians() > Math.PI) {
        Angle tmp = a1;
        a1 = a2;
        a2 = tmp;
      }

      return new AutoValue_Angle_Range(a1, a2);
    }
  }

  public static Angle origin() {
    return ORIGIN;
  }
  public abstract double radians();

  @Memoized
  public double cos() {
    return Math.cos(radians());
  }

  @Memoized
  public double sin() {
    return Math.sin(radians());
  }
  
  public static Angle add(Angle a1, Angle a2) {
    return ofRadians(a1.radians() + a2.radians());
  }
  
  public static Angle acuteDiff(Angle a1, Angle a2) {
    double diff = Math.abs(a2.radians() - a1.radians());
    if (diff > Math.PI) {
      diff = 2 * Math.PI - diff;
    }
    
    return ofRadians(diff);
  }
  
  public static Angle counterClockwiseMidpoint(Angle a1, Angle a2) {
    if (a1.radians() > a2.radians()) {
      return Angle.ofRadians((a1.radians() + a2.radians()) / 2 + Math.PI);
    } else {
      return Angle.ofRadians((a1.radians() + a2.radians()) / 2);
    }
  }

  public static Angle ofRadians(double radians) {
    while (radians >= 2 * Math.PI) {
      radians -= 2 * Math.PI;
    }
    while (radians < 0) {
      radians += 2 * Math.PI;
    }
    return new AutoValue_Angle(radians);
  }

  public static Angle from(Point p1, Point p2) {
    return ofRadians(Math.atan2(p2.y() - p1.y(), p2.x() - p1.x()));
  }
}

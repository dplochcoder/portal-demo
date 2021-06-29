package geom;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class OrientedRectangle {
  public abstract Rectangle rectangle();
  public abstract Angle angle();

  public static OrientedRectangle create(Rectangle rectangle, Angle angle) {
    return new AutoValue_OrientedRectangle(rectangle, angle);
  }
}

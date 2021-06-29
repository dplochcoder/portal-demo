package geom;

import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public final class Polygon {

  private final ImmutableList<Point> points;
  private final ImmutableList<LineSegment> lineSegments;

  private Polygon(ImmutableList<Point> points) {
    Preconditions.checkArgument(points.size() >= 3, "Polygons require at least 3 points");

    this.points = points;

    ImmutableList.Builder<LineSegment> lineSegmentsBuilder = ImmutableList.builder();
    Point prev = points.get(points.size() - 1);
    for (Point next : points) {
      lineSegmentsBuilder.add(LineSegment.create(prev, next));
      prev = next;
    }
    lineSegments = lineSegmentsBuilder.build();
  }

  public int numPoints() {
    return points.size();
  }

  public Point point(int index) {
    return points.get(index);
  }

  public ImmutableList<Point> points() {
    return points;
  }

  public LineSegment lineSegment(int index) {
    return lineSegments.get(index);
  }
  
  public Polygon translate(double dx, double dy) {
    return transform(AffineTransform.getTranslateInstance(dx, dy));
  }
  
  public Polygon transform(AffineTransform tx) {
    Polygon.Builder polyBuilder = builder();
    points().forEach(p -> polyBuilder.addPoint(p.transform(tx)));
    return polyBuilder.build();
  }

  public ImmutableList<LineSegment> lineSegments() {
    return lineSegments;
  }
  
  private Shape shape = null;
  public Shape toShape() {
    if (shape == null) {
      Path2D.Double p2d = new Path2D.Double();
      p2d.moveTo(point(0).x(), point(0).y());
      for (int i = 1; i < numPoints(); i++) {
        p2d.lineTo(point(i).x(), point(i).y());
      }
      p2d.closePath();
      shape = p2d;
    }
    return shape;
  }
  
  private Rectangle boundingRect = null;
  public Rectangle boundingRect() {
    if (boundingRect == null) {
      double minX = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      double minY = Double.POSITIVE_INFINITY;
      double maxY = Double.NEGATIVE_INFINITY;
      for (Point p : points()) {
        if (p.x() < minX) {
          minX = p.x();
        }
        if (p.x() > maxX) {
          maxX = p.x();
        }
        if (p.y() < minY) {
          minY = p.y();
        }
        if (p.y() > maxY) {
          maxY = p.y();
        }
      }
      boundingRect = Rectangle.create(Point.create((minX+maxX)/2, (minY+maxY)/2), (maxX-minX), (maxY-minY));
    }
    return boundingRect;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final List<Point> points = new ArrayList<>();

    public Builder addPoint(Point p) {
      points.add(p);
      return this;
    }
    
    public Builder addPoint(double x, double y) {
      return addPoint(Point.create(x, y));
    }
    
    public Builder addLine(double dx, double dy) {
      return addPoint(lastPoint().translate(dx, dy));
    }
    
    public Point lastPoint() {
      Preconditions.checkState(points.size() > 0);
      return points.get(points.size() - 1);
    }

    public Polygon build() {
      return new Polygon(ImmutableList.copyOf(points));
    }
  }

}

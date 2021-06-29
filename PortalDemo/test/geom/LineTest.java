package geom;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;

public class LineTest {
  private void assertPoint(Point p, double x, double y) {
    assertThat(p.x()).isWithin(0.0001).of(x);
    assertThat(p.y()).isWithin(0.0001).of(y);
  }
  
  @Test
  public void testIntersections() {
    Line xAxis = Line.from(Point.origin(), Point.create(10, 0));
    Line yAxis = Line.from(Point.origin(), Point.create(0, 10));
    
    assertPoint(Line.intersection(xAxis, yAxis).get(), 0, 0);
  }
  
  @Test
  public void testDiagonal() {
    Line up = Line.from(Point.origin(), Point.create(1, 1));
    Line down = Line.from(Point.origin(), Point.create(1, -1));
    
    assertPoint(Line.intersection(up, down).get(), 0, 0);
  }
  
  @Test
  public void testEquilateral() {
    Line left = Line.from(Point.create(-1, 0), Point.create(-0.5, 1));
    Line right = Line.from(Point.create(1, 0), Point.create(0.5, 1));
    
    assertPoint(Line.intersection(left, right).get(), 0, 2);
  }
  
  @Test
  public void testAngleHorz() {
    Line five = Line.from(Point.create(0, 5), Point.create(10, 5));
    Line up = Line.from(Point.create(1, 0), Point.create(2, 1));
    
    assertPoint(Line.intersection(five, up).get(), 6, 5);
  }
}
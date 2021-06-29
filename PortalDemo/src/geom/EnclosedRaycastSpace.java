package geom;

import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import graphics.Renderer;

public final class EnclosedRaycastSpace {

  public static final class PolygonRef {
    private final EnclosedRaycastSpace parent;
    private final Polygon polygon;
    private final Map<Integer, PortalLink> portalLinks = new HashMap<>();

    private final ImmutableList<PointRef> pointRefs;
    private final ImmutableList<LineSegmentRef> lineSegmentRefs;

    private PolygonRef(EnclosedRaycastSpace parent, Polygon polygon) {
      this.parent = parent;
      this.polygon = polygon;
      this.lineSegmentRefs = IntStream.range(0, polygon.numPoints())
          .mapToObj(i -> new LineSegmentRef(this, i)).collect(ImmutableList.toImmutableList());
      this.pointRefs = IntStream.range(0, polygon.numPoints()).mapToObj(i -> new PointRef(this, i))
          .collect(ImmutableList.toImmutableList());
    }

    public Polygon polygon() {
      return polygon;
    }

    public PointRef pointRef(int i) {
      return pointRefs.get(i);
    }

    public ImmutableList<PointRef> pointRefs() {
      return pointRefs;
    }

    public LineSegmentRef lineSegmentRef(int i) {
      return lineSegmentRefs.get(i);
    }

    public ImmutableList<LineSegmentRef> lineSegmentRefs() {
      return lineSegmentRefs;
    }
  }

  public static final class PointRef {
    private final PolygonRef parent;
    private final int index;
    private final ImmutableSet<LineSegmentRef> lineSegments;

    private PointRef(PolygonRef parent, int index) {
      this.parent = parent;
      this.index = index;
      this.lineSegments = ImmutableSet.of(parent.lineSegmentRef(index),
          parent.lineSegmentRef((index + 1) % parent.polygon.numPoints()));
    }

    public Point point() {
      return parent.polygon().point(index);
    }

    public ImmutableSet<LineSegmentRef> lineSegments() {
      return lineSegments;
    }
  }

  public static final class LineSegmentRef {
    private final PolygonRef parent;
    private final int index;

    private LineSegmentRef(PolygonRef parent, int index) {
      this.parent = parent;
      this.index = index;
    }

    public LineSegment lineSegment() {
      return parent.polygon().lineSegment(index);
    }

    public PointRef p1() {
      return parent.pointRef(index == 0 ? (parent.polygon().numPoints() - 1) : (index - 1));
    }

    public PointRef p2() {
      return parent.pointRef(index);
    }

    public boolean hasPortalLink() {
      return parent.portalLinks.containsKey(index);
    }
    
    public PortalLink portalLink() {
      return parent.portalLinks.get(index);
    }

    public boolean containsPointRef(PointRef pointRef) {
      return p1().equals(pointRef) || p2().equals(pointRef);
    }
  }

  public static final class PortalLink {
    private static final double MINIMUM_SIZE_SQUARED = 30*30;
    private static final double MAXIMUM_DELTA = 0.01;

    private final LineSegmentRef source;
    private final LineSegmentRef destination;
    private final Angle rotation;
    private final AffineTransform tx;
    private final AffineTransform inv;

    private PortalLink(LineSegmentRef source, LineSegmentRef destination, boolean flip) {
      this.source = source;
      this.destination = destination;

      LineSegment src = source.lineSegment();
      LineSegment dst = destination.lineSegment();
      Angle a1 = Angle.from(src.p1(), src.p2());
      Angle a2 = flip ? Angle.from(dst.p2(), dst.p1()) : Angle.from(dst.p1(), dst.p2());
      this.rotation = Angle.ofRadians(a2.radians() - a1.radians());

      Point mid1 = src.midPoint();
      Point mid2 = dst.midPoint();
      this.tx = AffineTransform.getTranslateInstance(mid2.x() - mid1.x(), mid2.y() - mid1.y());
      this.tx.rotate(rotation.radians(), mid1.x(), mid1.y());
      try {
        this.inv = tx.createInverse();
      } catch (NoninvertibleTransformException ex) {
        throw new AssertionError(ex);
      }
    }
    
    public LineSegment srcSegment() {
      return source.lineSegment();
    }

    public LineSegmentRef destSegmentRef() {
      return destination;
    }
    
    public EnclosedRaycastSpace destSpace() {
      return destination.parent.parent;
    }

    public Angle rotation() {
      return rotation;
    }
    
    public AffineTransform transform() {
      return new AffineTransform(tx);
    }
    
    public AffineTransform invTransform() {
      return new AffineTransform(inv);
    }
  }

  private final Polygon exteriorPolygon;
  private final PolygonRef exteriorPolygonRef;
  private final Renderer renderer;
  private final List<Polygon> interiorRaycastPolygons = new ArrayList<>();
  private final List<PolygonRef> interiorRaycastPolygonRefs = new ArrayList<>();

  public EnclosedRaycastSpace(Polygon exteriorPolygon, Renderer renderer) {
    this.exteriorPolygon = checkNotNull(exteriorPolygon);
    this.exteriorPolygonRef = new PolygonRef(this, exteriorPolygon);
    this.renderer = checkNotNull(renderer);
  }

  public static void createPortal(LineSegmentRef a, LineSegmentRef b) {
    createPortal(a, b, false);
  }

  public static void createFlippedPortal(LineSegmentRef a, LineSegmentRef b) {
    createPortal(a, b, true);
  }

  private static void createPortal(LineSegmentRef a, LineSegmentRef b, boolean flip) {
    Preconditions.checkArgument(a.lineSegment().lengthSquared() >= PortalLink.MINIMUM_SIZE_SQUARED,
        "portal too small");
    Preconditions.checkArgument(b.lineSegment().lengthSquared() >= PortalLink.MINIMUM_SIZE_SQUARED,
        "portal too small");
    Preconditions.checkArgument(!a.equals(b), "self-referential portal");
    Preconditions.checkArgument(
        Math.abs(a.lineSegment().length() - b.lineSegment().length()) < PortalLink.MAXIMUM_DELTA,
        "Portals have different sizes");

    Preconditions.checkArgument(!a.hasPortalLink());
    Preconditions.checkArgument(!b.hasPortalLink());

    a.parent.portalLinks.put(a.index, new PortalLink(a, b, flip));
    b.parent.portalLinks.put(b.index, new PortalLink(b, a, flip));
  }

  public Polygon exteriorPolygon() {
    return exteriorPolygon;
  }

  public PolygonRef exteriorPolygonRef() {
    return exteriorPolygonRef;
  }
  
  public PolygonRef interiorRaycastPolygonRef(int i) {
    return interiorRaycastPolygonRefs.get(i);
  }

  public Stream<PolygonRef> polygonRefs() {
    return Streams.concat(Stream.of(exteriorPolygonRef), interiorRaycastPolygonRefs.stream());
  }

  public Renderer renderer() {
    return renderer;
  }

  public Optional<PortalLink> intersectingPortalLinks(LineSegment movementVector) {
    if (movementVector.lengthSquared() == 0.0) {
      return Optional.empty();
    }

    return polygonRefs().flatMap(p -> p.portalLinks.values().stream())
        .filter(pl -> LineSegment.intersection(movementVector, pl.srcSegment()).isPresent())
        .findFirst();
  }

  public void debugRenderPortals(Graphics2D g2d) {
    g2d.setColor(Color.orange);
    g2d.setStroke(new BasicStroke(5.0f));
    polygonRefs().flatMap(s -> s.portalLinks.values().stream()).forEach(pl -> {
      LineSegment line = pl.srcSegment();
      g2d.drawLine((int) line.p1().x(), (int) line.p1().y(), (int) line.p2().x(),
          (int) line.p2().y());
    });
  }

  public PolygonRef addInteriorRaycastPolygon(Polygon polygon) {
    interiorRaycastPolygons.add(polygon);
    
    PolygonRef ref = new PolygonRef(this, polygon);
    interiorRaycastPolygonRefs.add(ref);
    return ref;
  }
}

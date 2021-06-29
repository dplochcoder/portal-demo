package geom;

import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.geom.AffineTransform;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Sets;

public final class RayTrace {

  @AutoValue
  abstract static class RadialPointRef {
    abstract RadialVector vector();

    abstract EnclosedRaycastSpace.PointRef ref();

    private final Point point() {
      return ref().point();
    }

    private static RadialPointRef create(Point origin, EnclosedRaycastSpace.PointRef dest) {
      return new AutoValue_RayTrace_RadialPointRef(RadialVector.from(origin, dest.point()), dest);
    }
  }

  @AutoValue
  public abstract static class RecursiveRayTrace {
    public abstract EnclosedRaycastSpace.PortalLink portalLink();

    public abstract RayTrace rayTrace();

    private static RecursiveRayTrace create(EnclosedRaycastSpace.PortalLink portalLink,
        RayTrace rayTrace) {
      return new AutoValue_RayTrace_RecursiveRayTrace(portalLink, rayTrace);
    }
  }

  private final EnclosedRaycastSpace root;
  private final Polygon scope;
  private final ImmutableList<RecursiveRayTrace> recursiveRayTraces;

  private RayTrace(EnclosedRaycastSpace root, Polygon scope,
      ImmutableList<RecursiveRayTrace> recursiveRayTraces) {
    this.root = checkNotNull(root);
    this.scope = checkNotNull(scope);
    this.recursiveRayTraces = checkNotNull(recursiveRayTraces);
  }

  public EnclosedRaycastSpace space() {
    return root;
  }

  public Polygon scope() {
    return scope;
  }

  public ImmutableList<RecursiveRayTrace> recursiveRayTraces() {
    return recursiveRayTraces;
  }

  public static RayTrace buildRadialTrace(EnclosedRaycastSpace space, Point origin,
      double maxDistance) {
    return buildRadialTrace(space, origin, maxDistance, null, null);
  }

  private static class RecursivePolyBuilder {
    private final Point origin;
    private final double maxDistance;
    private final double maxDistanceSquared;
    private final Polygon.Builder polyBuilder = Polygon.builder();
    private final ImmutableList.Builder<RecursiveRayTrace> recursiveRayTracesBuilder =
        ImmutableList.builder();

    private Point firstPoint = null;
    private ImmutableSet<EnclosedRaycastSpace.PortalLink> firstPortalLinks = ImmutableSet.of();
    private Point prevPoint = null;
    private ImmutableSet<EnclosedRaycastSpace.PortalLink> prevPortalLinks = ImmutableSet.of();

    public RecursivePolyBuilder(Point origin, double maxDistance) {
      this.origin = origin;
      this.maxDistance = maxDistance;
      this.maxDistanceSquared = maxDistance * maxDistance;
    }

    public void addPoint(Point p) {
      addPointInternal(p, ImmutableSet.of());
    }

    public void addPoint(RadialPointRef pointRef) {
      addPointInternal(pointRef.point(),
          pointRef.ref().lineSegments().stream().filter(ls -> ls.hasPortalLink())
              .map(ls -> ls.portalLink()).collect(ImmutableSet.toImmutableSet()));
    }

    public void addPoint(Point p, EnclosedRaycastSpace.LineSegmentRef lineSegment) {
      addPointInternal(p, lineSegment.hasPortalLink() ? ImmutableSet.of(lineSegment.portalLink())
          : ImmutableSet.of());
    }

    private void addPointInternal(Point p,
        ImmutableSet<EnclosedRaycastSpace.PortalLink> portalLinks) {
      polyBuilder.addPoint(p);
      if (firstPoint == null) {
        firstPoint = p;
        firstPortalLinks = portalLinks;
      }

      if (prevPoint != null) {
        maybeRecursiveRayTrace(prevPoint, prevPortalLinks, p, portalLinks);
      }

      prevPoint = p;
      prevPortalLinks = portalLinks;
    }

    private void maybeRecursiveRayTrace(Point a, Set<EnclosedRaycastSpace.PortalLink> portalLinksA,
        Point b, Set<EnclosedRaycastSpace.PortalLink> portalLinksB) {
      if (Line.tooClose(a, b)) {
        return;
      }

      Optional<EnclosedRaycastSpace.PortalLink> portalLinkOpt =
          Sets.intersection(portalLinksA, portalLinksB).stream().findFirst();
      if (!portalLinkOpt.isPresent()) {
        return;
      }

      Line l = Line.from(a, b);
      if (Point.distSquared(origin, a) > maxDistanceSquared
          && Point.distSquared(origin, b) > maxDistanceSquared
          && (Line.distanceSquared(l, origin) > maxDistanceSquared
              || Point.distSquared(a, b) < maxDistanceSquared)) {
        // Ignore.
        return;
      }

      EnclosedRaycastSpace.PortalLink portalLink = portalLinkOpt.get();
      AffineTransform tx = portalLink.transform();
      try {
        RayTrace rayTrace =
            buildRadialTrace(portalLink.destSpace(), origin.transform(tx), maxDistance,
                LineSegment.create(a.transform(tx), b.transform(tx)), portalLink.destSegmentRef());
        recursiveRayTracesBuilder.add(RecursiveRayTrace.create(portalLink, rayTrace));
      } catch (Exception ignore) {}
    }
    
    public Point lastPoint() {
      return polyBuilder.lastPoint();
    }

    public Polygon buildPolygon() {
      return polyBuilder.build();
    }

    public ImmutableList<RecursiveRayTrace> buildRecursiveRayTraces() {
      maybeRecursiveRayTrace(prevPoint, prevPortalLinks, firstPoint, firstPortalLinks);
      return recursiveRayTracesBuilder.build();
    }
  }

  public static RayTrace buildRadialTrace(EnclosedRaycastSpace space, Point origin,
      double maxDistance, LineSegment occlusionWindow, EnclosedRaycastSpace.LineSegmentRef portal) {
    Comparator<Angle> angleComparator = Comparator.comparing(Angle::radians);
    Predicate<RadialPointRef> filter = r -> true;
    Angle.Range range = null;
    if (occlusionWindow != null) {
      Angle a1 = Angle.from(origin, occlusionWindow.p1());
      Angle a2 = Angle.from(origin, occlusionWindow.p2());
      range = Angle.Range.createAcuteCounterClockwise(a1, a2);
      angleComparator = range.comparator();

      Angle.Range finalRange = range;
      filter = r -> {
        Angle angle = r.vector().angle();
        if (!finalRange.contains(angle)) {
          return false;
        }

        Point intersection = Line
            .intersection(RadialVector.create(r.vector().angle(), 1.0).lineFrom(origin),
                occlusionWindow.line())
            .orElseThrow(() -> new RuntimeException("bad occlusion window"));
        return Point.distSquared(origin, r.point()) > Point.distSquared(origin, intersection);
      };
    }

    ImmutableList<RadialPointRef> radialPointRefs = space.polygonRefs()
        .flatMap(p -> p.pointRefs().stream()).map(p -> RadialPointRef.create(origin, p))
        .filter(filter)
        .sorted(
            Comparator.comparing(RadialPointRef::vector, RadialVector.raycastSort(angleComparator)))
        .collect(ImmutableList.toImmutableList());

    // Compute all line segments that intersect the origin raycast.
    Set<EnclosedRaycastSpace.LineSegmentRef> activeSegments =
        space.polygonRefs().flatMap(p -> p.lineSegmentRefs().stream()).filter(p -> p != portal)
            .collect(Collectors.toCollection(HashSet::new));

    // Now, start at angle 0, and progress radially through the points.
    // At each point, we ray cast to find which segment should be continued.
    RadialPointRef prevRadialPointRef =
        radialPointRefs.isEmpty() ? null : radialPointRefs.get(radialPointRefs.size() - 1);
    Angle midStart;
    if (occlusionWindow != null) {
      midStart = Angle.counterClockwiseMidpoint(range.start(),
          radialPointRefs.isEmpty() ? range.end() : radialPointRefs.get(0).vector().angle());
    } else {
      midStart = Angle.counterClockwiseMidpoint(prevRadialPointRef.vector().angle(),
          radialPointRefs.get(0).vector().angle());
    }
    Line midStartCast = RadialVector.createSquared(midStart, 1.0).lineFrom(origin);
    EnclosedRaycastSpace.LineSegmentRef active =
        rayCast(origin, midStart, midStartCast, activeSegments, occlusionWindow);

    RecursivePolyBuilder polyBuilder = new RecursivePolyBuilder(origin, maxDistance);
    if (occlusionWindow != null) {
      polyBuilder.addPoint(occlusionWindow.p1());
      Line startCast = RadialVector.createSquared(range.start(), 1.0).lineFrom(origin);
      polyBuilder.addPoint(Line.intersection(active.lineSegment().line(), startCast)
          .orElseThrow(() -> new RuntimeException("bad midstart")), active);
    }
    for (int i = 0; i < radialPointRefs.size() + 1; i++) {
      if (i == radialPointRefs.size() && occlusionWindow != null) {
        break; 
      }
      boolean occlusionInitial = i == 0 && occlusionWindow != null;
      boolean occlusionTerminal = i == radialPointRefs.size() - 1 && occlusionWindow != null;

      RadialPointRef radialPointRef = radialPointRefs.get(i % radialPointRefs.size());
      RadialPointRef nextRadialPointRef = radialPointRefs.get((i + 1) % radialPointRefs.size());
      // As we encounter each point, several things can happen:
      // 1) A new line segment closer to the observer is encountered.
      // - in this case, we must add new points - one for the current intersection with the
      // the old segment, and one for an endpoint of the new segment.
      // 2) The point is behind the current line segment.
      // - Nothing is done here, the point is ignored.
      // 3) An endpoint of the current line segment is encountered.
      // - It can be assumed that no other segment is closer, since that would mean
      // intersecting segments, and we assume none of those.
      // - Add a point for the current segment, do nothing else.
      // 4) The current segment no longer covers the angle inspected.
      // - In this case, do a new ray cast at the midpoint between the angle of the previous
      // point and this one. This identifies the segment that comes after the previous one
      // and possibly before the new one, which means an addition of up to 3 points to the
      // polygon.
      ImmutableSet<EnclosedRaycastSpace.LineSegmentRef> newSegments =
          radialPointRef.ref().lineSegments();
      if (portal != null && newSegments.contains(portal)) {
        newSegments =
            newSegments.stream().filter(p -> p != portal).collect(ImmutableSet.toImmutableSet());
      }
      activeSegments.addAll(newSegments);
      if (newSegments.contains(active)) {
        // Case 3.
        polyBuilder.addPoint(radialPointRef);
      } else {
        Line rayLine = radialPointRef.vector().lineFrom(origin);
        Optional<Point> currentIntersection =
            singleRayCast(origin, radialPointRef.vector().angle(), rayLine, active.lineSegment());

        if (!currentIntersection.isPresent()) {
          // Case 4.
          Angle prev = occlusionInitial ? range.start() : prevRadialPointRef.vector().angle();
          Angle mid = Angle.counterClockwiseMidpoint(prev, radialPointRef.vector().angle());
          Line midCast = RadialVector.createSquared(mid, 1.0).lineFrom(origin);
          Line prevCast = RadialVector.createSquared(prev, 1.0).lineFrom(origin);
          active = rayCast(origin, mid, midCast, activeSegments, occlusionWindow);
          activeSegments.addAll(newSegments);

          if (!active.containsPointRef(prevRadialPointRef.ref())) {
            EnclosedRaycastSpace.LineSegmentRef localActive = active;
            Point dropOff = Line.intersection(active.lineSegment().line(), prevCast)
                .orElseThrow(() -> new RuntimeException(localActive.lineSegment() + "; " + mid));
            polyBuilder.addPoint(dropOff, active);
          }

          // Now, check if a pull-up point to the current point is needed.
          if (!active.containsPointRef(radialPointRef.ref())) {
            // The midSegment is different, so add a pull-up point.
            Point pullUp = Line.intersection(active.lineSegment().line(), rayLine).get();
            if (Point.distSquared(origin, pullUp) > Point.distSquared(origin,
                radialPointRef.point())) {
              polyBuilder.addPoint(pullUp, active);

              // Disambiguate the active segment
              Set<EnclosedRaycastSpace.LineSegmentRef> disambig = new HashSet<>(newSegments);
              Angle nextMid = Angle.counterClockwiseMidpoint(radialPointRef.vector().angle(),
                  occlusionTerminal ? range.end() : nextRadialPointRef.vector().angle());
              Line nextMidCast = RadialVector.createSquared(nextMid, 1.0).lineFrom(origin);
              active = rayCast(origin, nextMid, nextMidCast, disambig, occlusionWindow);

              polyBuilder.addPoint(radialPointRef);
            }
          } else {
            polyBuilder.addPoint(radialPointRef);
          }
        } else if (Point.distSquared(origin, currentIntersection.get()) < Point.distSquared(origin,
            radialPointRef.point())) {
          // Case 2.
          // Do nothing.
        } else {
          // Case 1.
          polyBuilder.addPoint(currentIntersection.get(), active);
          polyBuilder.addPoint(radialPointRef);

          // Disambiguate the active segment
          Set<EnclosedRaycastSpace.LineSegmentRef> disambig = new HashSet<>(newSegments);
          Angle nextMid = Angle.counterClockwiseMidpoint(radialPointRef.vector().angle(),
              occlusionTerminal ? range.end() : nextRadialPointRef.vector().angle());
          Line nextMidCast = RadialVector.createSquared(nextMid, 1.0).lineFrom(origin);
          active = rayCast(origin, nextMid, nextMidCast, disambig, occlusionWindow);
        }
      }

      prevRadialPointRef = radialPointRef;
    }

    // Close out the final midpoint.
    if (occlusionWindow != null) {
      Angle midEnd = Angle.counterClockwiseMidpoint(radialPointRefs.isEmpty() ? range.start()
          : radialPointRefs.get(radialPointRefs.size() - 1).vector().angle(), range.end());
      Line midEndCast = RadialVector.create(midEnd, 1.0).lineFrom(origin);
      EnclosedRaycastSpace.LineSegmentRef prevActive = active;
      active = rayCast(origin, range.end(), midEndCast, activeSegments, occlusionWindow);
      if (active != prevActive) {
        Line pullUpCast = Line.from(origin, polyBuilder.lastPoint());
        polyBuilder.addPoint(Line.intersection(pullUpCast, active.lineSegment().line())
            .orElseThrow(() -> new RuntimeException("foo")), active);
      }
      Line endCast = RadialVector.create(range.end(), 1.0).lineFrom(origin);
      polyBuilder.addPoint(Line.intersection(active.lineSegment().line(), endCast)
          .orElseThrow(() -> new RuntimeException("bad midstart")), active);
      polyBuilder.addPoint(occlusionWindow.p2());
    }

    return new RayTrace(space, polyBuilder.buildPolygon(), polyBuilder.buildRecursiveRayTraces());
  }

  private static Optional<Point> singleRayCast(Point origin, Angle angle, Line line,
      LineSegment segment) {
    Optional<Point> intersectionOpt = Line.intersection(line, segment.line());
    if (!intersectionOpt.isPresent()) {
      return Optional.empty();
    }

    Point intersection = intersectionOpt.get();
    if (!segment.containsLinePoint(intersection)
        || Angle.acuteDiff(RadialVector.from(origin, intersection).angle(), angle)
            .radians() > Math.PI / 2) {
      return Optional.empty();
    }

    return intersectionOpt;
  }

  private static EnclosedRaycastSpace.LineSegmentRef rayCast(Point origin, Angle ray, Line line,
      Set<EnclosedRaycastSpace.LineSegmentRef> segments, LineSegment occlusionWindow) {
    EnclosedRaycastSpace.LineSegmentRef best = null;
    double bestDist = Double.POSITIVE_INFINITY;
    double minDist = Double.NEGATIVE_INFINITY;
    if (occlusionWindow != null) {
      minDist =
          Point.distSquared(origin, Line.intersection(occlusionWindow.line(), line).get()) - 1e-6;
    }

    Set<EnclosedRaycastSpace.LineSegmentRef> toRemove = new HashSet<>();
    for (EnclosedRaycastSpace.LineSegmentRef segment : segments) {
      Optional<Point> intersection = singleRayCast(origin, ray, line, segment.lineSegment());
      if (!intersection.isPresent()) {
        toRemove.add(segment);
        continue;
      }

      double dist = Point.distSquared(intersection.get(), origin);
      if (dist > minDist && dist < bestDist) {
        bestDist = dist;
        best = segment;
      }
    }

    segments.removeAll(toRemove);
    return checkNotNull(best);
  }

  // TODO: Build portal trace.

}

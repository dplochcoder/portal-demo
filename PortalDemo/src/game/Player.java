package game;

import java.util.Optional;
import geom.Angle;
import geom.EnclosedRaycastSpace;
import geom.Line;
import geom.LineSegment;
import geom.Point;
import geom.RadialVector;

public class Player {
  private Angle camera = Angle.origin();
  private EnclosedRaycastSpace space;
  private Point position;
  
  public Player(EnclosedRaycastSpace space, Point position) {
    this.space = space;
    this.position = position;
  }
  
  public Angle camera() {
    return camera;
  }
  
  public void move(RadialVector movement) {
    Point newPosition = position.translate(movement);
    LineSegment travel = LineSegment.create(position, newPosition);
    
    Optional<EnclosedRaycastSpace.PortalLink> portalLinkOpt = space.intersectingPortalLinks(travel);
    if (portalLinkOpt.isPresent()) {
      EnclosedRaycastSpace.PortalLink portalLink = portalLinkOpt.get();
      space = portalLink.destSpace();
      position = newPosition.transform(portalLink.transform());
      if (Line.distanceSquared(portalLink.destSegmentRef().lineSegment().line(), position) < 1e-12) {
        // Nudge.
        position = position.translate(
            RadialVector.create(
                Angle.add(movement.angle(), portalLink.rotation()), 0.001));
      }
      camera = Angle.add(camera, portalLink.rotation());
    } else {
      position = newPosition;
    }
  }
  
  public void rotateCamera(Angle change) {
    camera = Angle.add(camera, change);
  }
  
  public EnclosedRaycastSpace space() {
    return space;
  }
  
  public Point position() {
    return position;
  }
}

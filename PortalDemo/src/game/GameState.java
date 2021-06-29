package game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import geom.Angle;
import geom.RadialVector;
import geom.RayTrace;
import geom.Rectangle;
import geom.Vector;

public class GameState {
  private static final double MOVEMENT_SPEED = 2.0;
  private static final double ROTATION_SPEED = Math.PI / 60;
  private static final int PLAYER_RADIUS = 20;
  private static final double MAX_VISIBILITY = 800;

  private final Player player;

  public GameState(Player player) {
    this.player = player;
  }

  public void tick(InputState inputState) {
    player.move(movementVector(inputState));
    player.rotateCamera(cameraChange(inputState));
  }

  public void render(Graphics2D g2d, int width, int height) {
    g2d.translate(width/2 - player.position().x(), height/2 - player.position().y());
    g2d.rotate(-player.camera().radians(), player.position().x(), player.position().y());

    RayTrace trace = null;
    try {
      trace = RayTrace.buildRadialTrace(player.space(), player.position(), MAX_VISIBILITY);
    } catch (Exception ex) { ex.printStackTrace(); }

    if (trace != null) {
      renderRecursively(g2d, trace, this::renderBackground);
      renderRecursively(g2d, trace, this::renderPlayer);
    } else {
      Shape prev = g2d.getClip();
      g2d.clip(player.space().exteriorPolygon().toShape());
      player.space().renderer().render(g2d, Rectangle.create(player.position(), width, height));
      g2d.setClip(prev);
    }
  }
  
  private void renderBackground(Graphics2D g2d, RayTrace trace) {
    Shape prev = g2d.getClip();
    g2d.clip(trace.scope().toShape());
    trace.space().renderer().render(g2d, trace.scope().boundingRect());
    g2d.setClip(prev);
  }
  
  private void renderPlayer(Graphics2D g2d, RayTrace trace) {
    if (trace.space() == player.space() && trace.scope().toShape().contains(player.position().x(), player.position().y())) {
      g2d.setColor(Color.red);
      g2d.fillOval((int) (player.position().x() - PLAYER_RADIUS / 2),
          (int) (player.position().y() - PLAYER_RADIUS / 2), PLAYER_RADIUS, PLAYER_RADIUS);
    }
  }
  
  @FunctionalInterface
  private static interface RayTraceRenderer {
    void render(Graphics2D g2d, RayTrace trace);
  }
  
  private void renderRecursively(Graphics2D g2d, RayTrace trace, RayTraceRenderer rayTraceRenderer) {
    rayTraceRenderer.render(g2d, trace);

    for (RayTrace.RecursiveRayTrace recursiveTrace : trace.recursiveRayTraces()) {
      AffineTransform tx = g2d.getTransform();
      g2d.transform(recursiveTrace.portalLink().invTransform());
      renderRecursively(g2d, recursiveTrace.rayTrace(), rayTraceRenderer);
      g2d.setTransform(tx);
    }
  }

  private RadialVector movementVector(InputState inputState) {
    List<Vector> vectors = new ArrayList<>();
    if (inputState.playerControlDown().isPressed()) {
      vectors.add(Vector.create(0, 1));
    }
    if (inputState.playerControlUp().isPressed()) {
      vectors.add(Vector.create(0, -1));
    }
    if (inputState.playerControlLeft().isPressed()) {
      vectors.add(Vector.create(-1, 0));
    }
    if (inputState.playerControlRight().isPressed()) {
      vectors.add(Vector.create(1, 0));
    }
    Vector movement = Vector.sum(vectors).normalized().multiply(MOVEMENT_SPEED);
    return movement.toRadial().rotate(player.camera());
  }
  
  private static Angle cameraChange(InputState inputState) {
    double rad = 0.0;
    if (inputState.playerControlCameraLeft().isPressed()) {
      rad -= ROTATION_SPEED;
    }
    if (inputState.playerControlCameraRight().isPressed()) {
      rad += ROTATION_SPEED;
    }
    return Angle.ofRadians(rad);
  }
}

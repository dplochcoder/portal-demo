package main;

import java.awt.Color;
import geom.EnclosedRaycastSpace;
import geom.EnclosedRaycastSpace.PolygonRef;
import geom.Point;
import geom.Polygon;
import geom.Rectangle;
import graphics.Renderer;

public class PortalLevel {
  public static EnclosedRaycastSpace level() {
    EnclosedRaycastSpace world =
        new EnclosedRaycastSpace(Rectangle.create(Point.origin(), 2000, 2000).asPolygon(),
            Renderer.forColor(Color.green.brighter()));

    Polygon magicPillar = Polygon.builder().addPoint(250, 250).addLine(100, 0).addLine(0, 50)
        .addLine(100, 0).addLine(0, -50).addLine(100, 0).addLine(0, 100).addLine(-50, 0)
        .addLine(0, 100).addLine(50, 0).addLine(0, 100).addLine(-100, 0).addLine(0, -50)
        .addLine(-100, 0).addLine(0, 50).addLine(-100, 0).addLine(0, -100).addLine(50, 0)
        .addLine(0, -100).addLine(-50, 0).build();
    PolygonRef magicPillarRef = world.addInteriorRaycastPolygon(magicPillar);

    EnclosedRaycastSpace insidePillar =
        new EnclosedRaycastSpace(Polygon.builder().addPoint(-500, -500).addPoint(-50, -500)
            .addPoint(-50, -550).addPoint(50, -550).addPoint(50, -500).addPoint(500, -500)
            .addPoint(500, -50).addPoint(550, -50).addPoint(550, 50).addPoint(500, 50)
            .addPoint(500, 500).addPoint(50, 500).addPoint(50, 550).addPoint(-50, 550)
            .addPoint(-50, 500).addPoint(-500, 500).addPoint(-500, 50).addPoint(-550, 50)
            .addPoint(-550, -50).addPoint(-500, -50).build(), Renderer.forColor(Color.blue));

    for (int i = 3; i <= 18; i += 5) {
      EnclosedRaycastSpace.createPortal(magicPillarRef.lineSegmentRef(i),
          insidePillar.exteriorPolygonRef().lineSegmentRef(i));
    }

    Polygon shortcutPillar = magicPillar.translate(-660, 0);
    PolygonRef shortcutRef = world.addInteriorRaycastPolygon(shortcutPillar);
    EnclosedRaycastSpace.createFlippedPortal(shortcutRef.lineSegmentRef(3),
        shortcutRef.lineSegmentRef(13));
    EnclosedRaycastSpace.createFlippedPortal(shortcutRef.lineSegmentRef(8),
        shortcutRef.lineSegmentRef(18));

    Polygon rotatingPillar = magicPillar.translate(0, -660);
    PolygonRef rotatingRef = world.addInteriorRaycastPolygon(rotatingPillar);
    EnclosedRaycastSpace.createFlippedPortal(rotatingRef.lineSegmentRef(3),
        rotatingRef.lineSegmentRef(8));
    EnclosedRaycastSpace.createFlippedPortal(rotatingRef.lineSegmentRef(13),
        rotatingRef.lineSegmentRef(18));

    Polygon infinitePillar = magicPillar.translate(-660, -660);
    PolygonRef infiniteRef = world.addInteriorRaycastPolygon(infinitePillar);

    EnclosedRaycastSpace infiniteRoom = new EnclosedRaycastSpace(Polygon.builder().addPoint(0, 0)
        .addLine(50, 0).addLine(100, 0).addLine(50, 0).addLine(0, 50).addLine(25, 0).addLine(0, 100)
        .addLine(-25, 0).addLine(0, 50).addLine(-50, 0).addLine(-100, 0).addLine(-50, 0)
        .addLine(0, -50).addLine(-25, 0).addLine(0, -100).addLine(25, 0).build(),
        Renderer.forColor(Color.orange));
    EnclosedRaycastSpace.createPortal(infiniteRef.lineSegmentRef(3),
        infiniteRoom.exteriorPolygonRef().lineSegmentRef(2));
    EnclosedRaycastSpace.createPortal(infiniteRef.lineSegmentRef(13),
        infiniteRoom.exteriorPolygonRef().lineSegmentRef(10));
    EnclosedRaycastSpace.createFlippedPortal(infiniteRoom.exteriorPolygonRef().lineSegmentRef(6),
        infiniteRoom.exteriorPolygonRef().lineSegmentRef(14));

    return world;
  }
}

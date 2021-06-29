package game;

import geom.EnclosedRaycastSpace;

public class Room {

  private final String id;
  private final EnclosedRaycastSpace raycastSpace;
  
  public Room(String id, EnclosedRaycastSpace raycastSpace) {
    this.id = id;
    this.raycastSpace = raycastSpace;
  }
  
}

package game;

public enum KeyState {
  PRESSED, // Frame 1 press
  HELD, // Frame 2+ press
  UNPRESSED, // Frame 1 release
  UNHELD; // Frame 2+ release

  public boolean isPressed() {
    return this == PRESSED || this == HELD;
  }
}

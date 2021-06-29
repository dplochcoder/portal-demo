package game;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.concurrent.GuardedBy;

public class InputStateManager implements KeyListener {

  @GuardedBy("this")
  private InputState prevInputState;

  @GuardedBy("this")
  private InputState inputState;

  public InputStateManager() {
    prevInputState = InputState.builder().build();
    inputState = InputState.builder().build();
  }

  private KeyState updateKeyStateBetweenEvents(KeyState oldKeyState, KeyState newKeyState) {
    if (oldKeyState == KeyState.HELD && newKeyState == KeyState.PRESSED) {
      return KeyState.HELD;
    } else if (oldKeyState == KeyState.UNHELD && newKeyState == KeyState.UNPRESSED) {
      return KeyState.UNHELD;
    } else {
      return newKeyState;
    }
  }

  private KeyState updateKeyStateBetweenFrames(KeyState oldKeyState, KeyState newKeyState) {
    if (newKeyState != oldKeyState) {
      return newKeyState;
    }

    switch (newKeyState) {
      case PRESSED:
        return KeyState.HELD;
      case UNPRESSED:
        return KeyState.UNHELD;
      default:
        return newKeyState;
    }
  }

  public synchronized InputState nextInputState() {
    // Update idempotent key states.
    ImmutableList<KeyState> oldKeyStates = prevInputState.keyStateList();
    ImmutableList<KeyState> newKeyStates = inputState.keyStateList();

    List<KeyState> newNewKeyStates = new ArrayList<>();
    for (int i = 0; i < oldKeyStates.size(); i++) {
      newNewKeyStates.add(updateKeyStateBetweenFrames(oldKeyStates.get(i), newKeyStates.get(i)));
    }

    prevInputState = inputState.toBuilder().setKeyStateList(newNewKeyStates).build();
    inputState = prevInputState;
    return inputState;
  }

  private synchronized void updateKeyState(KeyEvent e, KeyState newState) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_Q:
        inputState = inputState.toBuilder()
            .setPlayerControlCameraLeft(
                updateKeyStateBetweenEvents(inputState.playerControlCameraLeft(), newState))
            .build();
        break;
      case KeyEvent.VK_E:
        inputState = inputState.toBuilder()
            .setPlayerControlCameraRight(
                updateKeyStateBetweenEvents(inputState.playerControlCameraRight(), newState))
            .build();
        break;
      case KeyEvent.VK_A:
      case KeyEvent.VK_LEFT:
        inputState = inputState
            .toBuilder()
            .setPlayerControlLeft(
                updateKeyStateBetweenEvents(inputState.playerControlLeft(), newState))
            .build();
        break;
      case KeyEvent.VK_D:
      case KeyEvent.VK_RIGHT:
        inputState = inputState
            .toBuilder()
            .setPlayerControlRight(
                updateKeyStateBetweenEvents(inputState.playerControlRight(), newState))
            .build();
        break;
      case KeyEvent.VK_S:
      case KeyEvent.VK_DOWN:
        inputState = inputState
            .toBuilder()
            .setPlayerControlDown(
                updateKeyStateBetweenEvents(inputState.playerControlDown(), newState))
            .build();
        break;
      case KeyEvent.VK_W:
      case KeyEvent.VK_UP:
        inputState = inputState
            .toBuilder()
            .setPlayerControlUp(
                updateKeyStateBetweenEvents(inputState.playerControlUp(), newState))
            .build();
        break;
      default:
        break;
    }
  }

  @Override
  public synchronized void keyPressed(KeyEvent e) {
    updateKeyState(e, KeyState.PRESSED);
  }

  @Override
  public synchronized void keyReleased(KeyEvent e) {
    updateKeyState(e, KeyState.UNPRESSED);
  }

  @Override
  public synchronized void keyTyped(KeyEvent e) {}

}

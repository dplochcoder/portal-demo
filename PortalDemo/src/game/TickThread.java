package game;

import com.google.errorprone.annotations.concurrent.GuardedBy;

public class TickThread implements Runnable {
  private static final double FPS = 60.0;
  private static final int MAX_SKIP_FRAMES = 30;

  private final GameState gameState;
  private final InputStateManager inputStateManager;
  private final Runnable repaint;

  @GuardedBy("this")
  private boolean repainted = true;

  public TickThread(GameState gameState, InputStateManager inputStateManager, Runnable repaint) {
    this.gameState = gameState;
    this.inputStateManager = inputStateManager;
    this.repaint = repaint;
  }

  public synchronized void setRepainted(boolean repainted) {
    this.repainted = repainted;
  }

  private synchronized boolean repainted() {
    return this.repainted;
  }

  @Override
  public void run() {
    long frames = 0;
    long bigBangMillis = System.currentTimeMillis();

    while (true) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException ignore) {}

      if (!repainted()) continue;

      long elapsed = System.currentTimeMillis() - bigBangMillis;
      long expectedFrames = (long) (elapsed * FPS / 1000.0);

      long framesToPlay = Math.min(expectedFrames - frames, MAX_SKIP_FRAMES);
      for (long i = 0; i < framesToPlay; i++) {
        gameState.tick(inputStateManager.nextInputState());
      }
      frames = expectedFrames;

      setRepainted(false);
      repaint.run();
    }
  }

}
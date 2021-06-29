package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import game.GameState;
import game.InputStateManager;
import game.Player;
import game.TickThread;
import geom.Point;

public class Main {
  private static class MainPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final GameState gameState = new GameState(new Player(PortalLevel.level(), Point.create(100, 100)));
    private final TickThread tickThread;

    public MainPanel(InputStateManager inputStateManager) throws NoninvertibleTransformException {
      setDoubleBuffered(true);
      setPreferredSize(new Dimension(1000, 1000));
      
      this.tickThread = new TickThread(gameState, inputStateManager, () -> repaint());
      new Thread(tickThread).start();
    }

    @Override
    public void paintComponent(Graphics g) {
      Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      AffineTransform old = g2d.getTransform();
      g2d.setColor(Color.black);
      g2d.fillRect(-1, -1, getWidth() + 1, getHeight() + 1);

      gameState.render(g2d, getWidth(), getHeight());
      g2d.setTransform(old);

      tickThread.setRepainted(true);
    }
  }

  public static void main(String[] args) throws NoninvertibleTransformException {
    JFrame jFrame = new JFrame("Demo");
    jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    InputStateManager inputStateManager = new InputStateManager();
    jFrame.addKeyListener(inputStateManager);
    jFrame.setLayout(new BorderLayout());
    jFrame.add(new MainPanel(inputStateManager), BorderLayout.CENTER);

    jFrame.pack();
    jFrame.setVisible(true);
  }
}

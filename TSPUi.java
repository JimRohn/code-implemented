package adp.tsp;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The UI application that provides the window and has a main method.
 */
public class TSPUi extends JFrame {

  private static final long serialVersionUID = 1L;

  private static int MINIMUM_NUMBER_OF_CITIES = 4;
  
  private final BufferedImage image;
  private final ImagePanel imagePanel = new ImagePanel();
  private final JComboBox<String> numberOfCitiesCombo;
  private final JButton goButton = new JButton("Go");
  private final JButton cancelButton = new JButton("Cancel");
  private final JButton replayButton = new JButton("Replay longest to shortest");
  
  private final SortedSet<TSPRoute> allRoutes = new TreeSet<>();
     
  public TSPUi(final int width, final int height) {
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
 
    this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);    
    this.imagePanel.setImage(this.image);
    
    final JPanel mainPanel = new JPanel(new BorderLayout());
    final JPanel topPanel = new JPanel();
    
    final String[] cities = new String[6];
    for (int i = 0; i < cities.length; i++) {
      cities[i] = i + MINIMUM_NUMBER_OF_CITIES + " cities";
    }
    this.numberOfCitiesCombo = new JComboBox<String>(cities);
    this.goButton.addActionListener((ev)->runAnimation());
    this.cancelButton.addActionListener((ev)->cancel());
    this.replayButton.addActionListener((ev)->showLongestToShortest());
    this.cancelButton.setEnabled(false);
    this.replayButton.setEnabled(false);

    topPanel.add(this.numberOfCitiesCombo);
    topPanel.add(this.goButton);
    topPanel.add(this.cancelButton);
    topPanel.add(this.replayButton);

    mainPanel.add(topPanel, BorderLayout.NORTH);
    mainPanel.add(this.imagePanel, BorderLayout.CENTER);
    
    add(mainPanel);
    pack();
    setVisible(true);
    
  }

 private volatile Thread myThread = null;
  // I have created a flag below to check that only the cancel button interrupts the thread
  private volatile boolean cancelled = false;

  private void runAnimation() {
//the flag below reset the cancel flag back to false so that the thread can start running again
    cancelled=false;
  //I created a Thread and we gave the functionalities of the run animation method to the thread
    myThread = new Thread(()-> {
      final int numberOfCities = this.numberOfCitiesCombo.getSelectedIndex() + MINIMUM_NUMBER_OF_CITIES;
      this.goButton.setEnabled(false);
      this.replayButton.setEnabled(false);
      this.cancelButton.setEnabled(true);
      this.allRoutes.clear();
      this.imagePanel.resetPaintCallCounter();

      final TSP tsp = new TSP(numberOfCities, TSPUi.this.image.getWidth(), TSPUi.this.image.getHeight());
      tsp.setListener(new UIListener(TSPUi.this));
      tsp.findShortestRoute();
    });

    myThread.start();

  }

  private void cancel() {
    // I created the flag below and I only set it to true when I press the cancel button , meaning that's the only time the thread should stop
    cancelled = true;
// in order for the cancel button to pause the running thread we add interrupt mechanism Thread.interrupt and a cancel flag
    if(myThread.isAlive() && myThread != null && cancelled == true){
       myThread.interrupt();
    }

    System.out.println( "Cancelling...");
    this.goButton.setEnabled(true);
    this.cancelButton.setEnabled(false);
    //I had to add the line below to enable the replay button after I press Cancel
    this.replayButton.setEnabled(true);

  }
  //instead of the master thread to run the animation we gave the functionalities of the showLongestToShortest method to the thread
  private void showLongestToShortest() {
    //the flag below reset the cancel flag back to false so that the thread can start running again
    cancelled=false;
    myThread = new Thread((new Runnable() {
      @Override
      public void run() {
        replayButton.setEnabled(false);
        cancelButton.setEnabled(true);
        goButton.setEnabled(false);

        for(final TSPRoute route : TSPUi.this.allRoutes) {
          displayOneRoute(route, Color.WHITE);
        }
        displayBestRoute(TSPUi.this.allRoutes.last());
      }
    }));
    myThread.start();


  }

  public void newDisplayBestRoute(final TSPRoute bestRoute){
    //There are 2  ways of scheduling a job an a master thread: 1 is invoke later and the other is invoke and wait.
   //SwingUtilities.invokeLater(()->displayBestRoute(bestRoute)) ;

    try {
      SwingUtilities.invokeAndWait(()->displayBestRoute(bestRoute));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public void displayBestRoute(final TSPRoute bestRoute) {
    displayOneRoute(bestRoute, Color.GREEN);
    System.out.println("Paint calls: " + this.imagePanel.paintCalls());
    this.goButton.setEnabled(true);
    this.cancelButton.setEnabled(false);
    this.replayButton.setEnabled(true);
  }
  private void newdisplayOneRoute(final TSPRoute bestRoute, final Color color){
//    SwingUtilities.invokeLater(()->displayOneRoute(bestRoute,color));
    try {
      SwingUtilities.invokeAndWait(()->displayOneRoute(bestRoute,color));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private void displayOneRoute(final TSPRoute bestRoute, final Color color) {
    final Graphics2D g = (Graphics2D) this.image.getGraphics();
    g.setFont(Font.decode("SANSSERIF-BOLD-24"));
    g.setColor(Color.LIGHT_GRAY);
    g.fillRect(0, 0, this.image.getWidth(), this.image.getHeight());
    
    g.setColor(Color.BLACK);
    g.drawString(String.format("%.2f", bestRoute.distance()), 5, 24);

    g.setColor(color);
    g.setStroke(new BasicStroke(7f));
    paintPath(g, bestRoute);

    paintLocations(bestRoute.route(), g);

    this.imagePanel.repaint();
  }

  public void newdisplayRouteUpdate(final TSPRoute route, final TSPRoute bestRoute){
//    SwingUtilities.invokeLater(new Runnable() {
//      @Override
//      public void run() {
//        displayRouteUpdate(route,bestRoute);
//      }
//    });

    try {
      SwingUtilities.invokeAndWait(()->displayRouteUpdate(route,bestRoute));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
  public void displayRouteUpdate(final TSPRoute route, final TSPRoute bestRoute) {
    this.allRoutes.add(route);
    final Graphics2D g = (Graphics2D) this.image.getGraphics();
    g.setFont(Font.decode("SANSSERIF-BOLD-24"));
    g.setColor(Color.LIGHT_GRAY);
    g.fillRect(0, 0, this.image.getWidth(), this.image.getHeight());
    
    g.setColor(Color.BLACK);
    g.drawString(String.format("%.2f (%.2f)", route.distance(), bestRoute.distance()), 5, 24);

    g.setColor(Color.WHITE); 
    g.setStroke(new BasicStroke(7f));
    paintPath(g, bestRoute);

    g.setColor(Color.DARK_GRAY);
    g.setStroke(new BasicStroke(1f));
    paintPath(g, route);

    paintLocations(route.route(), g);

    // this call to paintImmediately is used to ensure that every call to displayUpdate
    // actually gets painted - if we used the asynchronous repaint() method as is more
    // usual, paint jobs could be discarded when a new paint job arrived, resulting in
    // just the last update actually being displayed. However, this approach means that
    // thousands of repaint events are on the edt and user interactivity is blocked - so no good!
    //this.imagePanel.paintImmediately(0,0,this.imagePanel.getWidth(), this.imagePanel.getHeight());
    
    this.imagePanel.repaint();
  }

  private void paintLocations(final Point[] locations, final Graphics2D g) {
    g.setColor(Color.MAGENTA);
    for(final Point p : locations) {
      g.fillOval(p.x - 5, p.y - 5, 15, 15);
      g.setColor(Color.BLUE);
    }
  }
  
  private void paintPath(final Graphics2D g, final TSPRoute path) {
    final Point[] locations = path.route();
    for (int i = 1; i < locations.length; i++) {
      final Point s = locations[i - 1];
      final Point e = locations[i];
      g.drawLine(s.x, s.y, e.x, e.y);
    }
    final Point s = locations[locations.length - 1];
    final Point e = locations[0];
    g.drawLine(s.x, s.y, e.x, e.y);    
  }

  
  public static void launch() {
    new TSPUi(500, 500);
  }
  
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(()->launch());
  }
  
}

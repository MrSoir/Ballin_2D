package gamepackage.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import ballin.Ballin;
import gamepackage.GridGameInterface;


public class MainGameFrame implements ActionListener {

	private enum ImplementedGames {
		ColorAreaGridGame, SnakeGridGame, BALLIN
	};
	private enum ImplementedAlgs {
		TowersOfHanoi, FraktalGenerator
	};
	public static ImplementedGames startupGame = ImplementedGames.BALLIN;
	public static ImplementedAlgs startupGameTwo = ImplementedAlgs.FraktalGenerator;
	private JFrame gameFrame;
	private MyPanel thePanel;
	private int panelWidth;
	private int panelHeight;
	private Rectangle controlArea;
	private Rectangle scoreArea;
	private Rectangle gameArea;
	private GridGameInterface myCurrentGame;
	
	public MainGameFrame() {

		// create first an empty JFrame
		gameFrame = new JFrame("My Game Frame");
		// ensure that all processes are stopped.
		gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);;
		gameFrame.setVisible(true);
		gameFrame.setLocationRelativeTo(null); // center the frame
		gameFrame.setSize(800, 600); // erstmal wird das fenster auf 800*600 gesetzt, der User kann das fenster
		// dann auf seine gewünschte größe setzten, ein listener innerhalb der Ballin-Klasse handelt das und 
		// reagiert entsprechend auf die größenänderung des Frames

		// create the MenuBar with all the games
		JMenuBar jmb = new JMenuBar();
		// Build the GameChoice menu.
		JMenu dummyMenu = new JMenu("platzhalter...");
		dummyMenu.setMnemonic(KeyEvent.VK_G);
		dummyMenu.setToolTipText("Select the Level to Play");
		// now add the menu to the JMenuBar and the JMenuBar to the JFrame
		jmb.add(dummyMenu);
		gameFrame.setJMenuBar(jmb);
		gameFrame.validate();
		
		gameFrame.addComponentListener(new ComponentListener(){
			@Override
			public void componentHidden(ComponentEvent arg0) {
			}
			@Override
			public void componentMoved(ComponentEvent arg0) {
			}
			@Override
			public void componentResized(ComponentEvent e) {
				panelWidth = gameFrame.getContentPane().getWidth();
				panelHeight = gameFrame.getContentPane().getHeight();
				resizeGamePanel();				
			}
			@Override
			public void componentShown(ComponentEvent arg0) {
			}
		});


		// Determine size of the panel area (the size remaining to display the
		// game)		
		panelWidth = gameFrame.getContentPane().getWidth();
		panelHeight = gameFrame.getContentPane().getHeight();
		
		initGame(startupGame, null);
	}
	private void initGame(ImplementedGames whichGame, ImplementedAlgs whichAlg) {
		if (thePanel != null) { // remove the old panel if one was already
			// created.
			gameFrame.remove(thePanel);
		}
		// now create a new panel and initialize the game within it.
		thePanel = new MainGameFrame.MyPanel();
		
		resizeGamePanel();
		myCurrentGame = new Ballin(gameFrame, thePanel, gameArea, controlArea);
		gameFrame.setTitle(myCurrentGame.getName());
		gameFrame.add(thePanel);
		gameFrame.validate();
		myCurrentGame.startGame();
	}
	private void resizeGamePanel(){
		resizeGamePanel(panelWidth, panelHeight, 20, 20);
	}

	private void resizeGamePanel(int width, int height, int widthConstraint, int heightConstraint) {
		// initalization of color picker area (on the left top)
		if(controlArea == null){
			controlArea = new Rectangle(5, 5, (2 * width / 3) - 10, 45);
		}else{
			controlArea.width = (2 * width / 3) - 10;
			controlArea.height = 45;
		}
		if(scoreArea == null){
		scoreArea = new Rectangle(5 + 2 * width / 3, 5, (width / 3) - 10, 45);
		}else{
			scoreArea.x = 5 + 2 * width / 3;
			scoreArea.width = (width / 3) - 10;
			scoreArea.height = 45;
		}
		int gameAreaWidth = ((width - 10) / widthConstraint) * widthConstraint;
		int gameAreaHeight = ((height - 60) / heightConstraint) * heightConstraint;
		if(gameArea == null){
			gameArea = new Rectangle((width - gameAreaWidth) / 2, 55 + (height - 60 - gameAreaHeight) / 2, gameAreaWidth,
					gameAreaHeight);
		}else{
			gameArea.x = (width - gameAreaWidth) / 2;
			gameArea.y = 55 + (height - 60 - gameAreaHeight) / 2;
			gameArea.width = gameAreaWidth;
			gameArea.height = gameAreaHeight;
		}
	}

	private class MyPanel extends JPanel {

		private static final long serialVersionUID = -1168947575194286689L;
		public void paintComponent(Graphics gg) {
			// first paint the default JPanel appearance.
			super.paintComponent(gg);
			// Cast to Graphics2D - higher flexibility in the graphics.
			Graphics2D g = (Graphics2D) gg;
			// Now draw the defaults background color - here WHITE.
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, panelWidth, panelHeight);

			// paint the current color field
			if(myCurrentGame != null){
				myCurrentGame.paintGameArea(g);
	
				// paint color picker area
				myCurrentGame.paintControlArea(g);
	
				// paint the game control and score area
				myCurrentGame.getScores().paintScore(g, scoreArea);
			}
		}
	}

	public void actionPerformed(ActionEvent arg0) {
		String cmd = arg0.getActionCommand();
		for (ImplementedGames ig : ImplementedGames.values()) {
			if (ig.toString().equals(cmd)) {
				myCurrentGame.closeGame();
				initGame(ig, null); // this call removes the old panel and adds
				// the new one.
				return;
			}
		}
		for (ImplementedAlgs ialg : ImplementedAlgs.values()) {
			if (ialg.toString().equals(cmd)) {
				myCurrentGame.closeGame();
				initGame(null, ialg); // this call removes the old panel and
				// adds the new one.
				return;
			}
		}
	}

}

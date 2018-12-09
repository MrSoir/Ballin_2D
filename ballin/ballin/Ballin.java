package ballin;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

import ballin.Client.ClientListener;
import ballin.Server.ClientIdServerObj;
import ballin.Server.ServerListener;
import gamepackage.GameScores;
import gamepackage.GridGameInterface;

public class Ballin implements GridGameInterface{//,ActionListener{
	
//	----------------------------------------------------------------------------------------------------
	
	// die wichtigsten member-variablen:
	
//	private Timer timer;
	BallinRunnable ballinThrd;
	
	private interface RunnableCaller{
		public void apply();
	}
	private class BallinRunnable implements Runnable{
		private boolean isRunning = true;
		private RunnableCaller caller;
		private int gameSpeed;
		private BallinRunnable(int gameSpeed, RunnableCaller caller){
			this.caller = caller;
			this.gameSpeed = gameSpeed;
		}
		@Override
		public void run() {
			while(isRunning){
				caller.apply();
				try {
					Thread.sleep(gameSpeed);
				} catch (InterruptedException e) {
					System.err.println("InterruptedException while while BallinRunnable was asleep");
				}
			}
			caller = null;
		}
		private void stop(){isRunning = false;}
		private void start(){ (new Thread(this)).start(); }
	}
	
	private Rectangle gameArea;
	private Rectangle controlArea;
	
	private JPanel myPanel;
	
//	private double playerAngle = 0;
	private double PLAYER_ANGLE_WIDTH = 40;
	
	private Rectangle courtRectOuter;
	private Rectangle courtRect;
	
	private double centX,
				   centY; // centX, centY = Spielzentrum
	private double innerCircFact = 0.05;
	private double rad; // rad und diam stellen die groesse des magnetrings dar
	private double diam;
	private double innerDiam;
		
	// die folgenden werte legen die spieler- und spielballgeschw. fest und die kraft des magneten.
	// die spielerballgeschw. ist in pixelbewegungen pro sekunde zu sehen.
	private LevelInfo levelInfo = new LevelInfo();
	
	private JFrame inFrame;
	
	private int gameSpeed = 30;
	
	private int PLAYER_ID = 0; 
	// Player-Id: Aufgrund des Mehrspielermodus gibt es die klasse Player. die Player-Klasse enthaelt eine id. 
	// über die private variable Balllin.PLAYER_ID erkennt man, welcher der im Spiel befindlichen Player
	// der eigene ist. Im Single-Player modus gibt es nur einen Player mit der id == 0. Der eigene Spieler wird 
	// rot gezeichnet (Magnet und Magnetbaelle), die anderen Spieler erhalten die dezenteren Farben aus dem
	// Color-Array playerColors.
	
	// es gibt in dem spiel magnetbaelle, targetbaelle und hindernisse.
	// fuer die hindernisse verwende ich folgenden trick: sie besitzen eine 
	// masse von unendlich und eine bewegungsgeschw. von 0. dasselbe gilt fuer 
	// targetbaelle, die in ihrem gate gelandet sind. damit werden sie zwar aus dem 
	// spiel genommen, verweilen aber als zusaetzliche hindernisse auf dem spielfeld
	private List<MagnetBall> magnetBalls = new ArrayList<>();
	private List<TargetBall> targetBalls = new ArrayList<>();
	private List<ObstacleBall> obstacleBalls = new ArrayList<>();
	private List<Ball> borderBalls = new ArrayList<>();
	private int MAX_ALLOWED_WALL_CONTACTS = 3; // maximale anzahl an wandkollisionen die ein targetball haben darf, um noch in sein gate zu duerfen
	private long levelStartTime = System.currentTimeMillis();
	private List<Gate> gates = new ArrayList<>();

//	-----------------------------------------------------------------------------------------------------
	
	// es folgen diverse statische innere (Helfers-)Klassen 
	// (ich erstelle ungern solch kleine Klassen in eigenstaendigen Java-Dateien, blaeht m.E. Projekte unnoetig auf)
	// die Klassen muessen statisch sein, damit sie ueber das Netzwerk versendet werden koennen
	
	// LevelInfo speichert die relevantesten Eigenschaften eines Levels ab, die es definieren:
	// (die variablen sind deshalb in diese LevelInfo-Klasse ausgelagert, damit beim Multiplayer-Modus
	// nur dieses eine Objekt und nicht dutzende Variablen einzeln auf die Reise geschickt werden muessen)
	private static class LevelInfo implements Serializable{
		private static final long serialVersionUID = -659535922302793505L;
		private int LEVEL = 0;
		private int NUM_OF_LEVELS = 5;
		
		private double OBSTACLE_SPEED = 0.3d;

		private int MAX_MAGNET_BALL_SPEED = 250;
		private int MAX_TARGET_BALL_SPEED = 250;
		private int MAGNET_FORCE = 50;
		
		private double MAGNET_BALL_REL_SIZE = 0.03;
		private double TARGET_BALL_REL_SIZE = 0.03;
		private double OBSTACLE_REL_SIZE    = 0.07;
		
		private long levelStartTime = System.currentTimeMillis();
		
		private LevelInfo createCopy(){
			LevelInfo copy =  new LevelInfo();
			copy.LEVEL = LEVEL;
			copy.OBSTACLE_SPEED = OBSTACLE_SPEED;
			copy.MAX_MAGNET_BALL_SPEED = MAX_MAGNET_BALL_SPEED; 
			copy.MAX_TARGET_BALL_SPEED = MAX_TARGET_BALL_SPEED; 
			copy.MAGNET_FORCE = MAGNET_FORCE; 
			copy.MAGNET_BALL_REL_SIZE = MAGNET_BALL_REL_SIZE; 
			copy.TARGET_BALL_REL_SIZE = TARGET_BALL_REL_SIZE; 
			copy.OBSTACLE_REL_SIZE = OBSTACLE_REL_SIZE; 
			copy.levelStartTime = levelStartTime;
			return copy;
		}
	}
	
	// Player: diese klasse kam aufgrund des mehrspielermodus hinzu. Im Single-Player-Modus gibt es nur einen Spieler
	// dessen id == 0 ist (siehe erklärung weiter oben zu Ballin.PLAYER_ID
	public static class Player implements Serializable{
		private static final long serialVersionUID = -3921129906612506332L;
		private final int playerId;
		private double playerAngle = 0d;
		private int scored = 0;
		
		private Player(final int playerId){
			this.playerId = playerId;
		}
		
		public double getPlayerAngle(){return playerAngle;}
		public void setPlayerAngle(double angle){playerAngle = angle;}
		public int getPlayerID(){return playerId;}
//		public void setPlayerID(int id){playerId = id;}
		public Player createCopy(){
			Player copy = new Player(playerId);
			copy.playerAngle = playerAngle;
			copy.scored = scored;
			return copy;
		}
	}
	
	private static class ObstacleBall extends Ball{
		private static final long serialVersionUID = 190267484051582402L;
		
		private ObstacleBall(Point center, Vector velocity, double mass, double radius) {
			super(center, velocity, mass, radius);
		}
				
		private void moveObstacle(Point gameCent, double angleOffs){
			Vector vec = gameCent.getVector(getCenter());
			double angle = gameCent.getAngle(getCenter());

			angle = (angle + angleOffs);
			double xOffs = Math.cos(Math.toRadians(angle)) * vec.getLength();
			double yOffs = Math.sin(Math.toRadians(angle)) * vec.getLength();
			
			Point newCent = gameCent.add(new Point(xOffs,-yOffs)).toPoint();
			this.setCenter( newCent );
		}
		public ObstacleBall createCopy(){
			ObstacleBall copy = new ObstacleBall(this.getCenter(), this.getVelocity(), this.getMass(), this.getRadius());
			return copy;
		}
	}
	
	public static class TargetBall extends Ball{
		private static final long serialVersionUID = -3890382396862031009L;
		// die Klasse TargetBall ist eine erweiterung der Klasse Ball, da diese baelle zusaetzliche funktionalitate bieten muss:
		// es muss ermittelt werden, wie haeufig der targetball seit dem letzten magnetball-kontakt
		// die banden beruerht hat. Nur wenn diese einen gewissen schwellenwert (MAX_ALLOWED_WALL_CONTACTS) unterschreitet,
		// darf der spielball das gate passieren
		private boolean isAllowedToPassGate = false;
		private Gate gate;
		private Player player = null;
		
		public TargetBall(Vector velocity, double mass, double radius, Gate gate){
			super(velocity, mass, radius);
			this.gate = gate;
		}
		public TargetBall(Point center, Vector velocity, double mass, double radius, Gate gate){
			super(center, velocity, mass, radius);
			this.gate = gate;
		}
		public void isAllowedToPassGate(boolean bool){isAllowedToPassGate = bool;}
		public boolean isAllowedToPassGate(){return isAllowedToPassGate;}
		public Gate getGate(){return gate;}
		public void moveBall(double millisecs, Rectangle gameBorder, double playerAngle, 
				double playerForce, double maxSpeed,
				int MAX_ALLOWED_WALL_CONTACTS){
			moveBall(millisecs, gameBorder, playerAngle, playerForce, maxSpeed);
			if(this.hitWall() > MAX_ALLOWED_WALL_CONTACTS){
				this.isAllowedToPassGate(false);
			}
		}
		public void setPlayer(Player player){
			this.player = player;
		}
		public Player getPlayer(){return player;}
		
		public TargetBall createCopy(){
			TargetBall  ball = new TargetBall(this.getCenter().createCopy(), this.getVelocity().createCopy(), 
					this.getMass(), this.getRadius(), this.getGate().createCopy());
			ball.isAllowedToPassGate(this.isAllowedToPassGate);
			ball.setMoveable(this.isMoveable());
			ball.hitPlayer(this.hitPlayer());
			ball.hitWall(this.hitWall());
			if(player != null){
				ball.setPlayer(this.player.createCopy());
			}
			return ball;
		}
	}
	public static class MagnetBall extends Ball{
		private static final long serialVersionUID = -3890382396862031009L;
		// die Klasse TargetBall ist eine erweiterung der Klasse Ball, da diese baelle zusaetzliche funktionalitate bieten muss:
		// es muss ermittelt werden, wie haeufig der targetball seit dem letzten magnetball-kontakt
		// die banden beruerht hat. Nur wenn diese einen gewissen schwellenwert (MAX_ALLOWED_WALL_CONTACTS) unterschreitet,
		// darf der spielball das gate passieren
		private Player player;
		
		public MagnetBall(Vector velocity, double mass, double radius, Player player){
			super(velocity, mass, radius);
			this.player = player;
		}
		public MagnetBall(Point center, Vector velocity, double mass, double radius, Player player){
			super(center, velocity, mass, radius);
			this.player = player;
		}
		public MagnetBall(Point center, Vector velocity, double mass, double radius){
			super(center, velocity, mass, radius);
			this.player = null;
		}
		public Player getPlayer(){return player;}
		
		public MagnetBall createCopy(){
			MagnetBall  ball = new MagnetBall(this.getCenter().createCopy(), this.getVelocity().createCopy(), 
					this.getMass(), this.getRadius(), this.getPlayer().createCopy());
			ball.setMoveable(this.isMoveable());
			ball.hitPlayer(this.hitPlayer());
			ball.hitWall(this.hitWall());
			return ball;
		}
	}
	
	public static class Gate implements Serializable{
		private static final long serialVersionUID = 8806321129900553348L;
		// ein gate ist ein tor, in das der targetball hineinfliegen muss
		// es gibt 4 unterschiedliche gates, eines 
		// 		oben (borderID == 0)
		// 		rechts (borderID == 1)
		// 		unten (borderID == 2)
		// 		links (borderID == 3).
		// daher auch die 4 farben: Gate.colors
		// pos ist relativ zur spielfeldflaeche, hier mit 0.5 immer zentriert und mit 0.3 30% der spielfeldbreite/-hoehe
		private int borderID = 0;
		private double pos = 0.5;
		private double width = 0.3;
//		private boolean hit = false;
		private Color col;
		
		private Gate(int borderId){
			this.borderID = borderId;
			col = createGateColor();
		}
		private Color[] colors = new Color[]{
				new Color(0,255,0),
				new Color(0,0,255),
				new Color(255,0,255),
				new Color(255,160,160)
		};
		private static int colorID = 0;
		private Color createGateColor(){
			return colors[colorID++ % colors.length];
		}
		public Color getColor(){return col;}
		public String getColorString(){
			return String.format("	1=%s | 2=%s | 3=%s", col.getRed(), col.getGreen(), col.getBlue());
		}
		
		public Gate createCopy(){
			Gate copy = new Gate(this.borderID);
			copy.pos = this.pos;
			copy.width = this.width;
			copy.col = new Color(col.getRed(), col.getGreen(), col.getBlue());
			return copy;
		}
	}
	
//-----------------------------------------------------------------------------------------------------------------
	
	// Konstruktor:
	
	public Ballin(JFrame inFrame, JPanel yourPanel, Rectangle gameArea, Rectangle controlArea){
		setJMenu(inFrame);
		
		createSinglePlayer();
		
		this.gameArea = gameArea;
		this.controlArea = controlArea;
		myPanel = yourPanel;
		
		// einen listener dem jFrame anhaengen, damit das zeichnen der Spielflaeche innerhalb des
		// fensters immer korrekt zentriert bleibt (aendern der Fenstergroesse ist in meiner version erlaubt)
		inFrame.addComponentListener(new ComponentListener(){
			@Override
			public void componentHidden(ComponentEvent arg0) {
			}
			@Override
			public void componentMoved(ComponentEvent arg0) {
			}
			@Override
			public void componentResized(ComponentEvent e) {
				// fenstergroessenaenderung handeln:
				revalidateDimensions();
			}
			@Override
			public void componentShown(ComponentEvent arg0) {
			}
		});
		
		// sobald der spieler die Maus bewegt, wird über die Cursor-Position die neue 
		// Position des Magneten berechnet: (mouseDragged wird auch abgefangen, falls ein spieler denkt er muesse
		// die maus zum bewegen des magneten gedrueckt halten. Ist aber auch besser fuer den Fall, dass der Spieler
		// während des Spiels beim Bewegen der Maus versehentlich eine Maustaste klickt):
		yourPanel.addMouseMotionListener(new MouseMotionListener(){
			@Override
			public void mouseDragged(MouseEvent e) {
				movePlayer(new Point(e.getX(), e.getY()));
			}
			@Override
			public void mouseMoved(MouseEvent e) {
				movePlayer(new Point(e.getX(), e.getY()));
			}
		});
		this.inFrame = inFrame;
		
		inFrame.setFocusable(true);
		
		initFullSnakeGamePanel(false);
		myPanel.repaint();
	}
	
//	--------------------------------------------------------------------------------------
	
	private JMenu multiplayerMenu;

	private void setJMenu(final JFrame frame){
		// das speziell fuer das spiel Ballin angepasste menue erstellen.
		// mittels des menues kann man 1. in ein beliebiges level springen,
		// 2. die relevanten, fuer das spielgeschehen interessanten, stellschrauben manipulieren
		// z.B. die maixmale magnetballgeschwindigkeit, -groesse und die Magnetanziehungskraft
		JMenuBar jmb = new JMenuBar();

		JMenuItem menuItem = new JMenuItem("Select Level");
		menuItem.addActionListener((e)->{
			showLevelSelectorDialog(frame);
		});
		JMenu levelMenu = new JMenu("Level Choice");
		levelMenu.setMnemonic(KeyEvent.VK_G);
		levelMenu.setToolTipText("Select the Level to Play");
		levelMenu.add(menuItem);
		jmb.add(levelMenu);
		
		JMenu prefMenu = new JMenu("Preferences");
		prefMenu.setMnemonic(KeyEvent.VK_G);
		prefMenu.setToolTipText("Preferences");
		jmb.add(prefMenu);
		
		addPrefMenuItem(frame, prefMenu, "set magnet-balls max speed", "max magnet speed", (val)->{levelInfo.MAX_MAGNET_BALL_SPEED = (int)val; sendLevelInfoToClients();}, 50, 1000);
		addPrefMenuItem(frame, prefMenu, "set target-balls max speed", "max target speed", (val)->{levelInfo.MAX_TARGET_BALL_SPEED = (int)val;sendLevelInfoToClients();}, 50, 1000);
		addPrefMenuItem(frame, prefMenu, "set magnet force", "magnet force", (val)->{levelInfo.MAGNET_FORCE = (int)val;sendLevelInfoToClients();}, 1, 200);	
		addPrefMenuItem(frame, prefMenu, "set obstacle speed", "obstacle speed", (val)->{levelInfo.OBSTACLE_SPEED = val/10d;sendLevelInfoToClients();}, 0, 20);
		addPrefMenuItem(frame, prefMenu, "set magnet-balls size", "magnet ball size", 
				(val)->{
					levelInfo.MAGNET_BALL_REL_SIZE = val;
					for(Ball ball: magnetBalls){
						ball.setRadius((int)courtRect.width*levelInfo.MAGNET_BALL_REL_SIZE);
					}
					sendLevelInfoToClients();
				}, 0.01, 0.1);	
		addPrefMenuItem(frame, prefMenu, "set target-balls size", "target ball size", 
				(val)->{
					levelInfo.TARGET_BALL_REL_SIZE = val;
					for(Ball ball: targetBalls){
						ball.setRadius((int)(courtRect.width*levelInfo.TARGET_BALL_REL_SIZE));
					}
					sendLevelInfoToClients();
				}, 0.01, 0.1);
		addPrefMenuItem(frame, prefMenu, "set obstacle size", "obstacle size", 
				(val)->{
					levelInfo.OBSTACLE_REL_SIZE = val;
					for(Ball ball: this.obstacleBalls){
						ball.setRadius((int)(courtRect.width*levelInfo.OBSTACLE_REL_SIZE));
					}
					sendLevelInfoToClients();
				}, 0.01, 0.1);	

		
		multiplayerMenu = new JMenu("Multiplayer");
		multiplayerMenu.setMnemonic(KeyEvent.VK_G);
		jmb.add(multiplayerMenu);
		setMultiplayerMenu();
		
		frame.setJMenuBar(jmb);
	}
	private void setMultiplayerMenu(){
		multiplayerMenu.removeAll();
		if(!isClient && !isServer){
			JMenuItem serverMI = new JMenuItem("create server");
			serverMI.addActionListener((e)->{
				createServer();
				setMultiplayerMenu();
			});
			JMenuItem clientMI = new JMenuItem("create cilent");
			clientMI.addActionListener((e)->{
				createClient(inFrame);
				setMultiplayerMenu();
			});
			
			multiplayerMenu.add(serverMI);
			multiplayerMenu.add(clientMI);
		}else if (isServer){
			JMenuItem serverMI = new JMenuItem("quit server");
			serverMI.addActionListener((e)->{
				closeServer();
			});
			multiplayerMenu.add(serverMI);
		}else if (isClient){
			JMenuItem clientMI = new JMenuItem("quit cilent");
			clientMI.addActionListener((e)->{
				closeClient();
			});
			multiplayerMenu.add(clientMI);
		}
	}
	
	private void addPrefMenuItem(final JFrame frame, JMenu prefMenu, 
			String itemString, String dialogHeading, VariableSetter varSetter,
			double minVal, double maxVal){
		JMenuItem preferencesItem = new JMenuItem(itemString);
		preferencesItem.addActionListener((e)->{
			if(!isClient){
				showPlayerSpeedDialog(frame, dialogHeading, varSetter, minVal, maxVal);
			}
		});
		prefMenu.add(preferencesItem);
	}
	private void showLevelSelectorDialog(final JFrame frame){
		if(!isClient){
			Object[] possibilities = new Object[50];
			for(int i=1; i <= 50; i++){
				possibilities[i-1] = ""+i;
			}
			String s = (String)JOptionPane.showInputDialog(
			                    frame,
			                    "",
			                    "Select Level",
			                    JOptionPane.PLAIN_MESSAGE,
			                    null,
			                    possibilities,
			                    "1");
	
			if (s != null && s.length() > 0) {
				try{
					setLevel(Integer.parseInt(s)-1);
				}catch (NumberFormatException e){
					return;
				}
			}
		}
	}
	
	private interface VariableSetter{
		public void setValue(double val);
	}
	private void showPlayerSpeedDialog(final JFrame frame, String dialogString, VariableSetter valueSetter,
			double minVal, double maxVal){
		if(!isClient){
		    String s = JOptionPane.showInputDialog(frame, 
		    		dialogString + String.format(" (%s <= x <= %s): ",minVal, maxVal));
		    if(s != null){
			    try{
					double val = Double.parseDouble(s);
					if(val >= minVal && val <= maxVal){
						valueSetter.setValue(val);
					}
					
				}catch (NumberFormatException e){
					return;
				}
		    }
		}
	}
	
	private void createSinglePlayer(){
		Player player = new Player(0);
		playersMap.clear();
		playersMap.put(0, player);
		this.PLAYER_ID = 0;
	}
	
		private double oldCentX = -1d, oldCentY = -1d;
		private double oldCourtWidth = -1d;
	private void revalidateDimensions(){
		double width = Math.min(gameArea.width, gameArea.height);
		
		// spielzentrum berechnen
		centX = getGameCenter().x;
		centY = getGameCenter().y;
		
		// radus des magnetkreises neu berechnen
		rad = width*0.5;
		diam = rad * 2;
		innerDiam = diam*(1d-innerCircFact);
		
		innerCircFact = 0.05;
		
		// court-dimensionen berechnen und das court-rectangle neu setzten
		double courtWidth = Math.sqrt(innerDiam*innerDiam / 2d) *(1d-innerCircFact);
		double courtWidthOuter = Math.sqrt(innerDiam*innerDiam / 2d);
		courtRect = createRectangle(centX,
									centY, 
									courtWidth, courtWidth).getBounds();
		courtRectOuter = createRectangle(centX, centY, courtWidthOuter, courtWidthOuter).getBounds();
		
		// falls sich die spielfeldgroesse verandert hat, muessen die spielebaelle und hindernisse
		// im verhaeltnis der groessenaenderung skaliert werden
		if(oldCentX != -1d){
			double resFactorWdth = courtRect.width / oldCourtWidth;
			
			Point newCent = new Point(centX, centY);
			Point oldCent = new Point(oldCentX, oldCentY);
			
			// skalierung der baelle:
			for(Ball ball: magnetBalls){
				resizeBallAfterFrameSizeChanged(ball, resFactorWdth, newCent, oldCent);
			}
			for(Ball ball: targetBalls){
				resizeBallAfterFrameSizeChanged(ball, resFactorWdth, newCent, oldCent);
			}
			for(Ball ball: obstacleBalls){
				resizeBallAfterFrameSizeChanged(ball, resFactorWdth, newCent, oldCent);
			}
			for(Ball ball: borderBalls){
				resizeBallAfterFrameSizeChanged(ball, resFactorWdth, newCent, oldCent);
			}
		}
		oldCentX = centX;
		oldCentY = centY;
		oldCourtWidth = courtRect.width;
	}
	private void resizeBallAfterFrameSizeChanged(Ball ball, double resFactorWdth, Point newCent, Point oldCent){
		// bei der skalierung eines balls muess seine position (center), seine groesse (radius) und die 
		// bewegungsgeschw. (velocity) angekpasst werden
		double newX = newCent.x + (ball.getCenter().x - oldCent.x) * resFactorWdth;
		double newY = newCent.y + (ball.getCenter().y - oldCent.y) * resFactorWdth;
		ball.setCenter(new Point(newX, newY));
		ball.setRadius(ball.getRadius() * resFactorWdth);
		ball.setVelocity(new Vector(ball.getVelocity().x * resFactorWdth,
									ball.getVelocity().y * resFactorWdth));
	}
	
	@Override
	public void startGame(){
		// beim start des spiels erstmal das spielkonzept dem spieler erklaeren
		JOptionPane.showMessageDialog(myPanel,
				String.format("Willkommen zu Ballin!%n"
						+ "In diesem Spiel existieren Magnetbälle (rot) und Spielbälle (andersfarbig) sowie Tore.%n"
						+ "Ziel ist es, die Spielbälle mittels den Magnetbällen in die Tore zu bugsieren.%n"
						+ "Dabei lassen sich die Magnetbälle nicht direkt steuern sondern lediglich%n"
						+ "deren Bewegung mittels des roten Magneten im äußeren Ring \"ablenken\".%n"
						+ "Der Magnet wird mit dem Maus-Cursor bewegt.%n"
						+ "Ein Spielball gilt erst als erfolgreich ins Tor gesteuert, wenn er%n"
						+ "		1.	von einem Magnetball angespielt wurde und%n"
						+ "		2.	er seit dem letzten Magnetballkontakt nicht öfter als %s-mal%n"
						+ "			von einer der Banden abgeprallt ist.%n"
						+ "		Dies dient dazu, zufällig zustandegekommene Tore auszuschließen.%n"
						+ "		Sobald es einem Spielball erlaubt ist das Tor zu passieren, erhält%n"
						+ "		er eine farbige Markierung (weißer kleiner Punkt im Ballzentrum)%n"
						+ "Es gibt 5 unterschiedliche Level, das erste noch ohne Hindernis, ab dem zweiten kommt%n"
						+ "jeweils eine weiteres hinzu.%n"
						+ "Sobald alle 5 Level durchgespielt sind beginnt der Level-Zyklus von vorn, %n"
						+ "diesmal aber mit mehr Bällen.", MAX_ALLOWED_WALL_CONTACTS),
				"Spielkonzept",
				JOptionPane.INFORMATION_MESSAGE);
		startTimer();
	}

	public void closeGame(){
		magnetBalls.clear();
		magnetBalls = null;
		targetBalls.clear();
		targetBalls = null;
		obstacleBalls.clear();
		obstacleBalls = null;
		borderBalls.clear();
		borderBalls = null;
		
		if(ballinThrd != null){
			ballinThrd.stop();
			ballinThrd = null;
		}
//		timer.stop();
//		timer = null;
		// ich arbeite mit mehreren listenern, und das mittels anonymer klassen.
		// um diese nicht mittels variablen zwischenspeichern zu muessen, werden hier die
		// verwendeten listener ganz allgemein ermittelt und entfernt:
		for(ComponentListener list: inFrame.getComponentListeners()){
			inFrame.removeComponentListener(list);
		}
		for(KeyListener key: inFrame.getKeyListeners()){
			inFrame.removeKeyListener(key);
		}
		for(MouseListener mouse: inFrame.getMouseListeners()){
			inFrame.removeMouseListener(mouse);
		}
		for(MouseMotionListener mouse: inFrame.getMouseMotionListeners()){
			inFrame.removeMouseMotionListener(mouse);
		}
		inFrame = null;
	}

	private void initFullSnakeGamePanel(boolean startTimer){	
		// bei jedem level-start muessen die baelle neu erstellt und gesetzt werden. dazu muss der alte schrott
		// erstmal beseitigt werden:
		magnetBalls.clear();
		targetBalls.clear();
		gates.clear();
		borderBalls.clear();
		
		// spielfeld-dimensionen ermitteln:
		revalidateDimensions();
		
		// die tore/gates erstellen:
		createGates();
		
		// die sieplbaelle und hindernisse erstellen:
		setBalls();
		setObstacles();
		
		if(startTimer)
			startTimer();
	}

	private void startTimer(){
		// die move-dates werden benoetigt, um die spielballgeschw. in abhaengigkeit der verstrichenen zeit seit
		// des letzten paint-aufrufs korrekt anwenden zu koennen:
		levelStartTime = lastMoveDate =  System.currentTimeMillis();
		if(!isClient){
			ballinThrd = new BallinRunnable(gameSpeed, ()->{nextStep();});
			ballinThrd.start();
//			timer = new Timer(gameSpeed, this); // timer is calling action events every 400ms, in this case actionPerformed() method
//			timer.start(); 
		}
	}
	private void createGates(){
		// am anfang gibt es nur ein tor, kommen mehr baelle in spaeteren levels hinzu,
		// steigt die anzahl der gates auf maximal 4:
		int gateCount = Math.min((int)(levelInfo.LEVEL / levelInfo.NUM_OF_LEVELS)+1, 4);
		for(int i=0; i < gateCount; i++){
			Gate gate = new Gate(i);
			gates.add(gate);
			borderBalls.addAll(createBorderBalls(i));
		}
		if(isServer){
			sendGatesToClients();
		}
	}
	private List<Ball> createBorderBalls(int i){
		Rectangle rect = getBorderRect(gates.get(i));
		Point p1 = null, p2 = null;
		double radius = 0d;
		switch(i){
		case 0:
		case 2:
			p1 = new Point(rect.getMinX(), rect.getMinY() + rect.getHeight()*0.5);
			p2 = new Point(rect.getMaxX(), rect.getMinY() + rect.getHeight()*0.5);
			radius = rect.getHeight()*0.9;
			break;
		case 1:
		case 3:
			p1 = new Point(rect.getMinX() + rect.getWidth()*0.5, rect.getMinY());
			p2 = new Point(rect.getMinX() + rect.getWidth()*0.5, rect.getMaxY());
			radius = rect.getWidth()*0.9;
			break;
		}
		if(p1 != null){
			return Arrays.asList(
					new Ball(p1, new Vector(0,0), Double.POSITIVE_INFINITY, radius),
					new Ball(p2, new Vector(0,0), Double.POSITIVE_INFINITY, radius)
					);
		}
		return null;
	}
	private void setBalls(){
		// magnet- und targetbaelle erstellen: es gibt 5 untersch. levels. wenn alle durchlaufen sind, kommt jeweils
		// ein weiterer magnet- und target-ball hinzu:
		int levelDiv = (int)(levelInfo.LEVEL / levelInfo.NUM_OF_LEVELS);
		int targetRad = (int)(courtRect.width * levelInfo.TARGET_BALL_REL_SIZE);
		
		int gateCounter = 0;
		
		Collection<Player> players = this.playersMap.values();
		for(Player player: players){
			addPlayerBalls(player);
		}
		
		for(int i=0; i < levelDiv+1; i++){
			// magnetBall erstellen:
			int gateId = gateCounter++ % gates.size();
			// zielball erstellen:
			TargetBall tarBall = (TargetBall)createBall(new TargetBall(new Vector( 90.0,-80.0), 5d, targetRad, gates.get(gateId)));
			targetBalls.add(tarBall);
		}
	}
	private void addPlayerBalls(Player player){
		int levelDiv = (int)(levelInfo.LEVEL / levelInfo.NUM_OF_LEVELS);
		int playerRad = (int)(courtRect.width * levelInfo.MAGNET_BALL_REL_SIZE);
		double ballOffs = 10;
		
		for(int i=0; i < levelDiv+1; i++){
			MagnetBall ball    = (MagnetBall)createBall(new MagnetBall(new Point(centX, rad+ballOffs), 
					new Vector(120.0,50.0), 5d, playerRad, player));
			magnetBalls.add(ball);
		}
	}
	private Ball createBall(Ball ball){
		boolean ballIsValid = true;
		double rad = ball.getRadius();
		
		// den ball im Spielfeld zufaellig platzieren und sicherstellen, dass der neue ball
		// nicht in einem anderen Ball oder einem Hindernis steckt
	    // wenn innerhalb von 50 schleifendurchlaeufen ein ball nicht korrekt platziert werden konnte, dann halt mit kollison...
		// der kollisions-algorithmus bricht deshalb nicht zusammen, macht also keine ernsthaften probleme und faellt eh nicht 
		// wirklich auf... denn wenn ein ball in einem anderen steckt driften sie entweder aufgrund der bewegungsrichtung
		// sofort auseinander oder falls nicht, wird ein zusammenstoss berechnet und daraufhin bewegen sich sich auseinander,
		// wodruch die baelle niemals "verkeilt" bleiben
		int loopCounter = 0;
		do{
			// zufaellige startposition des balls innerhalb des spielfelds berechnen:
			double x = courtRect.x + rad + Math.random()*(courtRect.width -2*rad);
			double y = courtRect.y + rad + Math.random()*(courtRect.height-2*rad);
			ball.setCenter(new Point(x,y));
			// ermitteln, ob neuer ball mit einem der bestehenden baelle oder einem hindernis zusammenstoesst:
			for(Ball b: magnetBalls){
				if (ballsIntersect(ball, b)){
					ballIsValid = false;
					break;
				}
			}
			if(ballIsValid){
				for(Ball b: targetBalls){
					if (ballsIntersect(ball, b)){
						ballIsValid = false;
						break;
					}
				}
			}
			if(ballIsValid){
				for(Ball b: obstacleBalls){
					if (ballsIntersect(ball, b)){
						ballIsValid = false;
						break;
					}
				}
			}
		}while(!ballIsValid && loopCounter++ < 50);
		// dem validen ball eine zufaellige beschleunigung geben:
		double velX = Math.random() * 120 * (Math.random() > 0.5 ? -1 : 1);
		double velY = Math.random() * 120 * (Math.random() > 0.5 ? -1 : 1);
		ball.setVelocity(new Vector(velX,velY));
		
		return ball;
	}
	private boolean ballsIntersect(Ball ball1, Ball ball2){
		// berechnen ob ein ball mit einem anderen ball kollidiert: dazu wird ermittelt, ob die summe
		// beider radien kleiner als die distanz zwischen den ballzentren ist:
		return ball1.getCenter().getVector(ball2.getCenter()).getLength() < ball1.getRadius() + ball2.getRadius();
	}
	private void setObstacles(){
		// hindernisse am anfang eines jeden levels setzen: es gibt 5 untersch. Levels.
		// im ersten Level gibt es noch kein hindernis, dann kommt immer eins hinzu, also maximal 4:
		obstacleBalls.clear();
		int levelID = levelInfo.LEVEL % levelInfo.NUM_OF_LEVELS;	
		double obstRad = courtRect.width * levelInfo.OBSTACLE_REL_SIZE;
		
		if(levelID == 0){
			// gar kein obstacle setzen
		}
		else if(levelID == 1){
			ObstacleBall obsticleBall = new ObstacleBall(new Point(centX, centY),  new Vector( 0.0,0.0), Double.POSITIVE_INFINITY, obstRad);
			obstacleBalls.add(obsticleBall);
		}else{
		
			double angle = 0d;
			double vecLength = Math.sqrt( Math.pow(courtRect.width*0.25, 2) * 2);
			
			for(int i=0; i < levelID; i++){
				Point obstacelCenter = new Point(centX, centY).add(new Point(
						Math.cos(Math.toRadians(angle)) * vecLength,
						Math.sin(Math.toRadians(angle)) * vecLength
						)).toPoint();
				ObstacleBall obsticleBall = new ObstacleBall(obstacelCenter,  new Vector( 0.0,0.0), Double.POSITIVE_INFINITY, obstRad);
				obstacleBalls.add(obsticleBall);
				angle += 360d/levelID;
			}
		}
	}

	private long lastMoveDate;
	private void nextStep(){
		if(!isClient){ 	// wenn es ein client ist, wird das repainting immer nur dann vorgenommen, wenn der server
						// die neuen baelle geschickt hat
			// nach jedem step-aufruf werden die baelle bewegt. 
			// innerhalb der ball.moveBall(...)-Methode wird geprueft, ob ein Ball mit einer 
			// bande kollidiert und dann entsprechend seine bewegungsrichtung angepasst:
			long curTime = System.currentTimeMillis();
			long timeSinceLastMove = curTime - lastMoveDate;
			
			// die werte MAX_MAGNET_BALL_SPEED, MAX_TARGET_BALL_SPEED und MAGNET_FORCE sind absolut, muessen
			// an die aktuelle fentsergroesse angepasste werden, da diese veraenderbar ist
			double factor = 500d;
			double abs_MAX_MAGNET_BALL_SPEED = (double)levelInfo.MAX_MAGNET_BALL_SPEED * courtRect.width / factor;
			double abs_MAX_TARGET_BALL_SPEED = (double)levelInfo.MAX_TARGET_BALL_SPEED * courtRect.width / factor;
			double abs_MAGNET_FORECE = (double)levelInfo.MAGNET_FORCE * courtRect.width / factor;
	
			for(MagnetBall ball: magnetBalls){
				ball.moveBall(timeSinceLastMove, courtRect, ball.player.playerAngle, abs_MAGNET_FORECE, abs_MAX_MAGNET_BALL_SPEED);
			}
			for(TargetBall ball: targetBalls){
				ball.moveBall(timeSinceLastMove, courtRect, 0, 0, abs_MAX_TARGET_BALL_SPEED, MAX_ALLOWED_WALL_CONTACTS);
			}
			if(levelInfo.LEVEL > 1){ // macht erst ab dem zweiten Level sinn!!
				Point gameCenter = this.getGameCenter();
				for(ObstacleBall ball: obstacleBalls){
					ball.moveObstacle(gameCenter, levelInfo.OBSTACLE_SPEED);
				}
			}
			lastMoveDate = curTime;
			
			// pruefen ob magnet &/ targetbaelle miteinander kollidieren
			checkForCollisions();
			checkFortargetBallsInGate(); // pruefen ob die baelle mit einem hindernis kollidieren
			
			myPanel.repaint();
			
			// wenn alle targetbaelle in ihrem jew. gate stecken, ist das level erfolgreich beendet:
			if(checkIfAllTargetBallsHitTheirGates()){
				nextLevel();
			}
			
			if(isServer){
				// Kopien der baelle und der player (player wegen deren playerAngles (winkel des magneten) den Clients schicken
				List<MagnetBall> magnetBallsCpy = new ArrayList<>(magnetBalls.size());
				for(MagnetBall ball: magnetBalls){
					magnetBallsCpy.add(ball.createCopy());
				}
				
				List<TargetBall> targetBallsCpy = new ArrayList<>(targetBalls.size());
				for(TargetBall ball: targetBalls){
					targetBallsCpy.add(ball.createCopy());
				}
				List<ObstacleBall> obstacleBallsCpy = new ArrayList<>(obstacleBalls.size());
				for(ObstacleBall ball: obstacleBalls){
					obstacleBallsCpy.add(ball.createCopy());
				}
				List<Player> players = new ArrayList<>(playersMap.values().size());
				for(Player player: playersMap.values()){
					players.add(player.createCopy());
				}
				sendObjectsToClients(new ServerToClientObj(magnetBallsCpy, targetBallsCpy, obstacleBallsCpy,
						players, 
						new Rectangle(courtRect.x, courtRect.y, courtRect.width, courtRect.height),
						getGameCenter()));
			}
		}
	}
	
	private void nextLevel(){
		if(ballinThrd != null){
			ballinThrd.stop();
			ballinThrd = null;
		}
//		timer.stop();
		
		String message = String.format("Congratulations - You won Level %s!!!%n", levelInfo.LEVEL+1);
		if(isServer){
			this.sendObjectsToClients(new MessageServerObj(message));
		}
		JOptionPane.showMessageDialog(myPanel, message);
		
		setLevel(levelInfo.LEVEL+1);
	}
	private void setLevel(int levelId){
		if(ballinThrd != null){
			ballinThrd.stop();
			ballinThrd = null;
		}
//		if(timer.isRunning()){
//			timer.stop();
//		}
		
		for(Player player: playersMap.values()){
			player.scored = 0;
		}
		// eventuell ein Spielende setzen ab LEVEL > 50 z.B., aber wozu eigentlich, wird ja immer verrückter...
		// einziges Problem: wenn zu viele bälle mal auf ein gate kommen, wird das irgendwann "verstopft" und lässt keine
		// weiteren mehr ins gate gelangen wodurch das Level praktisch nicht mehr beendet werden kann. Aber ich habe es 
		// mit Level 50 gespielt und sogar in diesem hohen Level funktionierts noch
//		if(levelInfo.LEVEL == 50){
//			JOptionPane.showMessageDialog(myPanel, String.format("Congratulations - You have successfully finished the Game!!!%n"
//					+ "Lets start all over again..."));
//			levelInfo.LEVEL = 0;
//		}else{
			levelInfo.LEVEL = levelId;
//		}
			
		if(isServer){
			sendLevelInfoToClients();
		}
		initFullSnakeGamePanel(true);
	}
	private boolean checkIfAllTargetBallsHitTheirGates(){
		// pruefen ob alle target-baelle im hindernis stecken:
		for(Ball ball: targetBalls){
			if(!Double.isInfinite(ball.getMass()))
				return false;
		}
		return true;
	}
	
		boolean collisionDetected = false;
	private void checkForCollisions(){
		// hier wird nach jedem spielzug geprueft, ob die spielbaelle mit irgendetwas kollidieren und
		// falls ja, gleich die daraus folgenden konsequenzen determiniert:
		for(int i=0; i < magnetBalls.size(); i++){
			for(int j=0; j < magnetBalls.size(); j++){
				if (j!=i){
					if (magnetBalls.get(i).collidation(magnetBalls.get(j))){
						collisionDetected = true;
					}
				}
			}
			for(int j=0; j < targetBalls.size(); j++){
				if (magnetBalls.get(i).collidation(targetBalls.get(j))){
					// kollidiert ein magnetball mit einem targetball, werden die targetball-eigenschaften
					// so gesetzt, dass es ihm erlaubt wird in sein gate zu steuern:
					targetBalls.get(j).hitPlayer(targetBalls.get(j).hitPlayer()+1);
					targetBalls.get(j).hitWall(0);
					targetBalls.get(j).isAllowedToPassGate(true);
					targetBalls.get(j).setPlayer(magnetBalls.get(i).getPlayer()); // damit man weiß, von wem der Ball zuletzt beruehrt wurde
					collisionDetected = true;
				}
			}
			for(int j=0; j < obstacleBalls.size(); j++){
				if (magnetBalls.get(i).collidation(obstacleBalls.get(j))){
					collisionDetected = true;
				}
			}
			for(int j=0; j < borderBalls.size(); j++){
				if(magnetBalls.get(i).collidation(borderBalls.get(j))){
					collisionDetected = true;
				}
			}
		}
		for(int i=0; i < targetBalls.size(); i++){
			for(int j=0; j < targetBalls.size(); j++){
				if (i != j && targetBalls.get(i).collidation(targetBalls.get(j))){
					collisionDetected = true;
				}
			}
			for(int j=0; j < obstacleBalls.size(); j++){
				if (targetBalls.get(i).collidation(obstacleBalls.get(j))){
					collisionDetected = true;
				}
			}
			for(int j=0; j < borderBalls.size(); j++){
				if (targetBalls.get(i).collidation(borderBalls.get(j))){
					collisionDetected = true;
				}
			}
		}
	}
	private void checkFortargetBallsInGate(){
		// hier wird geprueft, ob ein target-ball in seinem gate angekommen ist. dabei verwende ich folgenden trick:
		// wenn ein targetBall im gate angekommen ist, wird er aus dem spiel genommen, indem er eine Masse
		// von unendlich erhaelt (sozusagen zum schwarzen loch wird) und zudem seine beweungsgeschw. auf null gesetzt wird:
		for(TargetBall target: targetBalls){
			if( target.isAllowedToPassGate() &&
					!Double.isInfinite(target.getMass())){
				for(Gate gate : gates){
					if( target.getGate() == gate && ballHitsGate(target, gate)){
						target.setMass(Double.POSITIVE_INFINITY);
						target.setVelocity(new Vector(0d,0d));
						if(target.player != null){ // target.player kann eig. nicht null sein!
							if(playersMap.containsKey(target.player.playerId)){
								playersMap.get(target.player.playerId).scored++;
							}
						}
					}
				}
			}
		}
	}
	private boolean ballHitsGate(Ball ball, Gate gate){
		// checken, ob ein targetball seine gate beruehrt
	    Rectangle rect = getBorderRect(gate);

	    double distX = Math.abs(ball.getCenter().x - rect.x-rect.width /2);
	    double distY = Math.abs(ball.getCenter().y - rect.y-rect.height/2);
	    
	    if (distX > (rect.width /2 + ball.getRadius())) { return false; }
	    if (distY > (rect.height/2 + ball.getRadius())) { return false; }

	    if (distX <= (rect.width/2)) { return true; } 
	    if (distY <= (rect.height/2)) { return true; }

	    double dx=distX-rect.width/2;
	    double dy=distY-rect.height/2;
	    return (dx*dx+dy*dy<=(ball.getRadius()*ball.getRadius()));
	}

	public GameScores getScores() {
		return GameScores.createGameScores(new String[]{"Time elapsed"}, 
				new int[]{(int)((System.currentTimeMillis()-levelStartTime)/1000)});
	}


	public String getName() {
		return "Ballin'";
	}


//	@Override
//	public void actionPerformed(ActionEvent arg0) {
//		nextStep();
//	}


	private void movePlayer(Point e){
		// den magneten des spielers in aghaengigkeit der maus-position (Point e) setzen.
		// die magnetposition ist dabei ein winkel zwischen 0 und 360:
		double centX = getGameCenter().x,
			   centY = getGameCenter().y;
		
		if(e.x != centX && e.y != centY){ // da sonst der playerAngle == Double.NaN!!
			Vector mouseVect = getGameCenter().getVector(new Point(e.x, e.y));
			double playerAngle = getGameCenter().getVector(new Point(centX+100, centY)).getAngle(mouseVect);
			if(e.y < centY){
				playerAngle = 360-playerAngle;
			}
			if(playersMap.containsKey(PLAYER_ID)){
				playersMap.get(PLAYER_ID).playerAngle = playerAngle;
			}
		}
		if(isClient && playersMap.containsKey(PLAYER_ID)){
			sendObjectsToServer( new ClientToServerObj(playersMap.get(PLAYER_ID).playerAngle) );
		}
	}
	private Point getGameCenter(){
		return new Point(gameArea.x + gameArea.width *0.5,
						 gameArea.y + gameArea.height*0.5);
	}
	
	public void paintGameArea(Graphics2D g){
		
		g.setRenderingHint(
	            RenderingHints.KEY_ANTIALIASING,
	            RenderingHints.VALUE_ANTIALIAS_ON);
		
		// die flaeche komplett schwarz zeichnen:
		g.setColor(new Color(0,0,0));
		g.fillRect(gameArea.x, gameArea.y, gameArea.width, gameArea.height);

		// das spielfeld zeichnen:
		// 1. den magnetring:
		Color borderColor = new Color(50,50,50);
		g.setColor(borderColor);
	    g.fill(createRingShape(centX, centY, rad, rad*innerCircFact));
	    
	    // die magneten des eigenen spielder und der mitspieler zeichnen:
	    synchronized(playersMap){
	    	Collection<Player> players = this.playersMap.values();
	    	synchronized(players){
	    		Iterator<Player> i = players.iterator();
	    		Player ownPlayer = null;
	    		while(i.hasNext()){
	    			Player player = i.next();
	    			if(player.playerId != PLAYER_ID){ // erstmal die anderen player zeichnen, erst zuletzt den eigenen magneten zeichen, damit der eigene magnet nicht von den anderen magneten ueberzeichnet wird
		    			double playerAngle = player.playerAngle;
						g.setColor(playerColors[player.playerId % playerColors.length]);
						double startAngle = ((360-playerAngle)-this.PLAYER_ANGLE_WIDTH*0.2) %360;
					    g.fill(createRingShape(centX, centY, rad, rad*innerCircFact, startAngle, PLAYER_ANGLE_WIDTH));
	    			}else{
	    				ownPlayer = player;
	    			}
	    		}
	    		if(ownPlayer != null){
					g.setColor(Color.RED);
					double startAngle = ((360-ownPlayer.playerAngle)-this.PLAYER_ANGLE_WIDTH*0.2) %360;
				    g.fill(createRingShape(centX, centY, rad, rad*innerCircFact, startAngle, PLAYER_ANGLE_WIDTH));
	    		}
	    	}
	    }
		
	    // 2. den court zeichnen:
	    g.setColor(borderColor);
		g.fill( createRectShape(centX, centY, courtRect.width*(1+innerCircFact),  courtRect.width*(1+innerCircFact), innerCircFact) );
		
		
		// spielbaelle, hindernisse und gates zeichen:
		for(MagnetBall ball: magnetBalls){
			if(ball.player.playerId == PLAYER_ID){
				g.setColor(Color.RED);
			}else{
				g.setColor(playerColors[ball.player.playerId % playerColors.length]);
			}
			g.fill(ball.createShape());
		}
		
		for(TargetBall ball: targetBalls){
			g.setColor(Double.isInfinite(ball.getMass()) ? borderColor : ball.getGate().getColor());
			g.fill(ball.createShape());
			if(!Double.isInfinite(ball.getMass()) && ball.isAllowedToPassGate()){
				g.setColor(new Color(255,255,255));
				g.fill(Ball.createShape(ball.getCenter(),  ball.getRadius()*0.3));
			}
		}
		g.setColor(borderColor);
		for(Ball obsticle: obstacleBalls){
			g.fill(obsticle.createShape());
		}
		
		int i=0;
		synchronized(gates){
			for(Gate gate: gates){
				g.setColor(gate.getColor());
				g.fill(getBorderRect(gate));
				g.setColor(borderColor);
				g.fill(borderBalls.get(i*2).createShape());
				g.fill(borderBalls.get(i*2+1).createShape());
				i++;
			}
		}
		
		// das Spielzentrum fuer den spieler sichtbar machen, damit er besser den maus-cursor darum bewegen kann
		g.setColor(new Color(30,30,30));
		g.fill(Ball.createShape(new Point(centX, centY), 2));
	}
	
	private Rectangle getBorderRect(Gate gate){
		// hier wird das rectangle eines der 4 gates berechnet. Ein gate hat die Form eines Rectangles
		// und das Rectangle wird benoetigt um es 1. zu zeichnen und 2. zu berechnen, ob ein targetBall
		// damit kollidiert ist:
		switch(gate.borderID){
			case 0:
			case 2:
				double xStart = courtRect.x +  courtRect.width*gate.pos -  courtRect.width*gate.width*0.5;
				if(xStart < courtRect.x)
					xStart = courtRect.x;
				
				double xEnd   = courtRect.x +  courtRect.width*gate.pos +  courtRect.width*gate.width*0.5;
				if(xEnd > courtRect.x + courtRect.width)
					xEnd = courtRect.x + courtRect.width;
				
				int yStart = (gate.borderID == 0) ? (int)(courtRectOuter.getY()) : (int)(courtRect.getY()+courtRect.height-1);
				
				return new Rectangle((int)xStart,  yStart, (int)(xEnd-xStart), (int)(courtRect.getY()-courtRectOuter.getY()+1));
			case 1:
			case 3:
				yStart = (int)(courtRect.y +  courtRect.height*gate.pos - courtRect.height*gate.width*0.5);
				if(yStart < courtRect.y)
					yStart = courtRect.y;
								
				double yEnd = courtRect.y +  courtRect.height*gate.pos +  courtRect.height*gate.width*0.5;
				if(yEnd > courtRect.y + courtRect.height)
					yEnd = courtRect.y + courtRect.height;
				
				xStart = (gate.borderID == 3) ? (int)(courtRectOuter.x) : (int)(courtRect.x+courtRect.width-1);
				
				return new Rectangle((int)xStart,  yStart, (int)(courtRect.getX()-courtRectOuter.getX()+1), (int)(yEnd-yStart));
		}
		return null;
	}
	
	// helfer-Methoden um das rectangle und die ringe zu zeichnen:
	private static Rectangle2D.Double createRectangle(double centerX, double centerY, double width, double height){
		return new Rectangle2D.Double(centerX-width*0.5, centerY-height*0.5, width, height);
	}
	private static Shape createRectShape(double centerX, double centerY, double width, double height, double thickness){
		Rectangle2D outer = createRectangle(centerX, centerY, width, height);
		Rectangle2D inner = createRectangle(centerX, centerY, width*(1-thickness), height*(1-thickness));
		Area area = new Area(outer);
        area.subtract(new Area(inner));
        return area; 
	}
	private static Shape createRingShape(
	        double centerX, double centerY, double outerRadius, double thickness){
        return createRingShape(centerX, centerY, outerRadius, thickness, 0, 360);
    }
	private static Shape createRingShape(
	        double centerX, double centerY, double outerRadius, double thickness, double angle1, double angle2){
		Arc2D outer = new Arc2D.Double(
            centerX - outerRadius, 
            centerY - outerRadius,
            outerRadius + outerRadius, 
            outerRadius + outerRadius,
            angle1, angle2,
            Arc2D.PIE);
		Arc2D inner = new Arc2D.Double(
            centerX - outerRadius + thickness, 
            centerY - outerRadius + thickness,
            outerRadius + outerRadius - thickness - thickness, 
            outerRadius + outerRadius - thickness - thickness,
            angle1, angle2,
            Arc2D.PIE);
        Area area = new Area(outer);
        area.subtract(new Area(inner));
        return area;
    }


	public void paintControlArea(Graphics2D g) {
    	g.setColor(new Color(30,30,30));
		g.fill(this.controlArea);
		
		String[] strings = new String[]{
				String.format("Level: %s", levelInfo.LEVEL+1),
				String.format("magnet max speed: %s   |   target max speed: %s  |"
						+ "  magnet size: %s   |   target size: %s   |   obstacle size: %s", 
						levelInfo.MAX_MAGNET_BALL_SPEED, levelInfo.MAX_TARGET_BALL_SPEED,
						levelInfo.MAGNET_BALL_REL_SIZE, levelInfo.TARGET_BALL_REL_SIZE, levelInfo.OBSTACLE_REL_SIZE),
		        String.format("magnet force: %s", levelInfo.MAGNET_FORCE),
		};
		
        Font myBaseFont = g.getFont();
        int firstHeight = controlArea.y + controlArea.height / 2 + myBaseFont.getSize() / 2;
        int heightSteps = (int) (1.2 * myBaseFont.getSize());
        if (strings.length > 1) { // more than one line of scores to paint
            firstHeight -= (int) (((strings.length - 1) / 2.) * heightSteps);
        }
		g.setColor(new Color(180,180,180));
        
        for (int i = 0; i < strings.length; i++) {
            g.drawString(strings[i], controlArea.x + controlArea.width / 20, firstHeight + i * heightSteps);
        }
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
//	-----------------------------------------------------------------------------------------------------------
	
	// folgender Code handelt das multiplayer-verhalten über das netzwerk:
	// Naeheres dazu bitte der angehaengten PDF entnehmen!!!
	
	private Server server;
	private Client client;
	private boolean isServer = false;
	private boolean isClient = false;
	private HashMap<Integer, Player> playersMap = new HashMap<>();
	
	private String SERVER_NAME = "localhost";//"rotkaeppchen";
	private int PORT = 6066;
	
	private Color[] playerColors = new Color[]{
			new Color(0,50,0),
			new Color(0,0,50),
			new Color(50,0,50),
			new Color(50,20,20)
	};
	
	public static class ServerToClientObj implements Serializable{
		private static final long serialVersionUID = -5133262494235211178L;
		private List<MagnetBall> magnetBalls;
		private List<TargetBall> targetBalls;
		private List<ObstacleBall> obstacleBalls;
		private List<Player> players;
		private Rectangle serverCourtRect;
		private Point serverCenter;
		private ServerToClientObj(List<MagnetBall> magnetBalls, List<TargetBall> targetBalls,
				List<ObstacleBall> obstacleBalls,
				List<Player> players, Rectangle serverCourtRect, Point serverCenter){
			this.magnetBalls = magnetBalls;
			this.targetBalls = targetBalls;
			this.obstacleBalls = obstacleBalls;
			this.players = players;
			this.serverCourtRect = serverCourtRect;
			this.serverCenter = serverCenter;
		}
	}
	private static class ClientToServerObj implements Serializable{
		private static final long serialVersionUID = -3444790431699111949L;
		//		private final int playerId;
		private double playerAngle;
		private ClientToServerObj(double playerAngle){
			this.playerAngle = playerAngle;
//			this.playerId = playerId;
		}
	}

	public static class NewLevelServerObj implements Serializable{
		private static final long serialVersionUID = 3095372922149432638L;
		LevelInfo levelInfo;
		
		private NewLevelServerObj(LevelInfo levelInfo){
			this.levelInfo = levelInfo;
		}
	}
	private static class NewGatesServerObj implements Serializable{
		private static final long serialVersionUID = 65078448765518146L;
		private final List<Gate> gates;
		private final List<Ball> borderBalls;
		private NewGatesServerObj(List<Gate> gates, List<Ball> boderBalls){
			this.gates = gates;
			this.borderBalls = boderBalls;
		}
	}
	private static class PlayersServerObj implements Serializable{
		private static final long serialVersionUID = 3758340804042950445L;
		private List<Player> players;
		private PlayersServerObj(List<Player> players){
			this.players = players;
		}
		
	}
	private static class MessageServerObj implements Serializable{
		private static final long serialVersionUID = 2302070328266477550L;
		private String message;
		private MessageServerObj(String message){
			this.message = message;
		}
	}
	
	private void createServer(){
		try {
			if(isServer)
				if(server != null){
					server.close();
				}
				server = new Server(PORT, 
					new ServerListener(){
						@Override
						public void receiveClientObject(Object obj, int clientId){
							receiveObjectsFromClient(obj, clientId);
						}
						@Override
						public void clientAdded(int clientId){
							System.out.println("clientAdded!");
							Player player = new Player(clientId);
							playersMap.put(clientId, player);
							addPlayerBalls(player);
							sendLevelInfoToClients();
							sendGatesToClients();
							sendPlayersToClients();
						}
						@Override
						public void clientRemoved(int clientId){
							synchronized(playersMap){
								playersMap.remove(clientId);
								removePlayersMagnetBalls(clientId);
								sendPlayersToClients();
							}
						}
					}
			);
			isServer = true;
		} catch (IOException e) {
			System.err.printf("could not create server!%n");
//			e.printStackTrace();
		}
	}
	private void removePlayersMagnetBalls(int clientId){
		synchronized(magnetBalls){
			Iterator<MagnetBall> i = magnetBalls.iterator();
			while (i.hasNext()) {
			   MagnetBall ball = i.next(); // must be called before you can call i.remove()
			   if(ball.player.playerId == clientId){
				   i.remove();
			   }
			}
		}
	}
	private void createClient(JFrame frame){
		if(client != null)
			client.close();
		
		try{
			String server_name = JOptionPane.showInputDialog(frame, 
		    		String.format("Servername:"));
		    if(server_name != null){
		    	SERVER_NAME = server_name;
		    }
			client = new Client(SERVER_NAME, PORT, 
					new ClientListener(){
						public void receiveServerObject(Object obj){
							receiveObjectsFromServer(obj);
						}
						public void setClientId(int client_id){
							PLAYER_ID = client_id;
						}
					}
			);
			if(ballinThrd != null){
				ballinThrd.stop();
				ballinThrd = null;
			}
//			if(timer.isRunning()){
//				timer.stop();
//			}
			
			isClient = true;
		}catch(java.net.UnknownHostException e){
    		System.err.printf("Client.UnknownHostException: failed to connect to server '%s' on port '%s'%n", 
    				SERVER_NAME, PORT);
    	}catch(IOException e) {
    		System.err.printf("Client.IOException: failed to connect to server '%s' on port '%s'-> isClient: %s%n", 
    				SERVER_NAME, PORT, isClient);
    	}
	}
	private void sendLevelInfoToClients(){
		NewLevelServerObj servObj = new NewLevelServerObj(levelInfo.createCopy());
		sendObjectsToClients(servObj);
	}
	private AtomicBoolean alreadyReceiving = new AtomicBoolean(false);
	private void receiveObjectsFromServer(Object obj){
		if(!alreadyReceiving.get()){
			alreadyReceiving.set(true);
			
			handleObjectRecvdFromSrvr(obj);
			
			synchronized(clientReceivingQueue){
				while(!clientReceivingQueue.isEmpty()){
					Object recvdObj = clientReceivingQueue.remove(0);
					handleObjectRecvdFromSrvr(recvdObj);
				}
			}
			alreadyReceiving.set(false);
		}else{
			synchronized(clientReceivingQueue){
				if( !(obj instanceof ServerToClientObj) ){
					clientReceivingQueue.add(obj);
				}
			}
		}
	}
	private void relativiseBallFromServer(Ball ball, Rectangle serverCourtRect){
		double serverWidth = serverCourtRect.width;
		ball.setRadius(ball.getRadius() * courtRect.width / serverWidth);
		ball.getVelocity().x *= courtRect.width / serverWidth;
		ball.getVelocity().y *= courtRect.width / serverWidth;
		Point p = new Point(
				(((ball.getCenter().x - serverCourtRect.x) / serverCourtRect.width) * courtRect.width) + courtRect.x,
				(((ball.getCenter().y - serverCourtRect.y) / serverCourtRect.height) * courtRect.height) + courtRect.y
				);
		ball.setCenter(p);
	}
	private AtomicBoolean clientIsAlrRepainting = new AtomicBoolean(false);
	private void repaintClient(){
		if(!clientIsAlrRepainting.get()){
			clientIsAlrRepainting.set(true);
			myPanel.repaint();
			clientIsAlrRepainting.set(false);
		}
	}
	private void handleObjectRecvdFromSrvr(Object obj){
		if(obj instanceof ServerToClientObj){
			if(!clientIsAlrRepainting.get()){
				ServerToClientObj servToClient = (ServerToClientObj)obj;
				magnetBalls = servToClient.magnetBalls;
				targetBalls = servToClient.targetBalls;
				obstacleBalls = servToClient.obstacleBalls;
				for(Ball ball: magnetBalls){
					relativiseBallFromServer(ball, servToClient.serverCourtRect);
				}
				for(Ball ball: targetBalls){
					relativiseBallFromServer(ball, servToClient.serverCourtRect);
				}
				for(Player player: servToClient.players){
					Player refPlayer = playersMap.get(player.playerId);
					if(refPlayer != null)
						refPlayer.playerAngle = player.playerAngle;
				}
				repaintClient();
			}
//			myPanel.repaint();
//			(new Thread(()->{myPanel.repaint();})).start();
		}
		else if (obj instanceof NewLevelServerObj){
			NewLevelServerObj levelObj = (NewLevelServerObj)obj;
			this.levelInfo = levelObj.levelInfo;
			this.setLevel(levelObj.levelInfo.LEVEL);
		}
		else if (obj instanceof NewGatesServerObj){
			NewGatesServerObj gatesObj = (NewGatesServerObj)obj;
			gates = gatesObj.gates;
			borderBalls = gatesObj.borderBalls;
		}
		else if (obj instanceof ClientIdServerObj){
			ClientIdServerObj idObj = (ClientIdServerObj)obj;
			PLAYER_ID = idObj.getClientId();
		}
		else if (obj instanceof PlayersServerObj){
			PlayersServerObj playersObj = (PlayersServerObj)obj;
			System.out.printf("received PlayersServerObj: size: %s%n", playersObj.players.size());
			HashMap<Integer, Player> playersMap = new HashMap<>();
			for(Player player: playersObj.players){
				playersMap.put(player.playerId, player);
			}
			synchronized(this.playersMap){
				this.playersMap = playersMap;
			}
		}
		else if (obj instanceof MessageServerObj){
			showDialogThreaded(((MessageServerObj)obj).message);
		}
	}
	private List<Object> clientReceivingQueue = Collections.synchronizedList(new ArrayList<Object>());
	
	private void sendGatesToClients(){
		List<Gate> gatesCpy = new ArrayList<>(gates.size());
		for(Gate gate: gates){
			gatesCpy.add(gate.createCopy());
		}
		List<Ball> boderBallsCpy = new ArrayList<>(borderBalls.size());
		for(Ball ball: borderBalls){
			boderBallsCpy.add(ball.createCopy());
		}
		sendObjectsToClients(new NewGatesServerObj(gatesCpy, boderBallsCpy));
	}
	private void sendPlayersToClients(){
		List<Player> playersCpy = new ArrayList<>(playersMap.values().size());
		for(Player player: playersMap.values()){
			playersCpy.add(player.createCopy());
		}
		sendObjectsToClients(new PlayersServerObj(playersCpy));
	}
	private void showDialogThreaded(String msg){
		(new Thread(()->{
			JOptionPane.showMessageDialog(myPanel, msg);
		})).start();
	}
	private void sendObjectsToServer(Object obj){
		if(client != null){
			client.sendObjectToServer(obj);
		}
	}
	private void receiveObjectsFromClient(final Object obj, final int clientId){
		if(obj instanceof ClientToServerObj){
			ClientToServerObj clientObj = (ClientToServerObj)obj;
			if(playersMap.containsKey(clientId)){
				playersMap.get(clientId).playerAngle = clientObj.playerAngle;
			}
		}
	}
	private AtomicBoolean alrSendingObjects = new AtomicBoolean(false);
	private void sendObjectsToClients(Object obj){
		if(!alrSendingObjects.get()){
			alrSendingObjects.set(true);
			if(server != null){
				server.sendObjectsToClients(obj);
			}
			synchronized(serverSendingQueue){
				while(!serverSendingQueue.isEmpty()){
					Object queueObj = serverSendingQueue.remove(0);
					if(server != null){
						server.sendObjectsToClients(queueObj);
					}
				}
			}
			alrSendingObjects.set(false);
		}else{
			synchronized(serverSendingQueue){
				if( !(obj instanceof ServerToClientObj) ){
					serverSendingQueue.add(obj);
				}
			}
		}
	}
	private List<Object> serverSendingQueue = Collections.synchronizedList(new ArrayList<Object>());
	
	private void closeClient(){
		if(isClient){
			client.close();
			client = null;
			isClient = false;
			setMultiplayerMenu();
			createSinglePlayer();
			initFullSnakeGamePanel(true);
		}
	}
	private void closeServer(){
		if(isServer){
			server.close();
			server = null;
			isServer = false;
			setMultiplayerMenu();
		}
	}
	
	public void setBalls(List<MagnetBall> magnetBalls, List<TargetBall> targetBalls, List<ObstacleBall> obstacleBalls){
		this.magnetBalls = magnetBalls;
		this.targetBalls = targetBalls;
		this.obstacleBalls = obstacleBalls;
	}
	// Ende des multiplayerspezifischen Codes
//	-----------------------------------------------------------------------------------------------------------
}
package ballin;

import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.io.Serializable;

import ballin.Ballin.Gate;

public class Ball implements Serializable{
	private static final long serialVersionUID = 3127954173146881697L;
	private Point cent; // ballzentrum
	private Vector vel; // bewegungsgeschwindigkeit
	private double mass;
	private double rad; // radius
	private boolean moveable = true;
	private int hitWall = 0;
	private int hitPlayer = 0;
	
	public Point getCenter(){return cent;}
	public void setCenter(Point cent){this.cent = cent;}
	public double getRadius(){return rad;}
	public void setRadius(double rad){this.rad = rad;}
	
	public Ball createCopy(){
		return new Ball(cent.createCopy(),
				vel.createCopy(), mass, rad, moveable, hitWall, hitPlayer);
	}
	private Ball(Point cent, Vector vel, double mass, double radius, 
			boolean moveable,int hitWall, int hitPlayer){
		this(cent, vel, mass, radius);
		this.moveable = moveable;
		this.hitWall = hitWall;
		this.hitPlayer = hitPlayer;
	}
	public Ball(Vector velocity, double mass, double radius){
		this.cent = new Point();
		this.vel = velocity;
		this.mass = mass;
		this.rad = radius;
	}
	public Ball(Point center, Vector velocity, double mass, double radius){
		this.cent = center;
		this.vel = velocity;
		this.mass = mass;
		this.rad = radius;
	}
	
//	private Point lastCent;
	protected void moveBall(double millisecs, Rectangle gameBorder, double playerAngle, double playerForce, double maxSpeed){
		Vector velForce = new Vector( playerForce * Math.cos(Math.toRadians(playerAngle)),
									  playerForce * Math.sin(Math.toRadians(playerAngle)) );
		
		if(	!Double.isNaN(velForce.x) && 
			!Double.isNaN(velForce.y) &&
			!Double.isInfinite(velForce.x) && 
			!Double.isInfinite(velForce.y)){
			
			vel.x += velForce.x;
			vel.y += velForce.y;
			
			double curSpeed = vel.getLength();
			if(curSpeed > maxSpeed && curSpeed != 0d){
				vel.x = vel.x * maxSpeed/curSpeed;
				vel.y = vel.y * maxSpeed/curSpeed;
			}
			
			boolean hitWall = false;
	
			cent.x += vel.x * millisecs/1000;
			cent.y += vel.y * millisecs/1000; 
			double rad = (int)this.rad; // der int-cast inst notwendig, da es pixel-genau berechnet werden muss und es keine halben pixel gibt...
			if(cent.x + rad > gameBorder.x + gameBorder.width){
				cent.x = gameBorder.x + gameBorder.width -rad;
				vel.x = -1*Math.abs(vel.x);
				hitWall = true;
			}else if (cent.x -rad < gameBorder.x){
				cent.x = gameBorder.x +rad;
				vel.x = Math.abs(vel.x);
				hitWall = true;
			}
			
			if(cent.y + rad > gameBorder.y + gameBorder.height){
				cent.y = gameBorder.y + gameBorder.height -rad;
				vel.y = -1*Math.abs(vel.y);
				hitWall = true;
			}else if (cent.y -rad < gameBorder.y){
				cent.y = gameBorder.y +rad;
				vel.y = Math.abs(vel.y);
				hitWall = true;
			}
			if(hitWall){this.hitWall++;}
//			if( vel.x > maxSpeed || vel.y > maxSpeed ||
//					(lastCent!= null && lastCent.getLength(cent) > 100) ){
//				System.out.printf("%nin I%n");
//				System.out.printf("gameBorder.X: %s	gameBorder.Y: %s	gameBorder.width: %sn", 
//						gameBorder.x, gameBorder.y, gameBorder.width);
//				System.out.printf("lastX: %s	lastY: %s%n", lastCent.x, lastCent.y);
//				System.out.printf("centX: %s	centY: %s%n", cent.x, cent.y);
//				System.out.printf("velX: %s	velY: %s%n%n", vel.x, vel.y);
//			}
//			if( lastCent != null && cent.x < gameBorder.x || cent.x > gameBorder.x + gameBorder.width||
//					cent.y < gameBorder.y || cent.y > gameBorder.y + gameBorder.width ){
//				System.out.printf("%nin II%n");
//				System.out.printf("gameBorder.X: %s	gameBorder.Y: %s	gameBorder.width: %sn", 
//						gameBorder.x, gameBorder.y, gameBorder.width);
//				System.out.printf("lastX: %s	lastY: %s%n", lastCent.x, lastCent.y);
//				System.out.printf("centX: %s	centY: %s%n", cent.x, cent.y);
//				System.out.printf("velX: %s	velY: %s%n%n", vel.x, vel.y);
//			}
//			lastCent = cent;
		}
	}
	
	public double cos(double degree){
		return Math.cos(Math.toRadians(degree));
	}
	public double sin(double degree){
		return Math.sin(Math.toRadians(degree));
	}
	
	public boolean collidation(Ball ball){
		Vector dist = this.cent.getVector(ball.cent);
		
		// gegeben 2 Bälle: 'this' und 'ball'
		// wenn die länge des distanzvektors zwischen this und ball < this.radius + ball.radius, sind die bälle miteinander kollidiert:
		if(dist.getLength() <= this.rad + ball.rad){
			// falls 2 bealle miteinander kollidieren, wird deren physikalischer effekt im folgenden berechnet
			// und ihre bewegungsrichtungen werden entsprechend angepasst
			// (haengen ab von den eingangs-bewegungsrichtungen und der massen der aufeinandertreffenden baelle):
		    Vector delta = this.cent.sub(ball.cent);
		    double d = delta.getLength();

		    Vector mtd = delta.scalarMul(((this.rad + ball.rad)-d)/d); 
		    
		    double im1 = 1d / this.mass; 
		    double im2 = 1d / ball.mass;
		    
		    cent = cent.toVector().add(mtd.scalarMul(im1 / (im1 + im2))).toPoint();
		    ball.cent = ball.cent.toVector().sub(mtd.scalarMul(im2 / (im1 + im2))).toPoint();
		    
		    Vector v = (this.vel.sub(ball.vel));
		    double vn = v.getDot(mtd.normalize());
		    
		    if (vn > 0.0f) return false;
		    
		    double restitution = 0d;
		    double i = (-(1.0d + restitution) * vn) / (im1 + im2);
		    Vector impulse = mtd.scalarMul(i);
		    
		    this.vel = this.vel.add(impulse.scalarMul(im1));
		    ball.vel = ball.vel.sub(impulse.scalarMul(im2));
		    return true;
		}
		return false;
	}
	
	public void setMoveable(boolean moveable){this.moveable = moveable;}
	public boolean isMoveable(){return moveable;}
	
	public int hitWall(){return hitWall;}
	public void hitWall(int hitWall){this.hitWall = hitWall;}
	public int hitPlayer(){return hitPlayer;}
	public void hitPlayer(int hitPlayer){this.hitPlayer = hitPlayer;}
	public double getMass(){return mass;}
	public void setMass(double mass){this.mass = mass;}
	public Vector getVelocity(){return vel;}
	public void setVelocity(Vector velocity){vel = velocity;}
	
	// createShape erzeugt ein swing-Shape-Objekt, das gezeichnet werden kann (wird von der Klasse Ballin verwendet)
	public Ellipse2D.Double createShape(){
		return createShape(cent, rad);
	}
	public static Ellipse2D.Double createShape(Point cent, double rad){
		return new Ellipse2D.Double(cent.x-rad, cent.y-rad, rad*2, rad*2);
	}
	
	@Override
	public String toString(){
		return String.format("Ball: Point[%s|%s], Velocity[%s|%s], mass[%s], radius[%s], isMoveable[%s], hitWall[%s], hitPlayer[%s]%n",
				cent.x, cent.y, vel.x, vel.y, mass, rad, moveable, hitWall, hitPlayer);
	}
}

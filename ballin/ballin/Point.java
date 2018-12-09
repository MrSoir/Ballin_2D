package ballin;

import java.io.Serializable;

public class Point implements Serializable{
	private static final long serialVersionUID = -6583840492048107239L;
	public int id; // test, kann eventuell wieder gel�scht werden
	public double x, y;
	public Point(){
		x = 0d;
		y = 0d;
	}
	public Point(Point p){
		x = p.x;
		y = p.y;
	}
	public Point(double x, double y){
		this.x = x;
		this.y = y;
	}
	
	public double getAngle(Point p){
		double angle = getVector(p).getAngle();
		if(p.y > y){
			angle = 360-angle;
		}
		return angle;
	}
	
	public Vector getVector(Point p){
		return new Vector(p.x - x  ,  p.y - y);
	}
	
	public double getLength(Point p){
		return Math.sqrt((x-p.x)*(x-p.x) + (y-p.y)*(y-p.y));
	}
	public double getLength(){
		return Math.sqrt(x*x + y*y);
	}
	
	public Vector add(Point v){
		return new Vector(x+v.x, y+v.y);
	}
	public Vector sub(Point v){
		return new Vector(x-v.x, y-v.y);
	}
	public Vector mul(Point v){
		return new Vector(x*v.x, y*v.y);
	}
	public Vector div(Point v){
		return new Vector(x/v.x, y/v.y);
	}
	
	public Vector toVector(){return new Vector(x,y);}
	
	public Point createCopy(){
		return new Point(x,y);
	}
	
	@Override
	public String toString(){
		return String.format("Point: x: %s	y: %s", x, y);
	}

}
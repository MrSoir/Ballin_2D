package ballin;

import java.io.Serializable;

public class Vector implements Serializable{
	private static final long serialVersionUID = -9150006119179595271L;
	double x, y;
	
	public Vector(double x, double y){
		this.x = x;
		this.y = y;
	}
	
	public double getLength(){
		return Math.sqrt(x*x + y*y);
	}
	public Vector normalize(){
		double len = getLength();
		if (len != 0.0f)
		{
			x /= len;
			y /= len;
		}
		else
		{
			x = 0d;
			y = 0d;
		}

		return this;
	}
	public double getAngle(Vector v){
		double angleVal = getDot(v) / (getLength() * v.getLength());
		double angle = Math.toDegrees(Math.acos(angleVal));
		
		return angle;
	}
	public double getAngle(){
		Vector v = new Vector(10, 0);
		double angleVal = getDot(v) / (getLength() * v.getLength());
		double angle = Math.toDegrees(Math.acos(angleVal));
		
		return Double.isNaN(angle) ? 0d : angle;
	}
	public double getDot(Vector v){
		return x*v.x + y*v.y;
	}
	public double getCross(Vector v){ // x => cross product == kreuzprodukt
		return x*v.y - v.x*y;
	}
	
	public Vector scalarMul(double val){
		return new Vector(x*val, y*val);
	}
	public Vector scalarDiv(double val){
		return new Vector(x/val, y/val);
	}
	public Vector scalarAdd(double val){
		return new Vector(x+val, y+val);
	}
	public Vector scalarSub(double val){
		return new Vector(x-val, y-val);
	}
	
	public Vector add(Vector v){
		return new Vector(x+v.x, y+v.y);
	}
	public Vector sub(Vector v){
		return new Vector(x-v.x, y-v.y);
	}
	public Vector mul(Vector v){
		return new Vector(x*v.x, y*v.y);
	}
	public Vector div(Vector v){
		return new Vector(x/v.x, y/v.y);
	}
	
	public Point toPoint(){return new Point(x,y);}
	
	public Vector createCopy(){return new Vector(x,y);}
}

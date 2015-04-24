package net.clonecomputers.lab.starburst;

public class Pair {
	public int x=0;
	public int y=0;
	
	public Pair(int newX, int newY) {
		x=newX;
		y=newY;
	}
	
	public Pair() { }
	
	public String toString() {
		return "("+x+", "+y+")";
	}
}

package net.clonecomputers.lab.starburst;

import static java.lang.Math.*;

import java.util.*;

public class PixelOperationsList {
	private double removeOrderBias = .5; // default
	private Pair[] operations;
	private boolean[] scheduled; // to prevent duplicates
	private int width;
	private long start;
	private long end;
	private Random myRandom;
	
	public PixelOperationsList(int width, int height) {
		operations = new Pair[width*height];
		scheduled = new boolean[width*height];
		this.width = width;
		start = 0;
		end = 0;
		myRandom = new Random();
	}
	
	private int length() {
		return (int)(end-start);
	}
	
	private int wrap(long i) {
		return (int)(i % operations.length);
	}
	
	public synchronized void setRemoveOrderBias(double removeOrder) {
		this.removeOrderBias = removeOrder;
	}
	
	public double getRemoveOrderBias() {
		return removeOrderBias;
	}
	
	public synchronized boolean hasPoint() {
		return start != end;
	}
	
	public synchronized void addPoint(Pair p) {
		if(scheduled[p.x + p.y*width]) {
			return; // duplicates are silently ignored
		}
		scheduled[p.x + p.y*width] = true;
		operations[wrap(end++)] = p;
		if(end >= start + operations.length) {
			throw new ArrayIndexOutOfBoundsException("exceeded storage space of backing array");
		}
	}

	public synchronized void addPoint(int x, int y) {
		addPoint(new Pair(x,y));
	}

	public synchronized Pair getPoint() {
		if(!hasPoint()) return null;
		Pair retval = null;
		double removePoint = removeOrderBias==0? 0: pow(myRandom.nextDouble(), log(removeOrderBias)/log(.5));
		int removeIndex = wrap(start + (int)(removePoint * length()));
		retval = operations[removeIndex];
		int replaceIndex = wrap(removePoint < .5? start++: --end);
		operations[removeIndex] = operations[replaceIndex];
		operations[replaceIndex] = null; // let the GC do it's work
		scheduled[retval.x + retval.y*width] = false;
		return retval;
	}

	public synchronized void clear() {
		while(start < end) {
			Pair toRemove = operations[wrap(start++)];
			scheduled[toRemove.x + toRemove.y*width] = false;
			operations[wrap(start++)] = null;
		}
	}
}

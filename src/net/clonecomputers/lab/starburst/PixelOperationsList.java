package net.clonecomputers.lab.starburst;

import java.util.*;

public class PixelOperationsList {
	private RemoveOrder removeOrder = RemoveOrder.RANDOM; // default
	private Pair[] operations;
	private long start;
	private long end;
	private Random myRandom;
	
	public enum RemoveOrder {
		RANDOM(0),
		FIRST(1),
		LAST(2);
		
		private int val;
		private RemoveOrder(int val) {
			this.val = val;
		}
		
		public int i() {
			return val;
		}
		
		public static RemoveOrder get(int i) {
			if(i == 2) return LAST;
			if(i == 1) return FIRST;
			if(i == 0) return RANDOM;
			
			if(Math.random() > .8) return FIRST;
			else return RANDOM;
		}
	}
	
	public PixelOperationsList(int maxSize) {
		operations = new Pair[maxSize*2];// bigger in case of threading issues
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
	
	public synchronized void setRemoveOrder(RemoveOrder removeOrder) {
		this.removeOrder = removeOrder;
	}
	
	public synchronized void setRemoveOrder(int removeOrder) {
		this.removeOrder = RemoveOrder.get(removeOrder);
	}
	
	public RemoveOrder getRemoveOrder() {
		return removeOrder;
	}
	
	public synchronized boolean hasPoint() {
		return start != end;
	}
	
	public synchronized void addPoint(Pair p) {
		operations[wrap(end++)] = p;
	}

	public synchronized void addPoint(int x, int y) {
		operations[wrap(end++)] = new Pair(x,y);
	}

	public synchronized Pair getPoint() {
		if(!hasPoint()) return null;
		Pair retval = null;
		switch(removeOrder) {
		case LAST:
			retval = operations[wrap(--end)];
			operations[wrap(end)] = null; // let the GC do it's work
			return retval;
		case RANDOM:
			int i = wrap(start + myRandom.nextInt(length()));
			retval = operations[i];
			operations[i] = operations[wrap(--end)];
			operations[wrap(end)] = null; // let the GC do it's work
			return retval;
		case FIRST:
			retval = operations[wrap(start)];
			operations[wrap(start++)] = null; // let the GC do it's work
			return retval;
		default:
			throw new InternalError(); // missing case in swtich
		}
	}
}

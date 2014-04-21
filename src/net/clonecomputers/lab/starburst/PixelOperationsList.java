package net.clonecomputers.lab.starburst;

import java.util.*;

public class PixelOperationsList {
	private RemoveOrder removeOrder;
	private List<Pair> operations;
	private Random myRandom;
	
	public enum RemoveOrder {
		RANDOM,
		FIRST,
	}
	
	public PixelOperationsList() {
		operations = new ArrayList<Pair>();//Collections.synchronizedList(new ArrayList<Pair>());
		myRandom = new Random();
	}
	
	public synchronized void setRemoveOrder(RemoveOrder removeOrder) {
		this.removeOrder = removeOrder;
	}
	
	public RemoveOrder getRemoveOrder() {
		return removeOrder;
	}
	
	public synchronized boolean hasPoint() {
		return !operations.isEmpty();
	}

	public synchronized void addPoint(int x, int y) {
		operations.add(new Pair(x,y));
	}

	public synchronized Pair getPoint() {
		if (!operations.isEmpty()) {
			int index = 0;
			if(removeOrder == RemoveOrder.RANDOM) {
				index = myRandom.nextInt(operations.size());
				//RANDOMFACTOR<0? myRandom.nextInt(operations.size()): 0;
			}
			return operations.remove(index);
		}
		else {
			return null;
		}
		//  Pair retval=null;
		//  while (operations.size()>0&&(retval=operations.remove(0/*(int)(Math.random()*operations.size())*/))==null);
		//  return retval;
	}
}

package net.clonecomputers.lab.starburst.finalize;

import net.clonecomputers.lab.starburst.*;
import net.clonecomputers.lab.starburst.properties.*;

/**
 * A way of seeding an image
 * Must have a method called "seedImage", but it can take whatever args it likes
 */
public abstract class Finalizer {
	protected final Starburst s;
	
	public Finalizer(Starburst s) {
		this.s = s;
	}
	
	public abstract void finalizeImage(PropertyTreeNode properties);
}

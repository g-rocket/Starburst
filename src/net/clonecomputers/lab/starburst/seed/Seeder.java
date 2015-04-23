package net.clonecomputers.lab.starburst.seed;

import java.util.*;

import net.clonecomputers.lab.starburst.*;
import net.clonecomputers.lab.starburst.properties.*;

/**
 * A way of seeding an image
 * Must have a method called "seedImage", but it can take whatever args it likes
 */
public abstract class Seeder {
	protected final Starburst s;
	
	public Seeder(Starburst s) {
		this.s = s;
	}
	
	public abstract void seedImage(PropertyTreeNode properties);
}

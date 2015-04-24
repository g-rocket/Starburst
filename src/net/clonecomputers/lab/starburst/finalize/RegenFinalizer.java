package net.clonecomputers.lab.starburst.finalize;

import net.clonecomputers.lab.starburst.*;
import net.clonecomputers.lab.starburst.properties.*;

public class RegenFinalizer extends Finalizer {

	public RegenFinalizer(Starburst s) {
		super(s);
	}

	@Override
	public void finalizeImage(PropertyTreeNode properties) {
		boolean[][] localcurrent = new boolean[s.getImageWidth()][s.getImageHeight()];
		s.operations.addPoint(s.centerPair.x,s.centerPair.y);
		Pair myPair;
		while ((myPair = s.operations.getPoint()) != null) {
			int x=myPair.x, y=myPair.y;
			if (localcurrent[x][y]) continue;
			if (!s.current[x][y]) s.fillPixel(x, y);
			if (((y+1)<s.getImageHeight())&&!localcurrent[x][y+1]) {
				s.operations.addPoint(x, y+1);
			}
			if (((x+1)<s.getImageWidth())&&!localcurrent[x+1][y]) {
				s.operations.addPoint(x+1, y);
			}
			if (((y-1)>=0)&&!localcurrent[x][y-1]) {
				s.operations.addPoint(x, y-1);
			}
			if (((x-1)>=0)&&!localcurrent[x-1][y]) {
				s.operations.addPoint(x-1, y);
			}
			localcurrent[x][y]=true;
		}
	}

}

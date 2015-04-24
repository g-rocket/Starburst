package net.clonecomputers.lab.starburst.finalize;

import net.clonecomputers.lab.starburst.*;
import net.clonecomputers.lab.starburst.properties.*;

public class LoopFromTopLeftFinalizer extends Finalizer {

	public LoopFromTopLeftFinalizer(Starburst s) {
		super(s);
	}

	@Override
	public void finalizeImage(PropertyTreeNode properties) {
		for (int x=0;x<s.getImageWidth();x++) {
			for (int y=0;y<s.getImageHeight();y++) {
				if (!s.current[x][y]) s.fillPixel(x, y);
			}
		}
	}

}

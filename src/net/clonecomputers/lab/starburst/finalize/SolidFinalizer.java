package net.clonecomputers.lab.starburst.finalize;

import java.awt.*;

import net.clonecomputers.lab.starburst.*;
import net.clonecomputers.lab.starburst.properties.*;

public class SolidFinalizer extends Finalizer {

	public SolidFinalizer(Starburst s) {
		super(s);
	}

	@Override
	public void finalizeImage(PropertyTreeNode properties) {
		int color = properties.get(Color.class, "color").getRGB();
		for (int x=0;x<s.getImageWidth();x++) {
			for (int y=0;y<s.getImageHeight();y++) {
				if (!s.current[x][y]) s.setPixel(x, y, color);
			}
		}
	}

}

package net.clonecomputers.lab.starburst.finalize;

import net.clonecomputers.lab.starburst.*;
import net.clonecomputers.lab.starburst.properties.*;

public class SquareFinalizer extends Finalizer {

	public SquareFinalizer(Starburst s) {
		super(s);
	}

	@Override
	public void finalizeImage(PropertyTreeNode properties) {
		drawSquares(properties.getAsDouble("squareDensity"));
		for (int x=0;x<s.getImageWidth();x++) {
			for (int y=0;y<s.getImageHeight();y++) {
				if (!s.current[x][y]) s.fillPixel(x, y);
			}
		}
	}
	
	private void drawSquares(double density) {
		for (int x=0; x<s.getImageWidth(); x++) {
			for (int y=0; y<s.getImageHeight(); y++) {
				if (!s.current[x][y] && s.rand.nextFloat() > density) {
					for (int i = x; i < x+10 && i < s.getImageWidth(); i++) {
						for (int j = y; j < y+10 && j < s.getImageHeight(); j++) {
							if (!s.current[i][j]) s.fillPixel(i, j);
						}
					}
				}
			}
		}
	}

}

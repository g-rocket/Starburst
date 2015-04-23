package net.clonecomputers.lab.starburst.seed;

import static java.lang.Math.*;
import static net.clonecomputers.lab.starburst.util.StaticUtils.*;

import java.awt.*;

import net.clonecomputers.lab.starburst.*;
import net.clonecomputers.lab.starburst.properties.*;

public class PointSeeder extends Seeder {
	public PointSeeder(Starburst s) {
		super(s);
	}
	
	public void seedImage(PropertyTreeNode properties) {
		for(int i = 0; i < properties.getAsInt("howMany"); i++) {
			Point p = randomRadialBiasedPoint(properties.getAsDouble("distribution"), s.getImageWidth(), s.getImageHeight());
			if(s.current[p.x][p.y]) continue;
			s.operations.addPoint(p.x, p.y);
			s.current[p.x][p.y] = true;
			s.setPixel(p.x, p.y, randomColor(s.rand));
		}
	}

	private Point randomRadialBiasedPoint(double bias, int width, int height) {
		Point p;
		do {
			double angle = s.rand.nextDouble() * 2*PI;
			double radiasBais = bias==0? 0: pow(s.rand.nextDouble(), log(bias)/log(.5));
			double radius = radiasBais * sqrt(width*width + height*height)/2; // maximum possible, may not be valid
			p = new Point((int)(radius*cos(angle)) + width/2, (int)(radius*sin(angle)) + height/2);
		} while(p.x < 0 || p.y < 0 || p.x >= width || p.y >= height); // keep trying until valid
		return p;
	}

}

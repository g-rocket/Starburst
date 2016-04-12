package net.clonecomputers.lab.starburst.seed;

import static java.lang.Math.*;
import static net.clonecomputers.lab.starburst.util.StaticUtils.*;

import java.awt.*;

import net.clonecomputers.lab.starburst.*;
import net.clonecomputers.lab.starburst.properties.*;

public class LineSeeder extends Seeder {

	public LineSeeder(Starburst s) {
		super(s);
	}
	
	public void seedImage(PropertyTreeNode properties) {
		int numberOfLines = (int)(properties.getAsDouble("distribution.density") * (s.getImageWidth()*s.getImageHeight()) / properties.getAsInt("distribution.length"));
		if (numberOfLines<1) numberOfLines = 1;
		for (int j = 0; j < numberOfLines; j++) {
			generateLine(properties.getAsInt("distribution.length"), properties.getSubproperty(String.class, "colorScheme"));
		}
	}
	
	private void generateLine(int length, Property<? extends String> colorScheme) {
		int c;
		if(colorScheme.getValue().equalsIgnoreCase("Varying")) {
			c = randomColor(s.rand);
		} else if(colorScheme.getValue().equalsIgnoreCase("Solid")) {
			c = colorScheme.get(Color.class, "color").getRGB();
		} else {
			throw new IllegalStateException(colorScheme+" is not a known colorScheme to generate a line");
		}
		double x = s.rand.nextInt(s.getImageWidth()), y = s.rand.nextInt(s.getImageHeight());
		double r = s.rand.nextInt(255), g = s.rand.nextInt(255), b = s.rand.nextInt(255);
		double rx = s.rand.nextInt(7)-3, ry = s.rand.nextInt(7)-3;
		double rr = s.rand.nextInt(7)-3, rg = s.rand.nextInt(7)-3, rb = s.rand.nextInt(7)-3;
		for (int i = 0; i < length; i++) {
			rx = lerp(rx, s.rand.nextInt(7)-3, .05);
			ry = lerp(ry, s.rand.nextInt(7)-3, .05);
			rr = lerp(rr, s.rand.nextInt(7)-3, .05);
			rg = lerp(rg, s.rand.nextInt(7)-3, .05);
			rb = lerp(rb, s.rand.nextInt(7)-3, .05);

			double d = hypot(rx, ry);
			double cd = sqrt(rr*rr + rg*rg + rb*rb);

			rx=rx/d; ry=ry/d;
			rr=rr/cd; rg=rg/cd; rb=rb/cd;

			x = x+rx; y = y+ry;
			r = r+rx; g = g+rx; b = b+ry;

			if (x<0) {
				x=0;
				rx=-rx;
			}
			if (y<0) {
				y=0;
				ry=-ry;
			}
			if (x>=s.getImageWidth()) {
				x=s.getImageWidth()-1;
				rx=-rx;
			}
			if (y>=s.getImageHeight()) {
				y=s.getImageHeight()-1;
				ry=-ry;
			}
			if (r<0) {
				r=0;
				rr=-rr;
			}
			if (g<0) {
				g=0;
				rg=-rg;
			}
			if (b<0) {
				b=0;
				rb=-rb;
			}
			if (r>=255) {
				r=255-1;
				rr=-rr;
			}
			if (g>=255) {
				g=255-1;
				rg=-rg;
			}
			if (b>=255) {
				b=255-1;
				rb=-rb;
			}
			if (colorScheme.getValue().equalsIgnoreCase("Varying")) c = color((int)r,(int)g,(int)b);
			s.current[(int)x][(int)y] = true;
			s.setPixel((int)x, (int)y, c);
		}
	}

}

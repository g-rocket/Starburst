package net.clonecomputers.lab.starburst.finalize;

import net.clonecomputers.lab.starburst.*;
import net.clonecomputers.lab.starburst.properties.*;

public class GenerateFromOutsidesOfHolesFinalizer extends Finalizer {

	public GenerateFromOutsidesOfHolesFinalizer(Starburst s) {
		super(s);
	}

	@Override
	public void finalizeImage(PropertyTreeNode properties) {
		/*System.out.println(s.operations);
		for(int y = 0; y < s.getImageHeight(); y++) {
			for(int x = 0; x < s.getImageWidth(); x++) {
				System.out.print(s.current[x][y]? '.':' ');
			}
			System.out.println();
		}*/
		s.fillOperations();
		double probabilityOfInclusion = s.properties.getAsDouble("probabilityOfInclusion");
		s.properties.set("probabilityOfInclusion", 1);
		s.generateImage();
		s.properties.set("probabilityOfInclusion", probabilityOfInclusion);
		s.operations.clear();
	}

}

package net.clonecomputers.lab.starburst;

import java.io.*;

import javax.swing.*;

import ar.com.hjg.pngj.*;
import ar.com.hjg.pngj.chunks.*;

public class ImageMetadataTest {

	public static void main(String[] args) {
		JFileChooser fc = new JFileChooser();
		fc.showOpenDialog(null);
		File file = fc.getSelectedFile();
		PngReader pngr = new PngReader(file);
		pngr.readSkippingAllRows(); // reads only metadata
		for (PngChunk c : pngr.getChunksList().getChunks()) {
			if (!ChunkHelper.isText(c)) continue;
			PngChunkTextVar ct = (PngChunkTextVar) c;
			String key = ct.getKey();
			String val = ct.getVal();
			System.out.printf("%s\n\t%s",key,val);
		}
		pngr.end(); // not necessary here, but good practice
	}

}

package com.mango.tms;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URL;

import javax.imageio.ImageIO;

public class TileGetter implements Runnable {

	public TileGetter() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	private void getImage(String imagePath, String cacheFilePath) throws Exception{
		//System.out.println(imagePath);
		BufferedImage bi = ImageIO.read(new URL(imagePath));
		File f = new File(new URI(cacheFilePath));
		f.mkdirs();
		ImageIO.write(bi, imagePath.substring(imagePath.lastIndexOf(".") + 1), f);
	}

}

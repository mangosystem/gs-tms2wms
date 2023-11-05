package com.mango.tms;

import java.awt.image.BufferedImage;
import java.util.Properties;

public interface IPathGenerator {

	String buildPath(Tile tile);
	
	//String buildPath(Tile tile, String attern);
	
	String shortPath(Tile tile);

	void init(Properties props);
	
	BufferedImage getMap(TileGenerator fTG, int level, double centerX, double centerY,
			int reqWidth, int reqHeight);
}

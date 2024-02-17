package com.mango.tms;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Properties;

import org.locationtech.jts.geom.Geometry;

public interface IPathGenerator {

	String buildPath(Tile tile);
	
	//String buildPath(Tile tile, String attern);
	
	String shortPath(Tile tile);

	void init(Properties props);
	
	void init(Map<String, Object> props);
	
	BufferedImage getMap(TileGenerator fTG, int level, double centerX, double centerY,
			int reqWidth, int reqHeight);
	
	public Tile[][] getTileSet(TileGenerator fTG, int level, Geometry roi) ;
	public Tile[][] getTileSet(TileGenerator fTG, int level, double centerX, double centerY, int reqWidth,
			int reqHeight);
}

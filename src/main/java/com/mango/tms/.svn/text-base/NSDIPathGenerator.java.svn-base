package com.mango.tms;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Properties;

import org.geotools.geometry.GeneralEnvelope;


public class NSDIPathGenerator implements IPathGenerator {

	private String fURLPattern;
	
	public void init(Properties props) {
		String value = props.getProperty("url.pattern");
		if (value == null) {
			throw new RuntimeException("url.pattern");
		}
		fURLPattern = value;
	}
	
	public static String replaceVariables(String source, String key,
			String replaceValue) {
		int index = source.indexOf(key);
		if (index > -1) {
			String pre = source.substring(0, index);
			String sub = source.substring(index + key.length());
			source = pre + replaceValue + sub;
		}
		return source;
	}
	
	public String buildPath(Tile tile) {
		String level = Integer.toString(tile.getLevel());
		String row = Integer.toString(tile.getGridY());
		String col = Integer.toString(tile.getGridX());

		String realPath = replaceVariables(fURLPattern, "%LEVEL%", level);
		realPath = replaceVariables(realPath, "%ROW%", row);
		realPath = replaceVariables(realPath, "%COL%", col);
		return realPath;
	}
	
	public String shortPath(Tile tile) {
		String level = Integer.toString(tile.getLevel());
		String row = Integer.toString(tile.getGridY());
		String col = Integer.toString(tile.getGridX());

		String realPath = File.separator + level;
		realPath = replaceVariables(realPath, "%ROW%", row);
		realPath = replaceVariables(realPath, "%COL%", col);
		return realPath;
	}

	public BufferedImage getMap(TileGenerator fTG, int level, double centerX,
			double centerY, int reqWidth, int reqHeight) {
		double[] resSet = fTG.getResolutions();
		double res = resSet[level];
		double halfW = reqWidth / 2 * res;
		double halfH = reqHeight / 2 * res;
		double width = fTG.getTileWidth() * res;
		double height = fTG.getTileHeight() * res;

		double[] mindp = new double[]{centerX - halfW,centerY - halfH};
		double[] maxdp = new double[]{centerX + halfW, centerY + halfH};
		
		GeneralEnvelope curEnv = new GeneralEnvelope(mindp, maxdp);
		curEnv.setCoordinateReferenceSystem(fTG.getTileCRS());
		
		int maxXTileNum = (int) Math.floor((fTG.getBounds().getMaximum(0) - fTG.getOriginX()) /width) -1;
		int maxYTileNum = (int) Math.floor((fTG.getOriginY() - fTG.getBounds().getMinimum(1)) /height) -1;
		
		int startTileX = (int) Math
				.floor((curEnv.getMinimum(0) - fTG.getOriginX()) / (width));
		int endTileX = (int) Math
				.ceil((curEnv.getMaximum(0) - fTG.getOriginX()) / (width)) - 1;

		int startTileY = (int) Math
				.floor((fTG.getOriginY() - curEnv.getMaximum(1)) / (height));
		int endTileY = (int) Math
				.ceil((fTG.getOriginY() - curEnv.getMinimum(1)) / (height)) - 1;
		
		int tileColCount = endTileX - startTileX + 1;
		int tileRowCount = endTileY - startTileY + 1;
		
		Tile[][] tiles = new Tile[tileRowCount][tileColCount];
		GeneralEnvelope fullEnv = null;
		for (int y = 0; y < tiles.length; y++) {
			int ay = startTileY + y;
			double minY = fTG.getOriginY() - (ay + 1) * height;
			double maxY = minY + height;
			int offsetY = y * fTG.getTileHeight();
			
			for (int x = 0; x < tiles[y].length; x++) {
				int ax = startTileX + x;
				double minX = fTG.getOriginX() + ax * width;
				double maxX = minX + width;
				int offsetX = x * fTG.getTileWidth();
				Rect rect = new Rect(offsetX, offsetY, fTG.getTileWidth(), fTG
						.getTileHeight());
				GeneralEnvelope env = new GeneralEnvelope(new double[]{minX, minY}, new double[]{maxX, maxY});
				env.setCoordinateReferenceSystem(fTG.getTileCRS());
				
				if (fullEnv == null) {
					fullEnv = new GeneralEnvelope(env);
				} else {
					fullEnv.add(env);
				}
				
				boolean include = fTG.getBounds().intersects(env, true);
				tiles[y][x] = new Tile(rect, level, resSet.length -1, include);
				tiles[y][x].setGridXY(ax, ay);
				
				if(!fTG.getBounds().intersects(env, false)) {
					tiles[y][x].setInclude(false);
				}
				
				if(maxXTileNum < ax || maxYTileNum < ay || ax < 0 || ay < 0) {
					tiles[y][x].setInclude(false);
				}
			}
		}

		int imageOffsetX = (int) ((fullEnv.getMinimum(0) - curEnv.getMinimum(0)) / res);
		int imageOffsetY = (int) ((curEnv.getMaximum(1) - fullEnv.getMaximum(1)) / res);

		BufferedImage bi = new BufferedImage(reqWidth, reqHeight,
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = bi.getGraphics();
		for (int y = 0; y < tiles.length; y++) {
			for (int x = 0; x < tiles[y].length; x++) {
				BufferedImage tileImage = fTG.getTileImage(tiles[y][x]);
				Rect r = tiles[y][x].getRect();
				g.drawImage(tileImage, r.getX() + imageOffsetX, r.getY()
						+ imageOffsetY, null);
			}
		}


		if (fTG.isOutline()) {
			g.setColor(new Color(0xFFFF0000));
			
			for (int y = 0; y < tiles.length; y++) {
				for (int x = 0; x < tiles[y].length; x++) {
					Rect r = tiles[y][x].getRect();
					g.drawRect(r.getX() + imageOffsetX,
							r.getY() + imageOffsetY, r.getWidth() -1, r
									.getHeight() -1);
				}
			}
		}

		return bi;
	}

}

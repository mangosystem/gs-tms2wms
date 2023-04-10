package com.mango.tms;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Properties;

import org.geotools.geometry.GeneralEnvelope;


public class NaverPathGenerator implements IPathGenerator {

	private String fURLPattern;

	public void init(Properties props) {
		String value = props.getProperty("url.pattern");
		if (value == null) {
			throw new RuntimeException("url.pattern");
		}
		fURLPattern = value;
	}

	public String buildPath(Tile tile) {
		StringBuffer sb = new StringBuffer();
		sb.append(fURLPattern);
		int level2 = Math.abs(13 - tile.getLevel());
		if (level2 < 10) {
			sb.append("0");
		}

		String x = Integer.toString(tile.getGridX());
		String y = Integer.toString(tile.getGridY());

		int xvalue = tile.getGridX();
		int yvalue = tile.getGridY();

		String fx = "0";
		String fy = "0";
		String sx = "0";
		String sy = "0";

		// 첫占쏙옙째 占쏙옙占썰리
		sb.append(level2).append("/");
		if (xvalue > 0) {
			fx = String.valueOf(Math.round(xvalue / 1024));
			sx = String.valueOf(Math.round(xvalue / 64));
		}
		if (yvalue > 0) {
			fy = String.valueOf(Math.round(yvalue / 1024));
			sy = String.valueOf(Math.round(yvalue / 64));
		}

		for (int i = 0, size = 5 - fx.length(); i < size; i++) {
			sb.append("0");
		}
		sb.append(fx).append("-");
		for (int i = 0, size = 5 - fy.length(); i < size; i++) {
			sb.append("0");
		}
		sb.append(fy).append("/");

		// 占싸뱄옙째 占쏙옙占썰리
		for (int i = 0, size = 5 - sx.length(); i < size; i++) {
			sb.append("0");
		}
		sb.append(sx).append("-");
		for (int i = 0, size = 5 - sy.length(); i < size; i++) {
			sb.append("0");
		}
		sb.append(sy).append("/");

		for (int i = 0, size = 5 - x.length(); i < size; i++) {
			sb.append("0");
		}
		sb.append(x).append("-");
		for (int i = 0, size = 5 - y.length(); i < size; i++) {
			sb.append("0");
		}
		sb.append(y).append(".jpg");
		return sb.toString();
	}
	
	@SuppressWarnings("unused")
	public String shortPath(Tile tile) {
		String level = Integer.toString(tile.getLevel());
		String row = Integer.toString(tile.getGridY());
		String col = Integer.toString(tile.getGridX());

		String realPath = File.pathSeparator + level;
//		realPath = replaceVariables(realPath, "%ROW%", row);
//		realPath = replaceVariables(realPath, "%COL%", col);
		return realPath;
	}

	public BufferedImage getMap(TileGenerator fTG, int level, double centerX, double centerY,
			int reqWidth, int reqHeight) {
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
		
		int startTileX = (int) Math
				.floor(((curEnv.getMinimum(0) - fTG.getOriginX()) / width));
		int endTileX = (int) Math
				.ceil(((curEnv.getMaximum(0) - fTG.getOriginX()) / width)) - 1;

		int startTileY = (int) Math
				.floor(((curEnv.getMinimum(1) - fTG.getOriginY()) / height));
		int endTileY = (int) Math
				.ceil(((curEnv.getMaximum(1) - fTG.getOriginY()) / height)) - 1;

		int tileColCount = endTileX - startTileX + 1;
		int tileRowCount = endTileY - startTileY + 1;

		Tile[][] tiles = new Tile[tileRowCount][tileColCount];
		GeneralEnvelope fullEnv = null;
		for (int y = 0; y < tiles.length; y++) {
			int ay = startTileY + y;
			double minY = fTG.getOriginY() + ay * height;
			double maxY = minY + height;
			int offsetY = (tiles.length - y - 1) * fTG.getTileHeight();
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
				tiles[y][x] = new Tile(rect, level, resSet.length - 1, include);
				tiles[y][x].setGridXY(ax, ay);
			}
		}

		int imageOffsetX = (int) ((fullEnv.getMinimum(0) - curEnv.getMinimum(0)) / res);
		int imageOffsetY = (int) ((curEnv.getMaximum(1) - fullEnv.getMaximum(1)) / res);

		BufferedImage bi = new BufferedImage(reqWidth, reqHeight,
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = bi.getGraphics();
		// for (int y = 0; y < tiles.length; y++) {
		// for (int x = 0; x < tiles[y].length; x++) {
		// BufferedImage tileImage = fTG.getTileImage(tiles[y][x]);
		// Rect r = tiles[y][x].getRect();
		// g.drawImage(tileImage, r.getX() + imageOffsetX, r.getY()
		// + imageOffsetY, null);
		// }
		// }

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
							r.getY() + imageOffsetY, r.getWidth(), r
									.getHeight());
				}
			}
		}

		return bi;
	}

}

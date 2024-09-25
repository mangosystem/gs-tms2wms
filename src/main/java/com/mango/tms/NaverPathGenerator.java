package com.mango.tms;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.geotools.geometry.GeneralBounds;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;


public class NaverPathGenerator implements IPathGenerator {

	private String fURLPattern;

	public void init(Properties props) {
		String value = props.getProperty("url.pattern");
		if (value == null) {
			throw new RuntimeException("url.pattern");
		}
		fURLPattern = value;
	}
	
	public void init(Map<String, Object> props) {
		String value = (String)props.get("url_pattern");
		if (value == null) {
			throw new RuntimeException("url_pattern");
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
		GeneralBounds curEnv = new GeneralBounds(mindp, maxdp);
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
		GeneralBounds fullEnv = null;
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
				GeneralBounds env = new GeneralBounds(new double[]{minX, minY}, new double[]{maxX, maxY});
				env.setCoordinateReferenceSystem(fTG.getTileCRS());
				
				if (fullEnv == null) {
					fullEnv = new GeneralBounds(env);
				} else {
					fullEnv.add(env);
				}
				
				boolean include = fTG.getBounds().intersects(env, true);
				tiles[y][x] = new Tile(rect, level, resSet.length - 1, include);
				tiles[y][x].setGridXY(ax, ay);
				tiles[y][x].setEnv(env);
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

	public Tile[][] getTileSet(TileGenerator fTG, int level, double centerX, double centerY, int reqWidth,
			int reqHeight) {
		double[] resSet = fTG.getResolutions();
		double res = resSet[level - 1];
		double reqHalfRealWidth = reqWidth / 2 * res;
		double reqHalfRealHeight = reqHeight / 2 * res;
		double tileRealwidth = fTG.getTileWidth() * res;
		double tileRealheight = fTG.getTileHeight() * res;

		double[] reqMinDp = new double[] { centerX - reqHalfRealWidth, centerY - reqHalfRealHeight };
		double[] reqMaxDp = new double[] { centerX + reqHalfRealWidth, centerY + reqHalfRealHeight };

		GeneralBounds reqEnv = new GeneralBounds(reqMinDp, reqMaxDp);
		reqEnv.setCoordinateReferenceSystem(fTG.getTileCRS());
//		System.out.println("x0:" + (new BigDecimal(reqEnv.getMinimum(0)).toString()));
//		System.out.println("y0:" + (new BigDecimal(reqEnv.getMinimum(1)).toString()));
//		System.out.println("x1:" + (new BigDecimal(reqEnv.getMaximum(0)).toString()));
//		System.out.println("y1:" + (new BigDecimal(reqEnv.getMaximum(1)).toString()));
		int startTileX = -1;
		int endTileX = -1;
		int startTileY = -1;
		int endTileY = -1;

		if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
			startTileX = (int) Math.floor((reqEnv.getMinimum(0) - fTG.getOriginX()) / (tileRealwidth));
			endTileX = (int) Math.ceil((reqEnv.getMaximum(0) - fTG.getOriginX()) / (tileRealwidth)) - 1;

			startTileY = (int) Math.floor((reqEnv.getMinimum(1) - fTG.getOriginY()) / (tileRealheight));
			endTileY = (int) Math.ceil((reqEnv.getMaximum(1) - fTG.getOriginY()) / (tileRealheight)) - 1;
		} else {
			startTileX = (int) Math.floor((reqEnv.getMinimum(0) - fTG.getOriginX()) / (tileRealwidth));
			endTileX = (int) Math.ceil((reqEnv.getMaximum(0) - fTG.getOriginX()) / (tileRealwidth)) - 1;

			startTileY = (int) Math.floor((fTG.getOriginY() - reqEnv.getMaximum(1)) / (tileRealheight));
			endTileY = (int) Math.ceil((fTG.getOriginY() - reqEnv.getMinimum(1)) / (tileRealheight)) - 1;
		}

		int tileColCount = endTileX - startTileX + 1;
		int tileRowCount = endTileY - startTileY + 1;

		Tile[][] tiles = new Tile[tileRowCount][tileColCount];
		GeneralBounds fullEnv = null;
		for (int y = 0; y < tiles.length; y++) {
			int ay = -1;
			ay = startTileY + y;
			if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
				ay = endTileY - y;
			}

			double minY = fTG.getOriginY() - (ay + 1) * tileRealheight;
			double maxY = minY + tileRealheight;
			if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
				minY = fTG.getOriginY() + (ay) * tileRealheight;
				maxY = minY + tileRealheight;
			}
			int offsetY = (y) * fTG.getTileHeight();
			if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
				offsetY = (y) * fTG.getTileHeight();
			}
			for (int x = 0; x < tiles[y].length; x++) {
				int ax = startTileX + x;
				double minX = fTG.getOriginX() + ax * tileRealwidth;
				double maxX = minX + tileRealwidth;
				int offsetX = x * fTG.getTileWidth();
				Rect rect = new Rect(offsetX, offsetY, fTG.getTileWidth(), fTG.getTileHeight());
				GeneralBounds env = new GeneralBounds(new double[] { minX, minY }, new double[] { maxX, maxY });
				env.setCoordinateReferenceSystem(fTG.getTileCRS());

				if (fullEnv == null) {
					fullEnv = new GeneralBounds(env);
				} else {
					fullEnv.add(env);
				}

				boolean include = fTG.getBounds().intersects(env, false);
				tiles[y][x] = new Tile(rect, level, resSet.length, include);
				tiles[y][x].setGridXY(ax, ay);
				tiles[y][x].setEnv(env);
			}
		}
		
		return tiles;
	}
	
	public Tile[][] getTileSet(TileGenerator fTG, int level, Geometry roi) {
		double[] resSet = fTG.getResolutions();
		double res = resSet[level - 1];
		double reqHalfRealWidth = roi.getEnvelopeInternal().getWidth() / 2.;
		double reqHalfRealHeight = roi.getEnvelopeInternal().getHeight() / 2.;
		double tileRealwidth = fTG.getTileWidth() * res;
		double tileRealheight = fTG.getTileHeight() * res;

		double[] reqMinDp = new double[] { roi.getEnvelopeInternal().centre().x - reqHalfRealWidth, roi.getEnvelopeInternal().centre().y - reqHalfRealHeight };
		double[] reqMaxDp = new double[] { roi.getEnvelopeInternal().centre().x + reqHalfRealWidth, roi.getEnvelopeInternal().centre().y + reqHalfRealHeight };

		GeneralBounds reqEnv = new GeneralBounds(reqMinDp, reqMaxDp);
		reqEnv.setCoordinateReferenceSystem(fTG.getTileCRS());
		int startTileX = -1;
		int endTileX = -1;
		int startTileY = -1;
		int endTileY = -1;

		if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
			startTileX = (int) Math.floor((reqEnv.getMinimum(0) - fTG.getOriginX()) / (tileRealwidth));
			endTileX = (int) Math.ceil((reqEnv.getMaximum(0) - fTG.getOriginX()) / (tileRealwidth)) - 1;

			startTileY = (int) Math.floor((reqEnv.getMinimum(1) - fTG.getOriginY()) / (tileRealheight));
			endTileY = (int) Math.ceil((reqEnv.getMaximum(1) - fTG.getOriginY()) / (tileRealheight)) - 1;
		} else {
			startTileX = (int) Math.floor((reqEnv.getMinimum(0) - fTG.getOriginX()) / (tileRealwidth));
			endTileX = (int) Math.ceil((reqEnv.getMaximum(0) - fTG.getOriginX()) / (tileRealwidth)) - 1;

			startTileY = (int) Math.floor((fTG.getOriginY() - reqEnv.getMaximum(1)) / (tileRealheight));
			endTileY = (int) Math.ceil((fTG.getOriginY() - reqEnv.getMinimum(1)) / (tileRealheight)) - 1;
		}

		int tileColCount = endTileX - startTileX + 1;
		int tileRowCount = endTileY - startTileY + 1;

		Tile[][] tiles = new Tile[tileRowCount][tileColCount];
		GeneralBounds fullEnv = null;
		GeometryFactory gf = new GeometryFactory();
		for (int y = 0; y < tiles.length; y++) {
			int ay = -1;
			ay = startTileY + y;
			if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
				ay = endTileY - y;
			}

			double minY = fTG.getOriginY() - (ay + 1) * tileRealheight;
			double maxY = minY + tileRealheight;
			if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
				minY = fTG.getOriginY() + (ay) * tileRealheight;
				maxY = minY + tileRealheight;
			}
			int offsetY = (y) * fTG.getTileHeight();
			if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
				offsetY = (y) * fTG.getTileHeight();
			}
			for (int x = 0; x < tiles[y].length; x++) {
				int ax = startTileX + x;
				double minX = fTG.getOriginX() + ax * tileRealwidth;
				double maxX = minX + tileRealwidth;
				int offsetX = x * fTG.getTileWidth();
				Rect rect = new Rect(offsetX, offsetY, fTG.getTileWidth(), fTG.getTileHeight());
				GeneralBounds env = new GeneralBounds(new double[] { minX, minY }, new double[] { maxX, maxY });
				env.setCoordinateReferenceSystem(fTG.getTileCRS());

				if (fullEnv == null) {
					fullEnv = new GeneralBounds(env);
				} else {
					fullEnv.add(env);
				}

				boolean include = fTG.getPolygon(gf).intersects(roi);
				tiles[y][x] = new Tile(rect, level, resSet.length, include);
				tiles[y][x].setGridXY(ax, ay);
				tiles[y][x].setEnv(env);
			}
		}
		
		return tiles;
	}
}

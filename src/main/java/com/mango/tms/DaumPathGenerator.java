package com.mango.tms;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.util.Properties;

import org.geotools.geometry.GeneralEnvelope;

public class DaumPathGenerator implements IPathGenerator {

	private String fURLPattern;

	public void init(Properties props) {
		String value = props.getProperty("url.pattern");
		if (value == null) {
			throw new RuntimeException("url.pattern");
		}
		fURLPattern = value;
	}

	public static String replaceVariables(String source, String key, String replaceValue) {
		int index = source.indexOf(key);
		if (index > -1) {
			String pre = source.substring(0, index);
			String sub = source.substring(index + key.length());
			source = pre + replaceValue + sub;
		}
		return source;
	}

	public String buildPath(Tile tile) {
		int sidx = 0;
		try {
			sidx = (int) (Math.random() * 4d) + tile.getTileGenerator().getfUrlServerStart();
		} catch (Exception e) {

		}
		String server = "" + sidx;
		String level = "L" + ((Integer.toString(tile.getLevel()) + "").length() <= 1
				? "" + (Integer.toString(tile.getLevel()) + "")
				: (Integer.toString(tile.getLevel()) + ""));
		String row = Integer.toString(tile.getGridY());
		String col = Integer.toString(tile.getGridX());
		//System.out.println(col + ":" + row + "=>" + tile.getGridX() + ":" + tile.getGridY());
		String realPath = replaceVariables(fURLPattern, "%SERVER%", server);
		realPath = replaceVariables(realPath, "%LEVEL%", level);
		realPath = replaceVariables(realPath, "%ROW%", row);
		realPath = replaceVariables(realPath, "%COL%", col);
		return realPath;
	}

	public String shortPath(Tile tile) {
		String realPath = "" + "/";

		String level = "L" + Integer.toString(tile.getLevel());
		String row = Integer.toString(tile.getGridY());
		String col = Integer.toString(tile.getGridX());

		realPath = realPath + level + "/";
		realPath = realPath + row + "/";
		realPath = realPath + col + (fURLPattern.substring(fURLPattern.lastIndexOf(".")));
		return realPath;
	}

	@SuppressWarnings("unused")
	public BufferedImage getMap(TileGenerator fTG, int level, double centerX, double centerY, int reqWidth,
			int reqHeight) {
//		level = fTG.getResolutions().length - level;
		double[] resSet = fTG.getResolutions();
		double res = resSet[level - 1];
		double reqHalfRealWidth = reqWidth / 2 * res;
		double reqHalfRealHeight = reqHeight / 2 * res;
		double tileRealwidth = fTG.getTileWidth() * res;
		double tileRealheight = fTG.getTileHeight() * res;

		int maxXTileNum = -1;
		int maxYTileNum = -1;

		maxXTileNum = (int) Math.floor((fTG.getBounds().getMaximum(0) - fTG.getOriginX()) / tileRealwidth);
		maxYTileNum = (int) Math.floor((fTG.getBounds().getMaximum(1) - fTG.getOriginY()) / tileRealheight);

		double[] reqMinDp = new double[] { centerX - reqHalfRealWidth, centerY - reqHalfRealHeight };
		double[] reqMaxDp = new double[] { centerX + reqHalfRealWidth, centerY + reqHalfRealHeight };
		GeneralEnvelope reqEnv = new GeneralEnvelope(reqMinDp, reqMaxDp);
		reqEnv.setCoordinateReferenceSystem(fTG.getTileCRS());

		int startTileX = (int) Math.floor((reqEnv.getMinimum(0) - fTG.getOriginX()) / (tileRealwidth));
		int endTileX = (int) Math.ceil((reqEnv.getMaximum(0) - fTG.getOriginX()) / (tileRealwidth)) - 1;

		int startTileY = (int) Math.floor((reqEnv.getMinimum(1) - fTG.getOriginY()) / (tileRealheight));
		int endTileY = (int) Math.ceil((reqEnv.getMaximum(1) - fTG.getOriginY()) / (tileRealheight)) - 1;

		int tileColCount = endTileX - startTileX + 1;
		int tileRowCount = endTileY - startTileY + 1;

		Tile[][] tiles = new Tile[tileRowCount][tileColCount];
		GeneralEnvelope fullEnv = null;
		for (int y = 0; y < tiles.length; y++) {
			int ay = endTileY - y;
			double minY = fTG.getOriginY() + ay * tileRealheight;
			double maxY = minY + tileRealheight;
			int offsetY = (y) * fTG.getTileHeight();
			for (int x = 0; x < tiles[y].length; x++) {
				int ax = startTileX + x;
				double minX = fTG.getOriginX() + ax * tileRealwidth;
				double maxX = minX + tileRealwidth;
				int offsetX = x * fTG.getTileWidth();
				Rect rect = new Rect(offsetX, offsetY, fTG.getTileWidth(), fTG.getTileHeight());
				GeneralEnvelope env = new GeneralEnvelope(new double[] { minX, minY }, new double[] { maxX, maxY });
				env.setCoordinateReferenceSystem(fTG.getTileCRS());

				if (fullEnv == null) {
					fullEnv = new GeneralEnvelope(env);
				} else {
					fullEnv.add(env);
				}

				boolean include = fTG.getBounds().intersects(env, true);
				tiles[y][x] = new Tile(rect, level, resSet.length, include);
				tiles[y][x].setGridXY(ax, ay);
				if (!fTG.getBounds().intersects(env, true)) {
					tiles[y][x].setInclude(true);
				}

//				if(maxXTileNum < ax || maxYTileNum < ay || ax < 0 || ay < 0) {
//					tiles[y][x].setInclude(false);
//				}
			}
		}

		int imageOffsetX = (int) ((fullEnv.getMinimum(0) - reqEnv.getMinimum(0)) / res);
		int imageOffsetY = (int) ((reqEnv.getMaximum(1) - fullEnv.getMaximum(1)) / res);

		BufferedImage bi = new BufferedImage(reqWidth, reqHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) bi.getGraphics();

		for (int y = 0; y < tiles.length; y++) {
			for (int x = 0; x < tiles[y].length; x++) {
				tiles[y][x].setTileGenerator(fTG);
				BufferedImage tileImage = fTG.getTileImage(tiles[y][x]);
				Rect r = tiles[y][x].getRect();
				g.drawImage(tileImage, r.getX() + imageOffsetX, r.getY() + imageOffsetY, null);
//				g.drawString(tiles[y][x].getGridX() + ":" + tiles[y][x].getGridY(), r.getX() + imageOffsetX / 2,
//						r.getY() + imageOffsetY / 2);
			}
		}

		if (fTG.isOutline()) {
			g.setColor(new Color(0xFFFF0000));
			for (int y = 0; y < tiles.length; y++) {
				for (int x = 0; x < tiles[y].length; x++) {
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					Rect r = tiles[y][x].getRect();
					g.drawRect(r.getX() + imageOffsetX, r.getY() + imageOffsetY, r.getWidth(), r.getHeight());
					g.drawRect(r.getX() + 1 + imageOffsetX, r.getY() + 1 + imageOffsetY, r.getWidth() - 1,
							r.getHeight() - 1);

					FontRenderContext frc = g.getFontRenderContext();
					String str = "L" + tiles[y][x].getLevel() + " : X" + tiles[y][x].getGridX() + " : Y"
							+ tiles[y][x].getGridY();

					int fontSize = 12;
					if (bi.getWidth() > 256) {
						fontSize = 15;
					}
					g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
					GlyphVector gv = g.getFont().createGlyphVector(frc, str);
					Rectangle bounds = gv.getPixelBounds(null, x, y);
					g.drawString(str, r.getX() + imageOffsetX + 5,
							r.getY() + imageOffsetY + bounds.height + 5);
				}
			}
		}

		return bi;
	}

}

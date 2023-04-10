package com.mango.tms;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;

import org.geotools.geometry.GeneralEnvelope;

public class TMSPathGenerator extends PathGenerator {

	public BufferedImage getMap(TileGenerator fTG, int level, double centerX, double centerY, int reqWidth,
			int reqHeight) {
		double[] resSet = fTG.getResolutions();
		double res = resSet[level - 1];
		double reqHalfRealWidth = reqWidth / 2 * res;
		double reqHalfRealHeight = reqHeight / 2 * res;
		double tileRealwidth = fTG.getTileWidth() * res;
		double tileRealheight = fTG.getTileHeight() * res;

//		int maxXTileNum = -1;
//		int maxYTileNum = -1;
//
//		if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
//			maxXTileNum = (int) Math.floor((fTG.getBounds().getMaximum(0) - fTG.getOriginX()) / tileRealwidth);
//			maxYTileNum = (int) Math.floor((fTG.getBounds().getMaximum(1) - fTG.getOriginY()) / tileRealheight);
//		} else {
//			maxXTileNum = (int) Math.floor((fTG.getBounds().getMaximum(0) - fTG.getOriginX()) / tileRealwidth) - 1;
//			maxYTileNum = (int) Math.floor((fTG.getOriginY() - fTG.getBounds().getMinimum(1)) / tileRealheight) - 1;
//		}

		double[] reqMinDp = new double[] { centerX - reqHalfRealWidth, centerY - reqHalfRealHeight };
		double[] reqMaxDp = new double[] { centerX + reqHalfRealWidth, centerY + reqHalfRealHeight };

		GeneralEnvelope reqEnv = new GeneralEnvelope(reqMinDp, reqMaxDp);
		reqEnv.setCoordinateReferenceSystem(fTG.getTileCRS());
		int startTileX = -1;
		int endTileX = -1;
		int startTileY = -1;
		int endTileY = -1;

//		if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
		startTileX = (int) Math.floor((reqEnv.getMinimum(0) - fTG.getOriginX()) / (tileRealwidth));
		endTileX = (int) Math.ceil((reqEnv.getMaximum(0) - fTG.getOriginX()) / (tileRealwidth)) - 1;

		startTileY = (int) Math.floor((reqEnv.getMinimum(1) - fTG.getOriginY()) / (tileRealheight));
		endTileY = (int) Math.ceil((reqEnv.getMaximum(1) - fTG.getOriginY()) / (tileRealheight)) - 1;
//		} else {
//			startTileX = (int) Math.floor((reqEnv.getMinimum(0) - fTG.getOriginX()) / (tileRealwidth));
//			endTileX = (int) Math.ceil((reqEnv.getMaximum(0) - fTG.getOriginX()) / (tileRealwidth)) - 1;
//
//			startTileY = (int) Math.floor((fTG.getOriginY() - reqEnv.getMaximum(1)) / (tileRealheight));
//			endTileY = (int) Math.ceil((fTG.getOriginY() - reqEnv.getMinimum(1)) / (tileRealheight)) - 1;
//		}

		int tileColCount = endTileX - startTileX + 1;
		int tileRowCount = endTileY - startTileY + 1;

		Tile[][] tiles = new Tile[tileRowCount][tileColCount];
		GeneralEnvelope fullEnv = null;
		for (int y = 0; y < tiles.length; y++) {
			int ay = -1;
			ay = startTileY + y;
//			if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
			ay = endTileY - y;
//			}

			double minY = fTG.getOriginY() - (ay + 1) * tileRealheight;
			double maxY = minY + tileRealheight;
//			if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
			minY = fTG.getOriginY() + (ay) * tileRealheight;
			maxY = minY + tileRealheight;
//			}
			int offsetY = (y) * fTG.getTileHeight();
//			if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
			offsetY = (y) * fTG.getTileHeight();
//			}
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
			}
		}

		int imageOffsetX = (int) ((fullEnv.getMinimum(0) - reqEnv.getMinimum(0)) / res);
		int imageOffsetY = (int) ((reqEnv.getMaximum(1) - fullEnv.getMaximum(1)) / res);

		BufferedImage bi = new BufferedImage(reqWidth, reqHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) bi.getGraphics();

		for (int y = 0; y < tiles.length; y++) {
			for (int x = 0; x < tiles[y].length; x++) {
				BufferedImage tileImage = getTileImage(fTG, tiles[y][x]);
				Rect r = tiles[y][x].getRect();
				g.drawImage(tileImage, r.getX() + imageOffsetX, r.getY() + imageOffsetY, null);
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
					g.drawString(str, r.getX() + imageOffsetX + 5, r.getY() + imageOffsetY + bounds.height + 5);
				}
			}
		}

		return bi;
	}
}

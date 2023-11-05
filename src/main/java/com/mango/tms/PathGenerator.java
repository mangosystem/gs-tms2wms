package com.mango.tms;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.geotools.geometry.GeneralEnvelope;

public class PathGenerator implements IPathGenerator {

	public String fURLPattern;

	public void init(Properties props) {
		String value = props.getProperty("url.pattern");
		if (value == null) {
			throw new RuntimeException("url.pattern");
		}
		fURLPattern = value;
	}

	public static String replaceVariables(String source, String key, String replaceValue) {
		if(replaceValue == null || "".equalsIgnoreCase(replaceValue)) {
			return source;
		}
		int index = source.indexOf(key);
		if (index > -1) {
			String pre = source.substring(0, index);
			String sub = source.substring(index + key.length());
			source = pre + replaceValue + sub;
		}
		return source;
	}
	
	public String buildPath(Tile tile) {
		return buildPath(tile, null);
	}

	public String buildPath(Tile tile, String pattern) {
		//int sidx = 0;
		//try {
		//	sidx = (int) (Math.random() * 4d) + tile.getTileGenerator().getfUrlServerStart();
		//} catch (Exception e) {

		//}
		//String server = "" + sidx;
		String level = Integer.toString(tile.getLevel());
		String row = Integer.toString(tile.getGridY());
		String col = Integer.toString(tile.getGridX());

		String realPath = fURLPattern;

		realPath = replaceVariables(realPath, "%PATTERN%", pattern);
		realPath = replaceVariables(realPath, "%LEVEL%", level);
		realPath = replaceVariables(realPath, "%ROW%", row);
		realPath = replaceVariables(realPath, "%COL%", col);
		// System.out.println(realPath);
		return realPath;
	}

	public String shortPath(Tile tile) {
		String realPath = "" + "/";

		String level = "" + Integer.toString(tile.getLevel());
		String row = Integer.toString(tile.getGridY());
		String col = Integer.toString(tile.getGridX());

		int lvlIdx = fURLPattern.indexOf("%LEVEL%");
		int realPathStartIdx = fURLPattern.substring(0, lvlIdx).lastIndexOf("/");
		String subPath = fURLPattern.substring(realPathStartIdx + 1);
		subPath = replaceVariables(subPath, "%LEVEL%", level);
		subPath = replaceVariables(subPath, "%ROW%", row);
		subPath = replaceVariables(subPath, "%COL%", col);

		realPath = realPath + level + "/" + col + "/" + row;
//		realPath = realPath + level + "/";
//		realPath = realPath + col + "/";
//		realPath = realPath + row + (fURLPattern.substring(fURLPattern.lastIndexOf(".")));
		return realPath;
	}
	
	public BufferedImage getTileImage(TileGenerator fTG, Tile tile) {
		return getTileImage(fTG, tile, null); 
	}

	public BufferedImage getTileImage(TileGenerator fTG, Tile tile, String pattern) {
		tile.setLevel(tile.getLevel() + (fTG.getfServceStartLevel() - 1));
		String path = buildPath(tile, pattern);

		if (!tile.isInclude()) {
			return fTG.getBlank();
		}

		String cacheFilePath = "";
		try {
			if (fTG.isTileCache()) {
				if (fTG.getCahcePath().endsWith(File.separator)) {
					cacheFilePath = fTG.getCahcePath().substring(0, fTG.getCahcePath().length() - 1) + shortPath(tile);
				} else {
					cacheFilePath = fTG.getCahcePath() + shortPath(tile);
				}
				if (!(cacheFilePath.toLowerCase().endsWith("png") || cacheFilePath.toLowerCase().endsWith("jpg")
						|| cacheFilePath.toLowerCase().endsWith("gif"))) {
					cacheFilePath = cacheFilePath + "." + fTG.getfImageFormat();
				}
				File cacheFile = new File(new URI(cacheFilePath));
				if (cacheFile.exists()) {
					return ImageIO.read(cacheFile);
				}
			}
		} catch (Exception e) {
		}

		try {
			{
				URL u = new URL(path);
				String protocol = u.getProtocol().toLowerCase();
				BufferedImage bi = null;
				if("http".equals(protocol) || "https".equals(protocol)) {
					HttpURLConnection conn = (HttpURLConnection) u.openConnection();
					conn.setConnectTimeout(1000);
					conn.setReadTimeout(500);
					InputStream is = null;
					// Referer:http://map.vworld.kr/map/maps.do
					conn.addRequestProperty("User-Agent",
							"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36");
					if (conn.getResponseCode() == 200) {
						is = conn.getInputStream();
						bi = ImageIO.read(is);
						is.close();
						conn.disconnect();

						if (fTG.isTileCache()) {
							if (!(cacheFilePath.toLowerCase().endsWith("png") || cacheFilePath.toLowerCase().endsWith("jpg")
									|| cacheFilePath.toLowerCase().endsWith("gif"))) {
								cacheFilePath = cacheFilePath + ".jpg";
							}
							File f = new File(new URI(cacheFilePath));
							f.mkdirs();
							String format = path.substring(path.lastIndexOf(".") + 1);
							if (!(format.endsWith("png") || format.endsWith("jpg") || format.endsWith("gif"))) {
								format = fTG.getfImageFormat();
							}
							ImageIO.write(bi, format, f);
						}

						//return bi;
					} else {
						bi = fTG.getBlank();
					}
				} else {
					File imgFile = new File(u.toURI());
					if(imgFile.exists()) {
						bi = ImageIO.read(u);
					} else {
						bi = fTG.getBlank();
					}
				}
				
				if (fTG.isTileCache()) {
					if (!(cacheFilePath.toLowerCase().endsWith("png") || cacheFilePath.toLowerCase().endsWith("jpg")
							|| cacheFilePath.toLowerCase().endsWith("gif"))) {
						cacheFilePath = cacheFilePath + ".jpg";
					}
					File f = new File(new URI(cacheFilePath));
					f.mkdirs();
					String format = path.substring(path.lastIndexOf(".") + 1);
					if (!(format.endsWith("png") || format.endsWith("jpg") || format.endsWith("gif"))) {
						format = fTG.getfImageFormat();
					}
					ImageIO.write(bi, format, f);
				}
				
				return bi;
			}
		} catch (Exception e) {
			return fTG.getBlank();
		}
	}

	@Override
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

		if ("BT".equalsIgnoreCase(fTG.getPathYOrder())) {
			maxXTileNum = (int) Math.floor((fTG.getBounds().getMaximum(0) - fTG.getOriginX()) / tileRealwidth);
			maxYTileNum = (int) Math.floor((fTG.getBounds().getMaximum(1) - fTG.getOriginY()) / tileRealheight);
		} else {
			maxXTileNum = (int) Math.floor((fTG.getBounds().getMaximum(0) - fTG.getOriginX()) / tileRealwidth) - 1;
			maxYTileNum = (int) Math.floor((fTG.getOriginY() - fTG.getBounds().getMinimum(1)) / tileRealheight) - 1;
		}

		double[] reqMinDp = new double[] { centerX - reqHalfRealWidth, centerY - reqHalfRealHeight };
		double[] reqMaxDp = new double[] { centerX + reqHalfRealWidth, centerY + reqHalfRealHeight };

		GeneralEnvelope reqEnv = new GeneralEnvelope(reqMinDp, reqMaxDp);
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
		GeneralEnvelope fullEnv = null;
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
				GeneralEnvelope env = new GeneralEnvelope(new double[] { minX, minY }, new double[] { maxX, maxY });
				env.setCoordinateReferenceSystem(fTG.getTileCRS());

				if (fullEnv == null) {
					fullEnv = new GeneralEnvelope(env);
				} else {
					fullEnv.add(env);
				}

				boolean include = fTG.getBounds().intersects(env, true);
//				System.out.println("x0:" + (new BigDecimal(env.getMinimum(0)).toString()));
//				System.out.println("y0:" + (new BigDecimal(env.getMinimum(1)).toString()));
//				System.out.println("x1:" + (new BigDecimal(env.getMaximum(0)).toString()));
//				System.out.println("y1:" + (new BigDecimal(env.getMaximum(1)).toString()));
//				System.out.println("x0:" + (new BigDecimal(fTG.getBounds().getMinimum(0)).toString()));
//				System.out.println("y0:" + (new BigDecimal(fTG.getBounds().getMinimum(1)).toString()));
//				System.out.println("x1:" + (new BigDecimal(fTG.getBounds().getMaximum(0)).toString()));
//				System.out.println("y1:" + (new BigDecimal(fTG.getBounds().getMaximum(1)).toString()));
				tiles[y][x] = new Tile(rect, level, resSet.length, include);
				tiles[y][x].setGridXY(ax, ay);
//				if(!fTG.getBounds().intersects(env, false)) {
//					tiles[y][x].setInclude(true);
//				}
//				
//				if(maxXTileNum < ax || maxYTileNum < ay || ax < 0 || ay < 0) {
//					tiles[y][x].setInclude(true);
//				}
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

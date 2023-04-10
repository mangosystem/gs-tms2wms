package com.mango.tms;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Properties;

import javax.imageio.ImageIO;

public class PathGenerator {

	public String fURLPattern;
	
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
		String level = Integer.toString(tile.getLevel());
		String row = Integer.toString(tile.getGridY());
		String col = Integer.toString(tile.getGridX());

		String realPath = replaceVariables(fURLPattern, "%SERVER%", server);
//		String realPath = fURLPattern;
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

		realPath = realPath + level + File.separator + row + File.separator + col;
//		realPath = realPath + level + "/";
//		realPath = realPath + col + "/";
//		realPath = realPath + row + (fURLPattern.substring(fURLPattern.lastIndexOf(".")));
		return realPath;
	}
	
	public BufferedImage getTileImage(TileGenerator fTG, Tile tile) {
		tile.setLevel(tile.getLevel() + (fTG.getfServceStartLevel() - 1));
		String path = buildPath(tile);

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
				HttpURLConnection conn = (HttpURLConnection) u.openConnection();
				InputStream is = null;
				// Referer:http://map.vworld.kr/map/maps.do
				conn.addRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36");
				if (conn.getResponseCode() == 200) {
					is = conn.getInputStream();
					BufferedImage bi = ImageIO.read(is);
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

					return bi;
				} else {
					return fTG.getBlank();
				}
			}
		} catch (Exception e) {
			return fTG.getBlank();
		}
	}
}

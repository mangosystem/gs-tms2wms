package com.mango.tms;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Properties;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;

import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TileGenerator {

	private final int fTileWidth;
	private final int fTileHeight;
	private final double fTileOriginX;
	private final double fTileOriginY;
	private final double[] fResolutions;
	private final CoordinateReferenceSystem fTileCRS;
	private BufferedImage fBlank;
	private final GeneralEnvelope fBounds;
	private IPathGenerator fPathGenerator;
	private final String fName;
	private final String fServceMinLevel;
	private final String fServceMaxLevel;
	private int fServceStartLevel;
	private String fPathYOrder;
	private boolean fOutline;
	private boolean fTileCache;
	private String fCahcePath;
	private int fUrlServerCnt;
	private int fUrlServerStart;
	private String fImageFormat;

	@SuppressWarnings("deprecation")
	public TileGenerator(Properties props) throws Exception {
		String value = props.getProperty("tile.width");
		if (value == null) {
			throw new RuntimeException("tile.width ");
		}
		fTileWidth = Integer.parseInt(value);

		value = props.getProperty("tile.height");
		if (value == null) {
			throw new RuntimeException("tile.height ");
		}
		fTileHeight = Integer.parseInt(value);

		value = props.getProperty("tile.origin.x");
		if (value == null) {
			throw new RuntimeException("tile.origin.x ");
		}
		fTileOriginX = Double.parseDouble(value);

		value = props.getProperty("tile.origin.y");
		if (value == null) {
			throw new RuntimeException("tile.origin.y ");
		}
		fTileOriginY = Double.parseDouble(value);

		value = props.getProperty("url.y_order");
		if (value == null || "".equals(value.trim())) {
			value = props.getProperty("url.y_order");
			if (value == null || "".equals(value.trim())) {
				value = "TB";
			}
		}

		fPathYOrder = value;
		if (fPathYOrder == null || "".equals(fPathYOrder)) {
			fPathYOrder = "TB";
		}
		if (!"TB".equalsIgnoreCase(fPathYOrder) && !"BT".equalsIgnoreCase(fPathYOrder)) {
			throw new Exception("Y_ORDER Policy only [TB] or [BT]");
		}

		value = props.getProperty("resolutions");
		String[] values = null;
		if (value == null || "".equals(value.trim())) {
			value = props.getProperty("maxresolution");
			if (value == null || "".equals(value.trim())) {
				value = props.getProperty("maxResolution");
			}
			String zoom = props.getProperty("zoomlevel");
			fResolutions = new double[Integer.parseInt(zoom.trim())];
			fResolutions[0] = Double.parseDouble(value.trim());
			for (int i = 1; i < fResolutions.length; i++) {
				fResolutions[i] = fResolutions[i - 1] / 2.;
			}
		} else {
			value = props.getProperty("resolutions");
			values = value.split(",");

			if (values == null || values.length < 1) {
				throw new RuntimeException("resolutions ");
			}

			fResolutions = new double[values.length];
			for (int i = 0; i < values.length; i++) {
				fResolutions[i] = Double.parseDouble(values[i].trim());
			}
		}
		CoordinateReferenceSystem crs = null;
		value = props.getProperty("tile.crs.code");
		if (value != null) {
			crs = CRS.decode(value);
		}
		value = props.getProperty("tile.crs.wkt");
		if (value != null) {
			crs = CRS.parseWKT(value);
		}
		if (crs == null) {
			throw new RuntimeException("tile.crs.code or tile.crs.wkt ");
		}
		fTileCRS = crs;

		value = props.getProperty("extent");
		if (value == null) {
			throw new RuntimeException("extent ");
		}
		values = props.getProperty("extent", "-180,-180,180,180").split(",");
		fBounds = new GeneralEnvelope(fTileCRS);
		fBounds.setEnvelope(Double.parseDouble(values[0]), Double.parseDouble(values[1]), Double.parseDouble(values[2]),
				Double.parseDouble(values[3]));
		value = props.getProperty("path.generator");
		if (value == null) {
			throw new RuntimeException("path.generator ");
		}
		fPathGenerator = (IPathGenerator) Class.forName(value).newInstance();
		fPathGenerator.init(props);

		value = props.getProperty("blank.image.url");
		if (value != null && !"".equals(value.trim())) {
			try {
				File file = new File(value);
				if (file.exists()) {
					fBlank = ImageIO.read(file);
				} else {
					fBlank = ImageIO.read(new URL(value));
				}
			} catch (Exception e) {
				fBlank = new BufferedImage(fTileWidth, fTileHeight, BufferedImage.TYPE_INT_ARGB);
				Graphics g = fBlank.getGraphics();
				g.setColor(new Color(255, 255, 255, 0));
				g.fillRect(0, 0, fTileWidth, fTileHeight);
				// throw new RuntimeException("blank.image.url ", e);
			}
		} else {
			fBlank = new BufferedImage(fTileWidth, fTileHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics g = fBlank.getGraphics();
			g.setColor(new Color(255, 255, 255, 0));
			g.fillRect(0, 0, fTileWidth, fTileHeight);
		}

		value = props.getProperty("layer.name");
		if (value == null) {
			value = "unknown";
		}
		fName = value;

		fServceMinLevel = props.getProperty("service.level.min");
		fServceMaxLevel = props.getProperty("service.level.max");

		try {
			fServceStartLevel = Integer.parseInt(props.getProperty("service.start.level", "1"));
		} catch (Exception e) {
			fServceStartLevel = 1;
		}
		
		try {
			fUrlServerCnt = Integer.parseInt(props.getProperty("url.server.count", "4"));
		} catch (Exception e) {
			fUrlServerCnt = 4;
		}
		
		try {
			fUrlServerStart = Integer.parseInt(props.getProperty("url.server.start", "0"));
		} catch (Exception e) {
			fUrlServerStart = 0;
		}
		
		fImageFormat = props.getProperty("image.format", "png");

		value = props.getProperty("outline");
		if (value == null) {
			fOutline = false;
		} else {
			fOutline = Boolean.parseBoolean(value.trim());
		}

		value = props.getProperty("tile.cache");
		if (value == null) {
			fTileCache = false;
		} else {
			fTileCache = Boolean.parseBoolean(value.trim());
		}

		if (fTileCache) {
			value = props.getProperty("cache.path");
			if (value == null) {
				fTileCache = false;
			} else {

				if (fTileCache) {
					if (!(new File(value + "")).exists()) {
						File cacheDir = new File(value + "");
						cacheDir.mkdirs();
					} else if ((new File(value + "")).isFile()) {
						throw new RuntimeException("cache.path is not directory");
					}

					fCahcePath = (new File(value + "")).toURI().toString();
				}
			}
		}
	}

	public double getOriginX() {
		return fTileOriginX;
	}

	public double getOriginY() {
		return fTileOriginY;
	}

	public double[] getResolutions() {
		return fResolutions;
	}

	public CoordinateReferenceSystem getTileCRS() {
		return fTileCRS;
	}

	public int getTileWidth() {
		return fTileWidth;
	}

	public int getTileHeight() {
		return fTileHeight;
	}

	public BufferedImage getTileImage(Tile tile) {
		// System.out.println(fPathGenerator.buildPath(tile));
		if (!tile.isInclude()) {
			//System.out.println("BLANK");
			return fBlank;
		}

		String path = fPathGenerator.buildPath(tile);
		//System.out.println(path);
		String cacheFilePath = "";
		cacheFilePath = fCahcePath + fPathGenerator.shortPath(tile);

		try {
			if (fTileCache) {
				File cacheFile = new File(new URI(cacheFilePath));
				if (cacheFile.exists()) {
					// System.out.println("CacheFile - " + cacheFilePath);
					return ImageIO.read(cacheFile);
				}
			}
		} catch (Exception e) {

		}

//		String path = fPathGenerator.buildPath(tile);
		// System.out.println(path);
		try {
			File file = new File(path);
			if (file.exists()) {
				return ImageIO.read(file);
			} else {
				BufferedImage bi = null;
				try {
					bi = ImageIO.read(new URL(path));
					if (fTileCache) {
						File f = new File(new URI(cacheFilePath));
						f.mkdirs();
						ImageIO.write(bi, path.substring(path.lastIndexOf(".") + 1), f);
					}
				} catch (IIOException iie) {

				}
				return bi;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return fBlank;
		}
	}

	public double getTileOriginX() {
		return fTileOriginX;
	}

	public double getTileOriginY() {
		return fTileOriginY;
	}

	public BufferedImage getBlank() {
		return fBlank;
	}

	public GeneralEnvelope getBounds() {
		return fBounds;
	}

	public IPathGenerator getPathGenerator() {
		return fPathGenerator;
	}

	public void setPathGenerator(IPathGenerator pathGenerator) {
		fPathGenerator = pathGenerator;
	}

	public String getName() {
		return fName;
	}

	public String getServceMinLevel() {
		return fServceMinLevel;
	}

	public String getServceMaxLevel() {
		return fServceMaxLevel;
	}

	public String getPathYOrder() {
		return fPathYOrder;
	}

	public void setPathYOrder(String pathYOrder) {
		fPathYOrder = pathYOrder;
	}

	public boolean isOutline() {
		return fOutline;
	}

	public void setOutline(boolean outline) {
		fOutline = outline;
	}

	public boolean isTileCache() {
		return fTileCache;
	}

	public void setTileCache(boolean tileCache) {
		fTileCache = tileCache;
	}

	public String getCahcePath() {
		return fCahcePath;
	}

	public void setCahcePath(String cahcePath) {
		fCahcePath = cahcePath;
	}

	public int getfServceStartLevel() {
		return fServceStartLevel;
	}

	public void setfServceStartLevel(int fServceStartLevel) {
		this.fServceStartLevel = fServceStartLevel;
	}

	public int getfUrlServerCnt() {
		return fUrlServerCnt;
	}

	public int getfUrlServerStart() {
		return fUrlServerStart;
	}

	public void setfUrlServerCnt(int fUrlServerCnt) {
		this.fUrlServerCnt = fUrlServerCnt;
	}

	public void setfUrlServerStart(int fUrlServerStart) {
		this.fUrlServerStart = fUrlServerStart;
	}

	public String getfImageFormat() {
		return fImageFormat;
	}

	public void setfImageFormat(String fImageFormat) {
		this.fImageFormat = fImageFormat;
	}

}

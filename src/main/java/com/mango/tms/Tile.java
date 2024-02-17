package com.mango.tms;

import org.geotools.geometry.GeneralEnvelope;

public class Tile {
	private Rect fRect;
	private int fGridX = 0;
	private int fGridY = 0;
	private int fLevel = 0;
	private int fNumLevel = 0;
	private boolean isInclude;
	private TileGenerator tileGenerator;
	private GeneralEnvelope env;

	public Tile(Rect rect, int level, int numLevel, boolean isInclude) {
		fNumLevel = numLevel;
		fRect = rect;
		fLevel = level;
		this.isInclude = isInclude;
	}
	
	public Tile(Rect rect, int level, int numLevel) {
		fNumLevel = numLevel;
		fRect = rect;
		fLevel = level;
		isInclude = true;
	}
	
	public Tile(Rect rect, int level) {
		fRect = rect;
		fLevel = level;
		isInclude = true;
	}

	public Rect getRect() {
		return fRect;
	}

	public void setRect(Rect rect) {
		fRect = rect;
	}

	public int getGridX() {
		return fGridX;
	}

	public void setGridX(int gridX) {
		fGridX = gridX;
	}

	public int getGridY() {
		return fGridY;
	}

	public void setGridY(int gridY) {
		fGridY = gridY;
	}

	public void setGridXY(int ax, int ay) {
		fGridX = ax;
		fGridY = ay;
	}

	public int getLevel() {
		return fLevel;
	}

	protected void setLevel(int level) {
		fLevel = level;
	}

	public int getNumLevel() {
		return fNumLevel;
	}

	public void setNumLevel(int numLevel) {
		fNumLevel = numLevel;
	}

	public boolean isInclude() {
		return isInclude;
	}

	public void setInclude(boolean isInclude) {
		this.isInclude = isInclude;
	}

	public TileGenerator getTileGenerator() {
		return tileGenerator;
	}

	public void setTileGenerator(TileGenerator tileGenerator) {
		this.tileGenerator = tileGenerator;
	}

	public GeneralEnvelope getEnv() {
		return env;
	}

	public void setEnv(GeneralEnvelope env) {
		this.env = env;
	}
}

/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2006-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package com.mango.tms;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.filter.FunctionFinder;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

import it.geosolutions.jaiext.BufferedImageAdapter;

public final class TMSReader extends AbstractGridCoverage2DReader implements GridCoverageReader {

	private final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(TMSReader.class.toString());

//	private URL sourceURL;
//	private String[] levelsDirs;
	private ConcurrentHashMap<Integer, ImageMosaicReader> readers = new ConcurrentHashMap<Integer, ImageMosaicReader>();

	private TileGenerator fTG = null;

	public TMSReader(Object source, Hints uHints) throws IOException {
		if (this.hints == null)
			this.hints = new Hints();
		if (uHints != null) {
			this.hints.add(uHints);
		}
		this.hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);

		this.coverageFactory = CoverageFactoryFinder.getGridCoverageFactory(this.hints);

		FileInputStream fis = new FileInputStream((File) source);
		Properties prop = new Properties();
		prop.load(fis);
		try {
			fTG = new TileGenerator(prop);
		} catch (Exception e) {
			e.printStackTrace();
		}

		super.crs = fTG.getTileCRS();
		super.originalEnvelope = fTG.getBounds();
		super.originalGridRange = new GridEnvelope2D(0, 0, fTG.getTileWidth(), fTG.getTileHeight());
		super.coverageName = fTG.getName();

		fis.close();
//		 super.originalGridRange = fTG.
	}

	// private void parseMainFile(final URL sourceURL) throws IOException {
	//
	// if(LOGGER.isLoggable(Level.FINE)){
	// LOGGER.fine("Parsing pyramid properties file
	// at:"+sourceURL.toExternalForm());
	// }
	// BufferedInputStream propertyStream = null;
	// InputStream openStream = null;
	// try {
	// openStream = sourceURL.openStream();
	// propertyStream = new BufferedInputStream(openStream);
	// final Properties properties = new Properties();
	// properties.load(propertyStream);
	//
	// // load the envelope
	// final String envelope = properties.getProperty("Envelope2D");
	// String[] pairs = envelope.split(" ");
	// final double cornersV[][] = new double[2][2];
	// String pair[];
	// for (int i = 0; i < 2; i++) {
	// pair = pairs[i].split(",");
	// cornersV[i][0] = Double.parseDouble(pair[0]);
	// cornersV[i][1] = Double.parseDouble(pair[1]);
	// }
	// this.originalEnvelope = new GeneralEnvelope(cornersV[0],cornersV[1]);
	// this.originalEnvelope.setCoordinateReferenceSystem(crs);
	//
	// // overviews dir
	// numOverviews = Integer.parseInt(properties.getProperty("LevelsNum")) - 1;
	// levelsDirs = properties.getProperty("LevelsDirs").split(" ");
	//
	// // resolutions levels
	// final String levels = properties.getProperty("Levels");
	// pairs = levels.split(" ");
	// overViewResolutions = numOverviews >= 1 ? new double[numOverviews][2]:
	// null;
	// pair = pairs[0].split(",");
	// highestRes = new double[2];
	// highestRes[0] = Double.parseDouble(pair[0].trim());
	// highestRes[1] = Double.parseDouble(pair[1].trim());
	// for (int i = 1; i < numOverviews + 1; i++) {
	// pair = pairs[i].split(",");
	// overViewResolutions[i - 1][0] = Double.parseDouble(pair[0].trim());
	// overViewResolutions[i - 1][1] = Double.parseDouble(pair[1].trim());
	// }
	//
	// // name
	// coverageName = properties.getProperty("Name");
	//
	// // original gridrange (estimated)
	// originalGridRange = new GridEnvelope2D(
	// new Rectangle(
	// (int) Math.round(originalEnvelope.getSpan(0)/ highestRes[0]),
	// (int) Math.round(originalEnvelope.getSpan(1)/ highestRes[1])
	// )
	// );
	// final GridToEnvelopeMapper geMapper= new
	// GridToEnvelopeMapper(originalGridRange,originalEnvelope);
	// geMapper.setPixelAnchor(PixelInCell.CELL_CORNER);
	// raster2Model= geMapper.createTransform();
	//
	// if(LOGGER.isLoggable(Level.FINE)){
	// LOGGER.fine("Parsed pyramid properties file at:"+sourceURL.toExternalForm());
	// }
	// } finally {
	// // close input stream
	// if (propertyStream != null)
	// IOUtils.closeQuietly(propertyStream);
	//
	// if (openStream != null)
	// IOUtils.closeQuietly(openStream);
	// }
	//
	// }

	public TMSReader(Object source) throws IOException {
		this(source, null);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opengis.coverage.grid.GridCoverageReader#getFormat()
	 */
	public Format getFormat() {
		return new TMSFormat();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opengis.coverage.grid.GridCoverageReader#read(org.opengis.parameter
	 * .GeneralParameterValue[])
	 */
	public GridCoverage2D read(GeneralParameterValue[] params) throws IOException {
		// light check to see if this reader had been disposed, not synching for
		// performance. We'll check again later on.
		if (readers == null) {
			throw new IllegalStateException("This ImagePyramidReader has already been disposed");
		}

		ReferencedEnvelope requestedEnvelope1 = null;
		Rectangle dim1 = null;
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				@SuppressWarnings("rawtypes")
				final ParameterValue param = (ParameterValue) params[i];
				if (param == null) {
					continue;
				}
				final String name = param.getDescriptor().getName().getCode();
				if (name.equals(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString())) {
					final GridGeometry2D gg = (GridGeometry2D) param.getValue();
					requestedEnvelope1 = new ReferencedEnvelope((Envelope) gg.getEnvelope2D());
					dim1 = gg.getGridRange2D().getBounds();
					continue;
				}
			}
		}
		
		FunctionFinder ff = new FunctionFinder(null);
		List<Expression> exps = new ArrayList<>();
		Literal l = new LiteralExpressionImpl("lpath");
		exps.add(l);
		Function envFunc = ff.findFunction("env", exps);
		String lpath = envFunc.evaluate("lpath", String.class);
		
		System.out.println("<< lpath : " + lpath + " >>");

		ReferencedEnvelope transformedEnvelope = null;
		if (!CRS.equalsIgnoreMetadata(requestedEnvelope1.getCoordinateReferenceSystem(), fTG.getTileCRS())) {
			try {
				transformedEnvelope = requestedEnvelope1.transform(fTG.getTileCRS(), true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			transformedEnvelope = requestedEnvelope1;
		}

		double[] resSet = fTG.getResolutions();
		int level = getLevel(transformedEnvelope.getMinimum(0), transformedEnvelope.getMinimum(1),
				transformedEnvelope.getMaximum(0), transformedEnvelope.getMaximum(1), dim1.width, dim1.height);

		double centerx = transformedEnvelope.getMinimum(0)
				+ (transformedEnvelope.getMaximum(0) - transformedEnvelope.getMinimum(0)) / 2.;
		double centery = transformedEnvelope.getMinimum(1)
				+ (transformedEnvelope.getMaximum(1) - transformedEnvelope.getMinimum(1)) / 2.;
		int width = (int) Math
				.round(((transformedEnvelope.getMaximum(0) - transformedEnvelope.getMinimum(0)) / resSet[level - 1]));
		int height = (int) Math
				.round(((transformedEnvelope.getMaximum(1) - transformedEnvelope.getMinimum(1)) / resSet[level - 1]));

		BufferedImage bi = fTG.getPathGenerator().getMap(fTG, level, centerx, centery, width, height);

		// BufferedImage bi = new BufferedImage(dim1.width, dim1.height,
		// BufferedImage.TYPE_INT_ARGB);
		// // bi.getGraphics().setColor(new Color(255, 0, 0));
		// // bi.getGraphics().fillRect(0, 0, dim1.width, dim1.height);
		// Graphics g = bi.getGraphics();
		// g.setColor(new Color(255, 0, 0, 100));
		// g.fillRect(0, 0, dim1.width, dim1.height);
		// g.setColor(new Color(0, 0, 0));
		// g.drawString(dim1.width+":"+dim1.height + ":" + requestedEnvelope1,
		// 0, dim1.height/2);
		//
		// g.drawImage(ImageIO.read(new
		// URL("http://sports.phinf.naver.net//20130611_94/1370958350857JNEeR_JPEG/Untitled-2.jpg")),
		// 0, 0, null);
		//
		// System.out.println(requestedEnvelope1 + ":" + dim1.width + ":" +
		// dim1.height + ":" + g.getColor().getAlpha() +":" +
		// g.getColor().getRed() +":" + g.getColor().getGreen() +":" +
		// g.getColor().getBlue() +
		// "======================================================================================================================================================================================================================================================");
//		RenderedImage ri = null;
//		try {
//			ri = new BufferedImageAdapter(bi);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		return (new GridCoverageFactory()).create(fTG.getName(), bi, requestedEnvelope1);

		// GeneralEnvelope requestedEnvelope = null;
		// Rectangle dim = null;
		// OverviewPolicy overviewPolicy=null;
		// if (params != null) {
		//
		// //
		// // Checking params
		// //
		//
		// if (params != null) {
		// for (int i = 0; i < params.length; i++) {
		// @SuppressWarnings("rawtypes")
		// final ParameterValue param = (ParameterValue) params[i];
		// if (param == null){
		// continue;
		// }
		// final String name = param.getDescriptor().getName().getCode();
		// if
		// (name.equals(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString()))
		// {
		// final GridGeometry2D gg = (GridGeometry2D) param.getValue();
		// requestedEnvelope = new
		// GeneralEnvelope((Envelope)gg.getEnvelope2D());
		// dim = gg.getGridRange2D().getBounds();
		// continue;
		// }
		// if
		// (name.equals(AbstractGridFormat.OVERVIEW_POLICY.getName().toString()))
		// {
		// overviewPolicy = (OverviewPolicy) param.getValue();
		// continue;
		// }
		// }
		// }
		// }
		//
		// //
		// // Loading tiles
		// //
		// return loadTiles(requestedEnvelope, dim, params, overviewPolicy);
	}

//	private GridCoverage2D loadTiles(GeneralEnvelope requestedEnvelope, Rectangle dim, GeneralParameterValue[] params,
//			OverviewPolicy overviewPolicy) throws IOException {
//
//		//
//		// Check if we have something to load by intersecting the requested
//		// envelope with the bounds of the data set.
//		//
//		// If the requested envelope is not in the same crs of the data set crs
//		// we have to perform a conversion towards the latter crs before
//		// intersecting anything.
//		//
//
//		if (requestedEnvelope != null) {
//			if (!CRS.equalsIgnoreMetadata(requestedEnvelope.getCoordinateReferenceSystem(), this.crs)) {
//				try {
//					// transforming the envelope back to the data set crs
//					final MathTransform transform = CRS
//							.findMathTransform(requestedEnvelope.getCoordinateReferenceSystem(), crs, true);
//					if (!transform.isIdentity()) {
//						requestedEnvelope = CRS.transform(transform, requestedEnvelope);
//						requestedEnvelope.setCoordinateReferenceSystem(this.crs);
//
//						if (LOGGER.isLoggable(Level.FINE))
//							LOGGER.fine(new StringBuilder("Reprojected envelope ").append(requestedEnvelope.toString())
//									.append(" crs ").append(crs.toWKT()).toString());
//					}
//				} catch (TransformException e) {
//					throw new DataSourceException("Unable to create a coverage for this source", e);
//				} catch (FactoryException e) {
//					throw new DataSourceException("Unable to create a coverage for this source", e);
//				}
//			}
//			if (!requestedEnvelope.intersects(this.originalEnvelope, false))
//				return null;
//
//			// intersect the requested area with the bounds of this layer
//			requestedEnvelope.intersect(originalEnvelope);
//
//		} else {
//			requestedEnvelope = new GeneralEnvelope(originalEnvelope);
//
//		}
//		requestedEnvelope.setCoordinateReferenceSystem(this.crs);
//		// ok we got something to return
//		try {
//			return loadRequestedTiles(requestedEnvelope, dim, params, overviewPolicy);
//		} catch (TransformException e) {
//			throw new DataSourceException(e);
//		}
//
//	}

//	private GridCoverage2D loadRequestedTiles(GeneralEnvelope requestedEnvelope, Rectangle dim,
//			GeneralParameterValue[] params, OverviewPolicy overviewPolicy) throws TransformException, IOException {
//
//		// if we get here we have something to load
//
//		//
//		// compute the requested resolution
//		//
//		final ImageReadParam readP = new ImageReadParam();
//		Integer imageChoice = 0;
//		if (dim != null)
//			imageChoice = setReadParams(overviewPolicy, readP, requestedEnvelope, dim);
//
//		//
//		// Check to have the needed reader in memory
//		//
//
//		// light check to see if this reader had been disposed, not synching for
//		// performance.
//		if (readers == null) {
//			throw new IllegalStateException("This ImagePyramidReader has already been disposed");
//		}
//
//		ImageMosaicReader reader = readers.get(imageChoice);
//		if (reader == null) {
//
//			//
//			// we must create the underlying mosaic
//			//
//			final String levelDirName = levelsDirs[imageChoice.intValue()];
//			final URL parentUrl = URLs.getParentUrl(sourceURL);
//			// look for a shapefile first
//			final String extension = new StringBuilder(levelDirName).append("/").append(coverageName).append(".shp")
//					.toString();
//			final URL shpFileUrl = URLs.extendUrl(parentUrl, extension);
//			if (shpFileUrl.getProtocol() != null && shpFileUrl.getProtocol().equalsIgnoreCase("file")
//					&& !URLs.urlToFile(shpFileUrl).exists())
//				reader = new ImageMosaicReader(URLs.extendUrl(parentUrl, levelDirName), hints);
//			else
//				reader = new ImageMosaicReader(shpFileUrl, hints);
//			final ImageMosaicReader putByOtherThreadJustNow = readers.putIfAbsent(imageChoice, reader);
//			if (putByOtherThreadJustNow != null) {
//				// some other thread just did inserted this
//				try {
//					reader.dispose();
//				} catch (Exception e) {
//					if (LOGGER.isLoggable(Level.FINE)) {
//						LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
//					}
//				}
//
//				// use the other one
//				reader = putByOtherThreadJustNow;
//			}
//
//		}
//
//		//
//		// Abusing of the created ImageMosaicreader for getting a
//		// gridcoverage2d, then rename it
//		//
//		GridCoverage2D mosaicCoverage = reader.read(params);
//		if (mosaicCoverage != null) {
//			return new GridCoverage2D(coverageName, mosaicCoverage);
//		} else {
//			// the mosaic can still return null in corner cases, handle that
//			// gracefully
//			return null;
//		}
//	}

	@Override
	public synchronized void dispose() {
		super.dispose();

		// dispose all the underlying ImageMosaicReader if we need to
		if (readers == null) {
			return;
		}
		for (ImageMosaicReader reader : readers.values()) {
			try {
				reader.dispose();
			} catch (Exception e) {
				if (LOGGER.isLoggable(Level.FINE)) {
					LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
				}
			}
		}
		// now clear the map
		try {

			readers.clear();
		} catch (Exception e) {
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
			}
		}
		readers.clear();
		readers = null;
	}

	@Override
	public int getGridCoverageCount() {

		return 1;
	}

//	double[] getHighestRes() {
//		return highestRes;
//	}

	public int getLevel1(double minX, double minY, double maxX, double maxY, int width, int height) {
		double[] resSet = fTG.getResolutions();
		double res = (maxX - minX) / (double) width;
		double delta = Double.NaN;
		int level = resSet.length - 1;
		for (int i = resSet.length - 1; i >= 0; i--) {
			double x = resSet[i] - res;
			double d = Math.abs(x);
			if (Double.isNaN(delta)) {
				level = i;
				delta = d;
			} else if (d < delta) {
				delta = d;
				level = i;
			}
		}
		return level + 1;
	}

	public int getLevel(double minX, double minY, double maxX, double maxY, int width, int height) {
		double[] resolutions = fTG.getResolutions();
		double targetResolution = (maxX - minX) / (double) width;
		int level = resolutions.length - 1;
		double smallestResolutionDifference = Double.MAX_VALUE;

		for (int i = resolutions.length - 1; i >= 0; i--) {
			double resolutionDifference = Math.abs(resolutions[i] - targetResolution);
			if (resolutionDifference < smallestResolutionDifference) {
				smallestResolutionDifference = resolutionDifference;
				level = i;
			}
		}
		// System.out.println("minX : " + minX);
		// System.out.println("minY : " + minY);
		// System.out.println("maxX : " + maxX);
		// System.out.println("maxY : " + maxY);
		System.out.println("smallestResolutionDifference : " + smallestResolutionDifference);
		System.out.println("selectResolutionDifference : " + resolutions[level]);
		System.out.println("lvl : " + (level + 1));
		return level + 1;
	}

}

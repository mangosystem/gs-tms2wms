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
import java.awt.image.renderable.ParameterBlock;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBicubic;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.DataSourceException;
import org.geotools.filter.FunctionFinder;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

import it.geosolutions.jaiext.BufferedImageAdapter;

public final class TMSReader extends AbstractGridCoverage2DReader implements GridCoverageReader {

	private final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(TMSReader.class.toString());

//	private URL sourceURL;
//	private String[] levelsDirs;
	private ConcurrentHashMap<Integer, ImageMosaicReader> readers = new ConcurrentHashMap<Integer, ImageMosaicReader>();

	private TileGenerator fTG = null;
	private ImageReaderSpi readerSPI = null;

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
		if (fTG.getServiceBounds() != null) {
			super.originalEnvelope = fTG.getServiceBounds();
		}
		super.originalGridRange = new GridEnvelope2D(0, 0, fTG.getTileWidth(), fTG.getTileHeight());
		super.coverageName = fTG.getName();

		fis.close();

		final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("png");
		if (!it.hasNext())
			throw new DataSourceException("No reader avalaible for this source");
		final ImageReader reader = it.next();
		readerSPI = reader.getOriginatingProvider();
	}

	public TMSReader(Map<String, Object> map, Hints uHints) throws IOException {
		if (this.hints == null)
			this.hints = new Hints();
		if (uHints != null) {
			this.hints.add(uHints);
		}
		this.hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);

		this.coverageFactory = CoverageFactoryFinder.getGridCoverageFactory(this.hints);

		try {
			fTG = new TileGenerator(map);
		} catch (Exception e) {
			e.printStackTrace();
		}

		super.crs = fTG.getTileCRS();
		super.originalEnvelope = fTG.getBounds();
		if (fTG.getServiceBounds() != null) {
			super.originalEnvelope = fTG.getServiceBounds();
		}
		super.originalGridRange = new GridEnvelope2D(0, 0, fTG.getTileWidth(), fTG.getTileHeight());
		super.coverageName = fTG.getName();

		final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("png");
		if (!it.hasNext())
			throw new DataSourceException("No reader avalaible for this source");
		final ImageReader reader = it.next();
		readerSPI = reader.getOriginatingProvider();
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

	public GridCoverage2D read(GeneralParameterValue[] params) throws IOException {
		String sType = System.getProperty("tms2wms.type", "1");
		if (sType.equals("2")) {
			return read2(params);
		} else {
			return read1(params);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opengis.coverage.grid.GridCoverageReader#read(org.opengis.parameter
	 * .GeneralParameterValue[])
	 */
	public GridCoverage2D read2(GeneralParameterValue[] params) throws IOException {
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
		return (new GridCoverageFactory()).create(fTG.getName(), bi, requestedEnvelope1);
	}

	public GridCoverage2D read1(GeneralParameterValue[] params) throws IOException {
		// light check to see if this reader had been disposed, not synching for
		// performance. We'll check again later on.
		FunctionFinder ff = new FunctionFinder(null);
		List<Expression> exps = new ArrayList<>();
		Literal l = new LiteralExpressionImpl("wms_bbox");
		exps.add(l);
		Function envFunc = ff.findFunction("env", exps);
		ReferencedEnvelope envelope = envFunc.evaluate("wms_bbox", ReferencedEnvelope.class);

		exps = new ArrayList<>();
		l = new LiteralExpressionImpl("wms_crs");
		exps.add(l);
		envFunc = ff.findFunction("env", exps);
		CoordinateReferenceSystem crs = envFunc.evaluate("wms_crs", CoordinateReferenceSystem.class);

		exps = new ArrayList<>();
		l = new LiteralExpressionImpl("wms_width");
		exps.add(l);
		envFunc = ff.findFunction("env", exps);
		Integer reqWidth = envFunc.evaluate("wms_width", Integer.class);

		exps = new ArrayList<>();
		l = new LiteralExpressionImpl("wms_height");
		exps.add(l);
		envFunc = ff.findFunction("env", exps);
		Integer reqHeight = envFunc.evaluate("wms_height", Integer.class);

		exps = new ArrayList<>();
		l = new LiteralExpressionImpl("wms_srs");
		exps.add(l);
		envFunc = ff.findFunction("env", exps);
		String srs = envFunc.evaluate("wms_srs", String.class);

		exps = new ArrayList<>();
		l = new LiteralExpressionImpl("pattern");
		exps.add(l);
		envFunc = ff.findFunction("env", exps);
		String pattern = envFunc.evaluate("pattern", String.class);
		if (pattern == null || "".equals(pattern)) {
			pattern = null;
		}

		exps = new ArrayList<>();
		l = new LiteralExpressionImpl("max_level");
		exps.add(l);
		envFunc = ff.findFunction("env", exps);
		String maxLevelStr = envFunc.evaluate("max_level", String.class);
		Integer maxLevel = null;
		if (maxLevelStr == null || "".equals(maxLevelStr)) {
			maxLevel = null;
		} else {
			try {
				maxLevel = Integer.parseInt(maxLevelStr);
			} catch (Exception e) {

			}
		}

		if (fTG.isOutline()) {
			System.out.println("PATTERN : " + pattern);
		}

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

		if (fTG.getServiceMaxLevel() != null) {
			if (level > fTG.getServiceMaxLevel()) {
				level = fTG.getServiceMaxLevel();
			}
		}
		
		if(maxLevel != null) {
			if (level > maxLevel) {
				level = maxLevel;
			}
		}

		double centerx = transformedEnvelope.getMinimum(0)
				+ (transformedEnvelope.getMaximum(0) - transformedEnvelope.getMinimum(0)) / 2.;
		double centery = transformedEnvelope.getMinimum(1)
				+ (transformedEnvelope.getMaximum(1) - transformedEnvelope.getMinimum(1)) / 2.;
		int width = (int) Math
				.round(((transformedEnvelope.getMaximum(0) - transformedEnvelope.getMinimum(0)) / resSet[level - 1]));
		int height = (int) Math
				.round(((transformedEnvelope.getMaximum(1) - transformedEnvelope.getMinimum(1)) / resSet[level - 1]));

		BufferedImage bi = null;
		if (fTG.getPathGenerator() instanceof DynamicPathGenerator) {
			DynamicPathGenerator dp = (DynamicPathGenerator) fTG.getPathGenerator();
			bi = dp.getMap(fTG, level, centerx, centery, width, height, pattern);
		} else {
			bi = fTG.getPathGenerator().getMap(fTG, level, centerx, centery, width, height);
		}

		Integer imageChoice = Integer.valueOf(0);
		final ImageReadParam readP = new ImageReadParam();
		final Hints newHints = hints.clone();
		// Interpolation inter = new InterpolationNearest();
		// newHints.put(JAI.KEY_INTERPOLATION, inter);

		final ParameterBlock pbjRead = new ParameterBlock();
		pbjRead.add(convertBufferedImageToImageInputStream(bi));
		// pbjRead.add(wmsRequest ? ImageIO
		// .createImageInputStream(((URL) source).openStream()) : ImageIO
		// .createImageInputStream(source));
		pbjRead.add(imageChoice);
		pbjRead.add(Boolean.FALSE);
		pbjRead.add(Boolean.FALSE);
		pbjRead.add(Boolean.FALSE);
		pbjRead.add(null);
		pbjRead.add(null);
		pbjRead.add(readP);
		pbjRead.add(readerSPI.createReaderInstance());
		final RenderedOp coverageRaster = JAI.create("ImageRead", pbjRead, newHints);

		// GridCoverage2D 객체를 생성합니다.
		// GridCoverage2D gridCoverage = coverageFactory.create("ImageCoverage", bi,
		// requestedEnvelope1, sampleDimensions);
		// raster2Model= geMapper.createTransform();

		GridEnvelope2D gridRange2 = new GridEnvelope2D(new Rectangle((int) width, (int) height));
		final GridToEnvelopeMapper geMapper = new GridToEnvelopeMapper(gridRange2, requestedEnvelope1);
		geMapper.setPixelAnchor(PixelInCell.CELL_CORNER);
		// 그리고 생성된 gridCoverage 객체를 사용합니다.
		MathTransform rasterToModel = geMapper.createTransform();

//		final GridToEnvelopeMapper geMapper2 =
//                new GridToEnvelopeMapper(gridRange2, transformedEnvelope);
//		geMapper2.setPixelAnchor(PixelInCell.CELL_CORNER);
		// 그리고 생성된 gridCoverage 객체를 사용합니다.
		return createImageCoverage(coverageRaster, rasterToModel);
	}

	public static ImageInputStream convertBufferedImageToImageInputStream(BufferedImage bufferedImage)
			throws IOException {
		// BufferedImage의 픽셀 데이터를 바이트 배열로 저장
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

		// 바이트 배열을 ByteArrayInputStream으로 변환
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

		// ByteArrayInputStream을 ImageInputStream으로 변환
		ImageInputStream imageInputStream = ImageIO.createImageInputStream(byteArrayInputStream);

		return imageInputStream;
	}

	public GridCoverage2D getCoverage(int level, ReferencedEnvelope env) {
		double centerx = (env.getMaximum(0) + env.getMinimum(0)) / 2;
		double centery = (env.getMaximum(1) + env.getMinimum(1)) / 2;

		double res = getTG().getResolutions()[level - getTG().getfServceStartLevel() - 1];

		int width = (int) ((env.getMaximum(0) - env.getMinimum(0)) / res);
		int height = (int) ((env.getMaximum(1) - env.getMinimum(1)) / res);

		int reqLevel = getLevel(env.getMinimum(0), env.getMinimum(1), env.getMaximum(0), env.getMaximum(1), width,
				height);

		BufferedImage bi = fTG.getPathGenerator().getMap(fTG, reqLevel, centerx, centery, width, height);

//		try {
//			FileOutputStream fos = new FileOutputStream("E:\\smartseoulmap_blob\\export\\xxx.png");
//			ImageIO.write(bi, "png", fos);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		ReferencedEnvelope refEnv = new ReferencedEnvelope(env.getMinimum(0), env.getMaximum(0), env.getMinimum(1),
				env.getMaximum(1), env.getCoordinateReferenceSystem());
		return (new GridCoverageFactory()).create(fTG.getName(), bi, refEnv);
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
		// System.out.println("smallestResolutionDifference : " +
		// smallestResolutionDifference);
		// System.out.println("selectResolutionDifference : " + resolutions[level]);
		// System.out.println("lvl : " + (level + 1));
		return level + 1;
	}

	public TileGenerator getTG() {
		return fTG;
	}

	public void setfTG(TileGenerator TG) {
		this.fTG = TG;
	}

}

package com.mango.tms;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;

public final class TMSFormat extends AbstractGridFormat implements Format {

	private final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("com.mango.tms");

	private static final String SUGGESTED_TILESIZE = "SUGGESTED_TILE_SIZE";
	public static final ParameterDescriptor<String> SUGGESTED_TILE_SIZE = new DefaultParameterDescriptor<String>(
			SUGGESTED_TILESIZE, String.class, null, "256,256");

	public static final String TILE_SIZE_SEPARATOR = ",";

	/** Control the type of the final mosaic. */
	public static final ParameterDescriptor<Boolean> FADING = new DefaultParameterDescriptor<Boolean>("Fading",
			Boolean.class, new Boolean[] { Boolean.TRUE, Boolean.FALSE }, Boolean.FALSE);

	/** Control the transparency of the input coverages. */
	public static final ParameterDescriptor<Color> INPUT_TRANSPARENT_COLOR = new DefaultParameterDescriptor<Color>(
			"InputTransparentColor", Color.class, null, null);

	/** Control the transparency of the output coverage. */
	public static final ParameterDescriptor<Color> OUTPUT_TRANSPARENT_COLOR = new DefaultParameterDescriptor<Color>(
			"OutputTransparentColor", Color.class, null, null);

	/** Control the thresholding on the input coverage */
	public static final ParameterDescriptor<Integer> MAX_ALLOWED_TILES = new DefaultParameterDescriptor<Integer>(
			"MaxAllowedTiles", Integer.class, null, Integer.MAX_VALUE);

	/**
	 * Control the threading behavior for this plugin. This parameter contains the
	 * number of thread that we should use to load the granules. Default value is 0
	 * which means not additional thread, max value is 8.
	 */
	public static final ParameterDescriptor<Boolean> ALLOW_MULTITHREADING = new DefaultParameterDescriptor<Boolean>(
			"AllowMultithreading", Boolean.class, new Boolean[] { Boolean.TRUE, Boolean.FALSE }, Boolean.FALSE);

	/** Control the background values for the output coverage */
	public static final ParameterDescriptor<double[]> BACKGROUND_VALUES = new DefaultParameterDescriptor<double[]>(
			"BackgroundValues", double[].class, null, null);

	/**
	 * Creates an instance and sets the metadata.
	 */
	public TMSFormat() {
		setInfo();
	}

	/**
	 * Sets the metadata information for this format
	 */
	private void setInfo() {
		HashMap<String, String> info = new HashMap<String, String>();
		info.put("name", "TMS");
		info.put("description", "TMS2WMS Service");
		info.put("vendor", "onSpatial inc");
		info.put("docURL", "");
		info.put("version", "1.0");
		mInfo = info;

		// reading parameters
		readParameters = new ParameterGroup(new DefaultParameterDescriptorGroup(mInfo,
				new GeneralParameterDescriptor[] { READ_GRIDGEOMETRY2D, INPUT_TRANSPARENT_COLOR,
						OUTPUT_TRANSPARENT_COLOR, USE_JAI_IMAGEREAD, BACKGROUND_VALUES, SUGGESTED_TILE_SIZE,
						ALLOW_MULTITHREADING, MAX_ALLOWED_TILES }));

		writeParameters = null;
	}

	/**
	 * Retrieves a reader for this source object in case the provided source can be
	 * read using this plugin.
	 * 
	 * @param source Object
	 * @return An {@link TMSReader} if the provided object can be read using this
	 *         plugin or null.
	 */
	@Override
	public TMSReader getReader(Object source) {
		return getReader(source, null);
	}

	/**
	 * This methods throw an {@link UnsupportedOperationException} because this
	 * plugiin si read only.
	 */
	@Override
	public GridCoverageWriter getWriter(Object destination) {
		throw new UnsupportedOperationException("This plugin is a read only plugin!");
	}

	/**
	 * Takes the input and determines if it is a class that we can understand and
	 * then futher checks the format of the class to make sure we can read/write to
	 * it.
	 *
	 * @param input The object to check for acceptance.
	 * @return true if the input is acceptable, false otherwise
	 */
	@Override
	public boolean accepts(Object input, Hints hints) {
//		String pathname = "";

		if (input instanceof URL) {
			final URL url = (URL) input;
			final String protocol = url.getProtocol();
			if (protocol.equalsIgnoreCase("file")) {}
//				pathname = URLs.urlToFile(url).getPath();
			else {
				if (protocol.equalsIgnoreCase("http")) {
					return false;
				}
			}
		} else if (input instanceof File) {
//			File file = (File) input;
//			pathname = file.getAbsolutePath();
		} else if (input instanceof String) {
//			pathname = (String) input;
		} else {
			return false;
		}

//		if (!(pathname.endsWith(".properties"))) {
//			return false;
//		}

		// check it's a valid input file
//		Properties prop = new Properties();
//		prop.load(null);
//		
//		try (ImageInputStream is = ImageIO.createImageInputStream(new File(pathname))) {
//			Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
//			return readers.hasNext();
//		} catch (IOException e) {
//			return false;
//		}
		
		return true;

	}

	/**
	 * Retrieves a reader for this source object in case the provided source can be
	 * read using this plugin.
	 * 
	 * @param source Object
	 * @param hints  {@link Hints} to control the reader behaviour.
	 * @return An {@link TMSReader} if the provided object can be read using this
	 *         plugin or null.
	 */
	@Override
	public TMSReader getReader(Object source, Hints hints) {
		try {

			return new TMSReader(source, hints);
		} catch (MalformedURLException e) {
			if (LOGGER.isLoggable(Level.SEVERE))
				LOGGER.severe(new StringBuffer("impossible to get a reader for the provided source. The error is ")
						.append(e.getLocalizedMessage()).toString());
			return null;
		} catch (IOException e) {
			if (LOGGER.isLoggable(Level.SEVERE))
				LOGGER.severe(new StringBuffer("impossible to get a reader for the provided source. The error is ")
						.append(e.getLocalizedMessage()).toString());
			return null;
		}
	}

	/**
	 * Throw an exception since this plugin is readonly.
	 * 
	 * @return nothing.
	 */
	@Override
	public GeoToolsWriteParams getDefaultImageIOWriteParameters() {
		throw new UnsupportedOperationException("Unsupported method.");
	}

	@Override
	public GridCoverageWriter getWriter(Object destination, Hints hints) {
		throw new UnsupportedOperationException("Unsupported method.");
	}

	@Override
	public ParameterValueGroup getWriteParameters() {
		// TODO Auto-generated method stub
		return super.writeParameters;
	}

	@Override
	public ParameterValueGroup getReadParameters() {
		// TODO Auto-generated method stub
		return super.readParameters;
	}
}

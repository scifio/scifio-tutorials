
package io.scif.tutorials.core;

import io.scif.*;
import io.scif.config.SCIFIOConfig;
import io.scif.gui.BufferedImageReader;
import io.scif.io.RandomAccessInputStream;
import io.scif.io.RandomAccessOutputStream;
import io.scif.util.FormatTools;
import io.scif.util.SCIFIOMetadataTools;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;

import org.scijava.plugin.Plugin;

public class T3aCustomFormat {

	/**
	 * This sample class teaches how to add support for a custom format in SCIFIO
	 * <p>
	 * The goal of SCIFIO is to make implementing Formats as fast and clear as
	 * possible. If you find yourself struggling to create your own format even
	 * after reading through this example, you may find it helpful to look over
	 * <a href=
	 * "https://github.com/scifio/scifio/tree/73ec0996195724e2d589b5ab169f547083f783f1/src/main/java/io/scif/formats"
	 * > existing formats</a>, and the original <a href=
	 * "https://github.com/openmicroscopy/bioformats/tree/develop/components/formats-bsd/src/loci/formats/in"
	 * >Bio-Formats classes</a> from which they were derived.
	 * </p>
	 * <p>
	 * The {@link Format} class itself has three purposes:
	 * <ul>
	 * <li>Act as the unit of discovery, via the SciJava plugin mechanism</li>
	 * <li>Define the name and extension(s) of the format</li>
	 * <li>Define and generate the functional SCIFIO component classes</li>
	 * </ul>
	 * </p>
	 * <p>
	 * There are six potential component types in a {@code Format}:
	 * <ol>
	 * <li>Metadata</li>
	 * <li>Checker</li>
	 * <li>Parser</li>
	 * <li>Reader</li>
	 * <li>Writer</li>
	 * <li>Translator(s)</li>
	 * </ol>
	 * We will cover each here, and explain if and why you would implement each.
	 * </p>
	 *
	 * @author Richard Domander (Royal Veterinary College, London)
	 * @author Mark Hiner - documentation
	 */
	// The Plugin annotation allows the format to be discovered automatically -
	// satisfying the first role of the Format
	@Plugin(type = Format.class)
	public static class FictionalImageFormat extends AbstractFormat {

		/** A byte sequence which identifies a FIF file */
		public static final byte[] FIF_ID = { 0xC, 0x0, 0xF, 0xF, 0xE, 0xE };

		/**
		 * The length of the format's header section in bytes
		 * <p>
		 * The header contains the metadata of the file. You need to know its size
		 * so that you can start reading image data from the right position.
		 * </p>
		 */
		public static final int HEADER_LENGTH = 80;

		/**
		 * Declares a name for our {@link Format}
		 * <p>
		 * Not necessary to {@link Override}, but a good practice since otherwise
		 * the name will be empty
		 * </p>
		 */
		@Override
		public String getFormatName() {
			return "Fictional image format";
		}

		/**
		 * Lists the suffix(es) the {@link Format} is can open
		 * <p>
		 * NB: you shouldn't put a leading separator ('.') in the suffix Strings.
		 * </p>
		 * <p>
		 * NB: when adding your format to SCIFIO, remember to put the suffix to
		 * io.scif.service.FormatServiceTest.testGetSuffixes
		 * </p>
		 */
		@Override
		protected String[] makeSuffixArray() {
			return new String[] { "fif" };
		}

		// *** REQUIRED COMPONENTS ***

		/**
		 * The Metadata class contains all format-specific metadata. Your Metadata
		 * class should be filled with fields which define the image format. For
		 * example, things like acquisition date, instrument, excitation levels,
		 * etc. In the implementation of populateImageMetadata, the format-specific
		 * metadata is converted to a generalized ImageMetadata which can be
		 * consumed by other components (e.g. readers/writers). As the conversion to
		 * ImageMetadata is almost certainly lossy, preserving the original
		 * format-specific metadata provides components like Translators an
		 * opportunity to preserve as much original information as possible.
		 * <p>
		 * Each format-specific field in your Metadata class should be private, with
		 * a public get(), and set() methods if necessary. These fields should be
		 * annotated with {@link io.scif.Field} notations, and a label indicating
		 * the field's original name (as it might not be properly represented by
		 * Java camelCase naming conventions).
		 * </p>
		 */
		public static class Metadata extends AbstractMetadata {

			/** Unit of calibration */
			public static final String UNIT = "mm";

			/** The date format used in FIF. Does not include a time */
			public static final String DATE_FORMAT = "ddMMyyyy";

			/** Length of the instrument name String */
			public static final int INSTRUMENT_LENGTH = 20;

			/** Width of image in voxels */
			@Field(label = "Width")
			private int width;

			/** Height of image in voxels */
			@Field(label = "Height")
			private int height;

			/** Depth of image in voxel */
			@Field(label = "Depth")
			private int depth;

			/** Actual width of sample in mm */
			@Field(label = "Physical width")
			private int physicalWidth;

			/** Actual height of sample in mm */
			@Field(label = "Physical height")
			private int physicalHeight;

			/** Actual depth of sample in mm. There's no space between the slices */
			@Field(label = "Physical depth")
			private int physicalDepth;

			/**
			 * Date the image was created
			 * <p>
			 * NB Java 8 introduces a new Date/Time API, which improves on the old one
			 * in many ways. The {@link LocalDate} class is a part of the modern API.
			 * </p>
			 */
			@Field(label = "Depth")
			private LocalDate acquisitionDate = LocalDate.of(1900, 1, 1);

			/** Name of the instrument */
			@Field(label = "Instrument name")
			private String instrument = new String(new byte[INSTRUMENT_LENGTH]);

			/** Excitation level in eV */
			@Field(label = "Excitation level")
			private double excitationLevel;

			/** Voxel size in x dimension */
			private double xScale;

			/** Voxel size in y dimension */
			private double yScale;

			/** Voxel size in z dimension */
			private double zScale;

			/** Number of bytes in a whole plane of the image (w * h) */
			private long planeSize;

			public LocalDate getAcquisitionDate() {
				return acquisitionDate;
			}

			/** Returns the acquisition date as a ddMMyyyy int */
			public int getDateInt() {
				final int day = acquisitionDate.getDayOfMonth();
				final int month = acquisitionDate.getMonthValue();
				final int year = acquisitionDate.getYear();
				return day * 1_000_000 + month * 10_000 + year;
			}

			public int getDepth() {
				return depth;
			}

			public double getExcitationLevel() {
				return excitationLevel;
			}

			public int getHeight() {
				return height;
			}

			public String getInstrument() {
				return instrument;
			}

			/**
			 * Create a {@link String} that's padded with spaces (" ") to ensure that
			 * its length equals {@link #INSTRUMENT_LENGTH}
			 */
			public String getPaddedInstrument() {
				final StringBuilder builder = new StringBuilder(INSTRUMENT_LENGTH);
				for (int i = 0; i < INSTRUMENT_LENGTH; i++) {
					builder.append(" ");
				}

				return builder.replace(0, instrument.length(), instrument).toString();
			}

			public int getPhysicalDepth() {
				return physicalDepth;
			}

			public int getPhysicalHeight() {
				return physicalHeight;
			}

			public int getPhysicalWidth() {
				return physicalWidth;
			}

			public int getWidth() {
				return width;
			}

			public void setAcquisitionDate(final int date) {
				final String dateString = String.valueOf(date);
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
				try {
					acquisitionDate = LocalDate.parse(dateString, formatter);
				}
				catch (DateTimeParseException exception) {
					acquisitionDate = LocalDate.of(1900, 1, 1);
				}
			}

			public void setDepth(final int depth) {
				this.depth = depth;
			}

			public void setExcitationLevel(final double excitationLevel) {
				this.excitationLevel = excitationLevel;
			}

			public void setHeight(final int height) {
				this.height = height;
			}

			public void setInstrument(final String instrument) {
				if (instrument.length() > INSTRUMENT_LENGTH) {
					this.instrument = instrument.substring(0, INSTRUMENT_LENGTH).trim();
					return;
				}
				this.instrument = instrument.trim();
			}

			public void setPhysicalDepth(final int physicalDepth) {
				this.physicalDepth = physicalDepth;
			}

			public void setPhysicalHeight(final int physicalHeight) {
				this.physicalHeight = physicalHeight;
			}

			public void setPhysicalWidth(final int physicalWidth) {
				this.physicalWidth = physicalWidth;
			}

			public void setWidth(final int width) {
				this.width = width;
			}

			/** Calculates the scales of the image's axes from the metadata */
			private void calculateScales() {
				xScale = 1.0 * getPhysicalWidth() / getWidth();
				yScale = 1.0 * getPhysicalHeight() / getHeight();
				zScale = 1.0 * getPhysicalDepth() / getDepth();
			}

			/**
			 * This method must be implemented for a Metadata class. Essentially,
			 * format-specific metadata is be populated during Parsing or Translation.
			 * From the format-specific metadata, {@link ImageMetadata} information,
			 * common to all formats - such as height, width, etc - is populated here.
			 */
			@Override
			public void populateImageMetadata() {
				createImageMetadata(1);
				final ImageMetadata imageMeta = get(0);
				// FIF files are written in the little endian (least significant
				// byte first) order
				imageMeta.setLittleEndian(true);
				// FIF images are 16-bit
				imageMeta.setBitsPerPixel(16);
				// FIF store unsigned 16-bit values, having this field incorrect is
				// often a source of weird colours in the image
				imageMeta.setPixelType(FormatTools.UINT16);
				// We know for certain that FIF stores images in the x,y,z order
				imageMeta.setOrderCertain(true);
				// Defines how an N-dimensional image is divided into sub-regions.
				// Most often two. In the case of a 3D image, there are two planer
				// axes: X and Y, i.e. the 3D "stack" is divided into 2D "planes".
				// A 5D "hyperstack" with channel and time dimensions would be
				// divided into 2D planes as well.
				imageMeta.setPlanarAxisCount(2);

				calculateScales();

				// Add CalibratedAxes to the image. They describe the dimensions of the
				// image. Each dimension can have a type (spatial i.e. X,Y,Z, Channel,
				// Time...), unit and a scale. Adding scales and units is similar to
				// calling pixelWidth, setUnit etc. in the IJ1 Calibration class.
				// NB The ImageMetadata needs to have at least the X and Y axes present
				// to make use of AbstractReader.readPlane
				imageMeta.setAxes(new DefaultLinearAxis(Axes.X, UNIT, xScale),
					new DefaultLinearAxis(Axes.Y, UNIT, yScale), new DefaultLinearAxis(
						Axes.Z, UNIT, zScale));
				// Set the sizes of the dimensions in voxels. Same order as above.
				imageMeta.setAxisLengths(new long[] { width, height, depth });
				setPlaneSize();
			}

			/** Sets the number of pixels in an image plane */
			private void setPlaneSize() {
				final ImageMetadata imageMetadata = get(0);
				if (imageMetadata == null) {
					return;
				}
				final int bytesPerPixel = imageMetadata.getBitsPerPixel() / 8;
				planeSize = width * height * bytesPerPixel;
			}

			public long getPlaneSize() {
				return planeSize;
			}
		}

		/**
		 * The {@code Parser} is your interface with the image source. It has one
		 * purpose: to take the raw image information and generate a
		 * {@code Metadata} instance, populating all format-specific fields.
		 */
		public static class Parser extends AbstractParser<Metadata> {

			/**
			 * In this method we populate the given Metadata object
			 *
			 * @param stream A binary stream pointing to a FIF image
			 * @param meta A new instance of the format's {@link Metadata}
			 * @param config The current configuration of the environment. It has a
			 *          number of configuration options you may want to check
			 * @throws IOException Thrown if there's an error in reading the stream
			 */
			@Override
			protected void typedParse(RandomAccessInputStream stream, Metadata meta,
				SCIFIOConfig config) throws IOException, FormatException
			{
				// Because FIF is little endian we need to read the stream in that order
				stream.order(true);
				// Set the stream to the start of the metadata, after the id sequence
				stream.seek(FIF_ID.length);
				// Read the dimensions of the image. Each read advances the position in
				// the stream by the number of bytes in the type, e.g. four in the case
				// of a 32-bit int.
				meta.setWidth(stream.readInt());
				meta.setHeight(stream.readInt());
				meta.setDepth(stream.readInt());
				meta.setPhysicalWidth(stream.readInt());
				meta.setPhysicalHeight(stream.readInt());
				meta.setPhysicalDepth(stream.readInt());
				// Read the date saved as an integer
				meta.setAcquisitionDate(stream.readInt());
				// Read the name of the instrument used
				meta.setInstrument(stream.readString(Metadata.INSTRUMENT_LENGTH));
				// Read the excitation level
				meta.setExcitationLevel(stream.readDouble());

				// If true, then image will be displayed by mapping pixel values in the
				// range min-max to screen values in the range 0-255. Depending on your
				// format, the image may show really dark without this setting. Similar
				// to ImageProcessor.setMinAndMax
				config.imgOpenerSetComputeMinMax(true);

				// If the MetadataLevel is MINIMUM, you only need to populate the
				// minimum needed for generating ImageMetadata - extra "fluff" can
				// be skipped. Note that this is more of a performance option - if
				// ignored your format should still work, it just may be slower than
				// optimal in some circumstances.
				config.parserGetLevel();

				// If this flag is set, it signifies a desire to save the raw original
				// metadata, beyond what is represented in this format's fields.
				config.parserIsSaveOriginalMetadata();
			}
		}

		/**
		 * The purpose of the {@link Checker} is to determine if an image source is
		 * compatible with this {@link Format}. If you just want to use basic
		 * extension checking to determine compatibility, you do not need to
		 * implement any methods in this class - it's already handled for you in the
		 * {@link AbstractChecker} class.
		 * <p>
		 * However, if your format embeds an identifying flag - e.g. a magic string
		 * or number - then it should override
		 * {@link AbstractChecker#suffixSufficient},
		 * {@link AbstractChecker#suffixNecessary} and
		 * {@link AbstractChecker#isFormat(RandomAccessInputStream)} as appropriate.
		 * </p>
		 */
		public static class Checker extends AbstractChecker {

			/**
			 * By default, this method returns true, indicating that extension match
			 * alone is sufficient to determine compatibility. If this method returns
			 * false, then the {@link #isFormat(RandomAccessInputStream)} method will
			 * need to be checked.
			 */
			@Override
			public boolean suffixSufficient() {
				return false;
			}

			/**
			 * If suffixSufficient returns true, this method has no meaning. Otherwise
			 * if this method returns true (the default) then the extension will have
			 * to match in addition to the result of
			 * {@link #isFormat(RandomAccessInputStream)} If this returns false, then
			 * {@link #isFormat(RandomAccessInputStream)} is solely responsible for
			 * determining compatibility.
			 */
			@Override
			public boolean suffixNecessary() {
				return false;
			}

			/**
			 * By default, this method returns false and is not considered during
			 * extension checking. If your format uses a magic string, etc... then you
			 * should override this method and check for the string or value as
			 * appropriate.
			 */
			@Override
			public boolean isFormat(final RandomAccessInputStream stream)
				throws IOException
			{
				// A FIF file starts with the byte sequence 0xC, 0x0, 0xF, 0xF, 0xE,
				// 0xE, we check that the file is of this format by reading its first
				// six bytes and comparing them to this sequence
				final byte[] firstBytes = new byte[FIF_ID.length];
				// File might be shorter than 6 bytes, so check that at least six bytes
				// got read
				final int read = stream.read(firstBytes);
				return read == FIF_ID.length && Arrays.equals(FIF_ID, firstBytes);
			}
		}

		/**
		 * The Reader component uses parsed {@link Metadata} to determine how to
		 * extract pixel data from an image source. In the core SCIFIO library, an
		 * image plane can be returned as {@link ByteArrayPlane} or
		 * {@link BufferedImagePlane}, based on which {@link Reader} class is
		 * extended. Note that a {@link BufferedImageReader} converts BufferedImages
		 * to byte[], so a {@link ByteArrayReader} is typically faster and the
		 * default choice here. But select the class that makes the most sense for
		 * your format.
		 */
		public static class Reader extends ByteArrayReader<Metadata> {

			/**
			 * You must declare what domains your reader is associated with, based on
			 * the list of constants {@link FormatTools}. It is also sufficient to
			 * return an empty array here.
			 */
			@Override
			protected String[] createDomainArray() {
				// FIF images come from a scanning electron microscope
				return new String[] { FormatTools.SEM_DOMAIN };
			}

			/**
			 * The purpose of this method is to populate the provided {@link Plane}
			 * object by reading from the specified image and plane indices in the
			 * underlying image source.
			 * <p>
			 * planeMin and planeMax are dimensional indices determining the requested
			 * sub-region offsets into the specified plane. They correspond to image
			 * dimensions if the format uses relatively small images. However SCIFIO
			 * can also open very large images in tiles if they wouldn't fit memory
			 * otherwise. A tile is a sub-region of a slice in the image. In principle
			 * opening an image in tiles corresponds to opening a "virtual stack" in
			 * ImageJ1.
			 * </p>
			 */
			@Override
			public ByteArrayPlane openPlane(int imageIndex, long planeIndex,
				ByteArrayPlane plane, long[] planeMin, long[] planeMax,
				SCIFIOConfig config) throws FormatException, IOException
			{
				final RandomAccessInputStream stream = getStream();
				// Set the stream's position to the start of the plane's image data so
				// that bytes are read from to correct index
				stream.seek(HEADER_LENGTH + planeIndex * getMetadata().planeSize);
				// If your image format has unencoded data, the easiest thing to do is
				// to use the existing readPlane implementations.
				// See e.g. io.scif.formats.PCXFormat or JPEGFormat for examples how to
				// read encoded or compressed image data.
				return readPlane(stream, imageIndex, planeMin, planeMax, plane);
			}
		}

		// *** OPTIONAL COMPONENTS ***

		/**
		 * Writers are not implemented for proprietary formats, as doing so
		 * typically violates licensing. However, if your format is open source you
		 * are welcome to implement a writer.
		 */
		public static class Writer extends AbstractWriter<Metadata> {

			/**
			 * If your writer supports a compression type, you can declare that here.
			 * Otherwise it is sufficient to return an empty String[]
			 */
			@Override
			protected String[] makeCompressionTypes() {
				return new String[0];
			}

			/**
			 * Sets the source that will be written to during {@link #savePlane}
			 * calls. NB: resets any configuration on this writer.
			 *
			 * @param out The image source to write to.
			 */
			@Override
			public void setDest(final RandomAccessOutputStream out,
				final int imageIndex, final SCIFIOConfig config) throws FormatException,
				IOException
			{
				super.setDest(out, imageIndex, config);
				// Ensure that nothing has been written to the stream yet, so that we
				// can write the header
				if (getStream().length() == 0) writeHeader();
			}

			/**
			 * Writes the header of a FIF file to the output stream
			 */
			private void writeHeader() throws IOException {
				final RandomAccessOutputStream output = getStream();
				output.order(true);
				// Rewind the stream to file start
				output.seek(0);
				// Write the metadata into the file header
				final Metadata metadata = getMetadata();
				output.write(FIF_ID);
				output.writeInt(metadata.width);
				output.writeInt(metadata.height);
				output.writeInt(metadata.depth);
				output.writeInt(metadata.physicalWidth);
				output.writeInt(metadata.physicalHeight);
				output.writeInt(metadata.physicalDepth);
				output.writeInt(metadata.getDateInt());
				output.writeBytes(metadata.getPaddedInstrument());
				output.writeDouble(metadata.excitationLevel);
			}

			/**
			 * Writers take a source plane and save it to their attached output
			 * stream. The image and plane indices are references to the final output
			 * dataset
			 */
			@Override
			public void writePlane(int imageIndex, long planeIndex, Plane plane,
				long[] planeMin, long[] planeMax) throws FormatException, IOException
			{
				final byte[] buffer = plane.getBytes();
				// Ensure that the parameters are valid (throws an exception if not)
				checkParams(imageIndex, planeIndex, buffer, planeMin, planeMax);

				final Metadata meta = getMetadata();
				// Check that we're writing an entire plane i.e. slice of the image
				if (!SCIFIOMetadataTools.wholePlane(imageIndex, meta, planeMin,
					planeMax))
				{
					throw new FormatException(
						"FIF writer does not support writing image tiles");
				}

				// Check that the image has a compatible pixel type (e.g UINT16)
				final int type = meta.get(imageIndex).getPixelType();
				if (!pixelTypeSupported(type)) {
					final String typeString = FormatTools.getPixelTypeString(type);
					throw new FormatException("Unsupported image type " + typeString);
				}

				// Calculate the starting byte of this plane in the output stream
				final long width = planeMax[0];
				final long height = planeMax[1];
				final int bpp = FormatTools.getBytesPerPixel(type);
				final long planeStart = planeIndex * width * height * bpp;
				final RandomAccessOutputStream output = getStream();
				output.order(true);
				// Seek to the start of this plane
				output.seek(planeStart);
				// Write all the bytes of the plane in the output
				output.write(buffer);
			}

			/**
			 * Checks whether the pixel type is supported
			 *
			 * @param type The pixel type identifier as enumerated in
			 *          {@link FormatTools}
			 * @return True if supported, false if not
			 */
			private boolean pixelTypeSupported(int type) {
				final int[] types = getPixelTypes(getCompression());
				return Arrays.stream(types).anyMatch(t -> t == type);
			}
		}

		/**
		 * The purpose of a {@link Translator} is similar to that of a
		 * {@link Parser}: to populate the format-specific metadata of a
		 * {@link Metadata} object. However, while a {@link Parser} reads from an
		 * image source to perform this operation, a {@link Translator} reads from a
		 * {@link Metadata} object of another format.
		 * <p>
		 * There are two main reasons when you would want to implement a
		 * {@link Translator}:
		 * <ol>
		 * <li>If you implement a {@link Writer}, you should also implement a
		 * {@link Translator} to describe how {@link io.scif.Metadata} should be
		 * translated to your {@link Format}-specific metadata. This translator will
		 * then be called whenever SCIFIO writes out your format, and it will be
		 * able to handle any input format type. Essentially this is translating
		 * {@link ImageMetadata} to your format-specific metadata.</li>
		 * <li>If you are adding support for a new {@link Metadata} schema to
		 * SCIFIO, you will probably want to create Translators to and from your new
		 * {@link Metadata} schema and core SCIFIO Metadata classes. The purpose of
		 * these Translators is to more accurately or richly capture metadata
		 * information, without the lossy {@link ImageMetadata} intermediate that
		 * would be used by default translators.</li>
		 * </ol>
		 * This is a more advanced use case but mentioned for completeness. See
		 * <a href=
		 * "https://github.com/scifio/scifio-ome-xml/tree/master/src/main/java/io/scif/ome/translators">
		 * https://github.com/scifio/scifio-ome-xml/tree/master/src/main/java/io/scif/ome/translators</a>
		 * for examples of this case.
		 * </p>
		 */
		public static class Translator extends
			AbstractTranslator<io.scif.Metadata, Metadata>
		{

			/**
			 * Here we use the state in the ImageMetadata to populate format-specific
			 * metadata
			 */
			@Override
			protected void translateImageMetadata(List<ImageMetadata> source,
				Metadata dest)
			{
				ImageMetadata imageMeta = source.get(0);
				dest.setWidth(getAxisLength(imageMeta, Axes.X));
				dest.setHeight(getAxisLength(imageMeta, Axes.Y));
				dest.setDepth(getAxisLength(imageMeta, Axes.Z));
				dest.setPhysicalWidth(getCalibratedAxisLength(imageMeta, Axes.X));
				dest.setPhysicalHeight(getCalibratedAxisLength(imageMeta, Axes.Y));
				dest.setPhysicalDepth(getCalibratedAxisLength(imageMeta, Axes.Z));
			}

			/**
			 * Returns the calibrated length of the given axis
			 */
			private int getCalibratedAxisLength(ImageMetadata imageMeta,
				AxisType type)
			{
				// Return zero if metadata doesn't have the given axis type
				final CalibratedAxis axis = imageMeta.getAxis(type);
				if (axis == null) {
					return 0;
				}

				// Return length as is if the calibration isn't in the same unit as in
				// the format. Could use UnitService to try to convert the calibration
				// to millimetres, but omitted here for brevity
				final String unit = axis.unit();
				final long length = imageMeta.getAxisLength(axis);
				if (!Metadata.UNIT.equals(unit)) {
					return (int) length;
				}

				// Calculate and return the calibrated length of the axis
				final double scale = axis.averageScale(0.0, 1.0);
				return (int) Math.round(scale * length);
			}

			/**
			 * Returns the length of the given axis, i.e. the size of the dimension
			 */
			private int getAxisLength(ImageMetadata imageMeta, AxisType type) {
				final CalibratedAxis axis = imageMeta.getAxis(type);
				return axis == null ? 0 : (int) imageMeta.getAxisLength(axis);
			}

			/** Used for finding matching Translators */
			@Override
			public Class<? extends io.scif.Metadata> source() {
				return io.scif.Metadata.class;
			}

			/** Used for finding matching Translators */
			@Override
			public Class<? extends io.scif.Metadata> dest() {
				return io.scif.Metadata.class;
			}
		}
	}
}


package io.scif.tutorials.core;

import static io.scif.tutorials.core.T3aCustomFormat.FictionalImageFormat.*;
import static org.junit.Assert.*;

import io.scif.ByteArrayPlane;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.config.SCIFIOConfig;
import io.scif.formats.FakeFormat;
import io.scif.io.ByteArrayHandle;
import io.scif.io.RandomAccessInputStream;
import io.scif.io.RandomAccessOutputStream;
import io.scif.tutorials.core.T3aCustomFormat.FictionalImageFormat;
import io.scif.util.FormatTools;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDate;
import java.util.List;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.scijava.Context;

/**
 * Tests for {@link FictionalImageFormat} in {@link T3aCustomFormat}
 *
 * @author Richard Domander (Royal Veterinary College, London)
 */
public class FictionalImageFormatTest {

	// Create a context for testing, in a normal run it's automatically created by
	// SciJava
	private static final Context context = new Context();
	private static final Checker checker = new Checker();
	private static Parser parser;
	private static final FictionalImageFormat format = new FictionalImageFormat();

	@BeforeClass
	public static void oneTimeSetup() throws Exception {
		format.setContext(context);
		// Casting Parser to FictionalImageFormat.Parser
		parser = (Parser) format.createParser();
	}

	@AfterClass
	public static void oneTimeTearDown() throws Exception {
		// It's good manners to clear the resources your tests. This essential when
		// running multiple test cases, because you don't want the context created
		// here to affect other tests
		context.dispose();
	}

	/** Test that matching fails if file is too short */
	@Test
	public void testIsFormatFalseShortStream() throws Exception {
		final RandomAccessInputStream stream = new RandomAccessInputStream(context,
			new byte[] { 0xC, 0x0, 0xF });

		assertFalse(checker.isFormat(stream));
	}

	/** Test that matching fails if file has wrong start */
	@Test
	public void testIsFormatFalseIncorrectBytes() throws Exception {
		final RandomAccessInputStream stream = new RandomAccessInputStream(context,
			new byte[] { 0xC, 0x0, 0xF, 0xF, 0xE, 0xF });

		assertFalse(checker.isFormat(stream));
	}

	@Test
	public void testIsFormat() throws Exception {
		// Add an extra byte to the end to check that it doesn't affect the result
		final RandomAccessInputStream stream = new RandomAccessInputStream(context,
			new byte[] { 0xC, 0x0, 0xF, 0xF, 0xE, 0xE, 0x1 });

		assertTrue(checker.isFormat(stream));
	}

	/**
	 * Test that {@link Metadata} doesn't allow instrument names longer than
	 * {@link Metadata#INSTRUMENT_LENGTH}
	 */
	@Test
	public void testSetInstrumentCutsLength() throws Exception {
		final String instrument =
			"Lentokonesuihkuturbiinimoottoriapumekaanikkoaliupseerioppilas";
		// Cast io.scif.Metadata to FictionalImageFormat.Metadata
		final Metadata metadata = (Metadata) format.createMetadata();

		metadata.setInstrument(instrument);

		assertEquals(Metadata.INSTRUMENT_LENGTH, metadata.getInstrument().length());
	}

	/** Test that image metadata is populated correctly */
	@Test
	public void testPopulateImageMetadata() throws Exception {
		// SETUP
		final Metadata metadata = (Metadata) format.createMetadata();
		final AxisType[] types = { Axes.X, Axes.Y, Axes.Z };
		final int[] dimensions = { 10, 11, 12 };
		final int[] physicalDimensions = { 20, 26, 36 };
		metadata.setWidth(dimensions[0]);
		metadata.setPhysicalWidth(physicalDimensions[0]);
		metadata.setHeight(dimensions[1]);
		metadata.setPhysicalHeight(physicalDimensions[1]);
		metadata.setDepth(dimensions[2]);
		metadata.setPhysicalDepth(physicalDimensions[2]);

		// EXECUTE
		metadata.populateImageMetadata();

		// VERIFY
		final ImageMetadata imgMeta = metadata.get(0);
		assertTrue("Format should be little endian", imgMeta.isLittleEndian());
		assertTrue("Order should be certain", imgMeta.isOrderCertain());
		assertEquals("Should be 16 bits per pixel", 16, imgMeta.getBitsPerPixel());
		assertEquals("Wrong pixel type", FormatTools.UINT16, imgMeta
			.getPixelType());
		assertEquals("Wrong number of planar axes", 2, imgMeta
			.getPlanarAxisCount());
		final List<CalibratedAxis> axes = imgMeta.getAxes();
		assertEquals("Wrong number of axes", 3, axes.size());
		for (int i = 0; i < axes.size(); i++) {
			final CalibratedAxis axis = axes.get(i);
			assertEquals("Axis is wrong type", types[i], axis.type());
			assertEquals("Axis is wrong size", dimensions[i], imgMeta.getAxisLength(
				axis));
			assertEquals("Axis has wrong unit", Metadata.UNIT, axis.unit());
			final double expectedScale = 1.0 * physicalDimensions[i] / dimensions[i];
			assertEquals("Axis has wrong scale", expectedScale, axis.averageScale(0,
				1), 1e-12);

		}
	}

	/** Test that metadata from a FIF file header is parsed correctly */
	@Test
	public void testTypedParse() throws Exception {
		// SETUP
		final int width = 12;
		final int height = 13;
		final int depth = 14;
		final int physicalWidth = 24;
		final int physicalHeight = 26;
		final int physicalDepth = 28;
		final int date = 21122012;
		final String instrument = "Initech microscope  ";
		final double excitation = 0.12345;
		// Bytes in a plane = w * h * 2B (16-bits per pixel)
		final long planeSize = width * height * 2;
		// Create a mock FIF file
		final ByteBuffer buffer = ByteBuffer.allocate(HEADER_LENGTH);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		// Move to start of metadata (after FIF identifier)
		buffer.position(FIF_ID.length);
		// Write data to mock file
		buffer.putInt(width);
		buffer.putInt(height);
		buffer.putInt(depth);
		buffer.putInt(physicalWidth);
		buffer.putInt(physicalHeight);
		buffer.putInt(physicalDepth);
		buffer.putInt(date);
		buffer.put(instrument.getBytes());
		buffer.putDouble(excitation);
		final RandomAccessInputStream stream = new RandomAccessInputStream(context,
			buffer.array());
		final SCIFIOConfig config = new SCIFIOConfig();
		final Metadata metadata = new Metadata();

		// EXERCISE
		parser.typedParse(stream, metadata, config);
		metadata.populateImageMetadata();

		// VERIFY
		assertEquals("Width parsed incorrectly", width, metadata.getWidth());
		assertEquals("Height parsed incorrectly", height, metadata.getHeight());
		assertEquals("Depth parsed incorrectly", depth, metadata.getDepth());
		assertEquals("Physical width parsed incorrectly", physicalWidth, metadata
			.getPhysicalWidth());
		assertEquals("Physical height parsed incorrectly", physicalHeight, metadata
			.getPhysicalHeight());
		assertEquals("Physical depth parsed incorrectly", physicalDepth, metadata
			.getPhysicalDepth());
		assertEquals("Date parsed incorrectly", LocalDate.of(2012, 12, 21), metadata
			.getAcquisitionDate());
		assertEquals("Instrument parsed incorrectly", instrument.trim(), metadata
			.getInstrument());
		assertEquals("Excitation level parsed incorrectly", excitation, metadata
			.getExcitationLevel(), 1e-12);
		assertEquals("Plane size incorrect", planeSize, metadata.getPlaneSize());
	}

	// Not testing Reader since its pretty much just calls to existing
	// functionality
	@Test
	public void testOpenPlane() throws Exception {
		// SETUP
		final int width = 10;
		final int height = 10;
		final int depth = 3;
		final int planeBytes = width * height * 2;
		final int imageBytes = depth * planeBytes;
		final long[] planeMin = { 0, 0, 0 };
		final long[] planeMax = { width, height, depth };
		// Create a plane where the image data is read
		final ByteArrayPlane plane = new ByteArrayPlane(context);
		plane.setData(new byte[planeBytes]);
		// Create an input stream to simulate an image file
		final ByteBuffer buffer = ByteBuffer.allocate(HEADER_LENGTH + imageBytes);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.position(FIF_ID.length);
		// We only need image dimensions metadata for this test
		buffer.putInt(width);
		buffer.putInt(height);
		buffer.putInt(depth);
		final RandomAccessInputStream stream = new RandomAccessInputStream(context,
			buffer.array());
		// Cast io.scif.Reader to FictionalImageFormat.Reader
		final Reader reader = (Reader) format.createReader();
		reader.setSource(stream);

		// EXECUTE
		// Read the second plane from the stream
		reader.openPlane(0, 1, plane, planeMin, planeMax, new SCIFIOConfig());

		// VERIFY
		// Test if the stream has advanced to the correct position, i.e. if the
		// plane was read from the correct position in the file
		assertEquals(
			"Position of stream incorrect: should point to the beginning of the 3rd slice",
			HEADER_LENGTH + 2 * planeBytes, stream.getFilePointer());
	}

	@Test
	public void testWriteHeader() throws Exception {
		// SETUP
		// Create an instance of Metadata
		final Metadata meta = (Metadata) format.createMetadata();
		meta.setWidth(12);
		meta.setHeight(13);
		meta.setDepth(14);
		meta.setPhysicalWidth(24);
		meta.setPhysicalHeight(26);
		meta.setPhysicalDepth(27);
		meta.setAcquisitionDate(21122012);
		meta.setInstrument("Initech microscope  ");
		meta.setExcitationLevel(0.12345);
		meta.populateImageMetadata();
		// Create an output stream for the Writer to write into
		final ByteArrayHandle handle = new ByteArrayHandle();
		final RandomAccessOutputStream stream = new RandomAccessOutputStream(
			handle);
		final SCIFIOConfig config = new SCIFIOConfig();
		// Create an instance of Writer
		final Writer writer = (Writer) format.createWriter();
		writer.setMetadata(meta);

		// EXECUTE
		writer.setDest(stream, 0, config);

		// VERIFY
		// Seek to the beginning of metadata (after file identifier)
		handle.seek(FIF_ID.length);
		assertEquals("Width written incorrectly", meta.getWidth(), handle
			.readInt());
		assertEquals("Height written incorrectly", meta.getHeight(), handle
			.readInt());
		assertEquals("Depth written incorrectly", meta.getDepth(), handle
			.readInt());
		assertEquals("Physical width written incorrectly", meta.getPhysicalWidth(),
			handle.readInt());
		assertEquals("Physical height written incorrectly", meta
			.getPhysicalHeight(), handle.readInt());
		assertEquals("Physical depth written incorrectly", meta.getPhysicalDepth(),
			handle.readInt());
		assertEquals("Date written incorrectly", meta.getDateInt(), handle
			.readInt());
		byte[] bytes = new byte[Metadata.INSTRUMENT_LENGTH];
		handle.read(bytes);
		assertEquals(meta.getPaddedInstrument(), new String(bytes));
		assertEquals(meta.getExcitationLevel(), handle.readDouble(), 1e-15);
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	/**
	 * Tests that the writePlane() method throws the correct {@link Exception} if
	 * were trying to write an image tile instead of a whole plane
	 */
	@Test
	public void testWritePlaneThrowsExceptionIfNotWholePlane() throws Exception {
		// Setup the exception we expect
		thrown.expect(FormatException.class);
		thrown.expectMessage("FIF writer does not support writing image tiles");
		// Setup the Writer to write an image
		final Metadata meta = (Metadata) format.createMetadata();
		final int width = 10;
		meta.setWidth(width);
		final int height = 10;
		meta.setHeight(height);
		final int depth = 1;
		meta.setDepth(depth);
		meta.populateImageMetadata();
		final Writer writer = (Writer) format.createWriter();
		writer.setMetadata(meta);
		final RandomAccessOutputStream stream = new RandomAccessOutputStream(
			new ByteArrayHandle());
		final SCIFIOConfig config = new SCIFIOConfig();
		final ByteArrayPlane plane = new ByteArrayPlane(context, meta.get(0),
			new long[3], new long[] { width, height, depth });
		writer.setDest(stream, 0, config);

		// This call should now throw the expected FormatException
		writer.writePlane(0, 0, plane, new long[] { 1, 1, 0 }, new long[] { 5, 5,
			0 });
	}

	// Similarly we should test here that a FormatException is thrown if
	// imageMetadata has an unsupported pixel type (omitted for brevity)

	/**
	 * Tests if {@link Translator} translates {@link ImageMetadata} correctly to
	 * format specific {@link Metadata}
	 * <p>
	 * NB {@link Translator} isn't tested comprehensively, since we don't check if
	 * it handles missing axes etc. correctly. Tests left out for brevity.
	 * </p>
	 */
	@Test
	public void testTranslateImageMetadata() throws Exception {
		// SETUP
		// Create a file of FakeFormat with image metadata
		final String fakeImage =
			"16bit-unsigned&pixelType=uint16&indexed=false&planarDims=2&lengths=10,11,3&axes=X,Y,Z&scales=0.5,0.4,0.3&units=mm,mm,mm.fake";
		final FakeFormat fakeFormat = new FakeFormat();
		fakeFormat.setContext(context);
		final FakeFormat.Parser fakeParser = (FakeFormat.Parser) fakeFormat
			.createParser();
		final FakeFormat.Metadata source = fakeParser.parse(fakeImage);
		final Metadata dest = (Metadata) format.createMetadata();
		final Translator translator = new Translator();

		// EXECUTE
		translator.translate(source, dest);

		// VERIFY
		assertEquals("Width from ImageMetadata translated incorrectly", 10, dest
			.getWidth());
		assertEquals("Physical width from ImageMetadata translated incorrectly", 5,
			dest.getPhysicalWidth());
		assertEquals("Height from ImageMetadata translated incorrectly", 11, dest
			.getHeight());
		assertEquals("Physical height from ImageMetadata translated incorrectly", 4,
			dest.getPhysicalHeight());
		assertEquals("Depth from ImageMetadata translated incorrectly", 3, dest
			.getDepth());
		assertEquals("Physical depth from ImageMetadata translated incorrectly", 1,
			dest.getPhysicalDepth());
	}

}

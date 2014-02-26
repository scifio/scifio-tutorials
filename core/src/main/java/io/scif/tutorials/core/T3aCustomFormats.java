/*
 * #%L
 * SCIFIO tutorials for core and plugin use.
 * %%
 * Copyright (C) 2011 - 2014 Open Microscopy Environment:
 * 	- Board of Regents of the University of Wisconsin-Madison
 * 	- Glencoe Software, Inc.
 * 	- University of Dundee
 * %%
 * To the extent possible under law, the SCIFIO developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 * 
 * See the CC0 1.0 Universal license for details:
 * http://creativecommons.org/publicdomain/zero/1.0/
 * #L%
 */

package io.scif.tutorials.core;

import io.scif.AbstractChecker;
import io.scif.AbstractFormat;
import io.scif.AbstractMetadata;
import io.scif.AbstractParser;
import io.scif.AbstractTranslator;
import io.scif.AbstractWriter;
import io.scif.ByteArrayPlane;
import io.scif.ByteArrayReader;
import io.scif.Field;
import io.scif.Format;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.Plane;
import io.scif.SCIFIO;
import io.scif.config.SCIFIOConfig;
import io.scif.io.RandomAccessInputStream;

import java.io.IOException;
import java.util.Arrays;

import org.scijava.plugin.Plugin;

/**
 * Tutorial demonstrating defining your own image Format, and how to make that
 * Format available in any context.
 * 
 * @author Mark Hiner
 */
public class T3aCustomFormats {

	// Before looking at the main method, take a look at the SampleFormat defined
	// below.
	public static void main(final String... args) throws FormatException {
		// ------------------------------------------------------------------------
		// COMPARISON WITH BIO-FORMATS 4.X
		// In Bio-Formats 4.X, adding support for a new format required modifying
		// a hard-coded list of readers or writers. This could be a significant
		// barrier for including new formats.
		// In SCIFIO, we allow formats to be discovered automatically via 
		// annotation processing, or by manually adding it to a Context.
		// ------------------------------------------------------------------------

		// Let's start by creating a new context as we have in the other tutorials.
		// Creating a SCIFIO implicitly creates a new Context. During the
		// construction of this Context, services are loaded and plugins discovered
		// automatically. See the Context class documentation for more information
		// on controlling Context creation.
		final SCIFIO scifio = new SCIFIO();

		// ... and a sample image path:
		final String sampleImage = "notAnImage.scifiosmpl";

		// As SampleFormat below was annotated as a @Plugin it should be available
		// to our context:
		final Format format = scifio.format().getFormat(sampleImage);

		// Verify that we found the format plugin
		System.out.println("SampleFormat found via FormatService: " +
			(format != null));
	}

	/**
	 * This is a non-functional Format which adds "support" for a fictional
	 * ".scifiosmpl" image type.
	 * <p>
	 * Note the annotation: {@link Plugin}. All Formats are plugins for the
	 * SciJava Context, which allows them to be automatically discovered and
	 * instantiated as singletons whenever a Context is created.
	 * </p>
	 * <p>
	 * Contexts also use a special type of plugin, Service, for performing
	 * operations within the scope of that context. The FormatService plugin deals
	 * with managing Formats within the context.
	 * </p>
	 * 
	 * @author Mark Hiner
	 */
	@Plugin(type = Format.class)
	public static class SampleFormat extends AbstractFormat {

		// -- Format API Methods --

		// A lot of work is done for you in the AbstractFormat and Abstact component
		// classes. But you will always need to implement these methods when
		// defining a new Format.

		// First we have to declare a name for our Format.
		@Override
		public String getFormatName() {
			return "Sample data";
		}

		// Then we need to register what suffixes our Format is capable of opening.
		// Note that you shouldn't put a leading separator ('.') in the extension
		// Strings.
		@Override
		protected String[] makeSuffixArray() {
			return new String[] { "scifiosmpl" };
		}

		// *** MANDATORY COMPONENTS ***

		// Metadata represents the format-specific metadata. It requires one
		// method - populateImageMetadata - to be implemented. This method
		// is invoked automatically after parsing the format-specific metadata,
		// and should use this information to properly create and populate
		// ImageMetadata objects.
		public static class Metadata extends AbstractMetadata {

			// The io.scif.Field notation flags fields as significant for a
			// Metadata class, and is intended to represent the original state
			// of the metadata as it would be found in the image source.
			// The label tag allows preservation of naming schemes that would
			// be mangled by Java's variable naming practices.
			@Field(label = "Sky color")
			private String color;

			public void setColor(final String c) {
				color = c;
			}

			// This method must be implemented for each concrete Metadata class.
			// Essentially, format-specific metadata is assumed to be populated
			// during Parsing or Translation. From that metadata, ImageMetadata
			// information, common to all formats - such as height, width, etc -
			// can be populated here.
			@Override
			public void populateImageMetadata() {
				color = "orange";
			}
		}

		// Much of the Parser functionality is taken care of in the abstract layer.
		// The only method that needs to be implemented defines how an input
		// stream should be read to determine its format-specific metadata.
		public static class Parser extends AbstractParser<Metadata> {

			// Here we can populate a metadata object with format-specific information
			@Override
			public void typedParse(final RandomAccessInputStream stream,
				final Metadata meta, final SCIFIOConfig config) throws IOException,
				FormatException
			{
				meta.setColor("blue");
			}
		}

		// Checkers legitimately do not need any methods to be implemented. The
		// Format can answer what suffixes are associated with a given image
		// format, and since each component can reach its parent format, the
		// default Checker implementation can always match suffixes as long as
		// suffixSufficient == true (it is true by default).
		// If the suffix alone is insufficient for determining Format
		// compatibility, that can be declared here.
		public static class Checker extends AbstractChecker {

			// All the methods in this class are OPTIONAL overrides.
			// You need to think about your format to determine what should be
			// overridden.

			// If matching a file extension is not sufficient to determine
			// format compatibility, this method should return false.
			@Override
			public boolean suffixSufficient() {
				return true;
			}

			// If matching a file extension is required to determine format
			// compatibility, this method should return true.
			@Override
			public boolean suffixNecessary() {
				return true;
			}

			// If you need to read from the actual input stream to determine
			// format compatibility (e.g. there is a magic string, or similar)
			// this method should be overridden.
			@Override
			public boolean isFormat(final RandomAccessInputStream stream) {
				return false;
			}
		}

		// Each reader MUST implement the openPlane method, and must choose
		// a Plane type to return (e.g. ByteArrayPlane or BufferedImagePlane)
		// by extending the appropriate abstract class, or providing its own
		// typed method definitions.
		// Here we extend ByteArrayReader, signifying that this reader will
		// return ByteArrayPlanes.
		public static class Reader extends ByteArrayReader<Metadata> {

			// Any openPlane signature that contains a Plane object should
			// attempt to update that plane (e.g. via plane.populate() calls)
			// and avoid instantiating new planes. This allows a single Plane
			// instance to be reused through many openPlane calls.
			@Override
			public ByteArrayPlane openPlane(int imageIndex, long planeIndex,
				ByteArrayPlane plane, long[] planeMin, long[] planeMax,
				SCIFIOConfig config) throws FormatException, IOException
			{
				// update the data by reference
				final byte[] bytes = plane.getData();
				Arrays.fill(bytes, 0, bytes.length, (byte) 0);

				return plane;
			}

			// You must declare what domains your reader is associated with, based
			// on the list of constants in io.scif.util.FormatTools.
			// It is also sufficient to return an empty array here.
			@Override
			protected String[] createDomainArray() {
				return new String[0];
			}

		}

		// *** OPTIONAL COMPONENTS ***

		// Writers are not required components. They should not be created for
		// proprietary formats, but are good to implement for open formats.
		// Similar to the Reader, a Writer must implement its savePlane method
		// which writes the provided Plane object to disk. However, the
		// type of Plane is irrelevant for Writers, thanks to the
		// Plane.getBytes() method.
		public static class Writer extends AbstractWriter<Metadata> {

			// Take a provided data plane, of specified dimensionality, and
			// write it to the given indices on disk.
			@Override
			public void writePlane(int imageIndex, long planeIndex, Plane plane,
				long[] planeMin, long[] planeMax) throws FormatException, IOException
			{
				final byte[] bytes = plane.getBytes();

				System.out.println(bytes.length);
			}

			// If your writer supports a compression type, you can declare that here.
			// Otherwise it is sufficient to return an empty String[]
			@Override
			protected String[] makeCompressionTypes() {
				return new String[0];
			}
		}

		// Translators are not typically not required unless there is a
		// corresponding Writer for a given format. Then SCIFIO needs to know
		// how to convert other types of Metadata to the destination Format
		// for output.
		public static class Translator extends
			AbstractTranslator<io.scif.Metadata, Metadata>
		{

			// The source and dest methods are used for finding matching Translators
			// They require only trivial implementations.

			@Override
			public Class<? extends io.scif.Metadata> source() {
				return io.scif.Metadata.class;
			}

			@Override
			public Class<? extends io.scif.Metadata> dest() {
				return Metadata.class;
			}

			// This method is functionally equivalent to the Parser#typedParse method.
			// The goal of this method is to populate the format-specific information
			// of the destination Metadata. However, instead of using an input stream,
			// we are using a different Metadata as the source.
			// The type of the source is unknown, so we rely on its ImageMetadata.
			@Override
			protected void typedTranslate(io.scif.Metadata source, Metadata dest) {
				ImageMetadata iMeta = source.get(0);
				if (iMeta.isIndexed()) {
					dest.setColor("red");
				}
				else {
					dest.setColor("blue");
				}
			}

		}
	}
}

/*
 * #%L
 * SCIFIO tutorials for core and plugin use.
 * %%
 * Copyright (C) 2011 - 2016 Open Microscopy Environment:
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

import io.scif.AbstractTranslator;
import io.scif.Format;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.Metadata;
import io.scif.SCIFIO;
import io.scif.Translator;
import io.scif.formats.FakeFormat;

import java.io.IOException;
import java.util.List;

import org.scijava.plugin.Plugin;

/**
 * Tutorial demonstrating translation between Metadata types.
 * 
 * @author Mark Hiner
 */
public class T3bTranslatingMetadata {

	public static void main(final String... args) throws FormatException,
		IOException
	{
		// In the CustomFormats tutorial we demonstrated the process of defining
		// a custom image format and making it available to the SCIFIO framework.
		// We mentioned that Translators are defined in a Format when there is a
		// Writer, as we will then need to translate to this format to write out
		// data. However, any number of Translators can be defined, to and from
		// other Metadata types. Of course, we don't want a translator between
		// EVERY format pair. Instead, a major goal of SCIFIO is to provide and
		// facilitate exchange metadata formats, such that translators are defined
		// to and from existing Metadata classes and this open exchange metadata.
		// In this tutorial we therefore explore the use of Translators as stand-
		// alone classes.

		// As usual, we start by creating a SCIFIO and our trusty sample image.
		final SCIFIO scifio = new SCIFIO();
		final String sampleImage =
			"8bit-unsigned&pixelType=uint8&indexed=true&planarDims=3&lengths=50,50,3&axes=X,Y,Channel.fake";

		// First let's get a handle on a compatible Format, and parse the sample
		// image's Metadata
		final Format format = scifio.format().getFormat(sampleImage);
		final Metadata input = format.createParser().parse(sampleImage);

		// Now that we have some Metadata, let's find the MischeviousTranslator we
		// defined below.
		Translator t = null;

		// The translators() method in the SCIFIO service returns a
		// TranslatorService instance, which can be used to find compatible
		// Translators between the provided metadata types. In this case, since our
		// sample translator goes to and from FakeFormat.Metadata, we provide this
		// type as both parameters to the findTranslator method.
		// The final parameter, "true", is used to indicate that we want an
		// EXACT match. That is, it must be capable of translating the
		// concrete types of the given parameters.
		t = scifio.translator().findTranslator(input, input, true);

		// To try the MischeviousTranslator out, let's get another copy
		// of this image's Metadata.
		final Metadata output = format.createParser().parse(sampleImage);

		// Then we translate
		t.translate(input, output);

		scifio.getContext().dispose();

		// ------------------------------------------------------------------------
		// COMPARISON WITH BIO-FORMATS 4.X
		// In Bio-Formats 4.X, there was a single open-exchange format: OME-TIFF.
		// To convert between image formats, common metadata was stored in
		// loci.formats.CoreMetadata, and format-specific metadata was converted to
		// OME-XML which could then be used to write an OME-TIFF out. This
		// implementation was handled exclusively in the initFile method of each
		// reader. In SCIFIO, we provide io.scif.DatasetMetadata to record certain
		// common image characteristics, but any number of open-exchange formats
		// could be devised. It would just be a matter of defining translators
		// for converting from other image formats to the open format.
		// OME-XML will still exist as a SCIFIO plug-in to capture the OME-XML
		// schema in SCIFIO Metadata, and Bio-Formats will become a collection of
		// SCIFIO Formats with translators to OME-XML. But now there is room for
		// plug-ins in for disciplines that don't fit the OME-XML schema.
		// ------------------------------------------------------------------------
	}

	/*
	 * Our sample Translator class. Note that this class stands alone, and does
	 * need to be a nested class of one format. Image, for example, it translated
	 * between APNG metadata and Fake metadata - it would not make sense to store
	 * the translator in one class or another.
	 *
	 * For the purpose of this tutorial, we will translate from Fake metadata
	 * to Fake metadata. This is highly redundant and there is almost never a
	 * reason to implement such a translator.
	 */
	@Plugin(type = Translator.class)
	public static class MischeviousTranslator extends
		AbstractTranslator<FakeFormat.Metadata, FakeFormat.Metadata>
	{

		// -- Translator API methods --

		/*
		 * Note that Metadata parameters are passed by reference, to allow
		 * non-destructive translation. Multiple translation calls could actually be
		 * invoked in succession to collaboratively populate a single Metadata
		 * object.
		 */
		@Override
		protected void translateImageMetadata(final List<ImageMetadata> source,
			final FakeFormat.Metadata dest)
		{
			// Here we would put our translation implementation, as in T3a.
			System.out.println("Translating source: " + source + " to dest: " + dest);
		}

		@Override
		public Class<? extends Metadata> source() {
			return FakeFormat.Metadata.class;
		}

		@Override
		public Class<? extends Metadata> dest() {
			return FakeFormat.Metadata.class;
		}
	}
}

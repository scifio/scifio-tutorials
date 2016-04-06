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

import io.scif.Format;
import io.scif.FormatException;
import io.scif.Metadata;
import io.scif.Parser;
import io.scif.Reader;
import io.scif.SCIFIO;

import java.io.IOException;

/**
 * Demonstrates how individual components can be used together instead of the
 * convenience methods in the T1 tutorials. This is for more advanced SCIFIO
 * use, and generally would not be recommended for most developers.
 * 
 * @author Mark Hiner
 */
public class T2aUntypedComponents {

	public static void main(final String... args) throws FormatException,
		IOException
	{
		// In T1a-d we used a convenience method to obtain an initialized
		// reader. This glossed over the individual steps of opening an image, which
		// can also be accomplished manually through the SCIFIO components.

		// As always, we create a context and sample image path first.
		final SCIFIO scifio = new SCIFIO();
		final String sampleImage =
			"8bit-signed&pixelType=int8&lengths=50,50,3,5,7&axes=X,Y,Z,Channel,Time.fake";

		// This time we'll get a handle on the Format itself, which will allow us
		// to create the additional components. scifio.format() contains several
		// useful methods for dealing with Formats directly.
		// NB: This implicitly invokes an io.scif.Checker component to determine
		// format compatibility. These can also be used individually, e.g. if
		// given a list of Formats to test.
		final Format format = scifio.format().getFormat(sampleImage);

		// Typically the first thing we want to do, after confirming we have a
		// Format that can support an image, is parse the Metadata of that image.
		final Parser parser = format.createParser();
		final Metadata meta = parser.parse(sampleImage);

		// Metadata is used by other components, such as Readers, Writers, and
		// Translators to open, save, and convert -- respectively -- image
		// information. Assuming we're going to open an image, we'll need to
		// initialize a reader now.
		Reader reader = format.createReader();

		// Tells the reader which metadata object to use while reading.
		reader.setMetadata(meta);

		scifio.getContext().dispose();

		// ------------------------------------------------------------------------
		// COMPARISON WITH BIO-FORMATS 4.X
		// Bio-Formats 4.X readers used a setId method, which had to be called
		// separately from, say, setMetadataStore. You may notice that the SCIFIO
		// Reader class also has a similar setSource method. However, since the
		// Metadata knows the file it's associated with, setting one or the other
		// is sufficient. Reader.setSource() is basically a convenience for
		// the first steps of this tutorial.
		// ------------------------------------------------------------------------

		// It is important to note that by using components all originating from
		// a single Format instance, we can be sure that these components are
		// compatible with each other.
		// A method that accepted multiple individual components and expected them
		// to be compatible may not be particularly useful. But note that it would
		// also be unnecessary -- any component can find its parent Format through
		// the getFormat() method, which can then be used to create other
		// components.

		// NB: within each Context, Formats are singletons. So reader.getFormat() ==
		// parser.getFormat() in this method. However, the components created by
		// formats are each unique instances - so format.createReader() !=
		// format.createReader().

		// At this point we have an initialized Reader and caught up to 
		// T1aIntroToSCIFIO. We could now begin opening planes with our reader.
	}
}

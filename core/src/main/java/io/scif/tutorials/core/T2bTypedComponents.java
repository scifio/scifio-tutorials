/*
 * #%L
 * SCIFIO tutorials for core and plugin use.
 * %%
 * Copyright (C) 2011 - 2015 Open Microscopy Environment:
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

import io.scif.ByteArrayPlane;
import io.scif.FormatException;
import io.scif.SCIFIO;
import io.scif.formats.FakeFormat;
import io.scif.formats.FakeFormat.Parser;
import io.scif.formats.FakeFormat.Reader;

import java.io.IOException;

/**
 * In {@link T2aUntypedComponents} we looked at using the general,
 * interface-level SCIFIO components. Now we'll look at how to get access
 * to components with stronger typing.
 * 
 * @author Mark Hiner
 */
public class T2bTypedComponents {

	public static void main(final String... args) throws FormatException,
		IOException
	{
		// In IntroToSCIFIO we saw the general case of image opening, but what
		// if we know exactly what kind of image we're working with?

		final SCIFIO scifio = new SCIFIO();
		final String sampleImage =
			"8bit-unsigned&pixelType=uint8&indexed=true&planarDims=3&lengths=50,50,3&axes=X,Y,Channel.fake";

		// This time, since we know we have a .fake image, we'll get a handle to the
		// Fake format.
		final FakeFormat fakeFormat =
			scifio.format().getFormatFromClass(FakeFormat.class);

		// Two important points here:
		// 1 - getformatFromClass is overloaded. You can use any component's class
		// and get back the corresponding Format.
		// 2 - we didn't invoke the FakeFormat's constructor.
		// new FakeFormat() would have given us a Format instance with no context.
		// new FakeFormat(scifio) would have given us a Format with the correct
		// context, but wouldn't update the context's FakeFormat singleton. So it
		// would basically be an orphan Format instance.
		// Formats have no state, so as long as you want a Format that was
		// discovered as a plugin, you should access it via the desired context. We
		// will discuss manual Format instantiation in the CustomFormats tutorial.

		// Formats provide access to all other components, and with a typed Format
		// you can create typed components:

		final FakeFormat.Reader reader = (Reader) fakeFormat.createReader();
		final FakeFormat.Parser parser = (Parser) fakeFormat.createParser();

		// Now that we have typed components, we can guarantee the return type
		// for many methods, and access type-specific API:

		final FakeFormat.Metadata meta = parser.parse(sampleImage);

		// getColorTable isn't a part of the Metadata API, but since
		// FakeFormat.Metadata implements HasColorTable, we have access to this
		// method.
		System.out.println("Color table: " + meta.getColorTable(0, 0));

		reader.setMetadata(meta);

		// Typically we just get a Plane instance back from openPlane. But now we
		// know we're working with ByteArrayPlanes.
		final ByteArrayPlane plane = reader.openPlane(0, 0);

		System.out.println("Byte array plane: " + plane.getBytes().length);
	}
}

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

import io.scif.ByteArrayPlane;
import io.scif.FormatException;
import io.scif.SCIFIO;

import io.scif.formats.TestImgFormat;
import io.scif.formats.TestImgFormat.Parser;
import io.scif.formats.TestImgFormat.Reader;

import java.io.IOException;

import org.scijava.io.location.Location;

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
		
		Location sampleImageLocation = FakeTutorialImages.sampleIndexedImage();

		// This time, since we know we have a .fake image, we'll get a handle to the
		// Fake format.
		final TestImgFormat fakeFormat =
			scifio.format().getFormatFromClass(TestImgFormat.class);

		// Two important points here:
		// 1 - getformatFromClass is overloaded. You can use any component's class
		// and get back the corresponding Format.
		// 2 - we didn't invoke the TestImgFormat's constructor.
		// new TestImgFormat() would have given us a Format instance with no context.
		// new TestImgFormat(scifio) would have given us a Format with the correct
		// context, but wouldn't update the context's TestImgFormat singleton. So it
		// would basically be an orphan Format instance.
		// Formats have no state, so as long as you want a Format that was
		// discovered as a plugin, you should access it via the desired context. We
		// will discuss manual Format instantiation in the CustomFormats tutorial.

		// Formats provide access to all other components, and with a typed Format
		// you can create typed components:

		final TestImgFormat.Reader reader = (Reader) fakeFormat.createReader();
		final TestImgFormat.Parser parser = (Parser) fakeFormat.createParser();

		// Now that we have typed components, we can guarantee the return type
		// for many methods, and access type-specific API:

		final TestImgFormat.Metadata meta = parser.parse(sampleImageLocation);

		// getColorTable isn't a part of the Metadata API, but since
		// TestImgFormat.Metadata implements HasColorTable, we have access to this
		// method.
		System.out.println("Color table: " + meta.getColorTable(0, 0));

		reader.setMetadata(meta);

		// Typically we just get a Plane instance back from openPlane. But now we
		// know we're working with ByteArrayPlanes.
		final ByteArrayPlane plane = reader.openPlane(0, 0);

		System.out.println("Byte array plane: " + plane.getBytes().length);

		scifio.getContext().dispose();
	}
}

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

import io.scif.FormatException;
import io.scif.Reader;
import io.scif.SCIFIO;
import io.scif.Writer;
import io.scif.io.location.TestImgLocation;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.scijava.io.location.Location;
import org.scijava.io.location.LocationService;

/**
 * Tutorial demonstrating use of the Writer component.
 * 
 * @author Mark Hiner
 */
public class T1dSavingImagePlanes {

	public static void main(final String... args) throws FormatException,
		IOException, URISyntaxException
	{
		// In this tutorial, we're going to make our .fake sample image
		// "real". If you look at the TestImgFormat source code, you'll notice that
		// it doesn't have a functional Writer, so we'll have to translate
		// to a different Format that can write our fake planes to disk.

		final SCIFIO scifio = new SCIFIO();
		
		// Create a fake image and get its Location.
		Location sampleImageLocation = FakeTutorialImages.sampleImage();

		// We'll need a path to write to. By making it a ".png" we are locking in
		// the final format of the file on disk.
		final String outPath = "SCIFIOTutorial.png";
		
		// We can resolve this Location in the same way as T1aIntroToSCIFIO
		Location outLoction = scifio.get(LocationService.class).resolve(outPath); 

		// Clear the file if it already exists.
		File f = new File(outPath);
		if (f.exists()) f.delete();

		// We'll need a reader for the input image
		final Reader reader = scifio.initializer().initializeReader(sampleImageLocation);

		// .. and a writer for the output path
		final Writer writer =
			scifio.initializer().initializeWriter(sampleImageLocation, outLoction);

		// Note that these initialize methods are used for convenience.
		// Initializing a reader and a writer requires that you set the source
		// and metadata properly. Also note that the Metadata attached to a writer
		// describes how to interpret the incoming Planes, but may not reflect
		// the image on disk - e.g. if planes were saved in a different order
		// than on the input image. For accurate Metadata describing the saved
		// image, you need to re-parse it from disk.

		// Anyway, now that we have a reader and a writer, we can save all the
		// planes. We simply iterate over each image, and then each plane, writing
		// the planes out in order.
		for (int i = 0; i < reader.getImageCount(); i++) {
			for (int j = 0; j < reader.getPlaneCount(i); j++) {
				System.out.println("Writing image #" + i + ", plane #" + j);
				writer.savePlane(i, j, reader.openPlane(i, j));
			}
		}
		System.out.println("Wrote all planes to " + //
			new File(outPath).getAbsolutePath());

		// Note that this code is for illustration purposes only.
		// A more general solution would need higher level API that could account
		// for larger planes, etc..

		// close our components now that we're done. This is a critical step, as
		// many formats have footer information that is written when the writer is
		// closed.
		reader.close();
		writer.close();
		scifio.getContext().dispose();

		// That's it! There should be a new SCIFIOTutorial image in whichever
		// directory you ran this tutorial from.
	}
}

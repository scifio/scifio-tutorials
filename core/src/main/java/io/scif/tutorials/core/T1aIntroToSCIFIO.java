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

import io.scif.FormatException;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;

import java.io.IOException;

/**
 * An introduction to the SCIFIO API. Demonstrates basic plane reading.
 * 
 * @author Mark Hiner
 */
public class T1aIntroToSCIFIO {

	public static void main(final String... agrs) throws FormatException,
		IOException
	{
		// The most convenient way to program against the SCIFIO API is by using
		// an io.scif.SCIFIO instance. This is basically a "SCIFIO lens" on an
		// org.scijava.Context instance. There are several ways to create a SCIFIO
		// instance, e.g. if you already have a Context available, but for now we'll
		// assume we want a fresh Context.
		SCIFIO scifio = new SCIFIO();

		// Let's look at a sample scenario where we have an image path and we just
		// want to open the first 3 planes of a dataset as simply as possible:

		// The path to our sample image. Fake images are special because they don't
		// exist on disk - they are defined purely by their file name -  so they're
		// good for testing.
		final String sampleImage =
			"8bit-signed&pixelType=int8&lengths=50,50,3,5,7&axes=X,Y,Z,Channel,Time.fake";

		// This method checks the Context used by our SCIFIO instance for all its
		// known format plugins, and returns an io.scif.Reader capable of opening
		// the specified image's planes.
		final Reader reader = scifio.initializer().initializeReader(sampleImage);

		// ------------------------------------------------------------------------
		// COMPARISON WITH BIO-FORMATS 4.X
		// Bio-Formats 4.X used a single aggregated reader class:
		// loci.formats.ImageReader. This reader kept an instance of each other
		// known reader and delegated to the appropriate reader when working with a
		// given image format.
		// The SCIFIO context is similar to ImageReader, in that it keeps singleton
		// references to each Format type, and provides convenience methods for
		// creating appropriate components. But in SCIFIO, each image operation -
		// such as opening/saving a plane, parsing metadata, or checking image
		// format compatibility - is encapsulated in a single class. The context
		// is the entry point for gaining access to these components. Additionally,
		// each context allows separate loading of Formats, for differentiated
		// environments.
		// ------------------------------------------------------------------------

		// Here we open and "display" the actual planes.
		for (int i = 0; i < 3; i++) {
		// Pixels read from images in SCIFIO are returned as io.scif.Plane
		// objects, agnostic of the underlying data type (e.g. byte[] or
		// java.awt.BufferedImage)
			Plane plane = reader.openPlane(0, i);
			displayImage(i, plane);
		}

		// ------------------------------------------------------------------------
		// COMPARISON WITH BIO-FORMATS 4.X
		// In Bio-Formats 4.X, planes were opened via a reader.openBytes call which
		// typically had one less index parameter than in SCIFIO. This is because
		// Bio-Formats readers cached the current "series" for each reader.
		// In SCIFIO we have moved away from caching state whenever possible,
		// except on components designed to hold state (such as Metadata).
		// The data model used by SCIFIO follows the OME notation, that each path
		// points to a "Dataset," which contains 1 or more "Images" and each image
		// contains 1 or more "Planes" - typically planes are XY across some
		// number of dimensions.
		// In the openPlane call above, the Dataset is implicit (the "sampleImage")
		// the first index specifies the image number, and the second index
		// specifies the plane number - which would result in returning the
		// first 3 planes depending (whether these are Z, C, T, etc... slices 
		// depends on the structure of the dataset).
		// ------------------------------------------------------------------------
	}

	// Dummy method for demonstrating io.scif.Plane#getBytes()
	private static void displayImage(final int index, final Plane plane) {
		// All planes, regardless of type, can automatically convert their pixel data
		// to a byte[].
		System.out.println("plane " + index + ": " + plane + ", length: " +
			plane.getBytes().length);
	}
}

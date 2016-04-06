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

import java.io.IOException;

/**
 * WARNING: this code intentionally fails. For functional code, see
 * {@link T1cReadingTilesGood}.
 *
 * Tutorial demonstrating the wrong way to use the SCIFIO API to open large
 * images as tiles.
 * 
 * @author Mark Hiner
 */
public class T1bReadingTilesBad {

	public static void main(final String... args) throws FormatException,
		IOException
	{

		// Create a SCIFIO for this tutorial
		final SCIFIO scifio = new SCIFIO();

		// This time we're going to set up an imageID with planes that won't fit
		// in a byte array.
		final String hugeImage = "hugePlane&lengths=70000,80000.fake";

		// We initialize a reader as we did before
		final Reader reader = scifio.initializer().initializeReader(hugeImage);

		// Now we'll try the naive thing, and just open all the planes in this
		// dataset.
		for (int i = 0; i < reader.getImageCount(); i++) {
			for (int j = 0; j < reader.getPlaneCount(i); j++) {
				// There will be an exception thrown here. Planes that are greater 
				// than 2GB in size will not be instantiated.
				reader.openPlane(i, j);
			}
		}

		scifio.getContext().dispose();
	}
}

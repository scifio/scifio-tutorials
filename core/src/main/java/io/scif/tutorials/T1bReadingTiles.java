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

package io.scif.tutorials;

import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.Metadata;
import io.scif.Reader;
import io.scif.SCIFIO;

import java.io.IOException;

import net.imglib2.meta.Axes;

/**
 * Tutorial using the SCIFIO API to open image tiles.
 * 
 * @author Mark Hiner
 */
public class T1bReadingTiles {

	public static void main(final String... args) throws FormatException,
		IOException
	{

		// As always we'll need a SCIFIO for this tutorial
		final SCIFIO scifio = new SCIFIO();

		// This time we're going to set up a huge image path
		final String hugeImage = "hugePlane&lengths=70000,80000.fake";

		// We initialize a reader as we did before
		final Reader reader = scifio.initializer().initializeReader(hugeImage);

		// Now we'll try the naive thing, and just open all the planes in this
		// dataset.
		try {
			for (int i = 0; i < reader.getImageCount(); i++) {
				for (int j = 0; j < reader.getPlaneCount(i); j++) {
					reader.openPlane(i, j);
				}
			}
		}
		catch (final FormatException e) {
			System.out.println("Caught:\n" + e);
		}
		// There should be an exception caught above. Planes that are greater than
		// 2GB in size
		// will not be instantiated. The plane opening code prints each time it
		// opens a tile,
		// so if the output below is uncommented you probably will miss the
		// exception message.

		// We'll need some basic information about this dataset, so let's get a
		// reference to
		// its metadata.
		final Metadata meta = reader.getMetadata();

		for (int i = 0; i < reader.getImageCount(); i++) {

			ImageMetadata iMeta = meta.get(i);
			// These methods will compute the optimal width to use with
			// reader#openPlane
			final long optimalTileWidth = reader.getOptimalTileWidth(i);
			final long optimalTileHeight = reader.getOptimalTileHeight(i);

			// Then we need to figure out how many tiles are actually present in a
			// plane,
			// given the tile height and width
			final long tilesWide =
				(long) Math.ceil((double) iMeta.getAxisLength(Axes.X) /
					optimalTileWidth);
			final long tilesHigh =
				(long) Math.ceil((double) iMeta.getAxisLength(Axes.Y) /
					optimalTileHeight);

			long x, y = 0;

			// now we can open each tile, one at a time, for each plane in this image
			for (int j = 0; j < iMeta.getPlaneCount(); j++) {
				for (int tileX = 0; tileX < tilesWide; tileX++) {
					for (int tileY = 0; tileY < tilesHigh; tileY++) {

						// these are pointers to the position in the current plane
						x = tileX * optimalTileWidth;
						y = tileY * optimalTileHeight;

						// and then we compute the actual dimensions of the current tile, in
						// case
						// the image was not perfectly divisible by the optimal dimensions.
						final long actualTileWidth =
							Math.min(optimalTileWidth, iMeta.getAxisLength(Axes.X) - x);
						final long actualTileHeight =
							Math.min(optimalTileHeight, iMeta.getAxisLength(Axes.Y) - y);

						// Finally we open the current plane, using an openPlane signature
						// that allows us
						// to specify a sub-region of the current plane.
						// FIXME: uncomment these lines of code after the first time you run
						// this tutorial.
						// *** UNCOMMENT FOLLOWING LINES ***
//						System.out.println("Image:" + i + " Plane:" + j + " Tile:" + 
//						((tileX * tilesWide) + tileY + 1) + " -- " +
//						reader.openPlane(i, j, new long[]{x, y},
//						new long[]{actualTileWidth, actualTileHeight}));
						// ** STOP UNCOMMENTING LINES ***

						// Here, if we saved a reference to the returned Plane, we would do
						// any necessary
						// processing of the bytes.

						// NB: the openPlane signature we used creates a new plane each
						// time. If there
						// are a significant number of tiles being read, it may be more
						// efficient to
						// create a Plane ahead of time using the reader.createPlane method,
						// and then
						// just reuse it for all tiles of that size.
					}
				}
			}
		}
	}
}

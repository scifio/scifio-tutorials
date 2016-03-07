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
import io.scif.ImageMetadata;
import io.scif.Metadata;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;

import java.io.IOException;

import net.imagej.axis.Axes;

/**
 * Tutorial using the SCIFIO API to open a large image in tiles. Once planes
 * start to exceed 2GB in size, special considerations must be taken.
 * 
 * @author Mark Hiner
 */
public class T1cReadingTilesGood {

	public static void main(final String... args) throws FormatException,
		IOException
	{

		// As always we'll need a SCIFIO for this tutorial
		final SCIFIO scifio = new SCIFIO();

		// This time we're going to set up an imageID with planes that won't fit
		// in a 2GB byte array.
		final String hugeImage = "hugePlane&lengths=70000,80000.fake";

		// We initialize a reader as we did before
		final Reader reader = scifio.initializer().initializeReader(hugeImage);

		// In T1b we saw that the naive use of the API to open planes doesn't
		// work when the individual planes are too large.
		// To open tiles of the appropriate size, we'll need some basic information
		// about this dataset, so let's get a reference to its metadata.
		final Metadata meta = reader.getMetadata();

		// Iterate over each image in the dataset (there's just one in this case)
		for (int i = 0; i < reader.getImageCount(); i++) {
			ImageMetadata iMeta = meta.get(i);
			// These methods will compute the optimal width to use with
			// reader#openPlane
			final long optimalTileWidth = reader.getOptimalTileWidth(i);
			final long optimalTileHeight = reader.getOptimalTileHeight(i);

			// Then we need to figure out how many tiles are actually present in a
			// plane, given the tile height and width
			final long tilesWide =
				(long) Math.ceil((double) iMeta.getAxisLength(Axes.X) /
					optimalTileWidth);
			final long tilesHigh =
				(long) Math.ceil((double) iMeta.getAxisLength(Axes.Y) /
					optimalTileHeight);

			// now we can open each tile, one at a time, for each plane in this image
			long x, y = 0;
			for (int j = 0; j < iMeta.getPlaneCount(); j++) {
				for (int tileX = 0; tileX < tilesWide; tileX++) {
					for (int tileY = 0; tileY < tilesHigh; tileY++) {

						// these are pointers to the position in the current plane
						x = tileX * optimalTileWidth;
						y = tileY * optimalTileHeight;

						// and then we compute the actual dimensions of the current tile, in
						// case the image was not perfectly divisible by the optimal
						// dimensions.
						final long actualTileWidth =
							Math.min(optimalTileWidth, iMeta.getAxisLength(Axes.X) - x);
						final long actualTileHeight =
							Math.min(optimalTileHeight, iMeta.getAxisLength(Axes.Y) - y);

						// Finally we open the current plane, using an openPlane signature
						// that allows us to specify a sub-region of the current plane.
						Plane p =
							reader.openPlane(i, j, new long[] { x, y }, new long[] {
								actualTileWidth, actualTileHeight });

						// Here we would do any necessary processing of each tile's bytes.
						// In this, we'll just print out the plane and position.
						System.out.println("Image:" + (i+1) + " Plane:" + (j+1) + " Tile:" +
						((tileX * tilesWide) + tileY + 1) + " -- " + p);

						// NB: the openPlane signature we used creates a new plane each
						// time. If there are a significant number of tiles being read, it
						// may be more efficient to create a Plane ahead of time using the
						// reader.createPlane method, and then just reuse it for all tiles
						// of that size.
					}
				}
			}
		}

		scifio.getContext().dispose();
	}
}

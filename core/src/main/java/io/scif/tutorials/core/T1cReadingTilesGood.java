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
import io.scif.ImageMetadata;
import io.scif.Metadata;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;

import java.io.IOException;
import java.util.Arrays;

import net.imagej.axis.Axes;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;

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

		// This time we're going to set up an image with planes that won't fit
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
		for (int imageIndex = 0; imageIndex < reader.getImageCount(); imageIndex++) {
			ImageMetadata iMeta = meta.get(imageIndex);
			// These methods will compute the optimal width to use with
			// reader#openPlane
			// TODO:
			// Feel free to play with these values if you want to experiment
			// opening other images in tiles.
			// For example, if you have a 512x512 image and set the
			// optimalTileHeight to 2, and width to 512, you should
			// get 256 planes.
			final long optimalTileWidth = reader.getOptimalTileWidth(imageIndex);
			final long optimalTileHeight = reader.getOptimalTileHeight(imageIndex);

			// SCIFIO images are divided into "Planar" and "Non-Planar" axes
			// - "Planar" axes are used to specify physical plane sizes.
			// - "Non-Planar" axes are used to specify plane counts.
			// A tile is just a sub-region of an image.
			// To open a sub-region, we have to specify bounds over the planar axes.
			System.out.println("Planar axis count = " + iMeta.getPlanarAxisCount());
			System.out.println("Planar axis lengths = " + //
				Arrays.toString(iMeta.getAxesLengthsPlanar()));

			// These are the indices of the X and Y axis in the image (and thus
			// in the array of planar axes, since planar axes always "come first"
			// in the image)
			final int xAxis = iMeta.getAxisIndex(Axes.X);
			final int yAxis = iMeta.getAxisIndex(Axes.Y);
			System.out.println("xAxis = " + xAxis + ", yAxis = " + yAxis);

			// We need to figure out how many tiles are actually present in a
			// plane, given the tile height and width that we're using
			final long tilesWide =
				(long) Math.ceil((double) iMeta.getAxisLength(Axes.X) /
					optimalTileWidth);
			final long tilesHigh =
				(long) Math.ceil((double) iMeta.getAxisLength(Axes.Y) /
					optimalTileHeight);

			// Now we can open each tile, one at a time, for each plane in this image
			for (int planeIndex = 0; planeIndex < iMeta.getPlaneCount(); planeIndex++) {
				for (int tileX = 0; tileX < tilesWide; tileX++) {
					for (int tileY = 0; tileY < tilesHigh; tileY++) {

						// These are pointers to the offsets in the current plane,
						// from where we will start reading the next tile
						final long xMin = tileX * optimalTileWidth;
						final long yMin = tileY * optimalTileHeight;

						// We also need to check the lengths of our tile, to see
						// if they would run outside the image - due to the plane
						// not being perfectly divisible by the tile dimensions.
						final long xLength =
							Math.min(optimalTileWidth, iMeta.getAxisLength(Axes.X) - xMin);
						final long yLength =
							Math.min(optimalTileHeight, iMeta.getAxisLength(Axes.Y) - yMin);

						final Interval bounds = FinalInterval.createMinSize(xMin, yMin, xLength, yLength);

						// Finally we open the current plane, using an openPlane signature
						// that allows us to specify a sub-region of the current plane.
						Plane p =
							reader.openPlane(imageIndex, planeIndex, bounds);

						// Here we would do any necessary processing of each tile's bytes.
						// In this, we'll just print out the plane and position.
						System.out.println("Image:" + (imageIndex + 1) + //
							" Plane:" + (planeIndex + 1) + //
							" Tile:" + ((tileX * tilesWide) + tileY + 1) + //
							" Offset:" + xMin + "," + yMin + //
							" Length:" + xLength + "," + yLength + //
							" -- " + p);

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

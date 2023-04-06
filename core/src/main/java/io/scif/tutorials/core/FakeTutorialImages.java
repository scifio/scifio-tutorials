package io.scif.tutorials.core;

import org.scijava.io.location.Location;
import org.scijava.io.location.LocationService;

import io.scif.io.location.TestImgLocation;
import io.scif.io.location.TestImgLocation.Builder;

// A collection of functions creating fake image {@link Location}s for the SCIFIO 
// tutorials using {@link TestImgLocation.Builder}
public class FakeTutorialImages {
	
	public static Location hugeImage() {
		TestImgLocation.Builder b = new Builder();
		b.axes("X","Y");
		b.lengths(70000, 80000);
		Location location = b.build();
		return location;
	}
	
	// Note:
	// This is equivalent to using the LocationService to resolve the String input image from
	// T1aIntroToSCIFIO
	// final String sampleImage = 
	//     "8bit-unsigned&pixelType=uint8&lengths=50,50,3,5,7&axes=X,Y,Z,Channel,Time.fake";
	// Location location = scifio.get(LocationService.class).resolve(sampleImage);
	public static Location sampleImage() {
		TestImgLocation.Builder b = new Builder();
		b.pixelType("uint8"); // set the pixel type
		b.axes("X", "Y", "Z", "Channel", "Time"); // set new axis names
		b.lengths(50, 50, 3, 5, 7); // set new axis lengths
		Location location = b.build(); // build the final location
		return location;
	}
	
	public static Location sampleIndexedImage() {
		TestImgLocation.Builder b = new Builder();
		b.pixelType("uint8"); // set the pixel type
		b.indexed(true);
		b.planarDims(3);
		b.axes("X", "Y", "Channel"); // set new axis names
		b.lengths(50, 50, 3); // set new axis lengths
		Location location = b.build(); // build the final location
		return location;
	}
	
}

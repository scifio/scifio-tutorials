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

package io.scif.tutorials.ome;

import io.scif.FormatException;
import io.scif.Metadata;
import io.scif.SCIFIO;
import io.scif.ome.OMEMetadata;
import io.scif.tutorials.core.T1dSavingImagePlanes;

import java.io.IOException;

import loci.common.xml.XMLTools;

/**
 * Tutorial on obtaining OME-XML using SCIFIO.
 * 
 * @author Mark Hiner
 */
public class T1OpeningOMEXML {

	public static void main(final String... args) throws FormatException,
		IOException
	{
		// Creation of OME-XML metadata in SCIFIO is accomplished via translation.
		// The OME-XML component is essentially a collection of translators, from
		// specific formats to OME-XML, which define how to extract the OME-XML
		// schema. So, we will need to work with a sample image that has a defined
		// translator to OME-XML. Luckily we already have a tutorial which creates a
		// PNG image for us:
		T1dSavingImagePlanes.main();

		// Now we can reference the image we wrote via this path:
		final String samplePNG = "SCIFIOTutorial.png";

		// We'll need a context for discovering formats and translators
		final SCIFIO scifio = new SCIFIO();

		// This is the Metadata object we will translate to OMEXML Metadata;
		Metadata meta = null;

		// Since we are not going to be reading or writing any planes in this
		// tutorial, we only need to populate the Metadata. To help us out, there
		// is a very convenint method in the SCIFIO initializer:
		meta = scifio.initializer().parseMetadata(samplePNG);

		// Now that we have our source Metadata, we will need OME-XML Metadata to
		// translate to:

		final OMEMetadata omexml = new OMEMetadata(scifio.getContext());

		// And we'll use a convenience method from the translator service to
		// avoid manually constructing a Translator
		scifio.translator().translate(meta, omexml, true);

		// Now that we have our OME-XML we can print it:
		final String xml = omexml.getRoot().dumpXML();
		System.out.println(XMLTools.indentXML(xml, 3, true));
		scifio.getContext().dispose();
	}
}

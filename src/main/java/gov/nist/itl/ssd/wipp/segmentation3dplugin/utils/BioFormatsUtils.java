/*
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of
 * their official duties. Pursuant to title 17 Section 105 of the United
 * States Code this software is not subject to copyright protection and is
 * in the public domain. This software is an experimental system. NIST assumes
 * no responsibility whatsoever for its use by other parties, and makes no
 * guarantees, expressed or implied, about its quality, reliability, or
 * any other characteristic. We would appreciate acknowledgement if the
 * software is used.
 */
package gov.nist.itl.ssd.wipp.segmentation3dplugin.utils;

import ij.ImagePlus;
import loci.common.DebugTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BioFormatsUtils {

    private static final Logger LOGGER = Logger.getLogger(BioFormatsUtils.class.getName());

    public static ImagePlus readImage(String filepath) {
        DebugTools.enableLogging("WARN");
        ImagePlus imp;

        File file = new File(filepath);
        LOGGER.log(Level.INFO, "Loading " + file.getName() + " using BioFormats");

        try {
            ImporterOptions options = new ImporterOptions();
            options.setId(file.getAbsolutePath());
            options.setSplitChannels(false);
            options.setSplitTimepoints(false);
            options.setSplitFocalPlanes(false);
            options.setAutoscale(false);
            options.setVirtual(false);

            ImagePlus[] tmp = BF.openImagePlus(options);
            imp = tmp[0];

        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Cannot open image using BioFormats");
            return null;
        }

        return imp;
    }

    public static OMEXMLMetadata getMetadata(File tile) {
        OMEXMLMetadata metadata;
        try {
            OMEXMLService omeXmlService = new ServiceFactory().getInstance(
                    OMEXMLService.class);
            metadata = omeXmlService.createOMEXMLMetadata();
        } catch (DependencyException ex) {
            throw new RuntimeException("Cannot find OMEXMLService", ex);
        } catch (ServiceException ex) {
            throw new RuntimeException("Cannot create OME metadata", ex);
        }
        try (ImageReader imageReader = new ImageReader()) {
            IFormatReader reader;
            reader = imageReader.getReader(tile.getPath());
            reader.setOriginalMetadataPopulated(false);
            reader.setMetadataStore(metadata);
            reader.setId(tile.getPath());
        } catch (FormatException | IOException ex) {
            throw new RuntimeException("No image reader found for file "
                    + tile, ex);
        }

		return metadata;
}

}

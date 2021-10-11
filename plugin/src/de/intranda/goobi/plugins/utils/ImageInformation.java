package de.intranda.goobi.plugins.utils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.apache.pdfbox.pdmodel.PDPage;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.media.imageio.plugins.tiff.TIFFDirectory;
import com.sun.media.imageio.plugins.tiff.TIFFField;
import com.sun.media.jai.codec.FileSeekableStream;


public class ImageInformation {
	private float smallWidth, smallLength, largeWidth, largeLength;
	private float densityX;
	private float densityY;
	private double alpha;
	
	private String basename;



	/**
	 * Returns the ImageInformation of two images, where density is of the small image and alpha is the angle by which the small image was rotated
	 * about.
	 * 
	 * @param small
	 *            the small, rotated image, which lies in the large image
	 * @param large
	 *            the large image
	 * @return ImageInformation
	 * @throws IOException
	 */
	public static ImageInformation getInformation(File small, PDPage page) throws IOException {
		ImageInformation i = new ImageInformation();

		i.basename = small.getName().substring(0,small.getName().lastIndexOf('.'));
		FileSeekableStream stream = new FileSeekableStream(small);

		ImageReader imagereader = null; // ImageReader to read the class
		ImageInputStream iis = null; // specialized input stream for image

		if (small.getName().endsWith(".tif")) {

			TIFFDirectory tiffDirectory = null;

			try {

				// get the ImageReader first, before we can read the image
				Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("tiff");
				if (it.hasNext()) {
					imagereader = it.next();
				} else {

					throw new Exception("Imagereader for TIFF format couldn't be found!");
				}
				// read the stream
				iis = ImageIO.createImageInputStream(stream);
				imagereader.setInput(iis, true); // set the ImageInputStream as
				tiffDirectory = TIFFDirectory.createFromMetadata(imagereader.getImageMetadata(0));

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				stream.close();
			}

			// resolution
			TIFFField tiffield = tiffDirectory.getTIFFField(282); // resolution
			i.densityX = tiffield.getAsFloat(0);
			i.densityY = tiffield.getAsFloat(0);

			// size of small image
			tiffield = tiffDirectory.getTIFFField(256);// width
			i.smallWidth = tiffield.getAsFloat(0);
			tiffield = tiffDirectory.getTIFFField(257);// length
			i.smallLength = tiffield.getAsFloat(0);

			// large image:

			i.largeWidth = page.getTrimBox().getWidth() * i.densityX / 72;
			i.largeLength = page.getTrimBox().getHeight() * i.densityY / 72;

			i.alpha = Math
					.asin((0.5 * (i.largeLength - Math.sqrt(Math.pow(i.smallWidth, 2) + Math.pow(i.smallLength, 2) - Math.pow(i.largeWidth, 2))))
							/ i.smallWidth);

		} else if (small.getName().endsWith(".[Jj][Pp]2") || small.getName().endsWith(".JP2")) {
			Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("jpeg2000");
			if (it.hasNext()) {
				imagereader = it.next();
			} else {
				// ERROR - no ImageReader was found
				// TODO:
				System.out.println("Imagereader for Jpeg2000 couldn't be found");
				// throw new Exception("Imagereader for Jpeg2000 format couldn't be found!");
			}

			iis = ImageIO.createImageInputStream(stream);
			imagereader.setInput(iis, true);

			// ImageReadParam readParam = imagereader.getDefaultReadParam();
			// RenderedImage renderedImage = imagereader.readAsRenderedImage(0, readParam);

			IIOMetadata imageMetadata = null;

			try {
				imageMetadata = imagereader.getImageMetadata(0);
				Node domNode = imageMetadata.getAsTree("javax_imageio_1.0");

				try {
					float hps = Float.parseFloat(getHorizontalPixelSize(domNode));
					float vps = Float.parseFloat(getVerticalPixelSize(domNode));
					i.densityX = ((float) (25.4 / hps));
					i.densityY = ((float) (25.4 / vps));
				} catch (Exception e) {
					// can't read resolution information,
					// set some default values
					i.densityX = (100f);
					i.densityY = (100f);
				}
			} finally {

			}

		}

		return i;
	}

	private static String getVerticalPixelSize(Node domNode) {
		String result = null;

		Node dimensionnode = getFirstElementByName(domNode, "Dimension");
		if (dimensionnode == null) {
			return null; // markerSequence element not available
		}

		Node verticalpixelnode = getFirstElementByName(dimensionnode, "VerticalPixelSize");
		if (verticalpixelnode == null) {
			return null; // NumChannels element not available
		}

		Node attribute = getAttributeByName(verticalpixelnode, "value");
		if (attribute == null) {
			return null; // attribute not available
		}

		result = attribute.getNodeValue();

		return result;
	}

	private static String getHorizontalPixelSize(Node domNode) {
		String result = null;

		Node dimensionnode = getFirstElementByName(domNode, "Dimension");
		if (dimensionnode == null) {
			return null; // markerSequence element not available
		}

		Node horizontalpixelNode = getFirstElementByName(dimensionnode, "HorizontalPixelSize");
		if (horizontalpixelNode == null) {
			return null; // NumChannels element not available
		}

		Node attribute = getAttributeByName(horizontalpixelNode, "value");
		if (attribute == null) {
			return null; // attribute not available
		}

		result = attribute.getNodeValue();

		return result;
	}

	private static Node getFirstElementByName(Node inNode, String elementName) {
		NodeList list = inNode.getChildNodes();
		int i = 0;
		while (i < list.getLength()) {
			Node n = list.item(i);

			if ((n.getNodeType() == Node.ELEMENT_NODE) && (n.getNodeName().equals(elementName))) {
				return n;
			}
			i++;
		}
		return null;
	}

	private static Node getAttributeByName(Node inNode, String attributeName) {
		Node result = null;

		NamedNodeMap nnm = inNode.getAttributes();
		result = nnm.getNamedItem(attributeName);

		return result;
	}

	@Override
	public String toString() {
		return "densityX: " + densityX + " densityY: " + densityY + " alpha: " + alpha;
	}

	public float getSmallWidth() {
		return smallWidth;
	}

	public float getSmallLength() {
		return smallLength;
	}

	public float getLargeWidth() {
		return largeWidth;
	}

	public float getLargeLength() {
		return largeLength;
	}

	public double getAlpha() {
		return alpha;
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public float getDensityX() {
		return densityX;
	}

	public void setDensityX(float densityX) {
		this.densityX = densityX;
	}

	public float getDensityY() {
		return densityY;
	}

	public void setDensityY(float densityY) {
		this.densityY = densityY;
	}

    public String getBasename() {
        return basename;
    }
}
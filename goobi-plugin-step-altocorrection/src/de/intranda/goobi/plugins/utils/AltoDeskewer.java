package de.intranda.goobi.plugins.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;

public class AltoDeskewer {
    private static String version;
    private static String encoding;

    private static String altoNS;
    private static List<SimpleEntry<String, String>> namespaceList = new ArrayList<>();
    private static String schemaLocation;

    private static String measurementUnit = "";
    private static String ocrProcessing = "";
    private static String processingDateTime = "";
    private static String softwareCreator = "";
    private static String softwareName = "";
    private static String softwareVersion = "";

    private static int stringIdCount = 0;

    public static void deskewAlto(Path altoFile, List<Path> inputTifs, PDDocument inputPdf, Path outputFolder) throws IOException,
            XMLStreamException {
        PDDocumentCatalog catalog = inputPdf.getDocumentCatalog();

        InputStream in = Files.newInputStream(altoFile);
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader parser = factory.createXMLStreamReader(in);

        XMLOutputFactory output = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = null;

        ImageInformation currentInfo = null;

        int pageCount = 0;
        boolean isMeasurementUnit = false;
        boolean isProcessingDateTime = false;
        boolean isSoftwareCreator = false;
        boolean isSoftwareName = false;
        boolean isSoftwareVersion = false;

        while (parser.hasNext()) {
            //            System.out.println( "Event: " + parser.getEventType());

            switch (parser.getEventType()) {
                case XMLStreamConstants.START_DOCUMENT:
                    version = parser.getVersion();
                    encoding = parser.getEncoding();
                    break;

                case XMLStreamConstants.END_DOCUMENT:
                    parser.close();
                    break;

                case XMLStreamConstants.NAMESPACE:
                    break;

                case XMLStreamConstants.START_ELEMENT:
                    if (parser.getLocalName().equals("alto")) {
                        altoNS = parser.getNamespaceURI();
                        for (int j = 0; j < parser.getNamespaceCount(); j++) {
                            namespaceList.add(new SimpleEntry<String, String>(parser.getNamespacePrefix(j), parser.getNamespaceURI(j)));
                        }
                        schemaLocation = parser.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation");
                    } else if (parser.getLocalName().equals("Description")) {

                    } else if (parser.getLocalName().equals("MeasurementUnit")) {
                        isMeasurementUnit = true;
                    } else if (parser.getLocalName().equals("OCRProcessing")) {
                        ocrProcessing = parser.getAttributeValue(null, "ID");
                    } else if (parser.getLocalName().equals("ocrProcessingStep")) {

                    } else if (parser.getLocalName().equals("processingDateTime")) {
                        isProcessingDateTime = true;
                    } else if (parser.getLocalName().equals("processingSoftware")) {

                    } else if (parser.getLocalName().equals("softwareCreator")) {
                        isSoftwareCreator = true;
                    } else if (parser.getLocalName().equals("softwareName")) {
                        isSoftwareName = true;
                    } else if (parser.getLocalName().equals("softwareVersion")) {
                        isSoftwareVersion = true;
                    } else if (parser.getLocalName().equals("Styles")) {

                    } else if (parser.getLocalName().equals("ParagraphStyle")) {
                        //TODO: safe in hashmap plus add to files later.
                    } else if (parser.getLocalName().equals("Layout")) {

                    } else if (parser.getLocalName().equals("Page")) {
                        if (writer != null) {
                            writeEndDocument(writer);
                        }
                        currentInfo = ImageInformation.getInformation(inputTifs.get(pageCount).toFile(), (PDPage) catalog.getAllPages().get(
                                pageCount));
                        //                        System.out.println("processing " + currentInfo.getBasename());

                        writer = output.createXMLStreamWriter(Files.newOutputStream(Paths.get(outputFolder.toString(), currentInfo.getBasename()
                                + ".xml")), encoding);

                        pageCount++;
                        writeStartAndDescritption(writer);
                        copyElement(parser, writer);
                    } else if (parser.getLocalName().equals("PrintSpace")) {
                        //                        currentInfo.addLargeSize(Float.parseFloat(parser.getAttributeValue(null, "WIDTH")), Float.parseFloat(parser
                        //                                .getAttributeValue(null, "HEIGHT")));
                        deskewElement(parser, writer, currentInfo);
                    } else if (parser.getLocalName().equals("TextBlock")) {
                        deskewElement(parser, writer, currentInfo);
                    } else if (parser.getLocalName().equals("TextLine")) {
                        deskewElement(parser, writer, currentInfo);
                    } else if (parser.getLocalName().equals("String")) {
                        deskewElement(parser, writer, currentInfo);
                    } else if (parser.getLocalName().equals("SP")) {
                        deskewElement(parser, writer, currentInfo);
                    } else if (parser.getLocalName().equals("Illustration")) {
                        deskewElement(parser, writer, currentInfo);
                    } else if (parser.getLocalName().equals("ComposedBlock")) {
                        deskewElement(parser, writer, currentInfo);
                    } else {
                        copyElement(parser, writer);
                    }
                    break;

                case XMLStreamConstants.CHARACTERS:
                    if (isMeasurementUnit) {
                        measurementUnit += parser.getText().trim();
                    } else if (isProcessingDateTime) {
                        processingDateTime += parser.getText().trim();
                    } else if (isSoftwareCreator) {
                        softwareCreator += parser.getText().trim();
                    } else if (isSoftwareName) {
                        softwareName += parser.getText().trim();
                    } else if (isSoftwareVersion) {
                        softwareVersion += parser.getText().trim();
                    } else {
                        if (writer != null) {
                            writer.writeCharacters(parser.getText().trim());
                        }
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    if (isMeasurementUnit) {
                        isMeasurementUnit = false;
                    } else if (isProcessingDateTime) {
                        isProcessingDateTime = false;
                    } else if (isSoftwareCreator) {
                        isSoftwareCreator = false;
                    } else if (isSoftwareName) {
                        isSoftwareName = false;
                    } else if (isSoftwareVersion) {
                        isSoftwareVersion = false;
                    }
                    if (writer != null) {
                        writer.writeEndElement();
                    }
                    break;

                default:
                    break;
            }
            parser.next();
        }
    }

    private static void deskewElement(XMLStreamReader parser, XMLStreamWriter writer, ImageInformation i) throws XMLStreamException {
        boolean isPage = (parser.getLocalName().equals("Page"));
        String hpos = parser.getAttributeValue(null, "HPOS");
        String vpos = parser.getAttributeValue(null, "VPOS");
        String height = parser.getAttributeValue(null, "HEIGHT");
        String width = parser.getAttributeValue(null, "WIDTH");

        float top_left_x = 0, top_left_y = 0, bottom_right_x = 0, bottom_right_y = 0;
        if (hpos != null && vpos != null) {
            top_left_x = Integer.parseInt(hpos);
            top_left_y = Integer.parseInt(vpos);
            if (width != null) {
                bottom_right_y = top_left_y;
                bottom_right_x = top_left_x + Integer.parseInt(width);
                if (height != null) {
                    bottom_right_y += Integer.parseInt(height);
                }
            }
        }
        double top_left_x_new = (int) ((Math.cos(-i.getAlpha()) * (top_left_x - (i.getLargeWidth() * 0.5))) - (Math.sin(-i.getAlpha()) * (top_left_y
                - (i.getLargeLength() * 0.5))) + (i.getSmallWidth() * 0.5));

        double top_left_y_new = (int) ((Math.sin(-i.getAlpha()) * (top_left_x - (i.getLargeWidth() * 0.5))) + (Math.cos(-i.getAlpha()) * (top_left_y
                - (i.getLargeLength() * 0.5))) + (i.getSmallLength() * 0.5));

        double bottom_right_x_new = (int) ((Math.cos(-i.getAlpha()) * (bottom_right_x - (i.getLargeWidth() * 0.5))) - (Math.sin(-i.getAlpha())
                * (bottom_right_y - (i.getLargeLength() * 0.5))) + (i.getSmallWidth() * 0.5));

        double bottom_right_y_new = (int) ((Math.sin(-i.getAlpha()) * (bottom_right_x - (i.getLargeWidth() * 0.5))) + (Math.cos(-i.getAlpha())
                * (bottom_right_y - (i.getLargeLength() * 0.5))) + (i.getSmallLength() * 0.5));

        writer.writeStartElement(parser.getNamespaceURI(), parser.getLocalName());
        for (int j = 0; j < parser.getAttributeCount(); j++) {
            if (parser.getAttributeLocalName(j).equals("HPOS")) {
                //                System.out.println(((int) top_left_x_new));
                writer.writeAttribute(parser.getAttributeLocalName(j), Integer.toString(((int) top_left_x_new)));
            } else if (parser.getAttributeLocalName(j).equals("VPOS")) {
                //                System.out.println((int) top_left_y_new);
                writer.writeAttribute(parser.getAttributeLocalName(j), Integer.toString(((int) top_left_y_new)));
            } else if (parser.getAttributeLocalName(j).equals("HEIGHT")) {
                if (isPage) {
                    writer.writeAttribute("HEIGHT", Integer.toString((int) i.getSmallLength()));
                } else {
                    writer.writeAttribute(parser.getAttributeLocalName(j), Integer.toString(((int) (bottom_right_y_new - top_left_y_new))));
                }
            } else if (parser.getAttributeLocalName(j).equals("WIDTH")) {
                if (isPage) {
                    writer.writeAttribute("WIDTH", Integer.toString((int) i.getSmallWidth()));
                } else {
                    writer.writeAttribute(parser.getAttributeLocalName(j), Integer.toString(((int) (bottom_right_x_new - top_left_x_new))));
                }
            } else {
                if (parser.getAttributeNamespace(j) == null) {
                    writer.writeAttribute(parser.getAttributeLocalName(j), parser.getAttributeValue(j));
                } else {
                    writer.writeAttribute(parser.getAttributePrefix(j), parser.getAttributeNamespace(j), parser.getAttributeLocalName(j), parser
                            .getAttributeValue(j));
                }
            }
        }
        if (isPage) {
            if (parser.getAttributeValue(null, "HEIGHT") == null) {
                writer.writeAttribute("HEIGHT", Integer.toString((int) i.getSmallLength()));
            }
            if (parser.getAttributeValue(null, "WIDTH") == null) {
                writer.writeAttribute("WIDTH", Integer.toString((int) i.getSmallLength()));
            }
        }
        if (parser.getLocalName().equals("String") && parser.getAttributeValue(null, "ID") == null) {
            writer.writeAttribute("ID", "String_" + stringIdCount++);
        }
    }

    private static void writeStartAndDescritption(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartDocument(encoding, version);
        writer.setDefaultNamespace(altoNS);
        writer.writeStartElement(altoNS, "alto");
        writer.writeDefaultNamespace(altoNS);
        for (SimpleEntry<String, String> ns : namespaceList) {
            if (ns.getKey() != null) {
                writer.writeNamespace(ns.getKey(), ns.getValue());
            }
        }
        writer.writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", schemaLocation);
        writer.writeStartElement(altoNS, "Description");
        writer.writeStartElement(altoNS, "MeasurementUnit");
        writer.writeCharacters(measurementUnit);
        writer.writeEndElement();
        writer.writeStartElement(altoNS, "OCRProcessing");
        writer.writeAttribute("ID", ocrProcessing);
        writer.writeStartElement(altoNS, "ocrProcessingStep");
        writer.writeStartElement(altoNS, "processingDateTime");
        writer.writeCharacters(processingDateTime);
        writer.writeEndElement();
        writer.writeStartElement(altoNS, "processingSoftware");
        writer.writeStartElement(altoNS, "softwareCreator");
        writer.writeCharacters(softwareCreator);
        writer.writeEndElement();
        writer.writeStartElement(altoNS, "softwareName");
        writer.writeCharacters(softwareName);
        writer.writeEndElement();
        writer.writeStartElement(altoNS, "softwareVersion");
        writer.writeCharacters(softwareVersion);
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeStartElement(altoNS, "Layout");
    }

    private static void writeEndDocument(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

    private static void copyElement(XMLStreamReader parser, XMLStreamWriter writer) throws XMLStreamException {
        //        if(parser.getLocalName().equals("Page") && parser.getAttributeValue(null, "ID").equals("Page52")) {
        //            System.out.println("blubb");
        //        }
        writer.writeStartElement(parser.getNamespaceURI(), parser.getLocalName());
        for (int j = 0; j < parser.getAttributeCount(); j++) {
            if (parser.getAttributeNamespace(j) == null) {
                writer.writeAttribute(parser.getAttributeLocalName(j), parser.getAttributeValue(j));
            } else {
                writer.writeAttribute(parser.getAttributePrefix(j), parser.getAttributeNamespace(j), parser.getAttributeLocalName(j), parser
                        .getAttributeValue(j));
            }
        }
    }
}

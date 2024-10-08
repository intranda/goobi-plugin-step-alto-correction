package de.intranda.goobi.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.goobi.beans.Process;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.plugin.interfaces.AbstractStepPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;

import de.intranda.goobi.plugins.utils.AltoDeskewer;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.SwapException;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class AltoCorrectionPlugin extends AbstractStepPlugin implements IStepPlugin, IPlugin {

    private static final String PLUGIN_NAME = "AltoCorrectionPlugin";
    private static final Logger logger = Logger.getLogger(AltoCorrectionPlugin.class);

    @Override
    public String getTitle() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean execute() {

        try {
            Process process = myStep.getProzess();
            String inputFolder;
            inputFolder = process.getImagesTifDirectory(false);
            String altoOutputFolder = process.getOcrAltoDirectory();
            String pdfOutputFolder = process.getOcrPdfDirectory();

            List<Path> inputTifs = new ArrayList<>();
            Path altoFile = null;
            Path pdfInput = null;
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(inputFolder))) {
                for (Path p : ds) {
                    if (p.toString().toLowerCase().endsWith(".tif")) {
                        inputTifs.add(p);
                    }
                    if (p.toString().toLowerCase().endsWith(".xml")) {
                        altoFile = p;
                    }
                    if (p.toString().toLowerCase().endsWith(".pdf")) {
                        pdfInput = p;
                    }
                }
            }
            if (pdfInput == null || altoFile == null) {
                if (pdfInput != null) {
                    Path folder = Paths.get(altoOutputFolder);
                    Path file = Paths.get(altoOutputFolder + "/" + pdfInput.toFile().getName());
                    if (!Files.exists(folder)) {
                        Files.createDirectories(folder);
                    }
                    Files.move(pdfInput, file, StandardCopyOption.REPLACE_EXISTING);
                }
                if (altoFile != null) {
                    Path folder = Paths.get(pdfOutputFolder);
                    Path file = Paths.get(pdfOutputFolder + "/" + altoFile.toFile().getName());
                    if (!Files.exists(folder)) {
                        Files.createDirectories(folder);
                    }
                    Files.move(altoFile, file, StandardCopyOption.REPLACE_EXISTING);
                }
                // if one exists, move it
                Helper.setMeldung("Missing input data.");
                return true;
            }
            // move to source folder

            File folder = new File(altoOutputFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            folder = new File(pdfOutputFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            Collections.sort(inputTifs);
            PDDocument doc = Loader.loadPDF(pdfInput.toFile());
            AltoDeskewer.deskewAlto(altoFile, inputTifs, doc, Paths.get(altoOutputFolder));
            Splitter splitter = new Splitter();
            List<PDDocument> splitList = splitter.split(doc);
            int i = 0;
            for (PDDocument sDoc : splitList) {
                String tifName = inputTifs.get(i++).getFileName().toString();
                String newName = tifName.substring(0, tifName.lastIndexOf('.')) + ".pdf";
                sDoc.save(Paths.get(pdfOutputFolder, newName).toString());
                sDoc.close();
            }

            doc.close();
        } catch (SwapException | IOException | XMLStreamException e) {
            logger.error(e);
            Helper.setFehlerMeldung(e);
            return false;
        }

        return true;
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return null;
    }

}

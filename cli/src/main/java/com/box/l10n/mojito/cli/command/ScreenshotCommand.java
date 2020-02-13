package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.ImageClient;
import com.box.l10n.mojito.rest.client.ScreenshotClient;
import com.box.l10n.mojito.rest.entity.Locale;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.Screenshot;
import com.box.l10n.mojito.rest.entity.ScreenshotRun;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Uploads screenshots to Mojito
 *
 * Screenshots image can be in any of the following format: PNG, JPEG and GIF.
 * Extension can be lower or upper case. For JPEG (both jpg and jpeg are
 * supported).
 *
 * Assume the first sub directory contains the locale.
 *
 * Screenshots metadata are provided via a json file. The json file should have
 * the same filename as the image but with the file extension replace with
 * 'json'
 *
 * Screenshot metadata must contain: - sequence id (used to display the
 * screenshot in the right order later) - unique screenname (to be used across
 * runs to identify the screen) - - text units rendered target to link to
 * existing text unit in mojito
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"screenshot"}, commandDescription = "Upload screenshots")
public class ScreenshotCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ScreenshotCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {Param.REPOSITORY_LOCALES_MAPPING_LONG, Param.REPOSITORY_LOCALES_MAPPING_SHORT}, arity = 1, required = false, description = "Locale mapping, format: \"fr:fr-FR,ja:ja-JP\". "
            + "The keys contain BCP47 tags of the generated files and the values indicate which repository locales are used to fetch the translations.")
    String localeMappingParam;

    @Parameter(names = {Param.SOURCE_DIRECTORY_LONG, Param.SOURCE_DIRECTORY_SHORT}, arity = 1, required = false, description = "Directory to scan for screenshot")
    String sourceDirectoryParam;

    @Parameter(names = { "-rn" , "--run-name"}, arity = 1, required = false, description = "The name of the screenshot run to create/update")
    String screenshotRunName;

    @Autowired
    CommandHelper commandHelper;

    CommandDirectories commandDirectories;

    @Autowired
    ImageClient imageClient;

    @Autowired
    ScreenshotClient screenshotClient;

    /**
     * Contains a map of locale for generating localized file a locales defined
     * in the repository.
     */
    Map<String, String> localeMappings;

    Map<String, Locale> repositoryLocales;

    @Override
    public void execute() throws CommandException {

        consoleWriter.newLine().a("Upload Screenshots to repository: ").fg(Color.CYAN).a(repositoryParam).println(2);
        Repository repository = commandHelper.findRepositoryByName(repositoryParam);

        commandDirectories = new CommandDirectories(sourceDirectoryParam);
        localeMappings = commandHelper.getLocaleMapping(localeMappingParam);
        repositoryLocales = commandHelper.getSortedRepositoryLocales(repository);

        List<Path> listFilesWithExtensionInSourceDirectory = commandDirectories.listFilesWithExtensionInSourceDirectory("png", "jpg", "jpeg", "gif");

        ScreenshotRun screenshotRun = new ScreenshotRun();
        screenshotRun.setRepository(repository);

        if (screenshotRunName == null) {
            screenshotRun.setName(UUID.randomUUID().toString());
        } else {
            screenshotRun.setName(screenshotRunName);
        }

        for (Path imagePath : listFilesWithExtensionInSourceDirectory) {
            processImage(imagePath, screenshotRun);
        }

        screenshotClient.uploadScreenshots(screenshotRun);
    }

    void processImage(Path imagePath, ScreenshotRun screenshotRun) throws CommandException {
        try {
            Path metadataFilenameForImage = getMetadataFilenameForImage(imagePath);

            Screenshot screenshot = readMetadata(metadataFilenameForImage);
            screenshot.setName(FilenameUtils.getBaseName(imagePath.getFileName().toString()));
            Path relativePath = commandDirectories.getSourceDirectoryPath().relativize(imagePath);
            screenshot.setLocale(getLocaleFromImagePath(relativePath));

            String uploadPath = screenshotRun.getName() + "/" + relativePath.toString();
            screenshot.setSrc("api/images/" + uploadPath);
            uploadImage(imagePath, uploadPath);
            screenshotRun.getScreenshots().add(screenshot);
            consoleWriter.a("Uploaded screenshot: ").fg(Ansi.Color.CYAN).a(screenshot.getName()).reset().
                    a(" for locale: ").fg(Color.CYAN).a(screenshot.getLocale().getBcp47Tag()).println();

        } catch (InvalidLocaleException ile) {
            consoleWriter.fg(Color.YELLOW).a("Skip ").reset().a(ile.getMessage());
        }
    }

    Locale getLocaleFromImagePath(Path path) throws InvalidLocaleException {

        try {
            String localeStr = getFirstElementInPath(path);

            if (localeMappings != null && localeMappings.containsKey(localeStr)) {
                localeStr = localeMappings.get(localeStr);
            }

            if (!repositoryLocales.containsKey(localeStr)) {
                throw new IllegalArgumentException("The locale: " + localeStr + " is not supported by the repository");
            }

            return repositoryLocales.get(localeStr);
        } catch (IllegalArgumentException iae) {
            throw new InvalidLocaleException("The image: " + path.toString() + " must be included in a directory which path starts with a suported locale", iae);
        }
    }

    /**
     * Get the first element in a path and check that the path has at least 2
     * elements (last element should be a filename)
     *
     * @return first directory
     */
    String getFirstElementInPath(Path path) {

        if (path.getNameCount() < 2) {
            throw new IllegalArgumentException();
        }

        return path.getName(0).toString();
    }

    void uploadImage(Path image, String uploadPath) throws CommandException {
        logger.debug("Upload image: {} to path: {}", image.toString(), uploadPath);
        try {
            byte[] content = Files.readAllBytes(image);
            imageClient.uploadImage(uploadPath, content);
        } catch (IOException ex) {
            throw new CommandException("Failed to upload image: " + image.toString(), ex);
        }
    }

    Path getMetadataFilenameForImage(Path imagePath) {
        String imageFilename = imagePath.getFileName().toString();
        String metadataFilename = FilenameUtils.getBaseName(imageFilename) + ".json";
        return imagePath.resolveSibling(metadataFilename);
    }

    Screenshot readMetadata(Path metadataFilePath) throws CommandException {
        logger.debug("Read metadata for image: {}", metadataFilePath.toString());
        ObjectMapper mapper = new ObjectMapper();
        try {
            Screenshot screenshot;
            File metadataFile = metadataFilePath.toFile();
            if (metadataFilePath.toFile().exists()) {
                screenshot = mapper.readValue(metadataFile, Screenshot.class);
            } else {
                consoleWriter.a("No metadata file for image: ").fg(Color.CYAN).a(metadataFilePath.toString()).println().reset();
                screenshot = new Screenshot();
            }
            return screenshot;
        } catch (IOException ioe) {
            throw new CommandException("Can't parse: " + metadataFilePath.toString() + " for screenshot metadata", ioe);
        }
    }

    static class InvalidLocaleException extends Exception {

        public InvalidLocaleException(String string, IllegalArgumentException iae) {
            super(string, iae);
        }
    }
}

package com.box.l10n.mojito.service.drop.importer;

import com.box.l10n.mojito.service.drop.exporter.FileSystemDropExporter;
import com.box.sdk.BoxFile;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * A {@link DropImporter} that read content from the local File system, see related {@link
 * FileSystemDropExporter}.
 *
 * @author jaurambault
 */
@Configurable
public class FileSystemDropImporter implements DropImporter {

  static Logger logger = LoggerFactory.getLogger(FileSystemDropImporter.class);

  /** Id of the folder that contains the localized files to be imported */
  Path localizedFolderPath;

  /**
   * Creates an instance to import files from the specified folder.
   *
   * @param localizedFolderPath path of the folder that contains the localized files to be imported
   */
  public FileSystemDropImporter(Path localizedFolderPath) {
    this.localizedFolderPath = localizedFolderPath;
  }

  public Path getLocalizedFolderPath() {
    return localizedFolderPath;
  }

  @Override
  public void downloadFileContent(DropFile dropFile) throws DropImporterException {

    logger.debug("Download file content for dropFile: {}", dropFile.getId());

    try {
      String content = Files.toString(Paths.get(dropFile.getId()).toFile(), StandardCharsets.UTF_8);
      dropFile.setContent(content);
    } catch (IOException ex) {
      throw new DropImporterException("Cannot download drop file content", ex);
    }
  }

  @Override
  public List<DropFile> getFiles() throws DropImporterException {

    logger.debug("Gets drop files to be imported");
    List<DropFile> dropFiles = new ArrayList<>();

    File[] listFiles = localizedFolderPath.toFile().listFiles();

    for (File file : listFiles) {
      if (file.isFile()) {
        dropFiles.add(fileToDropFile(file));
      }
    }

    return dropFiles;
  }

  /**
   * Converts a {@link BoxFile} to {@link DropFile}
   *
   * @param file file to be converted
   * @return {@link DropFile} resulting of {@link BoxFile} conversion
   */
  private DropFile fileToDropFile(File file) {
    String name = file.getName();
    String bcp47Tag = getBcp47TagFromFileName(name);
    return new DropFile(file.toString(), bcp47Tag, name, Files.getFileExtension(name));
  }

  /**
   * Gets the BCP47 tag from the localized file filename.
   *
   * <p>The tag is at beginning of the filename and is delimited with '_'
   *
   * @param fileName
   * @return the language of the localized file
   */
  String getBcp47TagFromFileName(String fileName) {
    return fileName.substring(0, fileName.indexOf("_"));
  }
}

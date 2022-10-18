package com.box.l10n.mojito.cli.filefinder;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

import com.box.l10n.mojito.cli.filefinder.file.FileType;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File visitor used to find source files.
 *
 * @author jaurambault
 */
class SourceFileVisitor extends SimpleFileVisitor<Path> {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(SourceFileVisitor.class);

  /** List of source files found */
  final List<FileMatch> sourceMatches = new ArrayList<>();

  /** Pattern used to match source file path */
  private final FilePattern sourceFilePattern;

  /** File type of the source files */
  private final FileType fileType;

  /** Source directory from which the search is performed */
  private final Path sourceDirectory;

  /** Regular expression to match source file path in addition to sourceFilePattern */
  private final Pattern sourcePathFilterPattern;

  private DirectoryScanUtils directoryScanUtils;

  public SourceFileVisitor(
      FileType fileType,
      Path sourceDirectory,
      String sourcePathFilterRegex,
      DirectoryScanUtils directoryScanUtils) {
    this.fileType = Objects.requireNonNull((fileType));
    this.sourceFilePattern = fileType.getSourceFilePattern();
    this.sourceDirectory = Objects.requireNonNull(sourceDirectory);
    this.sourcePathFilterPattern =
        sourcePathFilterRegex == null ? null : Pattern.compile(sourcePathFilterRegex);
    ;
    this.directoryScanUtils = Objects.requireNonNull(directoryScanUtils);
  }

  /**
   * Gets the list of source files found
   *
   * @return the list of source files found
   */
  public List<FileMatch> getSourceMatches() {
    return sourceMatches;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
    return directoryScanUtils.shouldScan(dir) ? CONTINUE : SKIP_SUBTREE;
  }

  @Override
  public FileVisitResult visitFileFailed(Path file, IOException ioe) throws IOException {
    if (ioe instanceof AccessDeniedException) {
      logger.error(file + ": cannot access directory");
    } else {
      throw ioe;
    }
    return CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

    String relativePathStr = sourceDirectory.relativize(file).toString();

    if (relativePathStr.endsWith(fileType.getSourceFileExtension())) {

      Matcher matcher = sourceFilePattern.getPattern().matcher(relativePathStr);

      if (matcher.matches() && matchesSourcePathFilterRegex(relativePathStr)) {

        FileMatch fileMatch = new FileMatch();
        fileMatch.setFileType(fileType);
        fileMatch.setTarget(false);
        fileMatch.setPath(file);

        for (String group : sourceFilePattern.getGroups()) {
          fileMatch.addProperty(group, matcher.group(group));
        }

        if (!fileMatch.getSourcePath().equals(relativePathStr)) {
          throw new RuntimeException("this must be equal");
        }

        logger.debug("src name: " + fileMatch.getSourcePath());
        sourceMatches.add(fileMatch);
      }
    }

    return FileVisitResult.CONTINUE;
  }

  protected boolean matchesSourcePathFilterRegex(String path) {
    return sourcePathFilterPattern == null || sourcePathFilterPattern.matcher(path).matches();
  }
}

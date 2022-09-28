package com.box.l10n.mojito.cli.filefinder2;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * FileTreeWalker with "include" and "exclude" regex filters
 * <p>
 * The FileTreeWalker calls onIncludeFile() for files that don't match any of the "exclude" patterns
 * and that match one of the "include" patterns. In other words "exclude" has precedence over
 * "include". The pattern matching is done on the "start" relativized path
 * <p>
 * The "include" regex pattern applies to file only. The "exclude" pattern is applied to both files
 * and directories to avoid walking unecessary part of the tree.
 * <p>
 * The file passed to onIncludeFile() is from the SimpleFileVisitor, left unchanged
 */
public class FileTreeWalker {

  final Path start;
  final Consumer<Path> onIncludedFile;
  final List<Pattern> includePatterns;
  final List<Pattern> excludePatterns;

  public FileTreeWalker(Path start, Consumer<Path> onIncludedFile, List<String> includeRegexes,
      List<String> excludeRegexes) {
    this.start = start;
    this.onIncludedFile = onIncludedFile;
    this.includePatterns = includeRegexes.stream().map(Pattern::compile)
        .collect(Collectors.toList());
    this.excludePatterns = excludeRegexes.stream().map(Pattern::compile)
        .collect(Collectors.toList());
  }

  public void walkFileTree() {

    try {
      Files.walkFileTree(start, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            throws IOException {

          FileVisitResult fileVisitResult = FileVisitResult.CONTINUE;

          final Path relativeDirPath = start.relativize(dir);

          if (shouldExcludePath(relativeDirPath)) {
            fileVisitResult = fileVisitResult.SKIP_SUBTREE;
          }

          return fileVisitResult;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          final Path relativeFilePath = start.relativize(file);

          if (!shouldExcludePath(relativeFilePath)) {
            final boolean shouldIncludeFile = includePatterns.stream()
                .map(pattern -> pattern.matcher(relativeFilePath.toString())).anyMatch(
                    Matcher::matches);
            if (shouldIncludeFile) {
              onIncludedFile.accept(file);
            }
          }

          return FileVisitResult.CONTINUE;
        }

        private boolean shouldExcludePath(Path relativeDirPath) {
          return excludePatterns.stream()
              .map(pattern -> pattern.matcher(relativeDirPath.toString())).anyMatch(
                  Matcher::matches);
        }
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

package com.box.l10n.mojito.cli.filefinder2;

import com.google.common.collect.ImmutableList;
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
 * FileTreeWalker with Include and Exclude regex filters
 * <p>
 * - Calls onIncludeFile() for files that don't match any of the "exclude" patterns and that match
 * one of the "include" patterns. In other words "exclude" has precedence over "include". The
 * pattern matching is done on the "start" relativized path - The file passed to onIncludeFile() is
 * from the SimpleFileVisitor without any modification - "include" and "exclude" patterns apply to
 * files, not directories
 */
public class FileTreeWalker {

  final Path start;
  final Consumer<Path> onIncludedFile;
  private final List<Pattern> includePatterns;
  private final List<Pattern> excludePatterns;

  public FileTreeWalker(Path start, Consumer<Path> onIncludedFile, List<String> includeRegex,
      List<String> excludeRegex) {
    this.start = start;
    this.onIncludedFile = onIncludedFile;
    this.includePatterns = includeRegex.stream().map(Pattern::compile)
        .collect(Collectors.toList());
    this.excludePatterns = excludeRegex.stream().map(Pattern::compile)
        .collect(Collectors.toList());
  }

  public void walkFileTree() {

    try {
      Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          final Path relativePath = start.relativize(file);

          final boolean isExcluded = excludePatterns.stream()
              .map(pattern -> pattern.matcher(relativePath.toString())).anyMatch(
                  Matcher::matches);

          if (!isExcluded) {
            final boolean isIncluded = includePatterns.stream()
                .map(pattern -> pattern.matcher(relativePath.toString())).anyMatch(
                    Matcher::matches);
            if (isIncluded) {
              onIncludedFile.accept(file);
            }
          }

          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

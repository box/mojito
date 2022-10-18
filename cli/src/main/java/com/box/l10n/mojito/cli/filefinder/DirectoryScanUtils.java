package com.box.l10n.mojito.cli.filefinder;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Helps to figure out which directories need to be scanned based on given lists of "include"
 * patterns and "exclude" patterns.
 *
 * <p>Directory patterns are like directory paths but some subpaths can contain a wild card: "*",
 * for example "modules/*&#47;src/main". The wild card will exactly match one directory name (no
 * recursion in the directory).
 */
public class DirectoryScanUtils {

  private final Path rootDirectory;

  private final List<String[]> directoriesIncludePatterns;
  private final List<String[]> directoriesExcludePatterns;

  public DirectoryScanUtils(
      Path rootDirectory,
      List<String> directoriesIncludePatterns,
      List<String> directoriesExcludePatterns) {
    this.rootDirectory = Objects.requireNonNull(rootDirectory);
    this.directoriesIncludePatterns = splitPatterns(directoriesIncludePatterns);
    this.directoriesExcludePatterns = splitPatterns(directoriesExcludePatterns);
  }

  /**
   * Indicates if a directory should be scanned to find files to process.
   *
   * <p>The root directory should always be scanned. Other directories should be scanned based on
   * the "exclude" and "include" lists. The "exclude" list has precendence over the include list If
   * the directoriesExcludePatterns list is non-null, the directory must not match any of the
   * "exclude" patterns. If the directoriesIncludePatterns list is non-null, the directory will have
   * to match at least one of the "include" patterns.
   */
  public boolean shouldScan(Path dir) {
    return rootDirectory.equals(dir) || (!isExcludedDirectory(dir) && isIncludedDirectory(dir));
  }

  private boolean isExcludedDirectory(Path dir) {
    boolean exclude = false;
    if (directoriesExcludePatterns != null) {
      String[] relativePathSplit = getRelativePathSplit(dir);
      exclude =
          directoriesExcludePatterns.stream()
              .anyMatch(
                  s ->
                      s.length == relativePathSplit.length
                          && DirectoryScanUtils.shouldScanDirectory(s, relativePathSplit));
    }
    return exclude;
  }

  private boolean isIncludedDirectory(Path dir) {
    boolean include = true;
    if (directoriesIncludePatterns != null) {
      String[] relativePathSplit = getRelativePathSplit(dir);
      include =
          directoriesIncludePatterns.stream()
              .anyMatch(s -> DirectoryScanUtils.shouldScanDirectory(s, relativePathSplit));
    }
    return include;
  }

  private String[] getRelativePathSplit(Path dir) {
    Path relativePath = rootDirectory.relativize(dir);
    String[] relativePathSplit = relativePath.toString().split("/");
    return relativePathSplit;
  }

  /**
   * Indicates if a directory should be scanned given a pattern. A directory should be scanned if
   * its sub paths matches the ones in the directory pattern. If the directory is shorter than the
   * pattern, the directory just needs to match the beginning of the pattern.
   *
   * <p>The directory pattern is a list of sub paths. A sub path can either be a plain name or a
   * wildcard "*". The wildcard maps to exactly one sub path. Examples: [ "webapp", "locale" ], [
   * "*", "locale" ]
   *
   * <p>shouldScanDirectory([ "webapp", "locale" ], [ "webapp", "locale" ]) --> true
   * shouldScanDirectory([ "webapp", "locale" ], [ "webapp", "locale2" ]) --> false
   * shouldScanDirectory([ "webapp", "locale" ], [ "webapp" ]) --> true, we need to recurse to find
   * directories that potentially match shouldScanDirectory([ "*", "locale" ], [ "webapp" ]) -->
   * true shouldScanDirectory([ "*", "locale" ], [ "webapp", "locale" ]) --> true
   *
   * <p>Edge cases: - if the directory is an empty array, which is equivalent to ask should we scan
   * the root, then this will return true since we need to scan the root regardless of the pattern
   * to get started - if an empty array is provided as pattern it is equivalent to matching the root
   * and it will always be true regardless of the directory
   */
  static boolean shouldScanDirectory(String[] directoryPattern, String[] aDirectory) {
    boolean shouldScan = true;

    for (int i = 0; i < directoryPattern.length; i++) {

      if (i == aDirectory.length) {
        break;
      }

      if (!"*".equals(directoryPattern[i]) && !directoryPattern[i].equals(aDirectory[i])) {
        shouldScan = false;
        break;
      }
    }

    return shouldScan;
  }

  static List<String[]> splitPatterns(List<String> directoriesIncludePatterns) {
    List<String[]> splitted = null;

    if (directoriesIncludePatterns != null) {
      splitted =
          directoriesIncludePatterns.stream().map(s -> s.split("/")).collect(Collectors.toList());
    }

    return splitted;
  }
}

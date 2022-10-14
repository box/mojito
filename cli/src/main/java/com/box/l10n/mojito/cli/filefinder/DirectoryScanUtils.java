package com.box.l10n.mojito.cli.filefinder;

public class DirectoryScanUtils {
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
}

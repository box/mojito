package com.box.l10n.mojito.cli.filefinder;

import java.util.ArrayList;

/**
 * Contains the results of {@link FileFinder#find() }.
 *
 * @author jaurambault
 */
public class FileFinderResult {

  /** FileMatches for source files (files to be localized) */
  ArrayList<FileMatch> sources = new ArrayList<>();
  /** FileMatches for target files (localized files) */
  ArrayList<FileMatch> targets = new ArrayList<>();

  public ArrayList<FileMatch> getSources() {
    return sources;
  }

  public ArrayList<FileMatch> getTargets() {
    return targets;
  }
}

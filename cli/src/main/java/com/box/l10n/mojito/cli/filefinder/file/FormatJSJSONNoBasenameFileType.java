package com.box.l10n.mojito.cli.filefinder.file;

import java.util.Arrays;

/**
 * @author jeanaurambault
 */
public class FormatJSJSONNoBasenameFileType extends LocaleAsFileNameType {

  public FormatJSJSONNoBasenameFileType() {
    this.sourceFileExtension = "json";
    this.defaultFilterOptions =
        Arrays.asList(
            "noteKeyPattern=description",
            "extractAllPairs=false",
            "exceptions=defaultMessage",
            "removeKeySuffix=/defaultMessage");
  }
}

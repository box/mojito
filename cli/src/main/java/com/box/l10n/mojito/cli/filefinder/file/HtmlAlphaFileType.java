package com.box.l10n.mojito.cli.filefinder.file;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;

/** @author jaurambault */
public class HtmlAlphaFileType extends LocaleInNameFileType {
  public HtmlAlphaFileType() {
    this.sourceFileExtension = "html";
    this.filterConfigIdOverride = FilterConfigIdOverride.HTML_ALPHA;
  }
}

package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.utils.OptionsParser;
import java.util.List;
import net.sf.okapi.common.annotation.IAnnotation;

public class FilterOptions extends OptionsParser implements IAnnotation {

  public FilterOptions(List<String> options) {
    super(options);
  }
}

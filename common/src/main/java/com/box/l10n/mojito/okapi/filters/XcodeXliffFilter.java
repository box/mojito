package com.box.l10n.mojito.okapi.filters;

import java.util.List;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.skeleton.ISkeletonWriter;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import net.sf.okapi.filters.xliff.XLIFFSkeletonWriter;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author jyi
 */
@Configurable
public class XcodeXliffFilter extends XLIFFFilter {

  public static final String FILTER_CONFIG_ID = "okf_xliff@mojito-xcode";

  @Override
  public String getName() {
    return FILTER_CONFIG_ID;
  }

  @Override
  public List<FilterConfiguration> getConfigurations() {
    List<FilterConfiguration> list = super.getConfigurations();
    list.add(
        new FilterConfiguration(
            getName(),
            MimeTypeMapper.XLIFF_MIME_TYPE,
            getClass().getName(),
            "XCODEXLIFF",
            "Configuration for XCODE XLIFF documents. Supports XCODE specific metadata",
            "xcode_mojito.fprm",
            ".xliff"));
    return list;
  }

  @Override
  public ISkeletonWriter createSkeletonWriter() {
    XLIFFSkeletonWriter writer = new XcodeXliffSkeletonWriter(getParameters());
    return writer;
  }
}

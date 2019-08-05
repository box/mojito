package com.box.l10n.mojito.okapi.filters;

import java.util.List;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.encoder.EncoderManager;
import static net.sf.okapi.common.encoder.XMLEncoder.ESCAPEGT;
import static net.sf.okapi.common.encoder.XMLEncoder.ESCAPELINEBREAK;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import org.springframework.beans.factory.annotation.Configurable;

/**
 *
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
        list.add(new FilterConfiguration(getName(),
                MimeTypeMapper.XLIFF_MIME_TYPE,
                getClass().getName(),
                "XCODEXLIFF",
                "Configuration for XCODE XLIFF documents. Supports XCODE specific metadata",
                "xcode_mojito.fprm",
                ".xliff"));
        return list;
    }
    
	@Override
	public Event next () {
            return super.next();
        }

}

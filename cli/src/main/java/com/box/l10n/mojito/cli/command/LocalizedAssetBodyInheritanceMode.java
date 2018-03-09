package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.box.l10n.mojito.rest.entity.LocalizedAssetBody;

/**
 *
 * @author jaurambault
 */
public class LocalizedAssetBodyInheritanceMode implements IStringConverter<LocalizedAssetBody.InheritanceMode> {

    @Override
    public LocalizedAssetBody.InheritanceMode convert(String string) {

        LocalizedAssetBody.InheritanceMode statusEqualTarget = null;

        if (string != null) {
            try {
                statusEqualTarget = LocalizedAssetBody.InheritanceMode.valueOf(string.toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new ParameterException("Invalid \"inheritance type\" processing type [" + string + "]");
            }
        }

        return statusEqualTarget;
    }

}

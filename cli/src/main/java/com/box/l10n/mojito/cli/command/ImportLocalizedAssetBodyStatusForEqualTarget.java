package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.box.l10n.mojito.rest.entity.ImportLocalizedAssetBody;

/**
 *
 * @author jaurambault
 */
public class ImportLocalizedAssetBodyStatusForEqualTarget implements IStringConverter<ImportLocalizedAssetBody.StatusForEqualTarget> {

    @Override
    public ImportLocalizedAssetBody.StatusForEqualTarget convert(String string) {

        ImportLocalizedAssetBody.StatusForEqualTarget statusEqualTarget = null;

        if (string != null) {
            try {
                statusEqualTarget = ImportLocalizedAssetBody.StatusForEqualTarget.valueOf(string.toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new ParameterException("Invalid \"status equal target\" processing type [" + string + "]");
            }
        }

        return statusEqualTarget;
    }

}

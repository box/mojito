package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.box.l10n.mojito.rest.entity.ImportLocalizedAssetBody;

/**
 *
 * @author jaurambault
 */
public class ImportLocalizedAssetBodyStatusForSourceEqTarget implements IStringConverter<ImportLocalizedAssetBody.StatusForSourceEqTarget> {

    @Override
    public ImportLocalizedAssetBody.StatusForSourceEqTarget convert(String string) {

        ImportLocalizedAssetBody.StatusForSourceEqTarget sourceEqualsTargetProcessing = null;

        if (string != null) {
            try {
                sourceEqualsTargetProcessing = ImportLocalizedAssetBody.StatusForSourceEqTarget.valueOf(string.toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new ParameterException("Invalid \"source equals target\" processing type [" + string + "]");
            }
        }

        return sourceEqualsTargetProcessing;
    }

}

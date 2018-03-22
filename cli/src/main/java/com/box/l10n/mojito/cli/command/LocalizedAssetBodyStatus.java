package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.box.l10n.mojito.rest.entity.LocalizedAssetBody;

/**
 *
 * @author dragosv
 */
public class LocalizedAssetBodyStatus implements IStringConverter<LocalizedAssetBody.Status> {

    @Override
    public LocalizedAssetBody.Status convert(String string) {

        LocalizedAssetBody.Status status = null;

        if (string != null) {
            try {
                status = LocalizedAssetBody.Status.valueOf(string.toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new ParameterException("Invalid \"status\" processing type [" + string + "]");
            }
        }

        return status;
    }

}

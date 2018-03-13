package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.box.l10n.mojito.rest.entity.LocalizedAssetBody;

/**
 *
 * @author dragosv
 */
public class LocalizedAssetBodyInheritanceMode implements IStringConverter<LocalizedAssetBody.InheritanceMode> {

    @Override
    public LocalizedAssetBody.InheritanceMode convert(String string) {

        LocalizedAssetBody.InheritanceMode inheritanceMode = null;

        if (string != null) {
            try {
                inheritanceMode = LocalizedAssetBody.InheritanceMode.valueOf(string.toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new ParameterException("Invalid \"inheritance mode\" processing type [" + string + "]");
            }
        }

        return inheritanceMode;
    }

}

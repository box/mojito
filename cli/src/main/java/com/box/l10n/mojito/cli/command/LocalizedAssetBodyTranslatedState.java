package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.box.l10n.mojito.rest.entity.LocalizedAssetBody;

/**
 *
 * @author dragosv
 */
public class LocalizedAssetBodyTranslatedState implements IStringConverter<LocalizedAssetBody.TranslatedState> {

    @Override
    public LocalizedAssetBody.TranslatedState convert(String string) {

        LocalizedAssetBody.TranslatedState translatedState = null;

        if (string != null) {
            try {
                translatedState = LocalizedAssetBody.TranslatedState.valueOf(string.toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new ParameterException("Invalid \"translated state\" processing type [" + string + "]");
            }
        }

        return translatedState;
    }

}

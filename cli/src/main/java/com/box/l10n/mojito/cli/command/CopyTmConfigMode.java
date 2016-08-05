package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.box.l10n.mojito.rest.entity.CopyTmConfig;

/**
 *
 * @author jaurambault
 */
public class CopyTmConfigMode implements IStringConverter<CopyTmConfig.Mode> {

    @Override
    public CopyTmConfig.Mode convert(String string) {

        CopyTmConfig.Mode mode = null;

        if (string != null) {
            try {
                mode = CopyTmConfig.Mode.valueOf(string.toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new ParameterException("Invalid import status [" + string + "]");
            }
        }

        return mode;
    }

}

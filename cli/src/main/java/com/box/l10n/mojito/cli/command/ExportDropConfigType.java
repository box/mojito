package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.box.l10n.mojito.rest.entity.ExportDropConfig;

/**
 *
 * @author jaurambault
 */
public class ExportDropConfigType implements IStringConverter<ExportDropConfig.Type> {

    @Override
    public ExportDropConfig.Type convert(String string) {
        ExportDropConfig.Type type = null;

        if (string != null) {
            try {
                type = ExportDropConfig.Type.valueOf(string.toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new ParameterException("Invalid type [" + string + "]");
            }
        }

        return type;
    }

}

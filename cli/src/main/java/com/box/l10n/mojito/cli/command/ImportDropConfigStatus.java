package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.box.l10n.mojito.rest.entity.ImportDropConfig;

/**
 *
 * @author jaurambault
 */
public class ImportDropConfigStatus implements IStringConverter<ImportDropConfig.Status> {

    @Override
    public ImportDropConfig.Status convert(String string) {

        ImportDropConfig.Status status = null;

        if (string != null) {
            try {
                status = ImportDropConfig.Status.valueOf(string.toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new ParameterException("Invalid import status");
            }
        }

        return status;
    }

}

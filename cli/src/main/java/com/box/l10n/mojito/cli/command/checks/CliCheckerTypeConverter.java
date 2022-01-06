package com.box.l10n.mojito.cli.command.checks;

import com.beust.jcommander.IStringConverter;
import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.EnumConverter;

public class CliCheckerTypeConverter extends EnumConverter<CliCheckerType> {

    @Override
    protected Class<CliCheckerType> getGenericClass() {
        return CliCheckerType.class;
    }

}

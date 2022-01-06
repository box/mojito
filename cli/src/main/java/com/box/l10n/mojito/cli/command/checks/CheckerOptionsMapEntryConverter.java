package com.box.l10n.mojito.cli.command.checks;

import com.beust.jcommander.IStringConverter;
import com.box.l10n.mojito.cli.command.CommandException;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class CheckerOptionsMapEntryConverter implements IStringConverter<CheckerOptionsMapEntry> {

    @Override
    public CheckerOptionsMapEntry convert(String option) {
        if (StringUtils.isNotBlank(option) && option.contains(":")) {
            String[] splitArray = option.split(":", 2);
            String key = splitArray[0];
            if (isInvalidKey(key)) {
                throw new CommandException("Unknown checker option '" + key + "'");
            }
            return new CheckerOptionsMapEntry(key, splitArray[1]);
        } else {
            throw new CommandException("Checker options must be in the form 'option name':'value'");
        }
    }

    private boolean isInvalidKey(String key) {
        return Arrays.stream(CliCheckerParameters.values()).noneMatch(cliCheckerParameters -> cliCheckerParameters.getKey().equals(key));
    }
}

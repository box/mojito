package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import java.util.Arrays;

/**
 *
 * @author jaurambault
 */
public class GitBlameOverrideConverter implements IStringConverter<GitBlameCommand.OverrideType> {

    @Override
    public GitBlameCommand.OverrideType convert(String value) {

        GitBlameCommand.OverrideType overrideType = null;

        if (value != null) {
            try {
                overrideType = GitBlameCommand.OverrideType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException iae) {
                String msg = "Invalid overrideType type [" + value + "], should be one of: " + Arrays.toString(GitBlameCommand.OverrideType.values());
                throw new ParameterException(msg);
            }
        }

        return overrideType;
    }

}

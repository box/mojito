package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Splitter;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jaurambault
 */
public class TmTextUnitMappingConverter implements IStringConverter<Map<Long, Long>> {

    @Override
    public Map<Long, Long> convert(String value) {

        Map<Long, Long> map = null;

        if (value != null) {
            try {
                return Splitter.on(";").withKeyValueSeparator(":").split(value).entrySet().stream().
                        collect(Collectors.toMap(
                                e -> Long.valueOf(e.getKey()),
                                e -> Long.valueOf(e.getValue())));
            } catch (IllegalArgumentException iae) {
                throw new ParameterException("Invalid source to target textunit id mapping [" + value + "]");
            }
        }

        return map;
    }

}


package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import java.util.Arrays;

public abstract class EnumConverter<T extends Enum<T>> implements IStringConverter<T> {

    protected abstract Class<T> getGenericClass();

    @Override
    public final T convert(String value) {

        T result = null;
        if (value != null) {
            try {
                result = Enum.valueOf(getGenericClass(), value.toUpperCase());
            } catch (IllegalArgumentException iae) {
                String msg = "Invalid type [" + value + "], should be one of: " + Arrays.toString(getGenericClass().getEnumConstants());
                throw new ParameterException(msg);
            }
        }

        return result;
    }
}
package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.EnumConverter;
import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;

public class PlaceholderRegularExpressionConverter extends EnumConverter<PlaceholderRegularExpressions> {

    @Override
    protected Class<PlaceholderRegularExpressions> getGenericClass() {
        return PlaceholderRegularExpressions.class;
    }

}

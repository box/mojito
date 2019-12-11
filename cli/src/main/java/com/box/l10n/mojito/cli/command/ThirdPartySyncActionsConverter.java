package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.client.ThirdPartySync;

/**
 *
 * @author sdemyanenko
 */
public class ThirdPartySyncActionsConverter extends EnumConverter<ThirdPartySync.Action> {

    @Override
    protected Class<ThirdPartySync.Action> getGenericClass() {
        return ThirdPartySync.Action.class;
    }
}

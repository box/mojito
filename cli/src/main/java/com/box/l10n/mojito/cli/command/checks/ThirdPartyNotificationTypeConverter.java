package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.EnumConverter;
import com.box.l10n.mojito.notifications.service.ThirdPartyNotificationType;

public class ThirdPartyNotificationTypeConverter extends EnumConverter<ThirdPartyNotificationType> {

    @Override
    protected Class<ThirdPartyNotificationType> getGenericClass() {
        return ThirdPartyNotificationType.class;
    }

}

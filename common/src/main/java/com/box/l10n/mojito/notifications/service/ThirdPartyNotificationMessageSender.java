package com.box.l10n.mojito.notifications.service;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.text.MessageFormat;

public abstract class ThirdPartyNotificationMessageSender {
    
    public abstract void sendMessage(String message, ImmutableMap<String, String> serviceParameters) throws Exception;

}

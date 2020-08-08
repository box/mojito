package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFile;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@ConditionalOnProperty(value = "l10n.ThirdPartyTMS.impl", havingValue = "ThirdPartyTMSSmartling")
@Component
public class SmartlingResultProcessor {

    public void processPush(List<SmartlingFile> files,
                            SmartlingOptions options) {
        // TODO - To be implemented later
    }

    public void processPushTranslations(List<SmartlingFile> files,
                                        SmartlingOptions options){
        // TODO - To be implemented later
    }
}

package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFile;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingOptions;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingResultProcessor;

import java.util.ArrayList;
import java.util.List;

public class StubSmartlingResultProcessor extends SmartlingResultProcessor {

    List<SmartlingFile> pushFiles = new ArrayList<>();
    List<SmartlingFile> pushTranslationFiles = new ArrayList<>();
    SmartlingOptions options;

    public StubSmartlingResultProcessor() {
    }

    @Override
    public String processPush(List<SmartlingFile> files, SmartlingOptions options) {
        this.pushFiles = files;
        this.options = options;
        return "";
    }

    @Override
    public String processPushTranslations(List<SmartlingFile> files, SmartlingOptions options) {
        this.pushTranslationFiles = files;
        this.options = options;
        return "";
    }
}

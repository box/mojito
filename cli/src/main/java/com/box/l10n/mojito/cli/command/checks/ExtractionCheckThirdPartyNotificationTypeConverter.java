package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.EnumConverter;
import com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckThirdPartyNotificationService;

public class ExtractionCheckThirdPartyNotificationTypeConverter
    extends EnumConverter<ExtractionCheckThirdPartyNotificationService> {

  @Override
  protected Class<ExtractionCheckThirdPartyNotificationService> getGenericClass() {
    return ExtractionCheckThirdPartyNotificationService.class;
  }
}

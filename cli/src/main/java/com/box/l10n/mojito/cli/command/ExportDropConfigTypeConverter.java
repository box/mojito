package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.model.ExportDropConfig;

/**
 * @author jaurambault
 */
public class ExportDropConfigTypeConverter extends EnumConverter<ExportDropConfig.TypeEnum> {

  @Override
  protected Class<ExportDropConfig.TypeEnum> getGenericClass() {
    return ExportDropConfig.TypeEnum.class;
  }
}

package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.entity.ExportDropConfig;

/**
 * @author jaurambault
 */
public class ExportDropConfigTypeConverter extends EnumConverter<ExportDropConfig.Type> {

  @Override
  protected Class<ExportDropConfig.Type> getGenericClass() {
    return ExportDropConfig.Type.class;
  }
}

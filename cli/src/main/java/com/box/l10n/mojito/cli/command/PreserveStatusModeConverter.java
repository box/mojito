package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.entity.CopyTmConfig;

public class PreserveStatusModeConverter extends EnumConverter<CopyTmConfig.PreserveStatusMode> {

  @Override
  protected Class<CopyTmConfig.PreserveStatusMode> getGenericClass() {
    return CopyTmConfig.PreserveStatusMode.class;
  }
}

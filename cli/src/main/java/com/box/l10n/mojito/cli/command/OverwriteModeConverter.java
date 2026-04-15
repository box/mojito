package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.entity.CopyTmConfig;

public class OverwriteModeConverter extends EnumConverter<CopyTmConfig.OverwriteMode> {

  @Override
  protected Class<CopyTmConfig.OverwriteMode> getGenericClass() {
    return CopyTmConfig.OverwriteMode.class;
  }
}

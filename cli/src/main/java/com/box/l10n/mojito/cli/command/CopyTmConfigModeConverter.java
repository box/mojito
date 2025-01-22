package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.model.CopyTmConfig;

/**
 * @author jaurambault
 */
public class CopyTmConfigModeConverter extends EnumConverter<CopyTmConfig.ModeEnum> {

  @Override
  protected Class<CopyTmConfig.ModeEnum> getGenericClass() {
    return CopyTmConfig.ModeEnum.class;
  }
}

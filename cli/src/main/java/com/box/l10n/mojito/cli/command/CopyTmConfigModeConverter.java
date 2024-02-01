package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.entity.CopyTmConfig;

/**
 * @author jaurambault
 */
public class CopyTmConfigModeConverter extends EnumConverter<CopyTmConfig.Mode> {

  @Override
  protected Class<CopyTmConfig.Mode> getGenericClass() {
    return CopyTmConfig.Mode.class;
  }
}

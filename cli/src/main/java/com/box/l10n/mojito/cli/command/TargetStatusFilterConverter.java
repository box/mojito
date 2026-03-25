package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.entity.CopyTmConfig;

public class TargetStatusFilterConverter extends EnumConverter<CopyTmConfig.TargetStatusFilter> {

  @Override
  protected Class<CopyTmConfig.TargetStatusFilter> getGenericClass() {
    return CopyTmConfig.TargetStatusFilter.class;
  }
}

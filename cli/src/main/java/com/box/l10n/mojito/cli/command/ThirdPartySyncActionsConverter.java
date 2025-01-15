package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.model.ThirdPartySync;

/**
 * @author sdemyanenko
 */
public class ThirdPartySyncActionsConverter extends EnumConverter<ThirdPartySync.ActionsEnum> {

  @Override
  protected Class<ThirdPartySync.ActionsEnum> getGenericClass() {
    return ThirdPartySync.ActionsEnum.class;
  }
}

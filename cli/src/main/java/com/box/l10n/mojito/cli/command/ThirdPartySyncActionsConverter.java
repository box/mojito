package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.ThirdPartySyncAction;

/** @author sdemyanenko */
public class ThirdPartySyncActionsConverter extends EnumConverter<ThirdPartySyncAction> {

  @Override
  protected Class<ThirdPartySyncAction> getGenericClass() {
    return ThirdPartySyncAction.class;
  }
}

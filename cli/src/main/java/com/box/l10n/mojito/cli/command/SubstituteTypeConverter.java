package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.entity.LocalizedAssetBody;

public class SubstituteTypeConverter extends EnumConverter<LocalizedAssetBody.SubstituteType> {

  @Override
  protected Class<LocalizedAssetBody.SubstituteType> getGenericClass() {
    return LocalizedAssetBody.SubstituteType.class;
  }
}

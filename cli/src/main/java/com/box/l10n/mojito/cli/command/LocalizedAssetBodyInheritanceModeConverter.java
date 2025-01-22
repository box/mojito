package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.model.LocalizedAssetBody;

/**
 * @author dragosv
 */
public class LocalizedAssetBodyInheritanceModeConverter
    extends EnumConverter<LocalizedAssetBody.InheritanceModeEnum> {

  @Override
  protected Class<LocalizedAssetBody.InheritanceModeEnum> getGenericClass() {
    return LocalizedAssetBody.InheritanceModeEnum.class;
  }
}

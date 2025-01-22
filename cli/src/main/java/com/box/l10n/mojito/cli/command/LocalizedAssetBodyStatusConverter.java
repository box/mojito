package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.model.LocalizedAssetBody;

/**
 * @author dragosv
 */
public class LocalizedAssetBodyStatusConverter
    extends EnumConverter<LocalizedAssetBody.StatusEnum> {

  @Override
  protected Class<LocalizedAssetBody.StatusEnum> getGenericClass() {
    return LocalizedAssetBody.StatusEnum.class;
  }
}

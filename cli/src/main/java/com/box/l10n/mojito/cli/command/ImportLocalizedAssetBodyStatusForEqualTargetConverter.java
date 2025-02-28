package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.apiclient.model.ImportLocalizedAssetBody;

/**
 * @author jaurambault
 */
public class ImportLocalizedAssetBodyStatusForEqualTargetConverter
    extends EnumConverter<ImportLocalizedAssetBody.StatusForEqualTargetEnum> {

  @Override
  protected Class<ImportLocalizedAssetBody.StatusForEqualTargetEnum> getGenericClass() {
    return ImportLocalizedAssetBody.StatusForEqualTargetEnum.class;
  }
}

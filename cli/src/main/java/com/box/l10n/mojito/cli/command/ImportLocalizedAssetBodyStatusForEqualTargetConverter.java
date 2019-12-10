package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.entity.ImportLocalizedAssetBody;

/**
 *
 * @author jaurambault
 */
public class ImportLocalizedAssetBodyStatusForEqualTargetConverter extends EnumConverter<ImportLocalizedAssetBody.StatusForEqualTarget> {

    @Override
    protected Class<ImportLocalizedAssetBody.StatusForEqualTarget> getGenericClass() {
        return ImportLocalizedAssetBody.StatusForEqualTarget.class;
    }
}

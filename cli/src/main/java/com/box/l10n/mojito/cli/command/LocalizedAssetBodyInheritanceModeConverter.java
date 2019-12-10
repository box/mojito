package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.entity.LocalizedAssetBody;

/**
 *
 * @author dragosv
 */
public class LocalizedAssetBodyInheritanceModeConverter extends EnumConverter<LocalizedAssetBody.InheritanceMode> {

    @Override
    protected Class<LocalizedAssetBody.InheritanceMode> getGenericClass() {
        return LocalizedAssetBody.InheritanceMode.class;
    }
}

package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.entity.LocalizedAssetBody;

/**
 *
 * @author dragosv
 */
public class LocalizedAssetBodyStatusConverter extends EnumConverter<LocalizedAssetBody.Status> {

    @Override
    protected Class<LocalizedAssetBody.Status> getGenericClass() {
        return LocalizedAssetBody.Status.class;
    }
}

package com.box.l10n.mojito.service.assetTextUnit;

import com.box.l10n.mojito.immutables.JpaDTO;
import org.immutables.value.Value;

@Value.Immutable
@JpaDTO
public abstract class AbstractAssetTextUnitIdToMd5 {
    @Value.Parameter(order = 1)
    public abstract long getId();
    @Value.Parameter(order = 2)
    public abstract String getMd5();
}

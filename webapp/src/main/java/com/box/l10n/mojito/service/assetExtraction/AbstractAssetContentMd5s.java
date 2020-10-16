package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable(singleton = true)
@NoPrefixNoBuiltinContainer
public abstract class AbstractAssetContentMd5s {
    @Nullable
    abstract String getContentMd5();

    @Nullable
    abstract String getFilterOptionsMd5();
}

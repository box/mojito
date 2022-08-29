package com.box.l10n.mojito.immutables;

import org.immutables.value.Value;

@Value.Style(
    allParameters = true,
    of = "new",
    defaults = @Value.Immutable(builder = false),
    // Avoid annoying auto-complete and useless copies when working with Immutable collection
    // Could be nice to have for test syntax
    builtinContainerAttributes = false,
    // No prefix or suffix for generated immutable type
    typeImmutable = "*")
public @interface JpaDTO {}

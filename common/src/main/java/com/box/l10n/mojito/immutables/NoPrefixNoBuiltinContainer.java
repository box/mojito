package com.box.l10n.mojito.immutables;

import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@Value.Style(
        // Avoid annoying auto-complete and useless copies when working with Immutable collection
        // Could be nice to have for test syntax
        builtinContainerAttributes = false,
        // No prefix or suffix for generated immutable type
        typeImmutable = "*"
)
public @interface NoPrefixNoBuiltinContainer {
}

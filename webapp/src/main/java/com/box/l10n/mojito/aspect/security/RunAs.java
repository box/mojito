package com.box.l10n.mojito.aspect.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to replace the current authenticated session with another
 * {@link com.box.l10n.mojito.entity.security.user.User}given its username.  For example, this is useful
 * for scheduled jobs.  The aspect will place the original authenticated user back.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RunAs {
    String username() default "";
}

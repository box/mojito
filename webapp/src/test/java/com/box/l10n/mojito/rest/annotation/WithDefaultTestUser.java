package com.box.l10n.mojito.rest.annotation;

import com.box.l10n.mojito.rest.WithDefaultTestUserSecurityContextFactory;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/** @author wyau */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = WithDefaultTestUserSecurityContextFactory.class)
public @interface WithDefaultTestUser {}

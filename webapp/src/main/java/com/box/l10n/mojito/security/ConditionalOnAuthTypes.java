package com.box.l10n.mojito.security;

import java.lang.annotation.*;
import org.springframework.context.annotation.Conditional;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(AuthTypesCondition.class)
public @interface ConditionalOnAuthTypes {
  SecurityConfig.AuthenticationType[] anyOf() default {};

  boolean matchIfMissing() default false;
}

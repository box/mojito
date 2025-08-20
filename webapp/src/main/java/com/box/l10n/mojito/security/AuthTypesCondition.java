package com.box.l10n.mojito.security;

import com.box.l10n.mojito.security.SecurityConfig.AuthenticationType;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class AuthTypesCondition implements Condition {

  private static final String KEY = "l10n.security.authenticationType";

  @Override
  public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata md) {
    String raw = ctx.getEnvironment().getProperty(KEY, "");
    Set<String> types =
        Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toUpperCase)
            .collect(Collectors.toSet());

    var attrs = md.getAnnotationAttributes(ConditionalOnAuthTypes.class.getName());
    AuthenticationType[] required = (AuthenticationType[]) attrs.get("anyOf");
    boolean matchIfMissing = (boolean) attrs.get("matchIfMissing");

    if (types.isEmpty()) {
      return matchIfMissing;
    }
    return Arrays.stream(required).map(Enum::name).anyMatch(types::contains);
  }
}

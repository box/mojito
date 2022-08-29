package com.box.l10n.mojito.aspect.security;

import com.box.l10n.mojito.aspect.JsonRawString;
import com.box.l10n.mojito.security.UserDetailsImpl;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Aspect that implements behavior describe in {@link JsonRawString}.
 *
 * @author wyau
 */
@Aspect
public class RunAsAspect {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RunAsAspect.class);

  @Autowired UserDetailsService userDetailsService;

  @Around("methods()")
  public Object swapSecurityContext(ProceedingJoinPoint pjp) throws Throwable {
    Object result;

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    try {
      logger.debug("Swapping security authentiation");
      String usernameFromAnnotation = getUsernameFromAnnotation(pjp);
      setAuthenticationToUser(usernameFromAnnotation);
      result = pjp.proceed();
    } finally {
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    return result;
  }

  private String getUsernameFromAnnotation(ProceedingJoinPoint pjp) {
    MethodSignature ms = (MethodSignature) pjp.getSignature();
    Method m = ms.getMethod();
    RunAs runAs = m.getAnnotation(RunAs.class);

    return runAs.username();
  }

  @Pointcut("execution(@RunAs * *(..))")
  private void methods() {}

  protected void setAuthenticationToUser(String username) {
    logger.debug("Setting authention as user: {}", username);
    UserDetailsImpl user = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);
    SecurityContext securityContext = SecurityContextHolder.getContext();
    securityContext.setAuthentication(
        new UsernamePasswordAuthenticationToken(user, "", new ArrayList<GrantedAuthority>()));
  }
}

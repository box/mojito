package com.box.l10n.mojito.aspect;

import com.box.l10n.mojito.json.JsonValidator;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Aspect that implements behavior describe in {@link JsonRawString}.
 *
 * @author jaurambault
 */
@Aspect
public class JsonRawStringAspect {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(JsonRawStringAspect.class);

  @Autowired JsonValidator jsonValidator;

  @Around("methods()")
  public Object convertReturnedStringToRawJSON(ProceedingJoinPoint pjp) throws Throwable {

    checkMethodHasJsonRawValueAnnotation(pjp);

    String res = (String) pjp.proceed();

    if (!jsonValidator.isValidJsonString(res)) {
      JsonStringEncoder jsonStringEncoder = new JsonStringEncoder();
      StringBuilder sb = new StringBuilder();
      sb.append("\"").append(jsonStringEncoder.quoteAsString(res)).append("\"");
      res = sb.toString();
    }

    return res;
  }

  /**
   * TODO(P2) try to do this with aspect @DeclareError
   *
   * <p>Checks that the method annotated with {@link JsonRawString} also has the {@link
   * JsonRawValue} for Jackson to actually serialize as expected.
   *
   * @param pjp
   */
  private void checkMethodHasJsonRawValueAnnotation(ProceedingJoinPoint pjp) {

    MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
    Method targetMethod = methodSignature.getMethod();

    if (targetMethod.getAnnotation(JsonRawValue.class) == null) {
      throw new RuntimeException(
          "The method annotated with @JsonRawString must also be annotated with @JsonRawValue");
    }
  }

  @Pointcut("execution(@com.box.l10n.mojito.aspect.JsonRawString String *(..))")
  private void methods() {}
}

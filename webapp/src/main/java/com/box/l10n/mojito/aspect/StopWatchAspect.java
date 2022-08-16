package com.box.l10n.mojito.aspect;

import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.google.common.base.Stopwatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.lang.reflect.Method;

/**
 * Simple aspect to log time spent in a function.
 * 
 * @author jeanaurambault
 */
@Aspect
public class StopWatchAspect {
    
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(StopWatchAspect.class);

    @Around("methods()")
    public Object mesureTime(ProceedingJoinPoint pjp) throws Throwable {

        Stopwatch stopwatch = Stopwatch.createStarted();
        
        Object res = pjp.proceed();

        stopwatch.stop();
       
        log(getLevel(pjp), "{}#{} took: {}", pjp.getSignature().getDeclaringTypeName(), pjp.getSignature().getName(), stopwatch.toString());

        return res;
    }

    private Level getLevel(ProceedingJoinPoint pjp) {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method m = ms.getMethod();
        StopWatch annotation = m.getAnnotation(StopWatch.class);
        return annotation.level();
    }

    private void log(Level level, String format, Object... args) {
        switch (level) {
            case TRACE:
                logger.trace(format, args);
                break;
            case DEBUG:
                logger.debug(format, args);
                break;
            case INFO:
                logger.info(format, args);
                break;
            case WARN:
                logger.warn(format, args);
                break;
            case ERROR:
                logger.error(format, args);
                break;
        }
    }
    
    @Pointcut("execution(@com.box.l10n.mojito.aspect.StopWatch * *(..))")
    private void methods() {
    }
}

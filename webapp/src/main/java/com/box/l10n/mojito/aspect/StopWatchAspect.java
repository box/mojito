package com.box.l10n.mojito.aspect;

import com.google.common.base.Stopwatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
       
        logger.debug("{} took: {}", pjp.getSignature().getName(), stopwatch.toString());
        
        return res;
    }
    
    @Pointcut("execution(@com.box.l10n.mojito.aspect.StopWatch * *(..))")
    private void methods() {
    }
}

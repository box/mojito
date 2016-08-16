package com.box.l10n.mojito.aspect.util;

import com.box.l10n.mojito.service.pollableTask.AnnotatedMethodParam;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Test;
import org.mockito.Mockito;
import java.util.Iterator;
import java.util.List;
import static org.junit.Assert.assertEquals;

/**
 * @author jaurambault
 */
public class AspectJUtilsTest {

    AspectJUtils aspectJUtils = new AspectJUtils();

    @Test
    public void testFindAnnotatedMethodParam() throws NoSuchMethodException {
        AnnotatedMethodParam<ForTest> findAnnotatedMethodParam = aspectJUtils.findAnnotatedMethodParam(getJoinPointMock(), ForTest.class);
        assertEquals("nameForTest", findAnnotatedMethodParam.getAnnotation().name());
        assertEquals("stringForTest", findAnnotatedMethodParam.getArg());
    }

    @Test
    public void testFindAnnotatedMethodParams() throws NoSuchMethodException {
        List<AnnotatedMethodParam<ForTest>> findAnnotatedMethodParams = aspectJUtils.findAnnotatedMethodParams(getJoinPointMock(), ForTest.class);
        Iterator<AnnotatedMethodParam<ForTest>> iterator = findAnnotatedMethodParams.iterator();
        AnnotatedMethodParam<ForTest> next = iterator.next();
        assertEquals("nameForTest", next.getAnnotation().name());
        assertEquals("stringForTest", next.getArg());

        next = iterator.next();
        assertEquals("name2ForTest", next.getAnnotation().name());
        assertEquals(1L, next.getArg());
    }

    private ProceedingJoinPoint getJoinPointMock() throws NoSuchMethodException {
        MethodSignature signature = Mockito.mock(MethodSignature.class);
        Mockito.when(signature.getMethod()).thenReturn(this.getClass().getMethod("annotatedFunction", String.class, Object.class, Long.class));

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{"stringForTest", null, 1L});
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);

        return joinPoint;
    }

    public void annotatedFunction(@ForTest(name = "nameForTest") String s, Object skipped, @ForTest(name = "name2ForTest") Long l) {

    }
}

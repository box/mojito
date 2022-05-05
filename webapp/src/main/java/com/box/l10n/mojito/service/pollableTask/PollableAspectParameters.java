package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.aspect.util.AspectJUtils;
import com.box.l10n.mojito.entity.PollableTask;
import com.ibm.icu.text.MessageFormat;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains parameters associated with {@link PollableAspect}. Parameters come
 * from the {@link Pollable} annotation and the annotated arguments of the
 * wrapped function.
 *
 * @author jaurambault
 */
@Configurable
public class PollableAspectParameters {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PollableAspectParameters.class);

    public final static Long DEFAULT_TIMEOUT = -1L;

    @Autowired
    AspectJUtils aspectJUtils;

    Pollable annotation = null;
    String name = null;
    String message = null;
    int expectedSubTaskNumber = 0;
    Long parentId;
    boolean async = false;

    Long timeout = DEFAULT_TIMEOUT;

    ProceedingJoinPoint pjp;

    public PollableAspectParameters(ProceedingJoinPoint pjp) {
        this.pjp = pjp;
    }

    @PostConstruct
    public void postContruct() {
        initFromAnnotation();
        setParentIdFromParameter();
    }

    /**
     * Init this instance with information contained in the {@link Pollable}
     * annotation.
     */
    private void initFromAnnotation() {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method m = ms.getMethod();
        annotation = m.getAnnotation(Pollable.class);

        if (!annotation.name().isEmpty()) {
            name = annotation.name();
        } else {
            name = pjp.getSignature().getName();
        }

        if (!annotation.message().isEmpty()) {
            message = MessageFormat.format(annotation.message(), getMessageParams());
        }

        expectedSubTaskNumber = annotation.expectedSubTaskNumber();
        async = annotation.async();
        timeout = annotation.timeout();
    }

    /**
     * Gets message parameters by looking at the annotation and the function
     * parameters annotated with {@link MsgArg}.
     *
     * @return a map of message parameters, key is the parameter name and the
     * value the argument value.
     */
    private Map<String, Object> getMessageParams() {

        Map<String, Object> params = getMessageParamsFromAnnotation();

        List<AnnotatedMethodParam<MsgArg>> annotatedMethodParams = aspectJUtils.findAnnotatedMethodParams(pjp, MsgArg.class);

        for (AnnotatedMethodParam<MsgArg> annotatedMethodParam : annotatedMethodParams) {
            params.put(annotatedMethodParam.getAnnotation().name(), getMsgArgValue(annotatedMethodParam.getArg(), annotatedMethodParam.getAnnotation().accessor()));
        }

        return params;
    }

    /**
     * Gets message parameters from the annotation
     *
     * @return a map of message parameters, key is the parameter name and the
     * value the argument value.
     */
    private HashMap<String, Object> getMessageParamsFromAnnotation() {
        HashMap<String, Object> hashMap = new HashMap<>();

        for (MsgArg msgArg : annotation.msgArgs()) {
            hashMap.put(msgArg.name(), getMsgArgValue(pjp.getThis(), msgArg.accessor()));
        }

        return hashMap;
    }

    /**
     * Gets MsgArg value from target object using the accessor if specified.
     *
     * @param target the object from which the value should be extracted
     * @param accessor a method name to get
     * @return the target itself if no accessor is specified else the value
     * returned by the accessor.
     */
    private Object getMsgArgValue(Object target, String accessor) {
        Object res = null;

        if (target == null || "".equals(accessor)) {
            res = target;
        } else {
            try {
                Method method = target.getClass().getMethod(accessor, (Class<?>[]) null);
                res = method.invoke(target);
            } catch (NoSuchMethodException nsme) {
                String msg = "@MsgArg must be set on an Object that has a public function: " + accessor;
                logger.error(msg);
                throw new IllegalPollableAnnotationException(message, nsme);
            } catch (IllegalAccessException | InvocationTargetException e) {
                String msg = "Unexpected error trying to proces @MsgArg with access: " + accessor;
                logger.error(msg);
                throw new IllegalPollableAnnotationException(message, e);
            }
        }

        return res;
    }

    /**
     * Sets the the id of the parent task by looking at the annotated parameter
     * with {@link ParentTask}
     */
    private void setParentIdFromParameter() throws RuntimeException {

        AnnotatedMethodParam<ParentTask> findAnnotatedMethodParam = aspectJUtils.findAnnotatedMethodParam(pjp, ParentTask.class);

        if (findAnnotatedMethodParam != null) {
            Object o = findAnnotatedMethodParam.getArg();

            if (o instanceof Long) {
                parentId = (Long) o;
            } else if (o instanceof PollableTask) {
                parentId = ((PollableTask) o).getId();
            } else if (o != null) {
                throw new IllegalPollableAnnotationException("@ParentTask must be placed on a Long or Pollable param (this should be prevented by Aspectj checks)");
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getExpectedSubTaskNumber() {
        return expectedSubTaskNumber;
    }

    public void setExpectedSubTaskNumber(int expectedSubTaskNumber) {
        this.expectedSubTaskNumber = expectedSubTaskNumber;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }
}

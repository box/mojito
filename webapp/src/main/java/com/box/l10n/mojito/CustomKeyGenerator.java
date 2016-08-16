package com.box.l10n.mojito;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import java.lang.reflect.Method;

/**
 * @author aloison
 */
@Configurable
public class CustomKeyGenerator extends SimpleKeyGenerator {

    /**
     * Generates a key, taking into account the method and its params
     */
    @Override
    public Object generate(Object target, Method method, Object... params) {
        return generateKey(method, params);
    }
}

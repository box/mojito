package com.box.l10n.mojito.aspect;

import com.fasterxml.jackson.annotation.JsonRawValue;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to extend the behavior of {@link JsonRawValue}.
 *
 * <p>If {@link JsonRawValue} annotation is set then {@link JsonRawValue} must be set on the method
 * too else it will throw an exception.
 *
 * <p>It allows to return either a raw JSON string if the annotated method returns a valid JSON
 * representation or to encode the returned value as a String that can be use as raw String in JSON.
 *
 * <p>This is to prevent creating invalid JSON with Jackson serialization when the annotated method
 * doesn't return proper raw JSON.
 *
 * <p>One usage is to allow storage of semi-structure data in Hibernate and to get it serialize
 * safely by Jackson.
 *
 * @author jaurambault
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JsonRawString {}

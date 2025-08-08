/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker annotation (an analogue of {@code <int:converter/>}) to register
 * {@link org.springframework.core.convert.converter.Converter},
 * {@link org.springframework.core.convert.converter.GenericConverter} or
 * {@link org.springframework.core.convert.converter.ConverterFactory} beans for the {@code integrationConversionService}.
 * <p>
 * This annotation can be used at the {@code class} level for {@link org.springframework.stereotype.Component} beans
 * and on methods with {@link org.springframework.context.annotation.Bean}.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IntegrationConverter {

}

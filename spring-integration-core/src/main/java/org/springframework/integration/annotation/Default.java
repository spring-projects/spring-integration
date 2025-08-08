/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the class member has some default meaning.
 * <p>
 * The target parsing logic may vary.
 * One of the use-cases is service with several methods which are
 * selected for invocation at runtime by some condition.
 * The method with this {@link Default} annotation may mean
 * a fallback option when no one other method meets condition.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Default {

}

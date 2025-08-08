/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.dsl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import kotlin.DslMarker;

/**
 * The Kotlin {@link DslMarker} annotation for classes used in scope of DSL, including all the Java DSL classes.
 *
 * @author Artem Bilan
 *
 * @since 5.5.8
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Documented
@DslMarker
public @interface IntegrationDsl {

}

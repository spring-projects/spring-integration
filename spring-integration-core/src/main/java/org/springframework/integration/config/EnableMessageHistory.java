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

import org.springframework.context.annotation.Import;

/**
 * Enables {@link org.springframework.integration.history.MessageHistory}
 * for Spring Integration components.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MessageHistoryRegistrar.class)
public @interface EnableMessageHistory {

	/**
	 * The list of component name patterns to track (e.g. {@code "inputChannel", "out*", "*Channel", "*Service"}).
	 * By default all Spring Integration components are tracked.
	 * @return the list of component name patterns to track
	 */
	String[] value() default "*";

}

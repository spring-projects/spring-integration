/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides the message header {@code value} or {@code expression}.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface GatewayHeader {

	/**
	 * @return The name of the header.
	 */
	String name();

	/**
	 * @return The value for the header.
	 */
	String value() default "";

	/**
	 * @return The {@code Expression} to be evaluated to produce a value for the header.
	 */
	String expression() default "";

}

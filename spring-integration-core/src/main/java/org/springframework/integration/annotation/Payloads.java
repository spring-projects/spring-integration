/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks a method parameter as being a list of message payloads, for POJO handlers that deal with lists
 * of messages (e.g. aggregators and release strategies).
 * <p>
 * Example:
 * {@code void foo(@Payloads("city.name") List<String> cityName)} - will map the value of the 'name' property of the 'city'
 * property of all the payload objects in the input list.
 *
 * @author Dave Syer
 * @since 2.0
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Payloads {

	/**
	 * @return The expression for matching against nested properties of the payloads.
	 */
	String value() default "";

}

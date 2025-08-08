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
 * {@link org.springframework.messaging.support.ChannelInterceptor} components with this
 * annotation will be applied as global channel interceptors
 * using the provided {@code patterns} to match channel names.
 * <p>
 * This annotation can be used at the {@code class} level
 * for {@link org.springframework.stereotype.Component} beans
 * and on methods with {@link org.springframework.context.annotation.Bean}.
 * <p>
 * This annotation is an analogue of {@code <int:channel-interceptor/>}.
 *
 * @author Artem Bilan
 * @author Meherzad Lahewala
 *
 * @since 4.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlobalChannelInterceptor {

	/**
	 * An array of patterns against which channel names will be matched.
	 * Since version 5.0 negative patterns are also supported.
	 * A leading '!' negates the pattern match.
	 * Default is "*" (all channels).
	 * @return The pattern.
	 * @see org.springframework.integration.support.utils.PatternMatchUtils#smartMatch(String, String...)
	 */
	String[] patterns() default "*";

	/**
	 * The order of the interceptor. Interceptors with negative order values will be placed before any
	 * explicit interceptors on the channel; interceptors with positive order values will be
	 * placed after explicit interceptors.
	 * @return The order.
	 */
	int order() default 0;

}

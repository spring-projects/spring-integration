/*
 * Copyright © 2021 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2021-present the original author or authors.
 */

package org.springframework.integration.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides reactive configuration options for the consumer endpoint making
 * any input channel as a reactive stream source of data.
 *
 * @author Artem Bilan
 *
 * @since 5.5
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Reactive {

	/**
	 * @return the function bean name to be used in the
	 * {@link reactor.core.publisher.Flux#transform} on the input channel
	 * {@link reactor.core.publisher.Flux}.
	 */
	String value() default "";

}

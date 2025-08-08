/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.messaging.handler.annotation.ValueConstants;

/**
 * Messaging Annotation to mark a {@link org.springframework.context.annotation.Bean}
 * method for a {@link org.springframework.messaging.MessageChannel} to produce a
 * {@link org.springframework.integration.handler.BridgeHandler} and Consumer Endpoint.
 * <p>
 * The {@code inputChannel} for the {@link org.springframework.integration.endpoint.AbstractEndpoint}
 * is the {@link #value()} of this annotation and determines the type of endpoint -
 * {@link org.springframework.integration.endpoint.EventDrivenConsumer} or
 * {@link org.springframework.integration.endpoint.PollingConsumer}.
 * <p>
 * The {@link org.springframework.messaging.MessageChannel} {@link org.springframework.context.annotation.Bean}
 * is used as the {@code outputChannel} of the {@link org.springframework.integration.handler.BridgeHandler}.
 *
 * @author Artem Bilan
 * @author Chris Bono
 *
 * @since 4.0
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(BridgeFromRepeatable.class)
public @interface BridgeFrom {

	/**
	 * @return the inbound channel name to receive message for the
	 * {@link org.springframework.integration.handler.BridgeHandler}
	 */
	String value();

	/*
	 {@code SmartLifecycle} options.
	 Can be specified as 'property placeholder', e.g. {@code ${foo.autoStartup}}.
	 */
	String autoStartup() default "true";

	/**
	 * Specify a {@link org.springframework.context.SmartLifecycle} {@code phase} option.
	 * Defaults {@code Integer.MAX_VALUE / 2} for {@link org.springframework.integration.endpoint.PollingConsumer}
	 * and {@code Integer.MIN_VALUE} for {@link org.springframework.integration.endpoint.EventDrivenConsumer}.
	 * Can be specified as 'property placeholder', e.g. {@code ${foo.phase}}.
	 * @return the {@code SmartLifecycle} phase.
	 */
	String phase() default "";

	/**
	 * @return the {@link Poller} options for a polled endpoint
	 * ({@link org.springframework.integration.scheduling.PollerMetadata}).
	 * Mutually exclusive with {@link #reactive()}.
	 */
	Poller poller() default @Poller(ValueConstants.DEFAULT_NONE);

	/**
	 * @return the {@link Reactive} marker for a consumer endpoint.
	 * Mutually exclusive with {@link #poller()}.
	 * @since 5.5
	 */
	Reactive reactive() default @Reactive(ValueConstants.DEFAULT_NONE);

}

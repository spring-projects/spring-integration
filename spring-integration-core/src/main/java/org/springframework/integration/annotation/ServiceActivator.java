/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Indicates that a method is capable of handling a message or message payload.
 * <p>
 * A method annotated with @ServiceActivator may accept a parameter of type
 * {@link org.springframework.messaging.Message} or of the expected
 * Message payload's type. Any type conversion supported by
 * {@link org.springframework.beans.SimpleTypeConverter} will be applied to
 * the Message payload if necessary. Header values can also be passed as
 * Message parameters by using the
 * {@link org.springframework.messaging.handler.annotation.Header @Header} parameter annotation.
 * <p>
 * Return values from the annotated method may be of any type. If the return
 * value is not a Message, a reply Message will be created with that object
 * as its payload.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Yilin Wei
 * @author Chris Bono
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ServiceActivators.class)
public @interface ServiceActivator {

	/**
	 * Specify the channel from which this service activator will consume messages.
	 * If the channel does not exist, a {@code DirectChannel} with this name will be
	 * registered in the application context.
	 * @return The channel name.
	 */
	String inputChannel() default "";

	/**
	 * Specify the channel to which this service activator will send any replies.
	 * @return The channel name.
	 */
	String outputChannel() default "";

	/**
	 * Specify whether the service method must return a non-null value. This value is
	 * {@code false} by default, but if set to {@code true}, a
	 * {@link org.springframework.integration.handler.ReplyRequiredException} will is thrown when
	 * the underlying service method (or expression) returns a null value.
	 * Can be specified as 'property placeholder', e.g. {@code ${spring.integration.requiresReply}}.
	 * @return the requires reply flag.
	 */
	String requiresReply() default "";

	/**
	 * Specify a "chain" of {@code Advice} beans that will "wrap" the message handler.
	 * Only the handler is advised, not the downstream flow.
	 * @return the advice chain.
	 */
	String[] adviceChain() default {};

	/**
	 * Specify the maximum amount of time in milliseconds to wait when sending a reply
	 * {@link org.springframework.messaging.Message} to the {@code outputChannel}.
	 * Defaults to {@code 30} seconds.
	 * It is applied only if the output channel has some 'sending' limitations, e.g.
	 * {@link org.springframework.integration.channel.QueueChannel} with
	 * fixed a 'capacity'. In this case a {@link org.springframework.messaging.MessageDeliveryException} is thrown.
	 * The 'sendTimeout' is ignored in case of
	 * {@link org.springframework.integration.channel.AbstractSubscribableChannel} implementations.
	 * Can be specified as 'property placeholder', e.g. {@code ${spring.integration.sendTimeout}}.
	 * @return The timeout for sending results to the reply target (in milliseconds)
	 */
	String sendTimeout() default "";

	/**
	 * The {@link org.springframework.context.SmartLifecycle} {@code autoStartup} option.
	 * Can be specified as 'property placeholder', e.g. {@code ${foo.autoStartup}}.
	 * Defaults to {@code true}.
	 * @return the auto startup {@code boolean} flag.
	 */
	String autoStartup() default "";

	/**
	 * Specify a {@link org.springframework.context.SmartLifecycle} {@code phase} option.
	 * Defaults {@code Integer.MAX_VALUE / 2} for {@link org.springframework.integration.endpoint.PollingConsumer}
	 * and {@code Integer.MIN_VALUE} for {@link org.springframework.integration.endpoint.EventDrivenConsumer}.
	 * Can be specified as 'property placeholder', e.g. {@code ${foo.phase}}.
	 * @return the {@code SmartLifecycle} phase.
	 */
	String phase() default "";

	/**
	 * Specify whether the service method is async.
	 * This value is {@code false} by default.
	 * @return the async flag.
	 */
	String async() default "";

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

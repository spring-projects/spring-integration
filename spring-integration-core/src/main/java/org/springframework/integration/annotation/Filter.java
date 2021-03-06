/*
 * Copyright 2002-2021 the original author or authors.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.messaging.handler.annotation.ValueConstants;

/**
 * Indicates that a method is capable of playing the role of a Message Filter.
 * <p>
 * A method annotated with @Filter may accept a parameter of type
 * {@link org.springframework.messaging.Message} or of the expected
 * Message payload's type. Any type conversion supported by default or any
 * Converters registered with the "integrationConversionService" bean will be
 * applied to the Message payload if necessary. Header values can also be passed
 * as Message parameters by using the
 * {@link org.springframework.messaging.handler.annotation.Header @Header} parameter annotation.
 * <p>
 * The return type of the annotated method must be a boolean (or Boolean).
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Filter {

	/**
	 * Specify the channel from which this filter will consume messages.
	 * If the channel does not exist, a {@code DirectChannel} with this name will be
	 * registered in the application context.
	 * @return The channel name.
	 */
	String inputChannel() default "";

	/**
	 * Specify the channel to which this filter will send messages that pass the
	 * selector.
	 * @return The channel name.
	 */
	String outputChannel() default "";

	/**
	 * Specify the channel to which this filter will send messages that do no pass the
	 * selector.
	 * @return The channel name.
	 */
	String discardChannel() default "";

	/**
	 * Throw an exception if the filter rejects the message.
	 * Defaults to {@code false}.
	 * Can be specified as 'property placeholder', e.g. {@code ${spring.integration.throwExceptionOnRejection}}.
	 * @return the throw Exception on rejection flag.
	 */
	String throwExceptionOnRejection() default "";

	/**
	 * Specify a "chain" of {@code Advice} beans that will "wrap" the message handler.
	 * Only the handler is advised, not the downstream flow.
	 * @return the advice chain.
	 */
	String[] adviceChain() default { };

	/**
	 * When {@code true} (default) any discard action (and exception thrown) will occur
	 * within the scope of the advice class(es) in the chain. Otherwise, these actions
	 * will occur after the advice chain returns.
	 * Can be specified as 'property placeholder', e.g. {@code ${spring.integration.discardWithinAdvice}}.
	 * @return the discard within advice flag.
	 */
	String discardWithinAdvice() default "";

	/**
	 * Specify the maximum amount of time in milliseconds to wait when sending a reply
	 * {@link org.springframework.messaging.Message} to the {@link #outputChannel()}.
	 * Defaults to {@code -1} - blocking indefinitely.
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
	 * @return the {@link Poller} options for a polled endpoint
	 * ({@link org.springframework.integration.scheduling.PollerMetadata}).
	 * This attribute is an {@code array} just to allow an empty default (no poller).
	 * Mutually exclusive with {@link #reactive()}.
	 */
	Poller[] poller() default { };

	/**
	 * @return the {@link Reactive} marker for a consumer endpoint.
	 * Mutually exclusive with {@link #poller()}.
	 * @since 5.5
	 */
	Reactive reactive() default @Reactive(ValueConstants.DEFAULT_NONE);

}

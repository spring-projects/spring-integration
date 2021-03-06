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
 * Indicates that a method is capable of resolving to a channel or channel name
 * based on a message, message header(s), or both.
 * <p>
 * A method annotated with @Router may accept a parameter of type
 * {@link org.springframework.messaging.Message} or of the expected
 * Message payload's type. Any type conversion supported by
 * {@link org.springframework.beans.SimpleTypeConverter} will be applied to
 * the Message payload if necessary. Header values can also be passed as
 * Message parameters by using the
 * {@link org.springframework.messaging.handler.annotation.Header @Header} parameter annotation.
 * <p>
 * Return values from the annotated method may be either a Collection or Array
 * whose elements are either
 * {@link org.springframework.messaging.MessageChannel channels} or
 * Strings. In the latter case, the endpoint hosting this router will attempt
 * to resolve each channel name with the Channel Registry or with
 * {@link #channelMappings()}, if provided.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Router {

	/**
	 * Specify the channel from which this router will consume messages.
	 * If the channel does not exist, a {@code DirectChannel} with this name will be
	 * registered in the application context.
	 * @return The channel name.
	 */
	String inputChannel() default "";

	/**
	 * Specify the channel to which this router will send messages for which destination
	 * channels are not resolved and {@link #resolutionRequired()} is false.
	 * @return The channel name.
	 */
	String defaultOutputChannel() default "";

	/**
	 * The 'key=value' pairs to represent channelMapping entries
	 * @return the channelMappings.
	 * @see org.springframework.integration.router.AbstractMappingMessageRouter#setChannelMapping(String, String)
	 */
	String[] channelMappings() default { };

	/**
	 * Specify a prefix to be added to each channel name prior to resolution.
	 * @return the prefix.
	 */
	String prefix() default "";

	/**
	 * Specify a suffix to be added to each channel name prior to resolution.
	 * @return the suffix.
	 */
	String suffix() default "";

	/**
	 * Specify whether channel names must always be successfully resolved
	 * to existing channel instances.
	 * <p> If set to {@code true} (default), a {@link org.springframework.messaging.MessagingException}
	 * will be raised in case the channel cannot be resolved. Setting this attribute to {@code false},
	 * will cause any unresolvable channels to be ignored.
	 * Can be specified as 'property placeholder', e.g. {@code ${spring.integration.resolutionRequired}}.
	 * @return the resolution required flag.
	 */
	String resolutionRequired() default "";

	/**
	 * Specify whether sequence number and size headers should be added to each
	 * Message. Defaults to {@code false}.
	 * Can be specified as 'property placeholder', e.g. {@code ${spring.integration.applySequence}}.
	 * @return the apply sequence flag.
	 */
	String applySequence() default "";

	/**
	 * If set to {@code true} , failures to send to a message channel will
	 * be ignored. If set to {@code false} (default), a {@link org.springframework.messaging.MessageDeliveryException}
	 * will be thrown instead, and if the router resolves more than one channel,
	 * any subsequent channels will not receive the message.
	 * Please be aware that when using direct channels (single threaded),
	 * send-failures can be caused by exceptions thrown by components
	 * much further down-stream.
	 * Can be specified as 'property placeholder', e.g. {@code ${spring.integration.ignoreSendFailures}}.
	 * @return the ignore send failures flag.
	 */
	String ignoreSendFailures() default "";

	/**
	 * Specify the maximum amount of time in milliseconds to wait when sending a reply
	 * {@link org.springframework.messaging.Message} to the {@code outputChannel}.
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

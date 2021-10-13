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
 * Indicates that a method is capable of aggregating messages.
 * <p>
 * A method annotated with @Aggregator may accept a collection
 * of Messages or Message payloads and should return a single
 * Message or a single Object to be used as a Message payload.
 *
 * @author Marius Bogoevici
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Aggregator {

	/**
	 * @return The channel name for receiving messages to be aggregated
	 */
	String inputChannel() default "";

	/**
	 * @return The channel name for sending aggregated result messages
	 */
	String outputChannel() default "";

	/**
	 * @return The channel name for sending discarded messages (due to a timeout)
	 */
	String discardChannel() default "";

	/**
	 * Specify the maximum amount of time in milliseconds to wait when sending a reply
	 * {@link org.springframework.messaging.Message} to the {@link #outputChannel()}.
	 * Defaults to {@code -1} - blocking indefinitely.
	 * It is applied only if the output channel has some 'sending' limitations, e.g.
	 * {@link org.springframework.integration.channel.QueueChannel} with
	 * a fixed 'capacity' and is currently full.
	 * In this case a {@link org.springframework.messaging.MessageDeliveryException} is thrown.
	 * The 'sendTimeout' is ignored in case of
	 * {@link org.springframework.integration.channel.AbstractSubscribableChannel} implementations.
	 * Can be specified as 'property placeholder', e.g. {@code ${spring.integration.sendTimeout}}.
	 * @return The timeout for sending results to the reply target (in milliseconds)
	 */
	String sendTimeout() default "";

	/**
	 * Specify whether messages that expired should be aggregated and sent to the {@link #outputChannel()}
	 * or {@code replyChannel} from message headers. Messages are expired when their containing
	 * {@link org.springframework.integration.store.MessageGroup} expires. One of the ways of expiring MessageGroups
	 * is by configuring a {@link org.springframework.integration.store.MessageGroupStoreReaper}.
	 * However MessageGroups can alternatively be expired by simply calling
	 * {@code MessageGroupStore.expireMessageGroup(groupId)}. That could be accomplished via a ControlBus operation
	 * or by simply invoking that method if you have a reference to the
	 * {@link org.springframework.integration.store.MessageGroupStore} instance.
	 * Defaults to {@code false}.
	 * * Can be specified as 'property placeholder', e.g. {@code ${spring.integration.sendPartialResultsOnExpiry}}.
	 * @return Indicates whether to send an incomplete aggregate on expiry of the message group
	 */
	String sendPartialResultsOnExpiry() default "";

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

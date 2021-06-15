/*
 * Copyright 2014-2019 the original author or authors.
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

import org.springframework.core.annotation.AliasFor;

/**
 * Indicates that a method is capable of producing a {@link org.springframework.messaging.Message}
 * or {@link org.springframework.messaging.Message} {@code payload}.
 * <p>
 * A method annotated with {@code @InboundChannelAdapter} can't accept any parameters.
 * <p>
 * Return values from the annotated method may be of any type. If the return
 * value is not a {@link org.springframework.messaging.Message}, a {@link org.springframework.messaging.Message}
 * will be created with that object as its {@code payload}.
 * <p>
 * The result {@link org.springframework.messaging.Message} will be sent to the provided {@link #value()}.
 * <p>
 * {@code @InboundChannelAdapter} is an analogue of {@code <int:inbound-channel-adapter/>}. With that
 * the {@link org.springframework.integration.scheduling.PollerMetadata} is required to initiate
 * the method invocation. Or {@link #poller()} should be provided, or the
 * {@link org.springframework.integration.scheduling.PollerMetadata#DEFAULT_POLLER} bean has to be configured
 * in the application context.
 *
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InboundChannelAdapter {

	/**
	 * Alias for the {@link #channel()} attribute.
	 * @return the 'channel' bean name to send the {@link org.springframework.messaging.Message}.
	 */
	@AliasFor("channel")
	String value() default "";

	/**
	 * @return the 'channel' bean name to send the {@link org.springframework.messaging.Message}.
	 * @since 4.2.6
	 */
	@AliasFor("value")
	String channel() default "";

	/**
	 * {@code SmartLifecycle} options.
	 * Can be specified as 'property placeholder', e.g. {@code ${foo.autoStartup}}.
	 * @return if the channel adapter is started automatically or not.
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
	 * @return the {@link org.springframework.integration.annotation.Poller} options for a polled endpoint
	 * ({@link org.springframework.integration.scheduling.PollerMetadata}).
	 * This attribute is an {@code array} just to allow an empty default (no poller).
	 * Only one {@link org.springframework.integration.annotation.Poller} element is allowed.
	 * NOTE: a {@link Poller} here has {@link Poller#maxMessagesPerPoll()} set to 1 by default.
	 */
	Poller[] poller() default { };

}

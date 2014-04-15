/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
 * the {@link org.springframework.integration.scheduling.PollerMetadata} is required to to initiate
 * the method invocation. Or {@link #poller()} should be provided, or the
 * {@link org.springframework.integration.scheduling.PollerMetadata#DEFAULT_POLLER} bean has to be configured
 * in the application context.
 *
 *
 * @author Artem Bilan
 * @since 4.0
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface InboundChannelAdapter {

	/**
	 * @return the 'channel' bean name to send the {@link org.springframework.messaging.Message}.
	 */
	String value();

	/*
	 {@code SmartLifecycle} options.
	 Can be specified as 'property placeholder', e.g. {@code ${foo.autoStartup}}.
	 */
	String autoStartup() default "true";

	String phase() default "0";

	/**
	 * @return the {@link org.springframework.integration.annotation.Poller} options for a polled endpoint
	 * ({@link org.springframework.integration.scheduling.PollerMetadata}).
	 * This attribute is an {@code array} just to allow an empty default (no poller).
	 * Only one {@link org.springframework.integration.annotation.Poller} element is allowed.
	 */
	Poller[] poller() default {};
}

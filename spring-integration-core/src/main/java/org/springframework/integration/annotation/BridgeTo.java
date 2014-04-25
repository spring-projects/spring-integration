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
 * Messaging Annotation to mark {@link org.springframework.context.annotation.Bean}
 * method for {@link org.springframework.messaging.MessageChannel} to produce
 * {@link org.springframework.integration.handler.BridgeHandler} and Consumer Endpoint.
 * <p>
 * The {@link org.springframework.messaging.MessageChannel} {@link org.springframework.context.annotation.Bean}
 * marked with this annotation is used as an {@code inputChannel} for
 * {@link org.springframework.integration.endpoint.AbstractEndpoint}
 * and determine the type of endpoint -
 * {@link org.springframework.integration.endpoint.EventDrivenConsumer} or
 * {@link org.springframework.integration.endpoint.PollingConsumer}.
 * <p>
 * The {@link #value()} of this annotation is an {@code outputChannel} for
 * {@link org.springframework.integration.handler.BridgeHandler}.
 * If it isn't present the {@link org.springframework.integration.handler.BridgeHandler}
 * sends message to the {@code reply-channel} from message headers.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface BridgeTo {
	/**
	 * @return the outbound channel name to send message from
	 * {@link org.springframework.integration.handler.BridgeHandler}.
	 * Optional: means send message to the {@code reply-channel} from headers.
	 */
	String value() default "";

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

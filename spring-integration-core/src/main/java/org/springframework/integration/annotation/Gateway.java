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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.integration.context.IntegrationContextUtils;

/**
 * Indicates that an interface method is capable of mapping its parameters
 * to a message or message payload. These method-level annotations are detected
 * by the {@link org.springframework.integration.gateway.GatewayProxyFactoryBean}
 * where the annotation attributes can override the default channel settings.
 *
 * <p>A method annotated with @Gateway may accept a single non-annotated
 * parameter of type {@link org.springframework.messaging.Message}
 * or of the intended Message payload type. Method parameters may be mapped
 * to individual Message header values by using the
 * {@link org.springframework.messaging.handler.annotation.Header @Header}
 * parameter annotation. Alternatively, to pass the entire Message headers
 * map, a Map-typed parameter may be annotated with
 * {@link org.springframework.messaging.handler.annotation.Headers}.
 *
 * <p>Return values from the annotated method may be of any type. If the
 * declared return value is not a Message, the reply Message's payload will be
 * returned and any type conversion as supported by Spring's
 * {@link org.springframework.beans.SimpleTypeConverter} will be applied to
 * the return value if necessary.
 *
 * <p>Note: unlike @Publisher, this annotation is for exposing a
 * Messaging Endpoint based on a Proxy for the marked interface method.
 * The method invocation causes messaging interaction using an
 * AOP Advice. Method parameters become the part of sent message (payload, headers).
 * The method return value is the result (payload) of the messaging flow invoked by the
 * Proxy.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @see MessagingGateway
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Gateway {

	/**
	 * Specify the channel to which messages will be sent; overrides the encompassing
	 * gateway's default request channel.
	 * @return the channel name.
	 */
	String requestChannel() default "";

	/**
	 * Specify the channel from which reply messages will be received; overrides the
	 * encompassing gateway's default reply channel.
	 * @return the channel name.
	 */
	String replyChannel() default "";

	/**
	 * Specify the timeout (ms) when sending to the request channel - only applies if the
	 * send might block (such as a bounded {@code QueueChannel} that is currently full.
	 * Overrides the encompassing gateway's default request timeout.
	 * @return the timeout.
	 * @see #requestTimeoutExpression()
	 */
	long requestTimeout() default IntegrationContextUtils.DEFAULT_TIMEOUT;

	/**
	 * Specify a SpEL Expression to determine the timeout (ms) when sending to the request
	 * channel - only applies if the send might block (such as a bounded
	 * {@code QueueChannel} that is currently full. Overrides the encompassing gateway's
	 * default request timeout. Overrides {@link #requestTimeout()}.
	 * @return the timeout.
	 * @since 5.0
	 */
	String requestTimeoutExpression() default "";

	/**
	 * Specify the time (ms) that the thread sending the request will wait for a reply.
	 * The timer starts when the thread returns to the gateway, not when the request
	 * message is sent. Overrides the encompassing gateway's default reply timeout.
	 * @return the timeout.
	 * @see #replyTimeoutExpression()
	 */
	long replyTimeout() default IntegrationContextUtils.DEFAULT_TIMEOUT;

	/**
	 * Specify a SpEL Expression to determine the time (ms) that the thread sending
	 * the request will wait for a reply. The timer starts when the thread returns to the
	 * gateway, not when the request message is sent. Overrides the encompassing gateway's
	 * default reply timeout. Overrides {@link #replyTimeout()}.
	 * @return the timeout.
	 * @since 5.0
	 */
	String replyTimeoutExpression() default "";

	/**
	 * Specify a SpEL expression to determine the payload of the request message.
	 * @return the expression.
	 */
	String payloadExpression() default "";

	/**
	 * Specify additional headers that will be added to the request message.
	 * @return the headers.
	 */
	GatewayHeader[] headers() default {};

}

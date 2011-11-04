/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.transformer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.Lifecycle;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Content Enricher is a Message Transformer that invokes any downstream message flow via
 * its request channel and then applies values from the reply Message to the original payload.
 *
 * @author Mark Fisher
 * @since 2.1
 */
public class ContentEnricher extends AbstractReplyProducingMessageHandler implements Lifecycle {

	private final Map<Expression, Expression> propertyExpressions = new HashMap<Expression, Expression>();

	private final Gateway gateway = new Gateway();

	private final SpelExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

	private volatile boolean shouldClonePayload = false;

	private Expression requestPayloadExpression;

	/**
	 * Create a Content Enricher with the given request channel. An anonymous reply channel
	 * will be created for each request.
	 */
	public ContentEnricher(MessageChannel requestChannel) {
		this(requestChannel, null);
	}

	/**
	 * Create a Content Enricher with the given request and reply channels.
	 */
	public ContentEnricher(MessageChannel requestChannel, MessageChannel replyChannel) {
		Assert.notNull(requestChannel, "requestChannel must not be null");
		this.gateway.setRequestChannel(requestChannel);
		if (replyChannel != null) {
			this.gateway.setReplyChannel(replyChannel);
		}
		this.evaluationContext.addPropertyAccessor(new MapAccessor());
	}


	/**
	 * Provide the map of expressions to evaluate when enriching the target payload.
	 * The keys should simply be property names, and the values should be Expressions
	 * that will evaluate against the reply Message as the root object.
	 */
	public void setPropertyExpressions(Map<String, Expression> propertyExpressions) {
		Assert.notEmpty(propertyExpressions, "propertyExpressions must not be empty");
		synchronized (this.propertyExpressions) {
			this.propertyExpressions.clear();
			for (Map.Entry<String, Expression> entry : propertyExpressions.entrySet()) {
				String key = entry.getKey();
				Expression value = entry.getValue();
				Assert.notNull(key, "propertyExpressions key must not be null");
				Assert.notNull(value, "propertyExpressions value must not be null");
				this.propertyExpressions.put(parser.parseExpression(key), value);
			}
		}
	}

	/**
	 * Specify whether to clone payload objects to create the target object.
	 * This is only applicable for payload types that implement Cloneable.
	 */
	public void setShouldClonePayload(boolean shouldClonePayload) {
		this.shouldClonePayload = shouldClonePayload;
	}

	@Override
	public void onInit() {
		super.onInit();
		this.gateway.afterPropertiesSet();
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {

		final Object requestPayload = requestMessage.getPayload();

		final Object targetPayload;

		if (requestPayload instanceof Cloneable && this.shouldClonePayload) {
			try {
				Method cloneMethod = requestPayload.getClass().getMethod("clone", new Class<?>[0]);
				targetPayload = ReflectionUtils.invokeMethod(cloneMethod, requestPayload);
			}
			catch (Exception e) {
				throw new MessageHandlingException(requestMessage, "Failed to clone payload object", e);
			}
		} else {
			targetPayload = requestPayload;
		}

		final Message<?> replyMessage;

	    if (this.requestPayloadExpression==null) {
	    	replyMessage = this.gateway.sendAndReceiveMessage(requestMessage);
		} else {

			final Object requestMessagePayload = this.requestPayloadExpression.getValue(this.evaluationContext, requestMessage);

			Message<?> requestMessageWithCustomPayload = MessageBuilder.withPayload(requestMessagePayload)
					                                                   .copyHeaders(requestMessage.getHeaders())
			                                                           .build();

			replyMessage = this.gateway.sendAndReceiveMessage(requestMessageWithCustomPayload);

		}

		for (Map.Entry<Expression, Expression> entry : this.propertyExpressions.entrySet()) {
			Expression propertyExpression = entry.getKey();
			Expression valueExpression = entry.getValue();
			Object value = valueExpression.getValue(this.evaluationContext, replyMessage);
			propertyExpression.setValue(this.evaluationContext, targetPayload, value);
		}

		return targetPayload;
	}


	/*
	 * Lifecycle implementation
	 */

	public void start() {
		this.gateway.start();
	}

	public void stop() {
		this.gateway.stop();
	}

	public boolean isRunning() {
		return this.gateway.isRunning();
	}


	/**
	 * Internal gateway implementation for request/reply handling.
	 * Simply exposes the sendAndReceiveMessage method.
	 */
	private static final class Gateway extends MessagingGatewaySupport {

		@Override
		protected Message<?> sendAndReceiveMessage(Object object) {
			return super.sendAndReceiveMessage(object);
		}
	}

	/**
	 * By default the original message's payload will be used as payload
	 * that will be send to the request-channel.
	 *
	 * By providing a SpEL expression as value for this setter, a subset of the
	 * original payload, a header value or any other resolvable SpEL expression
	 * can be used as the basis for the payload, that will be send to the
	 * request-channel.
	 *
	 * If more sophisticated logic is required (e.g. changing the message
	 * headers etc.) please use additional downstream transformers.
	 *
	 */
	public void setRequestPayloadExpression(Expression requestPayloadExpression) {
		this.requestPayloadExpression = requestPayloadExpression;
	}


}

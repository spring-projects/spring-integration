/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.Arrays;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.ReactiveMessageHandlerAdapter;
import org.springframework.integration.handler.ReplyProducingMessageHandlerWrapper;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.util.StringUtils;

/**
 * FactoryBean for creating {@link ServiceActivatingHandler} instances.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author David Liu
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ServiceActivatorFactoryBean extends AbstractStandardMessageHandlerFactoryBean {

	private String[] headers;

	public void setNotPropagatedHeaders(String... headers) {
		this.headers = Arrays.copyOf(headers, headers.length);
	}

	@Override
	protected MessageHandler createMethodInvokingHandler(Object targetObject, String targetMethodName) {
		MessageHandler handler;
		handler = createDirectHandlerIfPossible(targetObject, targetMethodName);
		if (handler == null) {
			handler = configureHandler(
					StringUtils.hasText(targetMethodName)
							? new ServiceActivatingHandler(targetObject, targetMethodName)
							: new ServiceActivatingHandler(targetObject));
		}
		return handler;
	}

	/**
	 * If the target object is a {@link MessageHandler} and the method is 'handleMessage', return an
	 * {@link AbstractMessageProducingHandler} that wraps it.
	 * @param targetObject the object to check for Direct Handler requirements.
	 * @param targetMethodName the method name to check for Direct Handler requirements.
	 * @return the {@code targetObject} as a Direct {@link MessageHandler} or {@code null}.
	 */
	protected MessageHandler createDirectHandlerIfPossible(final Object targetObject, String targetMethodName) {
		MessageHandler handler = null;
		if ((targetObject instanceof MessageHandler || targetObject instanceof ReactiveMessageHandler)
				&& methodIsHandleMessageOrEmpty(targetMethodName)) {
			if (targetObject instanceof AbstractMessageProducingHandler) {
				// should never happen but just return it if it's already an AMPH
				return (MessageHandler) targetObject;
			}
			if (targetObject instanceof ReactiveMessageHandler) {
				handler = new ReactiveMessageHandlerAdapter((ReactiveMessageHandler) targetObject);
			}
			else {
				/*
				 * Return a reply-producing message handler so that we still get 'produced no reply' messages
				 * and the super class will inject the advice chain to advise the handler method if needed.
				 */
				handler = new ReplyProducingMessageHandlerWrapper((MessageHandler) targetObject);
			}
		}
		return handler;
	}

	@Override
	protected MessageHandler createExpressionEvaluatingHandler(Expression expression) {
		ExpressionEvaluatingMessageProcessor<Object> processor = new ExpressionEvaluatingMessageProcessor<>(expression);
		processor.setBeanFactory(getBeanFactory());
		ServiceActivatingHandler handler = new ServiceActivatingHandler(processor);
		handler.setPrimaryExpression(expression);
		return this.configureHandler(handler);
	}

	@Override
	protected <T> MessageHandler createMessageProcessingHandler(MessageProcessor<T> processor) {
		return configureHandler(new ServiceActivatingHandler(processor));
	}

	protected MessageHandler configureHandler(ServiceActivatingHandler handler) {
		postProcessReplyProducer(handler);
		return handler;
	}

	/**
	 * Always returns true - any {@link AbstractMessageProducingHandler} can
	 * be used directly.
	 */
	@Override
	protected boolean canBeUsedDirect(AbstractMessageProducingHandler handler) {
		return true;
	}

	@Override
	protected void postProcessReplyProducer(AbstractMessageProducingHandler handler) {
		super.postProcessReplyProducer(handler);
		if (this.headers != null) {
			handler.setNotPropagatedHeaders(this.headers);
		}
	}

}

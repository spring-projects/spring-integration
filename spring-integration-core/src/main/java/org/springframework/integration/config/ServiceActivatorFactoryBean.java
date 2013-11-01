/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.config;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

/**
 * FactoryBean for creating {@link ServiceActivatingHandler} instances.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class ServiceActivatorFactoryBean extends AbstractStandardMessageHandlerFactoryBean {

	private volatile Long sendTimeout;

	private volatile Boolean requiresReply;

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setRequiresReply(Boolean requiresReply) {
		this.requiresReply = requiresReply;
	}

	@Override
	MessageHandler createMethodInvokingHandler(Object targetObject, String targetMethodName) {
		MessageHandler handler = null;
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
	 * {@link AbstractReplyProducingMessageHandler} that wraps it.
	 */
	private MessageHandler createDirectHandlerIfPossible(final Object targetObject, String targetMethodName) {
		MessageHandler handler = null;
		if (targetObject instanceof MessageHandler
				&& this.methodIsHandleMessageOrEmpty(targetMethodName)) {
			if (targetObject instanceof AbstractReplyProducingMessageHandler) {
				// should never happen but just return it if it's already an ARPMH
				return (MessageHandler) targetObject;
			}
			/*
			 * Return a reply-producing message handler so that we still get 'produced no reply' messages
			 * and the super class will inject the advice chain to advise the handler method if needed.
			 */
			handler = new AbstractReplyProducingMessageHandler() {

				@Override
				protected Object handleRequestMessage(Message<?> requestMessage) {

					((MessageHandler) targetObject).handleMessage(requestMessage);
					return null;
				}
			};

		}
		return handler;
	}

	@Override
	MessageHandler createExpressionEvaluatingHandler(Expression expression) {
		ExpressionEvaluatingMessageProcessor<Object> processor = new ExpressionEvaluatingMessageProcessor<Object>(expression);
		processor.setBeanFactory(this.getBeanFactory());
		return this.configureHandler(new ServiceActivatingHandler(processor));
	}

	@Override
	<T> MessageHandler createMessageProcessingHandler(MessageProcessor<T> processor) {
		return this.configureHandler(new ServiceActivatingHandler(processor));
	}

	private MessageHandler configureHandler(ServiceActivatingHandler handler) {
		postProcessReplyProducer(handler);
		return handler;
	}


	/**
	 * Always returns true - any {@link AbstractReplyProducingMessageHandler} can
	 * be used directly.
	 */
	@Override
	protected boolean canBeUsedDirect(AbstractReplyProducingMessageHandler handler) {
		return true;
	}

	@Override
	protected void postProcessReplyProducer(AbstractReplyProducingMessageHandler handler) {
		if (this.sendTimeout != null) {
			handler.setSendTimeout(this.sendTimeout);
		}
		if (this.requiresReply != null) {
			handler.setRequiresReply(this.requiresReply);
		}
	}

}

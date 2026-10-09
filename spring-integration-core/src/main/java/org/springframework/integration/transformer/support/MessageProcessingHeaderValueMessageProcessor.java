/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.transformer.support;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 3.0
 */
public class MessageProcessingHeaderValueMessageProcessor extends AbstractHeaderValueMessageProcessor<Object>
		implements BeanFactoryAware {

	private final MessageProcessor<?> targetProcessor;

	public <T> MessageProcessingHeaderValueMessageProcessor(MessageProcessor<T> targetProcessor) {
		this.targetProcessor = targetProcessor;
	}

	public MessageProcessingHeaderValueMessageProcessor(Object targetObject) {
		this(targetObject, null);
	}

	public MessageProcessingHeaderValueMessageProcessor(Object targetObject, @Nullable String method) {
		this.targetProcessor = new MethodInvokingMessageProcessor<>(targetObject, method);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (this.targetProcessor instanceof BeanFactoryAware beanFactoryAware) {
			beanFactoryAware.setBeanFactory(beanFactory);
		}
	}

	public @Nullable Object processMessage(Message<?> message) {
		return this.targetProcessor.processMessage(message);
	}

}

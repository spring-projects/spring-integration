/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.filter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.Lifecycle;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.handler.AbstractMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A base class for {@link MessageSelector} implementations that delegate to
 * a {@link MessageProcessor}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public abstract class AbstractMessageProcessingSelector
		implements MessageSelector, BeanFactoryAware, ManageableLifecycle {

	private final MessageProcessor<Boolean> messageProcessor;

	public AbstractMessageProcessingSelector(MessageProcessor<Boolean> messageProcessor) {
		Assert.notNull(messageProcessor, "messageProcessor must not be null");
		this.messageProcessor = messageProcessor;
	}

	public void setConversionService(ConversionService conversionService) {
		if (this.messageProcessor instanceof AbstractMessageProcessor) {
			((AbstractMessageProcessor<Boolean>) this.messageProcessor).setConversionService(conversionService);
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (this.messageProcessor instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.messageProcessor).setBeanFactory(beanFactory);
		}
	}

	@Override
	public final boolean accept(Message<?> message) {
		Object result = this.messageProcessor.processMessage(message);
		Assert.notNull(result, "result must not be null");
		Assert.isAssignable(Boolean.class, result.getClass(), "a boolean result is required");
		return (Boolean) result;
	}

	@Override
	public void start() {
		if (this.messageProcessor instanceof Lifecycle) {
			((Lifecycle) this.messageProcessor).start();
		}
	}

	@Override
	public void stop() {
		if (this.messageProcessor instanceof Lifecycle) {
			((Lifecycle) this.messageProcessor).stop();
		}
	}

	@Override
	public boolean isRunning() {
		return !(this.messageProcessor instanceof Lifecycle) || ((Lifecycle) this.messageProcessor).isRunning();
	}

}

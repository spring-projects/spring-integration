/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.Lifecycle;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.handler.AbstractMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A base class for Router implementations that delegate to a
 * {@link MessageProcessor} instance.
 *
 * @author Mark Fisher
 * @author Ngoc Nhan
 * @since 2.0
 */
class AbstractMessageProcessingRouter extends AbstractMappingMessageRouter
		implements ManageableLifecycle {

	private final MessageProcessor<?> messageProcessor;

	AbstractMessageProcessingRouter(MessageProcessor<?> messageProcessor) {
		Assert.notNull(messageProcessor, "messageProcessor must not be null");
		this.messageProcessor = messageProcessor;
	}

	@Override
	public final void onInit() {
		super.onInit();
		if (this.messageProcessor instanceof AbstractMessageProcessor) {
			ConversionService conversionService = getConversionService();
			if (conversionService != null) {
				((AbstractMessageProcessor<?>) this.messageProcessor).setConversionService(conversionService);
			}
		}
		if (this.messageProcessor instanceof BeanFactoryAware beanFactoryAware && getBeanFactory() != null) {
			beanFactoryAware.setBeanFactory(this.getBeanFactory());
		}
	}

	@Override
	public void start() {
		if (this.messageProcessor instanceof Lifecycle lifecycle) {
			lifecycle.start();
		}
	}

	@Override
	public void stop() {
		if (this.messageProcessor instanceof Lifecycle lifecycle) {
			lifecycle.stop();
		}
	}

	@Override
	public boolean isRunning() {
		return !(this.messageProcessor instanceof Lifecycle lifecycle) || lifecycle.isRunning();
	}

	@Override
	protected @Nullable List<Object> getChannelKeys(Message<?> message) {
		Object result = this.messageProcessor.processMessage(message);
		return result == null ? null : Collections.singletonList(result);
	}

}

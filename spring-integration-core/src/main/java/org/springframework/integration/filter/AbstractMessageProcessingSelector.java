/*
 * Copyright 2002-2024 the original author or authors.
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
 * @author Ngoc Nhan
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
		if (this.messageProcessor instanceof BeanFactoryAware beanFactoryAware) {
			beanFactoryAware.setBeanFactory(beanFactory);
		}
	}

	protected MessageProcessor<Boolean> getMessageProcessor() {
		return this.messageProcessor;
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

}

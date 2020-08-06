/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.transformer;

import java.util.Arrays;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Base class for Message Transformers that delegate to a {@link MessageProcessor}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public abstract class AbstractMessageProcessingTransformer
		implements Transformer, BeanFactoryAware, ManageableLifecycle {

	private final MessageProcessor<?> messageProcessor;

	private BeanFactory beanFactory;

	private MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private boolean messageBuilderFactorySet;

	private String[] notPropagatedHeaders;

	private boolean selectiveHeaderPropagation;

	protected AbstractMessageProcessingTransformer(MessageProcessor<?> messageProcessor) {
		Assert.notNull(messageProcessor, "messageProcessor must not be null");
		this.messageProcessor = messageProcessor;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		if (this.messageProcessor instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.messageProcessor).setBeanFactory(beanFactory);
		}
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (!this.messageBuilderFactorySet) {
			if (this.beanFactory != null) {
				this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
			}
			this.messageBuilderFactorySet = true;
		}
		return this.messageBuilderFactory;
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

	/**
	 * Set headers that will NOT be copied from the inbound message if
	 * the handler is configured to copy headers.
	 * @param headers the headers to not propagate from the inbound message.
	 * @since 5.1
	 */
	public void setNotPropagatedHeaders(String... headers) {
		if (!ObjectUtils.isEmpty(headers)) {
			Assert.noNullElements(headers, "null elements are not allowed in 'headers'");
			this.notPropagatedHeaders = Arrays.copyOf(headers, headers.length);
		}

		this.selectiveHeaderPropagation = !ObjectUtils.isEmpty(this.notPropagatedHeaders);
	}

	@Override
	public final Message<?> transform(Message<?> message) {
		Object result = this.messageProcessor.processMessage(message);
		if (result == null) {
			return null;
		}
		if (result instanceof Message<?>) {
			return (Message<?>) result;
		}

		AbstractIntegrationMessageBuilder<?> messageBuilder;

		if (result instanceof AbstractIntegrationMessageBuilder<?>) {
			messageBuilder = (AbstractIntegrationMessageBuilder<?>) result;
		}
		else {
			messageBuilder = getMessageBuilderFactory().withPayload(result);
		}

		MessageHeaders requestHeaders = message.getHeaders();

		return messageBuilder
				.filterAndCopyHeadersIfAbsent(requestHeaders,
						this.selectiveHeaderPropagation ? this.notPropagatedHeaders : null)
				.build();
	}

}

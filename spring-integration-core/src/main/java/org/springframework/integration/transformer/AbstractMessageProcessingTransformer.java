/*
 * Copyright 2002-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Base class for Message Transformers that delegate to a {@link MessageProcessor}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Ngoc Nhan
 */
public abstract class AbstractMessageProcessingTransformer
		extends IntegrationObjectSupport
		implements Transformer, ManageableLifecycle {

	private final MessageProcessor<?> messageProcessor;

	private String @Nullable [] notPropagatedHeaders;

	private boolean selectiveHeaderPropagation;

	protected AbstractMessageProcessingTransformer(MessageProcessor<?> messageProcessor) {
		Assert.notNull(messageProcessor, "messageProcessor must not be null");
		this.messageProcessor = messageProcessor;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.messageProcessor instanceof BeanFactoryAware beanFactoryAware) {
			beanFactoryAware.setBeanFactory(getBeanFactory());
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

	/**
	 * Set headers that will NOT be copied from the inbound message if
	 * the handler is configured to copy headers.
	 * @param headers the headers do not propagate from the inbound message.
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
			throw new MessageTransformationException(message,
					"MessageProcessor returned null in: " + getComponentName());
		}
		if (result instanceof Message<?> messageToReply) {
			return messageToReply;
		}

		AbstractIntegrationMessageBuilder<?> messageBuilder;

		if (result instanceof AbstractIntegrationMessageBuilder<?> integrationMessageBuilder) {
			messageBuilder = integrationMessageBuilder;
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

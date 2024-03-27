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

package org.springframework.integration.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * A Transformer that adds statically configured header values to a Message.
 * Accepts the boolean 'overwrite' property that specifies whether values should
 * be overwritten. By default, any existing header values for a given key, will
 * <em>not</em> be replaced.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @author Artem Bilan
 * @author Gary Russell
 * @author Trung Pham
 */
public class HeaderEnricher extends IntegrationObjectSupport implements Transformer, IntegrationPattern {

	private final Map<String, ? extends HeaderValueMessageProcessor<?>> headersToAdd;

	private volatile MessageProcessor<?> messageProcessor;

	private volatile boolean defaultOverwrite = false;

	private volatile boolean shouldSkipNulls = true;

	public HeaderEnricher() {
		this(null);
	}

	/**
	 * Create a HeaderEnricher with the given map of headers.
	 * @param headersToAdd The headers to add.
	 */
	public HeaderEnricher(Map<String, ? extends HeaderValueMessageProcessor<?>> headersToAdd) {
		this.headersToAdd =
				headersToAdd != null
						? headersToAdd
						: new HashMap<String, HeaderValueMessageProcessor<Object>>();
	}

	public <T> void setMessageProcessor(MessageProcessor<T> messageProcessor) {
		this.messageProcessor = messageProcessor;
	}

	public void setDefaultOverwrite(boolean defaultOverwrite) {
		this.defaultOverwrite = defaultOverwrite;
	}

	/**
	 * Specify whether <code>null</code> values, such as might be returned from
	 * an expression evaluation, should be skipped. The default value is
	 * <code>true</code>. Set this to <code>false</code> if a
	 * <code>null</code> value should trigger <i>removal</i> of the
	 * corresponding header instead.
	 * @param shouldSkipNulls true when null values should be skipped.
	 */
	public void setShouldSkipNulls(boolean shouldSkipNulls) {
		this.shouldSkipNulls = shouldSkipNulls;
	}

	@Override
	public String getComponentType() {
		return "header-enricher";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.header_enricher;
	}

	@Override
	public void onInit() {
		boolean shouldOverwrite = this.defaultOverwrite;
		boolean checkReadOnlyHeaders = getMessageBuilderFactory() instanceof DefaultMessageBuilderFactory;
		shouldOverwrite = initializeHeadersToAdd(shouldOverwrite, checkReadOnlyHeaders);
		BeanFactory beanFactory = getBeanFactory();

		if (this.messageProcessor != null
				&& this.messageProcessor instanceof BeanFactoryAware
				&& beanFactory != null) {
			((BeanFactoryAware) this.messageProcessor).setBeanFactory(beanFactory);
		}

		if (!shouldOverwrite && !this.shouldSkipNulls) {
			logger.warn(() -> getComponentName() +
					" is configured to not overwrite existing headers. 'shouldSkipNulls = false' will have no effect");
		}
	}

	private boolean initializeHeadersToAdd(boolean shouldOverwrite, boolean checkReadOnlyHeaders) {
		boolean overwrite = shouldOverwrite;
		BeanFactory beanFactory = getBeanFactory();
		for (Entry<String, ? extends HeaderValueMessageProcessor<?>> entry : this.headersToAdd.entrySet()) {
			if (checkReadOnlyHeaders &&
					(MessageHeaders.ID.equals(entry.getKey()) || MessageHeaders.TIMESTAMP.equals(entry.getKey()))) {
				throw new BeanInitializationException(
						"HeaderEnricher cannot override 'id' and 'timestamp' read-only headers.\n" +
								"Wrong 'headersToAdd' [" + this.headersToAdd
								+ "] configuration for " + getComponentName());
			}

			HeaderValueMessageProcessor<?> processor = entry.getValue();
			if (processor instanceof BeanFactoryAware && beanFactory != null) {
				((BeanFactoryAware) processor).setBeanFactory(beanFactory);
			}
			Boolean processorOverwrite = processor.isOverwrite();
			if (processorOverwrite != null) {
				overwrite |= processorOverwrite;
			}
		}
		return overwrite;
	}

	@Override
	public Message<?> transform(Message<?> message) {
		MessageHeaders messageHeaders = message.getHeaders();

		AbstractIntegrationMessageBuilder<?> messageBuilder =
				getMessageBuilderFactory()
						.fromMessage(message);

		addHeadersFromMessageProcessor(message, messageBuilder);
		for (Map.Entry<String, ? extends HeaderValueMessageProcessor<?>> entry : this.headersToAdd.entrySet()) {
			String key = entry.getKey();
			HeaderValueMessageProcessor<?> valueProcessor = entry.getValue();

			Boolean shouldOverwrite = valueProcessor.isOverwrite();
			if (shouldOverwrite == null) {
				shouldOverwrite = this.defaultOverwrite;
			}

			boolean headerDoesNotExist = messageHeaders.get(key) == null;

			/*
			 * Only evaluate value expression if necessary
			 */
			if (headerDoesNotExist || shouldOverwrite) {
				Object value = valueProcessor.processMessage(message);
				if (value != null || !this.shouldSkipNulls) {
					messageBuilder.setHeader(key, value);
				}
			}
		}

		return messageBuilder.build();
	}

	private void addHeadersFromMessageProcessor(Message<?> message,
			AbstractIntegrationMessageBuilder<?> messageBuilder) {

		if (this.messageProcessor != null) {
			Object result = this.messageProcessor.processMessage(message);
			if (result instanceof Map) {
				MessageHeaders messageHeaders = message.getHeaders();
				Map<?, ?> resultMap = (Map<?, ?>) result;
				for (Entry<?, ?> entry : resultMap.entrySet()) {
					Object key = entry.getKey();
					if (key instanceof String) {
						if (this.defaultOverwrite || messageHeaders.get(key) == null) {
							messageBuilder.setHeader((String) key, entry.getValue());
						}
					}
					else {
						logger.debug(() -> "ignoring value for non-String key: " + key);
					}
				}
			}
			else {
				logger.debug(() -> "expected a Map result from processor, but received: " + result);
			}
		}
	}

}

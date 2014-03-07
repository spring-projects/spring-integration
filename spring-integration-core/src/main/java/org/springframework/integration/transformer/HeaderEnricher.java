/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * A Transformer that adds statically configured header values to a Message.
 * Accepts the boolean 'overwrite' property that specifies whether values should
 * be overwritten. By default, any existing header values for a given key, will
 * <em>not</em> be replaced.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @author Artem Bilan
 */
public class HeaderEnricher extends IntegrationObjectSupport implements Transformer, BeanNameAware, InitializingBean {

	private static final Log logger = LogFactory.getLog(HeaderEnricher.class);

	private final Map<String, ? extends HeaderValueMessageProcessor<?>> headersToAdd;

	private volatile MessageProcessor<?> messageProcessor;

	private volatile boolean defaultOverwrite = false;

	private volatile boolean shouldSkipNulls = true;

	public HeaderEnricher() {
		this(null);
	}

	/**
	 * Create a HeaderEnricher with the given map of headers.
	 *
	 * @param headersToAdd The headers to add.
	 */
	public HeaderEnricher(Map<String, ? extends HeaderValueMessageProcessor<?>> headersToAdd) {
		this.headersToAdd = (headersToAdd != null) ? headersToAdd
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
	 *
	 * @param shouldSkipNulls true when null values should be skipped.
	 */
	public void setShouldSkipNulls(boolean shouldSkipNulls) {
		this.shouldSkipNulls = shouldSkipNulls;
	}


	@Override
	public String getComponentType() {
		return "transformer"; // backwards compatibility
	}

	@Override
	public Message<?> transform(Message<?> message) {
		try {
			Map<String, Object> headerMap = new HashMap<String, Object>(message.getHeaders());
			this.addHeadersFromMessageProcessor(message, headerMap);
			for (Map.Entry<String, ? extends HeaderValueMessageProcessor<?>> entry : this.headersToAdd.entrySet()) {
				String key = entry.getKey();
				HeaderValueMessageProcessor<?> valueProcessor = entry.getValue();

				Boolean shouldOverwrite = valueProcessor.isOverwrite();
				if (shouldOverwrite == null) {
					shouldOverwrite = this.defaultOverwrite;
				}

				boolean headerDoesNotExist = headerMap.get(key) == null;

				/**
				 * Only evaluate value expression if necessary
				 */
				if (headerDoesNotExist || shouldOverwrite) {
					Object value = valueProcessor.processMessage(message);
					if (value != null || !this.shouldSkipNulls) {
						headerMap.put(key, value);
					}
				}
			}
			return this.getMessageBuilderFactory().withPayload(message.getPayload()).copyHeaders(headerMap).build();
		}
		catch (Exception e) {
			throw new MessagingException(message, "failed to transform message headers", e);
		}
	}

	@SuppressWarnings("rawtypes")
	private void addHeadersFromMessageProcessor(Message<?> message, Map<String, Object> headerMap) {
		if (this.messageProcessor != null) {
			Object result = this.messageProcessor.processMessage(message);
			if (result instanceof Map) {
				Map resultMap = (Map) result;
				for (Object key : resultMap.keySet()) {
					if (key instanceof String) {
						if (this.defaultOverwrite || headerMap.get(key) == null) {
							headerMap.put((String) key, resultMap.get(key));
						}
					}
					else if (logger.isDebugEnabled()) {
						logger.debug("ignoring value for non-String key: " + key);
					}
				}
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("expected a Map result from processor, but received: " + result);
			}
		}
	}

	@Override
	public void onInit() throws Exception {
		boolean shouldOverwrite = this.defaultOverwrite;
		for (HeaderValueMessageProcessor<?> processor : this.headersToAdd.values()) {
			if (processor instanceof BeanFactoryAware && this.getBeanFactory() != null) {
				((BeanFactoryAware) processor).setBeanFactory(this.getBeanFactory());
			}
			Boolean processerOverwrite = processor.isOverwrite();
			if (processerOverwrite != null) {
				shouldOverwrite |= processerOverwrite;
			}
		}
		if (this.messageProcessor != null && this.messageProcessor instanceof BeanFactoryAware && this.getBeanFactory() != null) {
			((BeanFactoryAware) this.messageProcessor).setBeanFactory(this.getBeanFactory());
		}
		if (!shouldOverwrite && !this.shouldSkipNulls) {
			logger.warn(this.getComponentName()
					+ " is configured to not overwrite existing headers. 'shouldSkipNulls = false' will have no effect");
		}
	}

}

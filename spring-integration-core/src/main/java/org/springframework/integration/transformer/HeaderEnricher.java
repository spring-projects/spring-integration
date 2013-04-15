/*
 * Copyright 2002-2012 the original author or authors.
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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.integration.support.MessageBuilder;

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
public class HeaderEnricher implements Transformer, BeanNameAware, InitializingBean {

	private static final Log logger = LogFactory.getLog(HeaderEnricher.class);

	private final Map<String, ? extends HeaderValueMessageProcessor<?>> headersToAdd;

	private volatile MessageProcessor<?> messageProcessor;

	private volatile boolean defaultOverwrite = false;

	private volatile boolean shouldSkipNulls = true;

	private Object beanName;

	public HeaderEnricher() {
		this(null);
	}

	/**
	 * Create a HeaderEnricher with the given map of headers.
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
	 */
	public void setShouldSkipNulls(boolean shouldSkipNulls) {
		this.shouldSkipNulls = shouldSkipNulls;
	}

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
			return MessageBuilder.withPayload(message.getPayload()).copyHeaders(headerMap).build();
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

	public static interface HeaderValueMessageProcessor<T> extends MessageProcessor<T> {

		Boolean isOverwrite();

	}

	static abstract class AbstractHeaderValueMessageProcessor<T> implements HeaderValueMessageProcessor<T> {

		// null indicates no explicit setting; use header-enricher's
		// 'default-overwrite' value
		private volatile Boolean overwrite = null;

		public void setOverwrite(Boolean overwrite) {
			this.overwrite = overwrite;
		}

		public Boolean isOverwrite() {
			return this.overwrite;
		}

	}

	static class StaticHeaderValueMessageProcessor<T> extends AbstractHeaderValueMessageProcessor<T> {

		private final T value;

		public StaticHeaderValueMessageProcessor(T value) {
			this.value = value;
		}

		public T processMessage(Message<?> message) {
			return this.value;
		}
	}

	static class ExpressionEvaluatingHeaderValueMessageProcessor<T> extends AbstractHeaderValueMessageProcessor<T>
			implements BeanFactoryAware {

		private static final ExpressionParser expressionParser = new SpelExpressionParser(new SpelParserConfiguration(
				true, true));

		private final ExpressionEvaluatingMessageProcessor<T> targetProcessor;

		/**
		 * Create a header value processor for the given Expression and the
		 * expected type of the expression evaluation result. The expectedType
		 * may be null if unknown.
		 */
		public ExpressionEvaluatingHeaderValueMessageProcessor(Expression expression, Class<T> expectedType) {
			this.targetProcessor = new ExpressionEvaluatingMessageProcessor<T>(expression, expectedType);
		}

		/**
		 * Create a header value processor for the given expression string and
		 * the expected type of the expression evaluation result. The
		 * expectedType may be null if unknown.
		 */
		public ExpressionEvaluatingHeaderValueMessageProcessor(String expressionString, Class<T> expectedType) {
			Expression expression = expressionParser.parseExpression(expressionString);
			this.targetProcessor = new ExpressionEvaluatingMessageProcessor<T>(expression, expectedType);
		}

		public void setBeanFactory(BeanFactory beanFactory) {
			this.targetProcessor.setBeanFactory(beanFactory);
		}

		public T processMessage(Message<?> message) {
			return this.targetProcessor.processMessage(message);
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang
	 * .String)
	 */
	public void setBeanName(String beanName) {
		this.beanName = beanName;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		boolean shouldOverwrite = this.defaultOverwrite;
		for (HeaderValueMessageProcessor<?> processor : this.headersToAdd.values()) {
			Boolean processerOverwrite = processor.isOverwrite();
			if (processerOverwrite != null) {
				shouldOverwrite |= processerOverwrite.booleanValue();
			}
		}
		if (!shouldOverwrite && !this.shouldSkipNulls) {
			logger.warn(this.beanName
					+ " is configured to not overwrite existing headers. 'shouldSkipNulls = false' will have no effect");
		}
	}

	static class MessageProcessingHeaderValueMessageProcessor extends AbstractHeaderValueMessageProcessor<Object> {

		private final MessageProcessor<?> targetProcessor;

		public <T> MessageProcessingHeaderValueMessageProcessor(MessageProcessor<T> targetProcessor) {
			this.targetProcessor = targetProcessor;
		}

		public MessageProcessingHeaderValueMessageProcessor(Object targetObject) {
			this(targetObject, null);
		}

		public MessageProcessingHeaderValueMessageProcessor(Object targetObject, String method) {
			this.targetProcessor = new MethodInvokingMessageProcessor<Object>(targetObject, method);
		}

		public Object processMessage(Message<?> message) {
			return this.targetProcessor.processMessage(message);
		}

	}

}

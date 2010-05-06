/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.integration.message.MessageBuilder;

/**
 * A Transformer that adds statically configured header values to a Message.
 * Accepts the boolean 'overwrite' property that specifies whether values
 * should be overwritten. By default, any existing header values for
 * a given key, will <em>not</em> be replaced.
 * 
 * @author Mark Fisher
 */
public class HeaderEnricher implements Transformer {

	private static final Log logger = LogFactory.getLog(HeaderEnricher.class);


	private final Map<String, ValueHolder> headersToAdd;

	private volatile MessageProcessor messageProcessor;

	private volatile boolean defaultOverwrite = false;


	public HeaderEnricher() {
		this(null);
	}

	/**
	 * Create a HeaderEnricher with the given map of headers.
	 */
	public HeaderEnricher(Map<String, ValueHolder> headersToAdd) {
		this.headersToAdd = (headersToAdd != null) ? headersToAdd : new HashMap<String, ValueHolder>();
	}


	public void setMessageProcessor(MessageProcessor messageProcessor) {
		this.messageProcessor = messageProcessor;
	}

	public void setDefaultOverwrite(boolean defaultOverwrite) {
		this.defaultOverwrite = defaultOverwrite;
	}

	public Message<?> transform(Message<?> message) {
		try {
			Map<String, Object> headerMap = new HashMap<String, Object>(message.getHeaders());
			this.addHeadersFromMessageProcessor(message, headerMap);
			for (Map.Entry<String, ValueHolder> entry : this.headersToAdd.entrySet()) {
				String key = entry.getKey();
				ValueHolder valueHolder = entry.getValue();
				Boolean shouldOverwrite = valueHolder.isOverwrite();
				if (shouldOverwrite == null) {
					shouldOverwrite = this.defaultOverwrite;
				}
				if (shouldOverwrite || headerMap.get(key) == null) {
					headerMap.put(key, valueHolder.evaluate(message));
				}
			}
	        return MessageBuilder.withPayload(message.getPayload()).copyHeaders(headerMap).build();
        }
		catch (Exception e) {
        	throw new MessagingException(message, "failed to transform message headers", e);
        }
	}

	@SuppressWarnings("unchecked")
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

	public static interface ValueHolder {

		Object evaluate(Message<?> message);

		Boolean isOverwrite();

	}


	static abstract class AbstractValueHolder implements ValueHolder {

		// null indicates no explicit setting; use header-enricher's 'default-overwrite' value
		private volatile Boolean overwrite = null;

		public void setOverwrite(Boolean overwrite) {
			this.overwrite = overwrite;
		}

		public Boolean isOverwrite() {
			return this.overwrite;
		}

	}


	static class StaticValueHolder extends AbstractValueHolder {

		private final Object value;

		public StaticValueHolder(Object value) {
			this.value = value;
		}

		public Object evaluate(Message<?> message) {
			return this.value;
		}
	}


	static class ExpressionHolder extends AbstractValueHolder {

		private static final ExpressionParser parser = new SpelExpressionParser();

		private final Class<?> expectedType;

		private final Expression expression;

		private final EvaluationContext evaluationContext;


		/**
		 * Create a holder object for the given expression String and the expected type
		 * of the expression evaluation result. The expectedType may be null if unknown.
		 */
		public ExpressionHolder(String expressionString, Class<?> expectedType) {
			this.expectedType = expectedType;
			this.expression = parser.parseExpression(expressionString);
			StandardEvaluationContext context = new StandardEvaluationContext();
			context.addPropertyAccessor(new MapAccessor());
			this.evaluationContext = context;
		}


		public Object evaluate(Message<?> message) throws ParseException, EvaluationException {
			return (this.expectedType != null)
					? this.expression.getValue(this.evaluationContext, message, this.expectedType)
					: this.expression.getValue(this.evaluationContext, message);
		}
	}


	static class MethodExpressionHolder extends AbstractValueHolder  {

		private final MethodInvokingMessageProcessor processor;

		public MethodExpressionHolder(Object targetObject, String method) {
			this.processor = new MethodInvokingMessageProcessor(targetObject, method);
		}

		public Object evaluate(Message<?> message) {
			return this.processor.processMessage(message);
		}
	}

}

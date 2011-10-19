/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.config;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for FactoryBeans that create MessageHandler instances.
 * 
 * @author Mark Fisher
 * @author Alexander Peters
 */
abstract class AbstractStandardMessageHandlerFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<MessageHandler>{

	private static final ExpressionParser expressionParser = new SpelExpressionParser(new SpelParserConfiguration(true,
			true));

	private volatile Object targetObject;

	private volatile String targetMethodName;

	private volatile Expression expression;

	public void setTargetObject(Object targetObject) {
		this.targetObject = targetObject;
	}

	public void setTargetMethodName(String targetMethodName) {
		this.targetMethodName = targetMethodName;
	}

	public void setExpressionString(String expressionString) {
		this.expression = expressionParser.parseExpression(expressionString);
	}

	public void setExpression(Expression expression) {
		this.expression = expression;
	}

	protected MessageHandler createHandler() {
		MessageHandler handler;
		if (this.targetObject == null) {
			Assert.isTrue(!StringUtils.hasText(this.targetMethodName),
					"The target method is only allowed when a target object (ref or inner bean) is also provided.");
		}
		if (this.targetObject != null) {
			Assert.state(this.expression == null,
					"The 'targetObject' and 'expression' properties are mutually exclusive.");
			if (this.targetObject instanceof MessageProcessor<?>) {
				handler = this.createMessageProcessingHandler((MessageProcessor<?>) this.targetObject);
			} else {
				handler = this.createMethodInvokingHandler(this.targetObject, this.targetMethodName);
			}
		} else if (this.expression != null) {
			handler = this.createExpressionEvaluatingHandler(this.expression);
		} else {
			handler = this.createDefaultHandler();
		}
		return handler;
	}

	/**
	 * Subclasses must implement this method to create the MessageHandler.
	 */
	abstract MessageHandler createMethodInvokingHandler(Object targetObject, String targetMethodName);

	MessageHandler createExpressionEvaluatingHandler(Expression expression) {
		throw new UnsupportedOperationException(this.getClass().getName() + " does not support expressions.");
	}

	<T> MessageHandler createMessageProcessingHandler(MessageProcessor<T> processor) {
		return this.createMethodInvokingHandler(processor, "processMessage");
	}

	MessageHandler createDefaultHandler() {
		throw new IllegalArgumentException("Exactly one of the 'targetObject' or 'expression' property is required.");
	}

	@SuppressWarnings("unchecked")
	<T> T extractTypeIfPossible(Object targetObject, Class<T> expectedType) {
		if (targetObject == null) {
			return null;
		}
		if (expectedType.isAssignableFrom(targetObject.getClass())) {
			return (T) targetObject;
		}
		if (targetObject instanceof Advised) {
			TargetSource targetSource = ((Advised) targetObject).getTargetSource();
			if (targetSource == null) {
				return null;
			}
			try {
				return extractTypeIfPossible(targetSource.getTarget(), expectedType);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		return null;
	}

}

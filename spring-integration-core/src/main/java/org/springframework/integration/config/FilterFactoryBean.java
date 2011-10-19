/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.config;

import org.springframework.expression.Expression;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.filter.MethodInvokingSelector;
import org.springframework.util.StringUtils;

/**
 * Factory bean for creating a Message Filter.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class FilterFactoryBean extends AbstractStandardMessageHandlerFactoryBean<MessageFilter> {

	private volatile MessageChannel discardChannel;

	private volatile Boolean throwExceptionOnRejection;

	private volatile Long sendTimeout;


	public void setDiscardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
	}

	public void setThrowExceptionOnRejection(Boolean throwExceptionOnRejection) {
		this.throwExceptionOnRejection = throwExceptionOnRejection;
	}

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@Override
	MessageFilter createMethodInvokingHandler(Object targetObject, String targetMethodName) {
		MessageSelector selector = null;
		if (targetObject instanceof MessageSelector) {
			selector = (MessageSelector) targetObject;
		}
		else if (StringUtils.hasText(targetMethodName)) {
			selector = new MethodInvokingSelector(targetObject, targetMethodName);
		}
		else {
			selector = new MethodInvokingSelector(targetObject);
		}
		return this.createFilter(selector);
	}

	@Override
	MessageFilter createExpressionEvaluatingHandler(Expression expression) {
		return this.createFilter(new ExpressionEvaluatingSelector(expression));
	}

	private MessageFilter createFilter(MessageSelector selector) {
		MessageFilter filter = new MessageFilter(selector);
		if (this.throwExceptionOnRejection != null) {
			filter.setThrowExceptionOnRejection(this.throwExceptionOnRejection);
		}
		if (this.discardChannel != null) {
			filter.setDiscardChannel(discardChannel);
		}
		if (this.sendTimeout != null) {
			filter.setSendTimeout(this.sendTimeout.longValue());
		}
		return filter;
	}

}

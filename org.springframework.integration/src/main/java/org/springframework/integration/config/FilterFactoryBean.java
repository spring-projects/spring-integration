/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.filter.MethodInvokingSelector;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.selector.MessageSelector;
import org.springframework.util.StringUtils;

/**
 * Factory bean for creating a Message Filter.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class FilterFactoryBean extends AbstractMessageHandlerFactoryBean {

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
	MessageHandler createMethodInvokingHandler(Object targetObject, String targetMethodName) {
		if (targetObject instanceof MessageSelector) {
			return this.createFilter((MessageSelector) targetObject);
		}
		if (StringUtils.hasText(targetMethodName)) {
			MessageSelector selector = new MethodInvokingSelector(targetObject, targetMethodName);
			return this.createFilter(selector);
		}
		throw new IllegalArgumentException("Filter must provide a target method" +
				" name if the 'ref' does not point to a MessageSelector instance.");
	}

	@Override
	MessageHandler createExpressionEvaluatingHandler(String expression) {
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

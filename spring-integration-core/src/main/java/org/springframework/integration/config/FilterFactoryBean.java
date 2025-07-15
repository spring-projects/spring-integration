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

package org.springframework.integration.config;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.filter.MethodInvokingSelector;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for creating a Message Filter.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author David Liu
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class FilterFactoryBean extends AbstractStandardMessageHandlerFactoryBean {

	@Nullable
	private volatile MessageChannel discardChannel;

	@Nullable
	private volatile Boolean throwExceptionOnRejection;

	@Nullable
	private volatile Boolean discardWithinAdvice;

	public void setDiscardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
	}

	public void setThrowExceptionOnRejection(Boolean throwExceptionOnRejection) {
		this.throwExceptionOnRejection = throwExceptionOnRejection;
	}

	public void setDiscardWithinAdvice(boolean discardWithinAdvice) {
		this.discardWithinAdvice = discardWithinAdvice;
	}

	@Override
	protected MessageHandler createMethodInvokingHandler(Object targetObject, @Nullable String targetMethodName) {
		MessageSelector selector = null;
		if (targetObject instanceof MessageSelector) {
			selector = (MessageSelector) targetObject;
		}
		else if (StringUtils.hasText(targetMethodName)) {
			this.checkForIllegalTarget(targetObject, targetMethodName);
			selector = new MethodInvokingSelector(targetObject, targetMethodName);
		}
		else {
			selector = new MethodInvokingSelector(targetObject);
		}
		return this.createFilter(selector);
	}

	@Override
	protected void checkForIllegalTarget(Object targetObject, @Nullable String targetMethodName) {
		if (targetObject instanceof AbstractReplyProducingMessageHandler
				&& this.methodIsHandleMessageOrEmpty(targetMethodName)) {
			throw new IllegalArgumentException("You cannot use 'AbstractReplyProducingMessageHandler.handleMessage()' "
					+ "as a filter - it does not return a result");
		}
	}

	@Override
	protected MessageHandler createExpressionEvaluatingHandler(Expression expression) {
		return this.createFilter(new ExpressionEvaluatingSelector(expression));
	}

	protected MessageFilter createFilter(MessageSelector selector) {
		MessageFilter filter = new MessageFilter(selector);
		postProcessReplyProducer(filter);
		return filter;
	}

	protected void postProcessFilter(MessageFilter filter) {
		if (this.throwExceptionOnRejection != null) {
			filter.setThrowExceptionOnRejection(this.throwExceptionOnRejection);
		}
		if (this.discardChannel != null) {
			filter.setDiscardChannel(this.discardChannel);
		}
		if (this.discardWithinAdvice != null) {
			filter.setDiscardWithinAdvice(this.discardWithinAdvice);
		}
	}

	@Override
	protected void postProcessReplyProducer(AbstractMessageProducingHandler handler) {
		super.postProcessReplyProducer(handler);

		if (!(handler instanceof MessageFilter)) {
			Assert.isNull(this.throwExceptionOnRejection,
					"Cannot set throwExceptionOnRejection if the referenced bean is "
							+ "an AbstractReplyProducingMessageHandler, but not a MessageFilter");
			Assert.isNull(this.discardChannel, "Cannot set discardChannel if the referenced bean is "
					+ "an AbstractReplyProducingMessageHandler, but not a MessageFilter");
			Assert.isNull(this.discardWithinAdvice, "Cannot set discardWithinAdvice if the referenced bean is "
					+ "an AbstractReplyProducingMessageHandler, but not a MessageFilter");
		}
		else {
			postProcessFilter((MessageFilter) handler);
		}
	}

	/**
	 * MessageFilter is an ARPMH. If a non-MessageFilter ARPMH is also a
	 * MessageSelector, MesageSelector wins and gets wrapped in a MessageFilter.
	 */
	@Override
	protected boolean canBeUsedDirect(AbstractMessageProducingHandler handler) {
		return handler instanceof MessageFilter
				|| (!(handler instanceof MessageSelector) && noFilterAttributesProvided());
	}

	private boolean noFilterAttributesProvided() {
		return this.discardChannel == null
				&& this.throwExceptionOnRejection == null
				&& this.discardWithinAdvice == null;
	}

	@Override
	protected Class<? extends MessageHandler> getPreCreationHandlerType() {
		return MessageFilter.class;
	}

}

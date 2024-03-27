/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.dsl;

import org.springframework.expression.Expression;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.filter.MethodInvokingSelector;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.integration.util.ClassUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;

/**
 * An {@link AbstractRouterSpec} for a {@link RecipientListRouter}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class RecipientListRouterSpec extends AbstractRouterSpec<RecipientListRouterSpec, RecipientListRouter> {

	protected RecipientListRouterSpec() {
		super(new RecipientListRouter());
	}

	/**
	 * Adds a recipient channel that is always selected.
	 * @param channelName the channel name.
	 * @return the router spec.
	 */
	public RecipientListRouterSpec recipient(String channelName) {
		return recipient(channelName, (String) null);
	}

	/**
	 * Adds a recipient channel that will be selected if the expression evaluates to 'true'.
	 * @param channelName the channel name.
	 * @param expression the expression.
	 * @return the router spec.
	 */
	public RecipientListRouterSpec recipient(String channelName, @Nullable String expression) {
		if (StringUtils.hasText(expression)) {
			return recipient(channelName, PARSER.parseExpression(expression));
		}
		else {
			this.handler.addRecipient(channelName);
			return _this();
		}
	}

	/**
	 * Adds a recipient channel that will be selected if the expression evaluates to 'true'.
	 * @param channelName the channel name.
	 * @param expression the expression.
	 * @return the router spec.
	 */
	public RecipientListRouterSpec recipient(String channelName, Expression expression) {
		ExpressionEvaluatingSelector selector = new ExpressionEvaluatingSelector(expression);
		this.handler.addRecipient(channelName, selector);
		this.componentsToRegister.put(selector, null);
		return _this();
	}

	/**
	 * Adds a recipient channel that will be selected if the selector's accept method returns 'true'.
	 * @param channelName the channel name.
	 * @param selector the selector.
	 * @return the router spec.
	 */
	public RecipientListRouterSpec recipientMessageSelector(String channelName, MessageSelector selector) {
		return recipient(channelName, selector);
	}

	/**
	 * Adds a recipient channel that will be selected if the selector's accept method returns 'true'.
	 * @param channelName the channel name.
	 * @param selector the selector.
	 * @param <P> the selector source type.
	 * @return the router spec.
	 */
	public <P> RecipientListRouterSpec recipient(String channelName, GenericSelector<P> selector) {
		MessageSelector messageSelector = wrapToMessageSelectorIfNecessary(selector);
		this.handler.addRecipient(channelName, messageSelector);
		return _this();
	}

	private <P> MessageSelector wrapToMessageSelectorIfNecessary(GenericSelector<P> selector) {
		MessageSelector messageSelector;
		if (selector instanceof MessageSelector) {
			messageSelector = (MessageSelector) selector;
		}
		else {
			messageSelector = new MethodInvokingSelector(selector, ClassUtils.SELECTOR_ACCEPT_METHOD);
		}
		return messageSelector;
	}

	/**
	 * Adds a recipient channel that always is selected.
	 * @param channel the recipient channel.
	 * @return the router spec.
	 */
	public RecipientListRouterSpec recipient(MessageChannel channel) {
		return recipient(channel, (String) null);
	}

	/**
	 * Adds a recipient channel that will be selected if the expression evaluates to 'true'.
	 * @param channel the recipient channel.
	 * @param expression the expression.
	 * @return the router spec.
	 */
	public RecipientListRouterSpec recipient(MessageChannel channel, @Nullable String expression) {
		if (StringUtils.hasText(expression)) {
			return recipient(channel, PARSER.parseExpression(expression));
		}
		else {
			this.handler.addRecipient(channel);
			return this;
		}
	}

	/**
	 * Adds a recipient channel that will be selected if the expression evaluates to 'true'.
	 * @param channel the recipient channel.
	 * @param expression the expression.
	 * @return the router spec.
	 */
	public RecipientListRouterSpec recipient(MessageChannel channel, Expression expression) {
		ExpressionEvaluatingSelector selector = new ExpressionEvaluatingSelector(expression);
		this.handler.addRecipient(channel, selector);
		this.componentsToRegister.put(selector, null);
		return this;
	}

	/**
	 * Adds a recipient channel that will be selected if the selector's accept method returns 'true'.
	 * @param channel the recipient channel.
	 * @param selector the selector.
	 * @return the router spec.
	 */
	public RecipientListRouterSpec recipientMessageSelector(MessageChannel channel, MessageSelector selector) {
		return recipient(channel, selector);
	}

	/**
	 * Adds a recipient channel that will be selected if the selector's accept method returns 'true'.
	 * @param channel the recipient channel.
	 * @param selector the selector.
	 * @param <P> the selector source type.
	 * @return the router spec.
	 */
	public <P> RecipientListRouterSpec recipient(MessageChannel channel, GenericSelector<P> selector) {
		MessageSelector messageSelector = wrapToMessageSelectorIfNecessary(selector);
		this.handler.addRecipient(channel, messageSelector);
		return this;
	}

	/**
	 * Adds a subflow that will be invoked if the selector's accept methods returns 'true'.
	 * @param selector the selector.
	 * @param subFlow the subflow.
	 * @return the router spec.
	 */
	public RecipientListRouterSpec recipientMessageSelectorFlow(MessageSelector selector, IntegrationFlow subFlow) {
		return recipientFlow(selector, subFlow);
	}

	/**
	 * Adds a subflow that will be invoked if the selector's accept methods returns 'true'.
	 * @param selector the selector.
	 * @param subFlow the subflow.
	 * @param <P> the selector source type.
	 * @return the router spec.
	 */
	public <P> RecipientListRouterSpec recipientFlow(GenericSelector<P> selector, IntegrationFlow subFlow) {
		MessageChannel channel = obtainInputChannelFromFlow(subFlow);
		return recipient(channel, selector);
	}

	/**
	 * Adds a subflow that will be invoked as a recipient.
	 * @param subFlow the subflow.
	 * @return the router spec.
	 */
	public RecipientListRouterSpec recipientFlow(IntegrationFlow subFlow) {
		return recipientFlow((String) null, subFlow);
	}

	/**
	 * Adds a subflow that will be invoked if the expression evaluates to 'true'.
	 * @param expression the expression.
	 * @param subFlow the subflow.
	 * @return the router spec.
	 */
	public RecipientListRouterSpec recipientFlow(@Nullable String expression, IntegrationFlow subFlow) {
		if (StringUtils.hasText(expression)) {
			return recipientFlow(PARSER.parseExpression(expression), subFlow);
		}
		else {
			MessageChannel channel = obtainInputChannelFromFlow(subFlow);
			return recipient(channel);
		}
	}

	/**
	 * Adds a subflow that will be invoked if the expression evaluates to 'true'.
	 * @param expression the expression.
	 * @param subFlow the subflow.
	 * @return the router spec.
	 */
	public RecipientListRouterSpec recipientFlow(Expression expression, IntegrationFlow subFlow) {
		MessageChannel channel = obtainInputChannelFromFlow(subFlow);
		return recipient(channel, expression);
	}

}

/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.transaction;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.JavaUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.BeanFactoryMessageChannelDestinationResolver;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The {@link FactoryBean} implementation (with {@code Builder} style) to be used
 * from JavaConfig to populate {@link DefaultTransactionSynchronizationFactory} bean.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
public class TransactionSynchronizationFactoryBean implements FactoryBean<DefaultTransactionSynchronizationFactory>,
		BeanFactoryAware {

	private static final String EXPRESSION_OR_CHANNEL_NEEDED =
			"At least one attribute ('expression' and/or 'messageChannel') must be defined";

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private final AtomicInteger counter = new AtomicInteger();

	private BeanFactory beanFactory;

	private volatile String beforeCommitExpression;

	private volatile String afterCommitExpression;

	private volatile String afterRollbackExpression;

	private volatile MessageChannel beforeCommitChannel;

	private volatile String beforeCommitChannelName;

	private volatile MessageChannel afterCommitChannel;

	private volatile String afterCommitChannelName;

	private volatile MessageChannel afterRollbackChannel;

	private volatile String afterRollbackChannelName;

	private volatile DestinationResolver<MessageChannel> channelResolver;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Specify the {@link DestinationResolver} strategy to use.
	 * The default is a BeanFactoryChannelResolver.
	 * @param resolver The channel resolver.
	 * @return current TransactionSynchronizationFactoryBean
	 * @since 4.1.3
	 */
	public TransactionSynchronizationFactoryBean channelResolver(DestinationResolver<MessageChannel> resolver) {
		Assert.notNull(resolver, "'channelResolver' must not be null");
		this.channelResolver = resolver;
		return this;
	}

	public TransactionSynchronizationFactoryBean beforeCommit(String expression) {
		return beforeCommit(expression, this.beforeCommitChannel);
	}

	public TransactionSynchronizationFactoryBean beforeCommit(String expression, String messageChannel) {
		Assert.state(StringUtils.hasText(expression) || StringUtils.hasText(messageChannel),
				EXPRESSION_OR_CHANNEL_NEEDED);
		this.beforeCommitExpression = expression;
		this.beforeCommitChannelName = messageChannel;
		this.beforeCommitChannel = null;
		return this;
	}

	public TransactionSynchronizationFactoryBean beforeCommit(MessageChannel messageChannel) {
		return beforeCommit(this.beforeCommitExpression, messageChannel);
	}

	public TransactionSynchronizationFactoryBean beforeCommit(String expression, MessageChannel messageChannel) {
		Assert.state(StringUtils.hasText(expression) || messageChannel != null,
				EXPRESSION_OR_CHANNEL_NEEDED);
		this.beforeCommitExpression = expression;
		this.beforeCommitChannel = messageChannel;
		this.beforeCommitChannelName = null;
		return this;
	}

	public TransactionSynchronizationFactoryBean afterCommit(String expression) {
		return afterCommit(expression, this.afterCommitChannel);
	}

	public TransactionSynchronizationFactoryBean afterCommit(String expression, String messageChannel) {
		Assert.state(StringUtils.hasText(expression) || StringUtils.hasText(messageChannel),
				EXPRESSION_OR_CHANNEL_NEEDED);
		this.afterCommitExpression = expression;
		this.afterCommitChannelName = messageChannel;
		this.afterCommitChannel = null;
		return this;
	}

	public TransactionSynchronizationFactoryBean afterCommit(MessageChannel messageChannel) {
		return afterCommit(this.afterCommitExpression, messageChannel);
	}

	public TransactionSynchronizationFactoryBean afterCommit(String expression, MessageChannel messageChannel) {
		Assert.state(StringUtils.hasText(expression) || messageChannel != null,
				EXPRESSION_OR_CHANNEL_NEEDED);
		this.afterCommitExpression = expression;
		this.afterCommitChannel = messageChannel;
		this.afterCommitChannelName = null;
		return this;
	}

	public TransactionSynchronizationFactoryBean afterRollback(String expression) {
		return afterRollback(expression, this.afterRollbackChannel);
	}

	public TransactionSynchronizationFactoryBean afterRollback(String expression, String messageChannel) {
		Assert.state(StringUtils.hasText(expression) || StringUtils.hasText(messageChannel),
				EXPRESSION_OR_CHANNEL_NEEDED);
		this.afterRollbackExpression = expression;
		this.afterRollbackChannelName = messageChannel;
		this.afterRollbackChannel = null;
		return this;
	}

	public TransactionSynchronizationFactoryBean afterRollback(MessageChannel messageChannel) {
		return afterRollback(this.afterRollbackExpression, messageChannel);
	}

	public TransactionSynchronizationFactoryBean afterRollback(String expression, MessageChannel messageChannel) {
		Assert.state(StringUtils.hasText(expression) || messageChannel != null,
				EXPRESSION_OR_CHANNEL_NEEDED);
		this.afterRollbackExpression = expression;
		this.afterRollbackChannel = messageChannel;
		this.afterRollbackChannelName = null;
		return this;
	}

	@Override
	public DefaultTransactionSynchronizationFactory getObject() {
		if (this.channelResolver == null) {
			this.channelResolver = new BeanFactoryMessageChannelDestinationResolver(this.beanFactory);
		}
		if (StringUtils.hasText(this.beforeCommitChannelName)) {
			this.beforeCommitChannel = this.channelResolver.resolveDestination(this.beforeCommitChannelName);
		}
		if (StringUtils.hasText(this.afterCommitChannelName)) {
			this.afterCommitChannel = this.channelResolver.resolveDestination(this.afterCommitChannelName);
		}
		if (StringUtils.hasText(this.afterRollbackChannelName)) {
			this.afterRollbackChannel = this.channelResolver.resolveDestination(this.afterRollbackChannelName);
		}

		ExpressionEvaluatingTransactionSynchronizationProcessor processor =
				new ExpressionEvaluatingTransactionSynchronizationProcessor();

		JavaUtils.INSTANCE
				.acceptIfHasText(this.beforeCommitExpression, (expression) ->
						processor.setBeforeCommitExpression(PARSER.parseExpression(expression)))
				.acceptIfHasText(this.afterCommitExpression, (expression) ->
						processor.setAfterCommitExpression(PARSER.parseExpression(expression)))
				.acceptIfHasText(this.afterRollbackExpression, (expression) ->
						processor.setAfterRollbackExpression(PARSER.parseExpression(expression)))
				.acceptIfNotNull(this.beforeCommitChannel, processor::setBeforeCommitChannel)
				.acceptIfNotNull(this.afterCommitChannel, processor::setAfterCommitChannel)
				.acceptIfNotNull(this.afterRollbackChannel, processor::setAfterRollbackChannel);

		if (this.beanFactory instanceof AutowireCapableBeanFactory) {
			((AutowireCapableBeanFactory) this.beanFactory).initializeBean(processor,
					getClass().getName() + "#" + TransactionSynchronizationFactoryBean.this.counter.incrementAndGet());
		}

		return new DefaultTransactionSynchronizationFactory(processor);
	}

	@Override
	public Class<?> getObjectType() {
		return DefaultTransactionSynchronizationFactory.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}

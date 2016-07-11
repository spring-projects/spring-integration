/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.handler.advice;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * The {@link MethodInterceptor} implementation for the
 * <a href="http://www.eaipatterns.com/IdempotentReceiver.html">Idempotent Receiver</a>
 * E.I. Pattern.
 * <p>
 * This {@link MethodInterceptor} works like a {@code MessageFilter} if {@link #discardChannel}
 * is provided or {@link #throwExceptionOnRejection} is set to {@code true}.
 * However if those properties aren't provided, this interceptor will create an new {@link Message}
 * with a {@link IntegrationMessageHeaderAccessor#DUPLICATE_MESSAGE} header when the
 * {@code requestMessage} isn't accepted by {@link MessageSelector}.
 * <p>
 * The {@code idempotent filtering} logic depends on the provided {@link MessageSelector}.
  * <p>
 * This class is designed to be used only for the {@link MessageHandler#handleMessage},
 * method.
 *
 * @author Artem Bilan
 * @since 4.1
 * @see org.springframework.integration.selector.MetadataStoreSelector
 * @see org.springframework.integration.config.IdempotentReceiverAutoProxyCreatorInitializer
 */
public class IdempotentReceiverInterceptor extends AbstractHandleMessageAdvice implements BeanFactoryAware {

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private final MessageSelector messageSelector;

	private volatile MessageChannel discardChannel;

	private volatile boolean throwExceptionOnRejection;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private volatile boolean messageBuilderFactorySet;

	private BeanFactory beanFactory;

	public IdempotentReceiverInterceptor(MessageSelector messageSelector) {
		Assert.notNull(messageSelector);
		this.messageSelector = messageSelector;
	}

	/**
	 * Specify the timeout value for sending to the discard channel.
	 * @param timeout the timeout in milliseconds
	 */
	public void setTimeout(long timeout) {
		this.messagingTemplate.setSendTimeout(timeout);
	}

	/**
	 * Specify whether this interceptor should throw a
	 * {@link MessageRejectedException} when its selector does not accept a
	 * Message. The default value is <code>false</code> meaning that rejected
	 * Messages will be discarded or
	 * enriched with {@link IntegrationMessageHeaderAccessor#DUPLICATE_MESSAGE}
	 * header and returned as normal to the {@code invocation.proceed()}.
	 * Typically this value would <em>not</em> be <code>true</code> when
	 * a discard channel is provided, but if it is, it will cause the
	 * exception to be thrown <em>after</em>
	 * the Message is sent to the discard channel,
	 * @param throwExceptionOnRejection true if an exception should be thrown.
	 * @see #setDiscardChannel(MessageChannel)
	 */
	public void setThrowExceptionOnRejection(boolean throwExceptionOnRejection) {
		this.throwExceptionOnRejection = throwExceptionOnRejection;
	}

	/**
	 * Specify a channel where rejected Messages should be sent. If the discard
	 * channel is null (the default), duplicate Messages will be enriched with
	 * {@link IntegrationMessageHeaderAccessor#DUPLICATE_MESSAGE} header
	 * and returned as normal to the {@code invocation.proceed()}. However,
	 * the 'throwExceptionOnRejection' flag determines whether rejected Messages
	 * trigger an exception. That value is evaluated regardless of the presence
	 * of a discard channel.
	 * <p>
	 * If there is needed just silently 'drop' rejected messages configure the
	 * {@link #discardChannel} to the {@code nullChannel}.
	 * @param discardChannel The discard channel.
	 * @see #setThrowExceptionOnRejection(boolean)
	 */
	public void setDiscardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (!this.messageBuilderFactorySet) {
			if (this.beanFactory != null) {
				this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
			}
			this.messageBuilderFactorySet = true;
		}
		return this.messageBuilderFactory;
	}

	@Override
	protected Object doInvoke(MethodInvocation invocation, Message<?> message) throws Throwable {
		boolean accept = this.messageSelector.accept(message);
		if (!accept) {
			boolean discarded = false;
			if (this.discardChannel != null) {
				this.messagingTemplate.send(this.discardChannel, message);
				discarded = true;
			}
			if (this.throwExceptionOnRejection) {
				throw new MessageRejectedException(message, "IdempotentReceiver '" + this
						+ "' rejected duplicate Message: " + message);
			}

			if (!discarded) {
				invocation.getArguments()[0] = getMessageBuilderFactory().fromMessage(message)
						.setHeader(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, true).build();
			}
			else {
				return null;
			}
		}
		return invocation.proceed();
	}

}

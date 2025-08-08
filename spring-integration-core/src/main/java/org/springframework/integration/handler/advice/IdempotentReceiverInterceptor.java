/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.handler.advice;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * The {@link org.aopalliance.intercept.MethodInterceptor} implementation for the
 * <a href="https://www.enterpriseintegrationpatterns.com/IdempotentReceiver.html">Idempotent Receiver</a>
 * E.I. Pattern.
 * <p>
 * This {@link org.aopalliance.intercept.MethodInterceptor} works like a
 * {@code MessageFilter} if {@link #discardChannel}
 * is provided or {@link #throwExceptionOnRejection} is set to {@code true}.
 * However if those properties aren't provided, this interceptor will create an new {@link Message}
 * with a {@link IntegrationMessageHeaderAccessor#DUPLICATE_MESSAGE} header when the
 * {@code requestMessage} isn't accepted by {@link MessageSelector}.
 * <p>
 * The {@code idempotent filtering} logic depends on the provided {@link MessageSelector}.
 * <p>
 * This class is designed to be used only for the
 * {@link org.springframework.messaging.MessageHandler#handleMessage},
 * method.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.1
 *
 * @see org.springframework.integration.selector.MetadataStoreSelector
 * @see org.springframework.integration.config.IdempotentReceiverAutoProxyCreatorInitializer
 */
public class IdempotentReceiverInterceptor extends AbstractHandleMessageAdvice {

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private final MessageSelector messageSelector;

	private MessageChannel discardChannel;

	private String discardChannelName;

	private boolean throwExceptionOnRejection;

	public IdempotentReceiverInterceptor(MessageSelector messageSelector) {
		Assert.notNull(messageSelector, "'messageSelector' must not be null");
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

	/**
	 * Specify a channel name where rejected Messages should be sent. If the discard
	 * channel is null (the default), duplicate Messages will be enriched with
	 * {@link IntegrationMessageHeaderAccessor#DUPLICATE_MESSAGE} header
	 * and returned as normal to the {@code invocation.proceed()}. However,
	 * the 'throwExceptionOnRejection' flag determines whether rejected Messages
	 * trigger an exception. That value is evaluated regardless of the presence
	 * of a discard channel.
	 * <p>
	 * If there is needed just silently 'drop' rejected messages configure the
	 * {@link #discardChannel} to the {@code nullChannel}.
	 * <p>
	 * Only applies if a {@link #setDiscardChannel(MessageChannel) discardChannel}
	 * is not provided.
	 * @param discardChannelName The discard channel name.
	 * @since 5.0.1
	 * @see #setThrowExceptionOnRejection(boolean)
	 */
	public void setDiscardChannelName(String discardChannelName) {
		this.discardChannelName = discardChannelName;
	}

	@Override
	public String getComponentType() {
		return "idempotent-receiver-interceptor";
	}

	@Override
	protected Object doInvoke(MethodInvocation invocation, Message<?> message) throws Throwable {
		boolean accept = this.messageSelector.accept(message);
		if (!accept) {
			boolean discarded = false;
			MessageChannel theDiscardChannel = obtainDiscardChannel();
			if (theDiscardChannel != null) {
				this.messagingTemplate.send(theDiscardChannel, message);
				discarded = true;
			}
			if (this.throwExceptionOnRejection) {
				throw new MessageRejectedException(message, "IdempotentReceiver '" + this
						+ "' rejected duplicate Message: " + message);
			}

			if (!discarded) {
				invocation.getArguments()[0] =
						getMessageBuilderFactory()
								.fromMessage(message)
								.setHeader(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, true)
								.build();
			}
			else {
				return null;
			}
		}
		return invocation.proceed();
	}

	private MessageChannel obtainDiscardChannel() {
		if (this.discardChannel == null && this.discardChannelName != null) {
			this.discardChannel = getChannelResolver().resolveDestination(this.discardChannelName);
		}
		return this.discardChannel;
	}

}

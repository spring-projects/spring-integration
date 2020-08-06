/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.filter;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.Lifecycle;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.handler.AbstractReplyProducingPostProcessingMessageHandler;
import org.springframework.integration.handler.DiscardingMessageHandler;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * Message Handler that delegates to a {@link MessageSelector}. If and only if
 * the selector {@link MessageSelector#accept(Message) accepts} the Message, it
 * will be passed to this filter's output channel. Otherwise the message will
 * either be silently dropped (the default) or will trigger the throwing of a
 * {@link MessageRejectedException} depending on the value of its
 * {@link #throwExceptionOnRejection} property. If a discard channel is
 * provided, the rejected Messages will be sent to that channel.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author David Liu
 */
public class MessageFilter extends AbstractReplyProducingPostProcessingMessageHandler
		implements DiscardingMessageHandler, ManageableLifecycle {

	private final MessageSelector selector;

	private boolean throwExceptionOnRejection;

	private MessageChannel discardChannel;

	private String discardChannelName;

	/**
	 * Create a MessageFilter that will delegate to the given {@link MessageSelector}.
	 * @param selector The message selector.
	 */
	public MessageFilter(MessageSelector selector) {
		Assert.notNull(selector, "selector must not be null");
		this.selector = selector;
	}


	/**
	 * Specify whether this filter should throw a
	 * {@link MessageRejectedException} when its selector does not accept a
	 * Message. The default value is <code>false</code> meaning that rejected
	 * Messages will be quietly dropped or sent to the discard channel if
	 * available. Typically this value would not be <code>true</code> when
	 * a discard channel is provided, but if so, it will still apply
	 * (in such a case, the Message will be sent to the discard channel,
	 * and <em>then</em> the exception will be thrown).
	 * @param throwExceptionOnRejection true if an exception should be thrown.
	 * @see #setDiscardChannel(MessageChannel)
	 */
	public void setThrowExceptionOnRejection(boolean throwExceptionOnRejection) {
		this.throwExceptionOnRejection = throwExceptionOnRejection;
	}

	/**
	 * Specify a channel where rejected Messages should be sent. If the discard
	 * channel is null (the default), rejected Messages will be dropped. However,
	 * the 'throwExceptionOnRejection' flag determines whether rejected Messages
	 * trigger an exception. That value is evaluated regardless of the presence
	 * of a discard channel.
	 * @param discardChannel The discard channel.
	 * @see #setThrowExceptionOnRejection(boolean)
	 */
	public void setDiscardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
	}

	public void setDiscardChannelName(String discardChannelName) {
		Assert.hasText(discardChannelName, "'discardChannelName' must not be empty");
		this.discardChannelName = discardChannelName;
	}

	/**
	 * Set to 'true' if you wish the discard processing to occur within any
	 * request handler advice applied to this filter. Also applies to
	 * throwing an exception on rejection. Default: true.
	 * @param discardWithinAdvice true to discard within the advice.
	 */
	public void setDiscardWithinAdvice(boolean discardWithinAdvice) {
		this.setPostProcessWithinAdvice(discardWithinAdvice);
	}

	@Override
	public MessageChannel getDiscardChannel() {
		String channelName = this.discardChannelName;
		if (channelName != null) {
			this.discardChannel = getChannelResolver().resolveDestination(channelName);
			this.discardChannelName = null;
		}
		return this.discardChannel;
	}


	@Override
	public String getComponentType() {
		return "filter";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.filter;
	}

	@Override
	protected void doInit() {
		Assert.state(!(this.discardChannelName != null && this.discardChannel != null),
				"'discardChannelName' and 'discardChannel' are mutually exclusive.");
		if (this.selector instanceof AbstractMessageProcessingSelector) {
			ConversionService conversionService = getConversionService();
			if (conversionService != null) {
				((AbstractMessageProcessingSelector) this.selector).setConversionService(conversionService);
			}
		}
		BeanFactory beanFactory = getBeanFactory();
		if (this.selector instanceof BeanFactoryAware && beanFactory != null) {
			((BeanFactoryAware) this.selector).setBeanFactory(beanFactory);
		}
	}

	@Override
	public void start() {
		if (this.selector instanceof Lifecycle) {
			((Lifecycle) this.selector).start();
		}
	}

	@Override
	public void stop() {
		if (this.selector instanceof Lifecycle) {
			((Lifecycle) this.selector).stop();
		}
	}

	@Override
	public boolean isRunning() {
		return !(this.selector instanceof Lifecycle) || ((Lifecycle) this.selector).isRunning();
	}

	@Override
	protected Object doHandleRequestMessage(Message<?> message) {
		if (this.selector.accept(message)) {
			return message;
		}
		else {
			return null;
		}
	}

	@Override
	public Object postProcess(Message<?> message, Object result) {
		if (result == null) {
			MessageChannel channel = getDiscardChannel();
			if (channel != null) {
				this.messagingTemplate.send(channel, message);
			}
			if (this.throwExceptionOnRejection) {
				throw new MessageRejectedException(message, "message has been rejected in filter: " + this);
			}
		}
		return result;
	}

	@Override
	protected boolean shouldCopyRequestHeaders() {
		return false;
	}

}

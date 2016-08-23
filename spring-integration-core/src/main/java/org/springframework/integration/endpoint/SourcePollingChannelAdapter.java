/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.util.Collection;

import org.aopalliance.aop.Advice;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.context.Lifecycle;
import org.springframework.integration.aop.AbstractMessageSourceAdvice;
import org.springframework.integration.context.ExpressionCapable;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.management.TrackableComponent;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A Channel Adapter implementation for connecting a
 * {@link MessageSource} to a {@link MessageChannel}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class SourcePollingChannelAdapter extends AbstractPollingEndpoint
		implements TrackableComponent {

	private volatile MessageSource<?> source;

	private volatile MessageChannel outputChannel;

	private volatile String outputChannelName;

	private volatile boolean shouldTrack;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	/**
	 * Specify the source to be polled for Messages.
	 *
	 * @param source The message source.
	 */
	public void setSource(MessageSource<?> source) {
		this.source = source;
		if (source instanceof ExpressionCapable) {
			setPrimaryExpression(((ExpressionCapable) source).getExpression());
		}
	}

	/**
	 * Specify the {@link MessageChannel} where Messages should be sent.
	 *
	 * @param outputChannel The output channel.
	 */
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Return this endpoint's source.
	 * @return the source.
	 * @since 4.3
	 */
	public MessageSource<?> getMessageSource() {
		return this.source;
	}

	public void setOutputChannelName(String outputChannelName) {
		Assert.hasText(outputChannelName, "'outputChannelName' must not be empty");
		this.outputChannelName = outputChannelName;
	}

	/**
	 * Specify the maximum time to wait for a Message to be sent to the
	 * output channel.
	 *
	 * @param sendTimeout The send timeout.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	/**
	 * Specify whether this component should be tracked in the Message History.
	 *
	 * @param shouldTrack true if the component should be tracked.
	 */
	@Override
	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
	}

	@Override
	public String getComponentType() {
		return (this.source instanceof NamedComponent) ?
				((NamedComponent) this.source).getComponentType() : "inbound-channel-adapter";
	}

	@Override
	protected boolean isReceiveOnlyAdvice(Advice advice) {
		return advice instanceof AbstractMessageSourceAdvice;
	}

	@Override
	protected void applyReceiveOnlyAdviceChain(Collection<Advice> chain) {
		if (AopUtils.isAopProxy(this.source)) {
			for (Advice advice : chain) {
				NameMatchMethodPointcutAdvisor sourceAdvice = new NameMatchMethodPointcutAdvisor(advice);
				sourceAdvice.addMethodName("receive");
				((Advised) this.source).addAdvice(advice);
			}
		}
		else {
			ProxyFactory proxyFactory = new ProxyFactory(this.source);
			for (Advice advice : chain) {
				proxyFactory.addAdvice(advice);
			}
			this.source = (MessageSource<?>) proxyFactory.getProxy(getBeanClassLoader());
		}
	}

	@Override
	protected void doStart() {
		if (this.source instanceof Lifecycle) {
			((Lifecycle) this.source).start();
		}
		super.doStart();
	}


	@Override
	protected void doStop() {
		if (this.source instanceof Lifecycle) {
			((Lifecycle) this.source).stop();
		}
		super.doStop();
	}


	@Override
	protected void onInit() {
		Assert.notNull(this.source, "source must not be null");
		Assert.state((this.outputChannelName == null && this.outputChannel != null)
				|| (this.outputChannelName != null && this.outputChannel == null),
				"One and only one of 'outputChannelName' or 'outputChannel' is required.");
		super.onInit();
		if (this.getBeanFactory() != null) {
			this.messagingTemplate.setBeanFactory(this.getBeanFactory());
		}
	}

	public MessageChannel getOutputChannel() {
		if (this.outputChannelName != null) {
			synchronized (this) {
				if (this.outputChannelName != null) {
					this.outputChannel = getChannelResolver().resolveDestination(this.outputChannelName);
					this.outputChannelName = null;
				}
			}
		}
		return this.outputChannel;
	}

	@Override
	protected void handleMessage(Message<?> message) {
		if (this.shouldTrack) {
			message = MessageHistory.write(message, this, this.getMessageBuilderFactory());
		}
		try {
			this.messagingTemplate.send(getOutputChannel(), message);
		}
		catch (Exception e) {
			if (e instanceof MessagingException) {
				throw (MessagingException) e;
			}
			else {
				throw new MessagingException(message, "Failed to send Message", e);
			}
		}
	}

	@Override
	protected Message<?> receiveMessage() {
		return this.source.receive();
	}

	@Override
	protected Object getResourceToBind() {
		return this.source;
	}

	@Override
	protected String getResourceKey() {
		return IntegrationResourceHolder.MESSAGE_SOURCE;
	}

}

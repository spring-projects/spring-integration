/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.handler.MessageHandlerNotRunningException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.integration.scheduling.Subscription;
import org.springframework.util.Assert;

/**
 * Base class for {@link MessageEndpoint} implementations to which Messages may be sent.
 * 
 * @author Mark Fisher
 */
public class TargetEndpoint extends AbstractEndpoint
		implements MessageTarget, MessageConsumingEndpoint, ChannelRegistryAware, InitializingBean, Lifecycle {

	private volatile MessageTarget target;

	private volatile Subscription subscription;

	private volatile MessageSelector selector;

	private volatile ChannelRegistry channelRegistry;

	private volatile boolean initialized;

	private volatile boolean running;


	public TargetEndpoint() {
	}

	public TargetEndpoint(MessageTarget target) {
		Assert.notNull(target, "target must not be null");
		this.target = target;
	}


	public MessageTarget getTarget() {
		return this.target;
	}

	public void setTarget(MessageTarget target) {
		Assert.notNull(target, "target must not be null");
		this.target = target;
	}

	public void setMessageSelector(MessageSelector selector) {
		this.selector = selector;
	}

	public Subscription getSubscription() {
		return this.subscription;
	}

	public void setSubscription(Subscription subscription) {
		this.subscription = subscription;
	}

	/**
	 * Set the channel registry to use for looking up channels by name.
	 */
	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	protected ChannelRegistry getChannelRegistry() {
		return this.channelRegistry;
	}

	public void afterPropertiesSet() {
		if (this.target instanceof ChannelRegistryAware && this.channelRegistry != null) {
			((ChannelRegistryAware) this.target).setChannelRegistry(this.channelRegistry);
		}
		this.initialized = true;
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		if (this.isRunning()) {
			return;
		}
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		this.running = true;
	}

	public void stop() {
		if (!this.isRunning()) {
			return;
		}
		if (this.target instanceof DisposableBean) {
			try {
				((DisposableBean) this.target).destroy();
			}
			catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn("exception occurred when destroying target", e);
				}
			}
		}
		this.running = false;
	}

	@Override
	protected final boolean doInvoke(Message<?> message) {
		if (logger.isDebugEnabled()) {
			logger.debug("endpoint '" + this + "' handling message: " + message);
		}
		if (!this.isRunning()) {
			throw new MessageHandlerNotRunningException(message);
		}
		if (this.selector != null && !this.selector.accept(message)) {
			return false;
		}
		return this.target.send(message);
	}

	@Override
	protected final boolean supports(Message<?> message) {
		return true;
	}

	public boolean send(Message<?> message) {
		return this.invoke(message);
	}

}

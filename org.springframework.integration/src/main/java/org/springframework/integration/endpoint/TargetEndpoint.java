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

import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.BlockingTarget;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.util.Assert;

/**
 * Base class for {@link MessageEndpoint} implementations to which Messages may be sent.
 * 
 * @author Mark Fisher
 */
public class TargetEndpoint extends AbstractEndpoint {

	private volatile MessageTarget target;

	private volatile MessageSelector selector;

	private volatile long receiveTimeout = 5000;

	private volatile long sendTimeout = 0;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


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

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	protected void initialize() {
		synchronized (this.initializationMonitor) {
	        if (this.initialized) {
	        	return;
	        }
	        if (this.target instanceof ChannelRegistryAware && this.getChannelRegistry() != null) {
	        	((ChannelRegistryAware) this.target).setChannelRegistry(this.getChannelRegistry());
	        }
	        this.initialized = true;
		}
	}

	@Override
	protected boolean supports(Message<?> message) {
		if (this.selector != null && !this.selector.accept(message)) {
			if (logger.isDebugEnabled()) {
				logger.debug("selector for endpoint '" + this + "' rejected message: " + message);
			}
			return false;
		}
		return true;
	}

	@Override
	protected final boolean handleMessage(Message<?> message) {
		return (this.sendTimeout >= 0 && this.target instanceof BlockingTarget) ?
				((BlockingTarget) this.target).send(message) : this.target.send(message);
	}

	public final boolean poll() {
		MessageChannel channel = this.getInputChannel();
		if (channel != null) {
			Message<?> receivedMessage = channel.receive(this.receiveTimeout);
			if (receivedMessage != null) {
				return this.handleMessage(receivedMessage);
			}
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("TargetEndpoint unable to resolve channel '" + this.getInputChannelName() + "'");
		}
		return false;
	}

}

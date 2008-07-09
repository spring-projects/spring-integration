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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandlerNotRunningException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.scheduling.Schedule;

/**
 * Base class for {@link MessageEndpoint} implementations.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractEndpoint implements MessageEndpoint, BeanNameAware, Lifecycle {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile String name;

	private volatile String inputChannelName;

	private MessageChannel inputChannel;

	private volatile String outputChannelName;

	private MessageChannel outputChannel;

	private final List<EndpointInterceptor> interceptors = new ArrayList<EndpointInterceptor>();

	private volatile Schedule schedule;

	private volatile EndpointTrigger trigger;

	private volatile ChannelRegistry channelRegistry;

	private volatile boolean autoStartup = true;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setBeanName(String beanName) {
		this.setName(beanName);
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	public void setTrigger(EndpointTrigger trigger) {
		this.trigger = trigger;
	}

	public EndpointTrigger getTrigger() {
		return this.trigger;
	}

	public void setInputChannelName(String inputChannelName) {
		this.inputChannelName = inputChannelName;
	}

	public String getInputChannelName() {
		return this.inputChannelName;
	}

	public void setInputChannel(MessageChannel channel) {
		this.inputChannel = channel; 
		this.inputChannelName = channel.getName();
	}

	public MessageChannel getInputChannel() {
		if (this.inputChannel == null &&
				(this.inputChannelName != null && this.channelRegistry != null)) {
			this.inputChannel = this.channelRegistry.lookupChannel(this.inputChannelName);
		}
		return this.inputChannel;
	}

	/**
	 * Set the name of the channel to which this endpoint should send reply
	 * messages.
	 */
	public void setOutputChannelName(String outputChannelName) {
		this.outputChannelName = outputChannelName;
	}

	public String getOutputChannelName() {
		return this.outputChannelName;
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
		this.outputChannelName = outputChannel.getName();
	}

	public MessageChannel getOutputChannel() {
		if (this.outputChannel == null &&
				(this.outputChannelName != null && this.channelRegistry != null)) {
			this.outputChannel = this.channelRegistry.lookupChannel(this.outputChannelName);
		}
		return this.outputChannel;
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

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void addInterceptor(EndpointInterceptor interceptor) {
		this.interceptors.add(interceptor);
	}

	public void setInterceptors(List<EndpointInterceptor> interceptors) {
		this.interceptors.clear();
		for (EndpointInterceptor interceptor : interceptors) {
			this.addInterceptor(interceptor);
		}
	}

	public List<EndpointInterceptor> getInterceptors() {
		return this.interceptors;
	}

	public String toString() {
		return (this.name != null) ? this.name : super.toString();
	}

	protected void initialize() {
	}

	public boolean isRunning() {
		return this.running;
	}

	public void afterPropertiesSet() {
		if (this.autoStartup) {
			this.start();
		}
		else {
			this.initialize();
		}
	}

	public void start() {
		this.initialize();
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				return;
			}
			this.running = true;
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				return;
			}
			this.running = false;
		}
	}

	public final boolean send(Message<?> message) {
		return this.send(message, 0);
	}

	private boolean send(final Message<?> message, final int index) {
		boolean result = false;
		if (index == 0) {
			for (EndpointInterceptor interceptor : interceptors) {
				if (!interceptor.preSend(message)) {
					return false;
				}
			}
		}
		if (index == interceptors.size()) {
			return this.doSend(message);
		}
		EndpointInterceptor nextInterceptor = interceptors.get(index);
		result = nextInterceptor.aroundSend(message, new MessageTarget() {
			@SuppressWarnings("unchecked")
			public boolean send(Message message) {
				return AbstractEndpoint.this.send(message, index + 1);
			}
		});
		if (index == this.interceptors.size()) {
			for (EndpointInterceptor interceptor : this.interceptors) {
				interceptor.postSend(message, result);
			}
		}
		return result;
	}

	private boolean doSend(Message<?> message) {
		if (message == null || message.getPayload() == null) {
			throw new IllegalArgumentException("Message and its payload must not be null.");
		}
		if (logger.isDebugEnabled()) {
			logger.debug("endpoint '" + this + "' handling message: " + message);
		}
		if (!this.isRunning()) {
			throw new MessageHandlerNotRunningException(message);
		}
		if (message.getPayload() instanceof EndpointVisitor) {
			((EndpointVisitor) message.getPayload()).visitEndpoint(this);
			return true;
		}
		if (!this.supports(message)) {
			throw new MessageRejectedException(message, "unsupported message");
		}
		return this.handleMessage(message);
	}

	protected abstract boolean supports(Message<?> message);

	protected abstract boolean handleMessage(Message<?> message);

}

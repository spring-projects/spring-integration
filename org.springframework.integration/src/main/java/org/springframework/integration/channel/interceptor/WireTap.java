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

package org.springframework.integration.channel.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.BlockingTarget;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.util.Assert;

/**
 * A {@link ChannelInterceptor} that publishes a copy of the intercepted message
 * to a secondary target while still sending the original message to the main channel.
 * 
 * @author Mark Fisher
 */
public class WireTap extends ChannelInterceptorAdapter implements Lifecycle {

	private static final Log logger = LogFactory.getLog(WireTap.class);

	private final MessageTarget target;

	private volatile long timeout = 0;

	private final MessageSelector selector;

	private volatile boolean running = true;


	/**
	 * Create a new wire tap with <em>no</em> {@link MessageSelector}.
	 * 
	 * @param target the MessageTarget to which intercepted messages will be sent
	 */
	public WireTap(MessageTarget target) {
		this(target, null);
	}

	/**
	 * Create a new wire tap with the provided {@link MessageSelector}.
	 * 
	 * @param target the target to which intercepted messages will be sent
	 * @param selector the selector that must accept a message for it to
	 * be sent to the intercepting target
	 */
	public WireTap(MessageTarget target, MessageSelector selector) {
		Assert.notNull(target, "target must not be null");
		this.target = target;
		this.selector = selector;
	}


	/**
	 * Specify the timeout value for sending to the intercepting target. Note
	 * that this value will only apply if the target is a {@link BlockingTarget}.
	 * The default value is 0.
	 * 
	 * @param timeout the timeout in milliseconds
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Check whether the wire tap is currently running.
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Restart the wire tap if it has been stopped. It is running by default.
	 */
	public void start() {
		this.running = true;
	}

	/**
	 * Stop the wire tap. To restart, invoke {@link #start()}.
	 */
	public void stop() {
		this.running = false;
	}

	/**
	 * Intercept the Message and, <em>if accepted</em> by the {@link MessageSelector},
	 * send it to the secondary target. If this wire tap's {@link MessageSelector} is
	 * <code>null</code>, it will accept all messages.
	 */
	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		if (this.running && (this.selector == null || this.selector.accept(message))) {
			boolean sent = (this.target instanceof BlockingTarget)
					? ((BlockingTarget) this.target).send(message, this.timeout)
					: this.target.send(message);
			if (!sent && logger.isWarnEnabled()) {
				logger.warn("failed to send message to WireTap target '" + this.target + "'");
			}
		}
		return message;
	}

}

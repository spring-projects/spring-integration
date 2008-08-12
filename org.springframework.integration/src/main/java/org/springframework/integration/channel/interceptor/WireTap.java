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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

	private final Log logger = LogFactory.getLog(this.getClass());

	private final MessageTarget target;

	private final List<MessageSelector> selectors = new CopyOnWriteArrayList<MessageSelector>();

	private volatile boolean running = true;


	/**
	 * Create a new wire tap with <em>no</em> {@link MessageSelector MessageSelectors}.
	 * 
	 * @param target the MessageTarget to which intercepted messages will be sent
	 */
	public WireTap(MessageTarget target) {
		Assert.notNull(target, "target must not be null");
		this.target = target;
	}

	/**
	 * Create a new wire tap with {@link MessageSelector MessageSelectors}.
	 * 
	 * @param target the target to which intercepted messages will be sent
	 * @param selectors the list of selectors that must accept a message for it to
	 * be sent to the intercepting target
	 */
	public WireTap(MessageTarget target, List<MessageSelector> selectors) {
		this(target);
		if (selectors != null) {
			this.selectors.addAll(selectors);
		}
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

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		if (this.running && this.selectorsAccept(message)) {
			boolean sent = (this.target instanceof BlockingTarget) ?
					((BlockingTarget) this.target).send(message, 0) : this.target.send(message);
			if (!sent && logger.isWarnEnabled()) {
					logger.warn("failed to send message to WireTap target '" + this.target + "'");
			}
		}
		return message;
	}

	/**
	 * If this wire tap has any {@link MessageSelector MessageSelectors}, check
	 * whether they accept the current message. If any of them do not accept it,
	 * the message will <em>not</em> be sent to the intercepting target.
	 */
	private boolean selectorsAccept(Message<?> message) {
		for (MessageSelector selector : this.selectors) {
			if (!selector.accept(message)) {
				return false;
			}
		}
		return true;
	}

}

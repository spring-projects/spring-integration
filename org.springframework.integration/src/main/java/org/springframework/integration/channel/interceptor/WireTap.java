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
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.util.Assert;

/**
 * A {@link ChannelInterceptor} that publishes a copy of the intercepted message
 * to a secondary channel while still sending the original message to the main channel.
 * 
 * @author Mark Fisher
 */
public class WireTap extends ChannelInterceptorAdapter implements Lifecycle {

	/** key for the attribute containing the original Message's id */
	public final static String ORIGINAL_MESSAGE_ID_KEY = "_wireTap.originalMessageId";


	private final Log logger = LogFactory.getLog(this.getClass());

	private final MessageChannel secondaryChannel;

	private final List<MessageSelector> selectors = new CopyOnWriteArrayList<MessageSelector>();

	private volatile boolean running = true;


	/**
	 * Create a new wire tap with <em>no</em> {@link MessageSelector MessageSelectors}.
	 * 
	 * @param secondaryChannel the channel to which duplicate messages will be sent
	 */
	public WireTap(MessageChannel secondaryChannel) {
		Assert.notNull(secondaryChannel, "'secondaryChannel' must not be null");
		this.secondaryChannel = secondaryChannel;
	}

	/**
	 * Create a new wire tap with {@link MessageSelector MessageSelectors}.
	 * 
	 * @param secondaryChannel the channel to which duplicate messages will be sent
	 * @param selectors the list of selectors that must accept a message for it to
	 * be sent to the secondary channel
	 */
	public WireTap(MessageChannel secondaryChannel, List<MessageSelector> selectors) {
		this(secondaryChannel);
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
			Message<?> duplicate = new GenericMessage<Object>(message.getPayload(), message.getHeader());
			duplicate.getHeader().setAttribute(ORIGINAL_MESSAGE_ID_KEY, message.getId());
			if (!this.secondaryChannel.send(duplicate, 0)) {
				if (logger.isWarnEnabled()) {
					logger.warn("Failed to send message to secondary channel '" + this.secondaryChannel.getName()
							+ "'. Check its capacity and whether it has any subscribers.");
				}
			}
		}
		return message;
	}

	/**
	 * If this wire tap has any {@link MessageSelector MessageSelectors}, check
	 * whether they accept the current message. If any of them do not accept it,
	 * the message will <em>not</em> be sent to the secondary channel.
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

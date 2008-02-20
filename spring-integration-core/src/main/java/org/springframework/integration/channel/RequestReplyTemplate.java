/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.integration.handler.ReplyHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHeader;
import org.springframework.util.Assert;

/**
 * A template that facilitates the implementation of request-reply usage
 * scenarios above one-way {@link MessageChannel MessageChannels}.
 * 
 * @author Mark Fisher
 */
public class RequestReplyTemplate {

	private final MessageChannel requestChannel;

	private final ExecutorService executor;

	private volatile long defaultSendTimeout = -1;

	private volatile long defaultReceiveTimeout = -1;


	/**
	 * Create a RequestReplyTemplate.
	 * 
	 * @param requestChannel the channel to which request messages will be sent
	 * @param executor the executor that will handle asynchronous (non-blocking)
	 * requests
	 */
	public RequestReplyTemplate(MessageChannel requestChannel, ExecutorService executor) {
		Assert.notNull(requestChannel, "'requestChannel' must not be null");
		Assert.notNull(executor, "'executor' must not be null");
		this.requestChannel = requestChannel;
		this.executor = executor;
	}

	/**
	 * Create a RequestReplyTemplate with a default single-threaded executor.
	 * 
	 * @param requestChannel the channel to which request messages will be sent
	 */
	public RequestReplyTemplate(MessageChannel requestChannel) {
		this(requestChannel, Executors.newSingleThreadExecutor());
	}


	/**
	 * Set the default timeout value for sending request messages. If not
	 * explicitly configured, the default will be an indefinite timeout.
	 * 
	 * @param defaultSendTimeout the timeout value in milliseconds
	 */
	public void setDefaultSendTimeout(long defaultSendTimeout) {
		this.defaultSendTimeout = defaultSendTimeout;
	}

	/**
	 * Set the default timeout value for receiving reply messages. If not
	 * explicitly configured, the default will be an indefinite timeout.
	 * 
	 * @param defaultReceiveTimeout the timeout value in milliseconds
	 */
	public void setDefaultReceiveTimeout(long defaultReceiveTimeout) {
		this.defaultReceiveTimeout = defaultReceiveTimeout;
	}

	/**
	 * Send a request message and wait for a reply message using the provided
	 * timeout values.
	 * 
	 * @param requestMessage the request message to send
	 * @param sendTimeout the timeout value for sending the request message
	 * @param receiveTimeout the timeout value for receiving a reply message
	 * 
	 * @return the reply message or <code>null</code>
	 */
	public Message<?> request(Message<?> requestMessage, long sendTimeout, long receiveTimeout) {
		SimpleChannel replyChannel = new SimpleChannel(0);
		requestMessage.getHeader().setReturnAddress(replyChannel);
		this.requestChannel.send(requestMessage, sendTimeout);
		return replyChannel.receive(receiveTimeout);
	}

	/**
	 * Send a request message and wait for a reply message using the default
	 * timeout values.
	 * 
	 * @param requestMessage the request message to send
	 * 
	 * @return the reply message or <code>null</code>
	 */
	public Message<?> request(Message<?> requestMessage) {
		return this.request(requestMessage, this.defaultSendTimeout, this.defaultReceiveTimeout);
	}

	/**
	 * Send a request message asynchronously to be handled by the provided
	 * {@link ReplyHandler}. The provided values will be used for the send
	 * and receive timeouts.
	 * 
	 * @param requestMessage the request message to send
	 * @param replyHandler the callback that will handle a reply message
	 * @param sendTimeout the timeout value for sending the request message
	 * @param receiveTimeout the timeout value for receiving the reply message
	 */
	public void request(final Message<?> requestMessage, final ReplyHandler replyHandler, final long sendTimeout,
			final long receiveTimeout) {
		final MessageHeader header = requestMessage.getHeader();
		this.executor.submit(new Runnable() {
			public void run() {
				Message<?> reply = request(requestMessage, sendTimeout, receiveTimeout);
				replyHandler.handle(reply, header);
			}
		});
	}

	/**
	 * Send a request message asynchronously to be handled by the provided
	 * {@link ReplyHandler}. Default values will be used for the send and
	 * receive timeouts.
	 * 
	 * @param requestMessage the request message to send
	 * @param replyHandler the callback that will handle a reply message
	 */
	public void request(final Message<?> requestMessage, final ReplyHandler replyHandler) {
		this.request(requestMessage, replyHandler, this.defaultSendTimeout, this.defaultReceiveTimeout);
	}

}

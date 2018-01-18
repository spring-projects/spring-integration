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

package org.springframework.integration.zeromq.outbound;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.zeromq.SocketType;
import org.zeromq.ZAuth;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * ZMQ implementation.
 *
 * @author Subhobrata Dey
 * @since 5.1
 *
 */
public class ZeromqMessageHandler extends AbstractZeromqMessageHandler
				implements ApplicationEventPublisherAware, Runnable {

	private final org.springframework.integration.zeromq.core.ZeromqClientFactory clientFactory;

	private volatile ZMQ.Context context;

	private volatile ZMQ.Socket client;

	private volatile ZMQ.Poller poller;

	private volatile ApplicationEventPublisher applicationEventPublisher;

	private volatile byte[] messagePayload;
	private Message<?> message;

	private final String credentialFilePrefix = "passwords";
	private final String credentialFileSuffix = "txt";

	/**
	 * Use this constructor for a single url (although it may be overridden
	 * if the server URI is provided by the {@link org.springframework.integration.zeromq.core.ZeromqClientFactory}).
	 * @param url the URL.
	 * @param clientId The client id.
	 * @param clientFactory The client factory.
	 */
	public ZeromqMessageHandler(String url, String clientId, org.springframework.integration.zeromq.core.ZeromqClientFactory clientFactory) {
		super(url, clientId);
		this.clientFactory = clientFactory;
	}

	/**
	 * Use this constructor if the server URI is provided by the {@link org.springframework.integration.zeromq.core.ZeromqClientFactory}.
	 * @param clientId The client id.
	 * @param clientFactory The client factory.
	 * @since 5.0.1
	 */
	public ZeromqMessageHandler(String clientId, org.springframework.integration.zeromq.core.ZeromqClientFactory clientFactory) {
		super(null, clientId);
		this.clientFactory = clientFactory;
	}

	/**
	 * Use this URL when you don't need additional {@link org.springframework.integration.zeromq.core.ZeromqClientFactory}.
	 * @param url The URL.
	 * @param clientId The client id.
	 */
	public ZeromqMessageHandler(String url, String clientId) {
		this(url, clientId, new org.springframework.integration.zeromq.core.DefaultZeromqClientFactory(SocketType.PUB.type()));
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.state(getConverter() instanceof org.springframework.integration.zeromq.support.ZeromqMessageConverter,
				"MessageConverter must be a ZeromqMessageConverter");
	}

	@Override
	protected void doStart() {
		checkConnection();
		this.poller = this.clientFactory.getPollerInstance(ZMQ.Poller.POLLOUT);
		ExecutorService executorService = Executors.newFixedThreadPool(this.clientFactory.getIoThreads());
		executorService.submit(this);
	}

	@Override
	protected void doStop() {
		try {
			ZMQ.Socket client = this.client;
			if (client != null) {
				if (getUrl() != null) {
					client.disconnect(getUrl());
				}
				else {
					client.disconnect(this.clientFactory.getServerURI());
				}
				client.close();
				this.poller.close();
				this.context.term();
				this.client = null;
			}
		}
		catch (ZMQException e) {
			logger.error("Failed to disconnect", e);
		}
	}

	private synchronized ZMQ.Socket checkConnection() throws ZMQException {
		if (this.client != null) {
			this.client.close();
			this.client = null;
		}
		if (this.client == null) {
			try {
				Assert.state(this.getUrl() != null || this.clientFactory.getServerURI() != null,
						"If no 'url' provided, clientFactory.getServerURIs() must not be null");
				this.context = this.clientFactory.getContext();
				ZMQ.Socket client = this.clientFactory.getClientInstance(getClientId(), this.getTopic());

				if (this.clientFactory.getUserName() != null && this.clientFactory.getPassword() != null) {
					ZAuth zAuth = this.clientFactory.getZAuth();
					zAuth.setVerbose(true);

					File file = File.createTempFile(this.credentialFilePrefix, this.credentialFileSuffix);
					String str = this.clientFactory.getUserName() + "=" + this.clientFactory.getPassword();
					BufferedWriter writer = new BufferedWriter(new FileWriter(file));
					writer.write(str);
					writer.close();

					zAuth.configurePlain("*", file.getAbsolutePath());
					client.setZAPDomain("global".getBytes(Charset.defaultCharset()));
					client.setPlainServer(true);
					file.deleteOnExit();
				}

				if (getUrl() != null) {
					client.bind(getUrl());
				}
				else {
					client.bind(this.clientFactory.getServerURI());
				}
				this.client = client;
				if (logger.isDebugEnabled()) {
					logger.debug("Client connected");
				}
			}
			catch (Exception e) {
				throw new MessagingException("Failed to connect", e);
			}
		}
		return this.client;
	}

	@Override
	public void publish(String topic, Object zmqMessage, Message<?> message) {
		try {
			Assert.isInstanceOf(byte[].class, zmqMessage);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if (topic != null || this.getTopic() != null) {
				baos.write(((topic != null ? topic : this.getTopic()) + " ").getBytes(Charset.defaultCharset()));
			}
			baos.write((byte[]) zmqMessage);
			this.messagePayload = baos.toByteArray();
			this.message = message;
		}
		catch (Exception e) {
			throw new MessageDeliveryException(message, "The '" + this + "' could not deliver message.", e);
		}
	}

	@Override
	public void run() {
		pollForMessages();
	}

	private void pollForMessages() {
		while (!Thread.currentThread().isInterrupted()) {
			this.poller.poll();
			if (this.poller.pollout(0)) {
				if (this.messagePayload != null) {
					this.client.send(this.messagePayload, 0);

					if (this.applicationEventPublisher != null) {
						this.applicationEventPublisher.publishEvent(
								new org.springframework.integration.zeromq.event.ZeromqMessageSentEvent(this, this.message, this.getTopic(),
										getClientId(), getClientType()));
					}
					this.messagePayload = null;
					this.message = null;
				}
			}
		}
	}
}

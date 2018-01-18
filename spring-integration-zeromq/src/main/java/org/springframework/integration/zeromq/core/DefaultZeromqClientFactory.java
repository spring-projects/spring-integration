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

package org.springframework.integration.zeromq.core;

import java.nio.charset.Charset;

import org.zeromq.SocketType;
import org.zeromq.ZAuth;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import org.springframework.util.Assert;

/**
 * Creates a default {@link ZeromqClientFactory} and a set of options as configured.
 *
 * @author Subhobrata Dey
 * @since 5.1
 *
 */
public class DefaultZeromqClientFactory implements ZeromqClientFactory {

	private volatile boolean cleanSession;

	private volatile String clientId;

	private volatile int clientType;

	private volatile int ioThreads = 1;

	private volatile ZMQ.Context context;
	private volatile ZContext zContext;

	private volatile ZMQ.Socket client;

	private volatile String password;

	private volatile String userName;

	private volatile String serverURI;

	private volatile ConsumerStopAction consumerStopAction = ConsumerStopAction.UNSUBSCRIBE_CLEAN;

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public void setClientType(int clientType) {
		this.clientType = clientType;
	}

	public void setIoThreads(int ioThreads) {
		this.ioThreads = ioThreads;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * Use this when using server instance.
	 * @param serverURI The URI.
	 * @since 5.0.1
	 */
	public void setServerURI(String serverURI) {
		Assert.notNull(serverURI, "'serverURI' must not be null.");
		this.serverURI = serverURI;
	}

	public DefaultZeromqClientFactory() {
		this.clientType = -1;
	}

	public DefaultZeromqClientFactory(int clientType) {
		this.clientType = clientType;
	}

	/**
	 * Get the consumer stop action.
	 * @return the consumer stop action.
	 * @since 5.0.1
	 */
	@Override
	public ConsumerStopAction getConsumerStopAction() {
		return this.consumerStopAction;
	}

	/**
	 * Set the consumer stop action. Determines whether we unsubscribe when the consumer stops.
	 * Default: {@link ConsumerStopAction#UNSUBSCRIBE_CLEAN}.
	 * @param consumerStopAction the consumer stop action.
	 * @since 5.0.1.
	 */
	public void setConsumerStopAction(ConsumerStopAction consumerStopAction) {
		this.consumerStopAction = consumerStopAction;
	}

	@Override
	public boolean cleanSession() {
		return this.cleanSession;
	}

	@Override
	public String getUserName() {
		return this.userName;
	}

	@Override
	public String getPassword() {
		return this.password;
	}

	@Override
	public ZMQ.Context getContext() {
		if (this.context == null) {
			this.context = ZMQ.context(this.ioThreads);
		}
		return this.context;
	}

	@Override
	public ZContext getZContext() {
		if (this.zContext == null) {
			this.zContext = new ZContext();
		}
		return this.zContext;
	}

	@Override
	public ZAuth getZAuth() {
		return new ZAuth(getZContext());
	}

	@Override
	public ZMQ.Socket getClientInstance(String clientId, String... topic) {
		Assert.isTrue(this.clientType >= 0,
				"Set Client Socket-Type using clientFactory.setClientType");

		if (this.getUserName() != null && this.getPassword() != null) {
			this.client = getZContext().createSocket(SocketType.type(this.clientType));
		}
		else {
			this.client = this.context.socket(SocketType.type(this.clientType));
		}

		this.client.setIdentity(clientId.getBytes(Charset.defaultCharset()));

		if (topic.length == 1 && topic[0] != null && this.clientType == SocketType.SUB.type()) {
			this.client.subscribe(topic[0].getBytes(Charset.defaultCharset()));
		}
		else if (this.clientType == SocketType.SUB.type()) {
			this.client.subscribe("".getBytes(Charset.defaultCharset()));
		}

		return this.client;
	}

	@Override
	public ZMQ.Poller getPollerInstance(int pollerType) {
		if (this.getUserName() != null && this.getPassword() != null) {
			ZMQ.Poller poller = getContext().poller(1);
			poller.register(this.client, pollerType);
			return poller;
		}
		else {
			ZMQ.Poller poller = getZContext().createPoller(1);
			poller.register(this.client, pollerType);
			return poller;
		}
	}

	@Override
	public String getServerURI() {
		return this.serverURI;
	}

	@Override
	public int getIoThreads() {
		return this.ioThreads;
	}

	@Override
	public String getClientId() {
		return this.clientId;
	}

	@Override
	public int getClientType() {
		return this.clientType;
	}
}

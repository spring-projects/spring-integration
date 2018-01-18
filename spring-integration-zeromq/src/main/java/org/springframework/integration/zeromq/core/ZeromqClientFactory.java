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

import org.zeromq.ZAuth;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * @author Subhobrata Dey
 * @since 5.1
 *
 */
public interface ZeromqClientFactory {

	/**
	 * Retrieve the username.
	 *
	 * @return The username to connect.
	 */
	String getUserName();

	/**
	 * Retrieve the password.
	 *
	 * @return The password to connect.
	 */
	String getPassword();

	ZMQ.Context getContext();

	ZContext getZContext();

	ZAuth getZAuth();

	/**
	 * Retrieve a client instance.
	 *
	 * @param clientId The client id.
	 * @param topic The topic.
	 * @return The client instance.
	 */
	ZMQ.Socket getClientInstance(String clientId, String... topic);

	/**
	 * Retrieve a poller instance.
	 *
	 * @param pollerType The pollerType for example POLLIN, POLLOUT
	 * @return The poller instance.
	 */
	ZMQ.Poller getPollerInstance(int pollerType);

	String getServerURI();

	boolean cleanSession();

	int getIoThreads();

	String getClientId();

	/**
	 * Get the client type.
	 * @return the client type, for example ZMQ.PUB, ZMQ.SUB, ZMQ.PUSH, ZMQ.PULL, ZMQ.REQ, ZMQ.REP
	 * @since 4.3
	 */
	int getClientType();

	/**
	 * Get the consumer stop action.
	 * @return the consumer stop action.
	 * @since 5.0.1
	 */
	ConsumerStopAction getConsumerStopAction();
}

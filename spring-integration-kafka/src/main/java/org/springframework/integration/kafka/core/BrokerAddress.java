/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.integration.kafka.core;

import org.springframework.util.StringUtils;

/**
 * Encapsulates the address of a Kafka broker.
 *
 * @author Marius Bogoevici
 */
public class BrokerAddress {

	public static final int DEFAULT_PORT = 9092;

	private final String host;

	private final int port;

	public BrokerAddress(String host, int port) {
		if (StringUtils.isEmpty(host)) {
			throw new IllegalArgumentException("Host cannot be empty");
		}
		this.host = host;
		this.port = port;
	}

	public BrokerAddress(String host) {
		this(host, DEFAULT_PORT);
	}

	public static BrokerAddress fromAddress(String address) {
		String[] split = address.split(":");
		if (split.length == 0 || split.length > 2) {
			throw new IllegalArgumentException("Expected format <host>[:<port>]");
		}
		if (split.length == 2) {
			return new BrokerAddress(split[0], Integer.parseInt(split[1]));
		}
		else {
			return new BrokerAddress(split[0]);
		}

	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	@Override
	public int hashCode() {
		return 31 * host.hashCode() + port;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		BrokerAddress brokerAddress = (BrokerAddress) o;

		if (port != brokerAddress.port) {
			return false;
		}
		if (!host.equals(brokerAddress.host)) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return host + ":" + port;
	}

}



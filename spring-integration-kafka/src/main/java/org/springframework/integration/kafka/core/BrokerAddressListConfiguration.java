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

import java.util.List;

/**
 * Kafka {@link Configuration} where the seed brokers are set up explicitly.
 *
 * @author Marius Bogoevici
 */
public class BrokerAddressListConfiguration extends AbstractConfiguration {

	private final List<BrokerAddress> brokerAddresses;

	public BrokerAddressListConfiguration(List<BrokerAddress> brokerAddresses) {
		this.brokerAddresses = brokerAddresses;
	}

	@Override
	protected List<BrokerAddress> doGetBrokerAddresses() {
		return brokerAddresses;
	}

}

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


package org.springframework.integration.kafka.rule;

import java.util.List;

import kafka.server.KafkaServer;
import org.I0Itec.zkclient.ZkClient;
import org.junit.rules.TestRule;

import org.springframework.integration.kafka.core.BrokerAddress;

/**
 * Common functionality for the Kafka JUnit rules
 *
 * @author Marius Bogoevici
 */
public interface KafkaRule extends TestRule {

	ZkClient getZkClient();

	BrokerAddress[] getBrokerAddresses();

	String getBrokersAsString();

	boolean isEmbedded();

	List<KafkaServer> getKafkaServers();
}

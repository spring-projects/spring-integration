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

package org.springframework.integration.jms;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.transport.vm.VMTransport;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Keeps an ActiveMQ {@link VMTransport} open for the duration of
 * all tests (avoids cycling the transport each time the last
 * connection is closed).
 *
 * @author Gary Russell
 *
 * @since 3.0
 *
 */
public abstract class ActiveMQMultiContextTests {

	public static final ActiveMQConnectionFactory amqFactory =
			new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");

	public static final CachingConnectionFactory connectionFactory =
			new CachingConnectionFactory(amqFactory);

	@BeforeClass
	public static void startUp() throws Exception {
		amqFactory.setTrustAllPackages(true);
		connectionFactory.setCacheConsumers(false);
		connectionFactory.createConnection().close();
	}

	@AfterClass
	public static void shutDown() {
		connectionFactory.resetConnection();
	}

}

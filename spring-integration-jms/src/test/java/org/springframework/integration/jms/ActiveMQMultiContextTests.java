/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.utils.ObjectInputStreamWithClassLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Keeps an ActiveMQ open for the duration of
 * all tests (avoids cycling the transport each time the last
 * connection is closed).
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 */
public abstract class ActiveMQMultiContextTests {

	public static final ActiveMQConnectionFactory amqFactory = new ActiveMQConnectionFactory("vm://0");

	public static final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(amqFactory);

	private static final EmbeddedActiveMQ broker = new EmbeddedActiveMQ();

	static {
		amqFactory.setDeserializationAllowList(ObjectInputStreamWithClassLoader.CATCH_ALL_WILDCARD);
		amqFactory.setRetryInterval(0);
	}

	@BeforeAll
	public static void startUp() throws Exception {
		Configuration configuration =
				new ConfigurationImpl()
						.setName("embedded-server")
						.setPersistenceEnabled(false)
						.setSecurityEnabled(false)
						.setJMXManagementEnabled(false)
						.setJournalDatasync(false)
						.addAcceptorConfiguration(new TransportConfiguration(InVMAcceptorFactory.class.getName()))
						.addAddressSetting("#",
								new AddressSettings()
										.setDeadLetterAddress(SimpleString.toSimpleString("dla"))
										.setExpiryAddress(SimpleString.toSimpleString("expiry")));
		broker.setConfiguration(configuration).start();
		connectionFactory.setCacheConsumers(false);
	}

	@AfterAll
	public static void shutDown() throws Exception {
		connectionFactory.destroy();
		amqFactory.createConnection().close();
		broker.stop();
	}

}

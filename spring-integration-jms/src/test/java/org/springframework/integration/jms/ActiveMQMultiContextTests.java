/*
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

import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Keeps an ActiveMQ open for the duration of
 * all tests (avoids cycling the transport each time the last
 * connection is closed).
 * <p>
 * The broker is started only once per JVM, but the shared {@link CachingConnectionFactory}
 * is reset after each test class to not share its cached connection and sessions
 * between independent test contexts.
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
		try {
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
											.setDeadLetterAddress(SimpleString.of("dla"))
											.setExpiryAddress(SimpleString.of("expiry")));
			broker.setConfiguration(configuration).start();
			connectionFactory.setCacheConsumers(false);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to start an embedded ActiveMQ Artemis broker", ex);
		}
	}

	@AfterAll
	public static void resetConnectionFactory() {
		connectionFactory.resetConnection();
	}

}

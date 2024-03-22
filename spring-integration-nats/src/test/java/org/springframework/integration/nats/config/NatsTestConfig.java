/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats.config;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.impl.NatsImpl;
import io.nats.spring.boot.autoconfigure.NatsConnectionProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Test config class for to create NATS connections
*
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 */
@Configuration
@PropertySource("/integration-nats.properties")
@EnableAsync
public class NatsTestConfig {

	private static final Log LOG = LogFactory.getLog(NatsTestConfig.class);

	/**
	 * Bean method to expose NatsConnectionProperties test bean with common attributes. NATS
	 * connection bean can be created in specific modules wherever required using this bean/ by
	 * enhancing the property definition.
	 *
	 * @param keyStoreLocation file path for the SSL Keystore
	 * @param keyStorePassword password used to unlock the keystore
	 * @param trustStoreLocation file path for the SSL trust store
	 * @param trustStorePassword password used to unlock the trust store
	 * @return Returns NatsConnectionProperties bean to use in creating NATS connections
	 * @throws IOException in case of general resolution/reading failures of keystore and truststore
	 *     files
	 */
	@Bean
	public NatsConnectionProperties natsEncryptedConnectionProperties(
			@NonNull @Value("${org.springframework.integration.nats.keyStorePath}") final Resource keyStoreLocation,
			@NonNull @Value("${org.springframework.integration.nats.keyStorePassword:}") final String keyStorePassword,
			@NonNull @Value("${org.springframework.integration.nats.trustStorePath}") final Resource trustStoreLocation,
			@NonNull @Value("${org.springframework.integration.nats.trustStorePassword}") final String trustStorePassword)
			throws IOException {
		final NatsConnectionProperties natsConnectionProperties = new NatsConnectionProperties();
		natsConnectionProperties.server(AbstractNatsIntegrationTestSupport.getNatsServerURL());
		natsConnectionProperties.setKeyStorePath(keyStoreLocation.getFile().getAbsolutePath());
		natsConnectionProperties.setKeyStorePassword(keyStorePassword.toCharArray());
		natsConnectionProperties.setTrustStorePath(trustStoreLocation.getFile().getAbsolutePath());
		natsConnectionProperties.setTrustStorePassword(trustStorePassword.toCharArray());
		natsConnectionProperties.setConnectionTimeout(Duration.ofSeconds(60));
		return natsConnectionProperties;
	}

	/**
	 * Method to create NATSConnection bean for testing purpose
	 *
	 * @param natsEncryptedConnectionProperties the connectionProperties bean
	 * @return the NATS connection
	 * @throws GeneralSecurityException if there is a problem setting up the SSL context
	 * @throws IOException if there is a problem reading a file or setting up the SSL context or if a
	 *     networking issue occurs
	 * @throws InterruptedException if the current thread is interrupted
	 */
	@Bean
	public Connection natsConnection(
			@NonNull final NatsConnectionProperties natsEncryptedConnectionProperties,
			@NonNull @Value("${org.springframework.integration.nats.credentials}") final Resource natsCredentials)
			throws GeneralSecurityException, IOException, InterruptedException {
		try {
			Options.Builder builder = natsEncryptedConnectionProperties.toOptionsBuilder();
			builder.authHandler(NatsImpl.credentials(natsCredentials.getFilename()));
			builder.connectionListener(new NatsConnectionListener());
			builder.connectionName("TEST_NATS_CONNECTION");
			return Nats.connect(builder.build());
		}
		catch (final GeneralSecurityException | IOException | InterruptedException e) {
			LOG.error("error connecting to nats", e);
			throw e;
		}
	}

	/** Connection Listener implementation to log connection events */
	class NatsConnectionListener implements ConnectionListener {
		public void connectionEvent(Connection natsConnection, Events event) {
			LOG.info(
					String.format(
							"Connection event - %s , connection URL=%s",
							event, natsConnection.getConnectedUrl()));
		}
	}
}

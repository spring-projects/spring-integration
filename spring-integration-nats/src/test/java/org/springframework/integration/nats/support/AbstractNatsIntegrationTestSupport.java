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

package org.springframework.integration.nats.support;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;
import io.nats.client.api.RetentionPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import org.springframework.integration.nats.NatsLocalTestServer;
import org.springframework.lang.NonNull;

/*
 Abstract class with helper method to start NATS on docker/local and create streams on demand

 <p>By default, this class tries to start NATS server in docker. To start the NATS server in
 local,embedded try setting the property nats_server_host to appropriate value before running the
 test.

 <p>Eg. -Dnats_server_host=local, -Dnats_js_enabled=true
*/

/**
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 * href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 * all stakeholders and contact</a>
 */
public abstract class AbstractNatsIntegrationTestSupport {

	private static final Log LOG = LogFactory.getLog(AbstractNatsIntegrationTestSupport.class);

	@NonNull
	private static final String SERVER_HOST = System.getProperty("nats_server_host", "docker");

	// System property to disable/enable jetstream for testing ( Default
	// enabled)
	private static final boolean jetStreamEnabled =
			Boolean.valueOf(System.getProperty("nats_js_enabled", "true"));
	private static final String tlsConfPath = "src/test/resources/tls.conf";
	private static final String tlsConfPathWithOutJS =
			"src/test/resources/tls_without_jetstream.conf";
	private static String natsServerURL =
			"nats://localhost:4222,nats://localhost:4223,nats://localhost:4224";
	// DOCKER server
	private static GenericContainer natsDocker;
	// NATS local server
	private static NatsLocalTestServer natsLocal;

	/**
	 * Even though "local" NATS server can always be started in specific port, "docker" instance
	 * running through Testcontainer approach will expose the running port of NATS server dynamically
	 * each time. Hence this property will updated once the NATS server is started in docker and will
	 * be available to use in creating NATS connection
	 *
	 * @return the natsServerURL
	 */
	public static String getNatsServerURL() {
		return natsServerURL;
	}

	@BeforeClass
	public static void setUp() throws Exception {
		startNatsServer();
	}

	@AfterClass
	public static void tearDown() {
		stopNatsServer();
	}

	public static void startNatsServer() throws Exception {
		switch (NatsAs.of(SERVER_HOST)) {
			case DOCKER:
				startNatsServerInTestContainer();
				return;
			case LOCAL:
				startNatsServerInLocal();
				return;
			case REMOTE: // do nothing es expected that docker server started manually
		}
	}

	public static void stopNatsServer() {
		switch (NatsAs.of(SERVER_HOST)) {
			case DOCKER:
				stopNatsServerInTestContainer();
				return;
			case LOCAL:
				stopNatsServerInLocal();
				return;
			case REMOTE: // do nothing es expected that docker server started manually
		}
	}

	public static void startNatsServerInLocal() throws InterruptedException {
		natsLocal =
				new NatsLocalTestServer(jetStreamEnabled ? tlsConfPath : tlsConfPathWithOutJS, 4222, true);
		Thread.sleep(5000);
		LOG.info("Nats server started in Local");
	}

	public static void stopNatsServerInLocal() {
		natsLocal.close();
	}

	public static void startNatsServerInTestContainer() throws InterruptedException {
		final String command =
				(jetStreamEnabled ? "-js --store_dir /data " : "")
						+ "--tls --tlscert=certs/cluster/server-dev.pem --tlskey=certs/cluster/server-dev-key.pem";
		natsDocker =
				new GenericContainer("nats:latest")
						.withExposedPorts(4222)
						.withCommand(command) //
						.withClasspathResourceMapping(
								"certs/cluster/server-dev.pem",
								"certs/cluster/server-dev.pem",
								BindMode.READ_ONLY) //
						.withClasspathResourceMapping(
								"certs/cluster/server-dev-key.pem",
								"certs/cluster/server-dev-key.pem",
								BindMode.READ_ONLY) //
						.withClasspathResourceMapping(
								"certs/cluster/intermediate_ca.pem",
								"certs/cluster/intermediate_ca.pem",
								BindMode.READ_ONLY) //
						.withClasspathResourceMapping(
								"/", "/data", BindMode.READ_WRITE); // storage directory "/data" mapping
		natsDocker.getPortBindings().add("4222:4222"); // port mapping
		natsDocker.start();
		natsServerURL = "nats://" + natsDocker.getHost() + ":" + natsDocker.getMappedPort(4222);
		LOG.info("Nats server started in Docker => " + natsServerURL);
	}

	public static void stopNatsServerInTestContainer() {
		natsDocker.stop();
	}

	public static Integer getNatsServerMappedPort() {
		return natsDocker.getMappedPort(4222);
	}

	/**
	 * This method uses NATS java client API to config remote (stand alone) NATS server with
	 * pre-configured STREAM. In PROD this will be configured directly on the server during server set
	 * up and booting process of server. This code is just simulate this behavior(stream setup) but
	 * using NATS java client API.
	 *
	 * @param natsConnection the natsConnection bean
	 * @param streamName     name of the stream to be created
	 * @param subjectName    name of the subject to be created
	 * @throws IOException           covers various communication issues with the NATS server such as timeout or
	 *                               interruption
	 * @throws JetStreamApiException the request had an error related to the data
	 */
	protected static void createStreamConfig(
			@NonNull final Connection natsConnection,
			@NonNull final String streamName,
			@NonNull final String subjectName)
			throws IOException, JetStreamApiException {
		final JetStreamManagement jsm = natsConnection.jetStreamManagement();
		LOG.info(natsConnection.getConnectedUrl());
		// remove old streams if already available
		try {
			jsm.deleteStream(streamName);
		}
		catch (final JetStreamApiException e) {
			// no action needed, if stream not found for deletion
		}
		// Build the stream configuration
		final StreamConfiguration streamConfig =
				StreamConfiguration.builder()
						.name(streamName)
						.subjects(subjectName)
						.storageType(StorageType.File)
						.retentionPolicy(RetentionPolicy.WorkQueue)
						.build();
		// Create the stream
		jsm.addStream(streamConfig);
	}

	/**
	 * This method uses NATS java client API to config remote (stand alone) NATS server with
	 * pre-configured CONSUMER. In PROD this will be configured directly on the server during server
	 * set up and booting process of server.
	 *
	 * @param natsConnection        the natsConnection bean
	 * @param streamName            name of the stream
	 * @param consumerConfiguration the consumer configuration to use.
	 * @throws IOException           covers various communication issues with the NATS server such as timeout or
	 *                               interruption
	 * @throws JetStreamApiException the request had an error related to the data
	 */
	protected static void createConsumer(
			@NonNull final Connection natsConnection,
			@NonNull final String streamName,
			@NonNull ConsumerConfiguration consumerConfiguration)
			throws IOException, JetStreamApiException {
		final JetStreamManagement jsm = natsConnection.jetStreamManagement();
		// Create/Update consumer
		ConsumerInfo consumerInfo = jsm.addOrUpdateConsumer(streamName, consumerConfiguration);
		LOG.info("Consumer Info: " + consumerInfo);
	}

	public enum NatsAs {

		// default value if -Dnats_server_host not set, the test start NATS with test container approach
		DOCKER("docker"),
		// assume local installation of NATS server where test is going to be run
		LOCAL("local"),
		// assumes the NATS server is going to be start manually
		REMOTE("remote");
		private static final Map<String, NatsAs> identifierMap;

		static {
			EnumSet<NatsAs> all = EnumSet.allOf(NatsAs.class);
			identifierMap = new HashMap<>();
			for (NatsAs gender : all) {
				identifierMap.put(gender.getId(), gender);
			}
		}

		private final String identifier;

		NatsAs(String identifier) {
			this.identifier = identifier;
		}

		public static NatsAs of(String identifier) {
			return identifierMap.get(identifier);
		}

		public String getId() {
			return identifier;
		}
	}
}

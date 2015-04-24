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

import static scala.collection.JavaConversions.asScalaBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.gs.collections.api.block.function.Function;
import com.gs.collections.impl.list.mutable.FastList;
import com.gs.collections.impl.utility.ListIterate;

import kafka.Kafka;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.SystemTime$;
import kafka.utils.TestUtils;
import kafka.utils.TestZKUtils;
import kafka.utils.Utils;
import kafka.utils.ZKStringSerializer$;
import kafka.zk.EmbeddedZookeeper;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkInterruptedException;
import org.junit.rules.ExternalResource;

import scala.collection.JavaConversions;

import org.springframework.integration.kafka.core.BrokerAddress;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
@SuppressWarnings("serial")
public class KafkaEmbedded extends ExternalResource implements KafkaRule {

	private int count;

	private boolean controlledShutdown;

	private List<Integer> kafkaPorts;

	private List<KafkaServer> kafkaServers;

	private EmbeddedZookeeper zookeeper;

	private ZkClient zookeeperClient;

	@SuppressWarnings("unchecked")
	public KafkaEmbedded(int count, boolean controlledShutdown) {
		this.count = count;
		this.controlledShutdown = controlledShutdown;
		this.kafkaPorts = JavaConversions.asJavaList((scala.collection.immutable.List) TestUtils.choosePorts(count));
	}

	public KafkaEmbedded(int count) {
		this(count, false);
	}

	@Override
	protected void before() throws Throwable {
		startZookeeper();
		int zkConnectionTimeout = 6000;
		int zkSessionTimeout = 6000;
		zookeeperClient = new ZkClient(TestZKUtils.zookeeperConnect(), zkSessionTimeout, zkConnectionTimeout,
				ZKStringSerializer$.MODULE$);
		kafkaServers = new ArrayList<KafkaServer>();
		for (int i = 0; i < count; i++) {
			Properties brokerConfigProperties = TestUtils.createBrokerConfig(i, kafkaPorts.get(i));
			brokerConfigProperties.put("controlled.shutdown.enable", Boolean.toString(controlledShutdown));
			KafkaServer server = TestUtils.createServer(new KafkaConfig(brokerConfigProperties), SystemTime$.MODULE$);
			kafkaServers.add(server);
		}
	}

	@Override
	protected void after() {
		for (KafkaServer kafkaServer : kafkaServers) {
			try {
				kafkaServer.shutdown();
			}
			catch (Exception e) {
				// do nothing
			}
			try {
				Utils.rm(kafkaServer.config().logDirs());
			}
			catch (Exception e) {
				// do nothing
			}
		}
		try {
			zookeeperClient.close();
		}
		catch (ZkInterruptedException e) {
			// do nothing
		}
		try {
			zookeeper.shutdown();
		}
		catch (Exception e) {
			// do nothing
		}
	}

	public List<KafkaServer> getKafkaServers() {
		return kafkaServers;
	}

	public KafkaServer getKafkaServer(int id) {
		return kafkaServers.get(id);
	}

	public EmbeddedZookeeper getZookeeper() {
		return zookeeper;
	}

	@Override
	public ZkClient getZkClient() {
		return zookeeperClient;
	}

	@Override
	public String getZookeeperConnectionString() {
		return zookeeper.connectString();
	}

	@Override
	public BrokerAddress[] getBrokerAddresses() {
		return ListIterate.collect(this.kafkaServers,
				new Function<KafkaServer, BrokerAddress>() {

					@Override
					public BrokerAddress valueOf(KafkaServer kafkaServer) {
						return new BrokerAddress(kafkaServer.config().hostName(), kafkaServer.config().port());
					}

				})
				.toArray(new BrokerAddress[this.kafkaServers.size()]);
	}

	public void bounce(BrokerAddress brokerAddress) {
		for (KafkaServer kafkaServer : getKafkaServers()) {
			if (brokerAddress.equals(new BrokerAddress(kafkaServer.config().hostName(), kafkaServer.config().port()))) {
				kafkaServer.shutdown();
			}
		}
	}

	public void startZookeeper() {
		zookeeper = new EmbeddedZookeeper(TestZKUtils.zookeeperConnect());
	}

	public void bounce(int index, boolean waitForPropagation) {
		kafkaServers.get(index).shutdown();
		if (waitForPropagation) {
			TestUtils.waitUntilMetadataIsPropagated(asScalaBuffer(kafkaServers), "test-topic", 0, 5000L);
		}
	}

	public void bounce(int index) {
		bounce(index, true);
	}

	public void restart(final int index) throws Exception {

		// retry restarting repeatedly, first attempts may fail

		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(10,
				Collections.<Class<? extends Throwable>,Boolean>singletonMap(Exception.class, true));

		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		backOffPolicy.setInitialInterval(100);
		backOffPolicy.setMaxInterval(1000);
		backOffPolicy.setMultiplier(2);

		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(retryPolicy);
		retryTemplate.setBackOffPolicy(backOffPolicy);


		retryTemplate.execute(new RetryCallback<Void, Exception>() {
			@Override
			public Void doWithRetry(RetryContext context) throws Exception {
				System.out.println("Retrying restart");
				kafkaServers.get(index).startup();
				return null;
			}
		});
	}

	@Override
	public String getBrokersAsString() {
		return FastList.newList(Arrays.asList(getBrokerAddresses()))
				.collect(new Function<BrokerAddress, String>() {

					@Override
					public String valueOf(BrokerAddress object) {
						return object.getHost() + ":" + object.getPort();
					}

				})
				.makeString(",");
	}

	@Override
	public boolean isEmbedded() {
		return true;
	}

}

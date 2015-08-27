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

package org.springframework.integration.kafka.listener;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.I0Itec.zkclient.ZkClient;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.kafka.core.BrokerAddress;
import org.springframework.integration.kafka.core.Configuration;
import org.springframework.integration.kafka.core.Connection;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.ConsumerException;
import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.KafkaConsumerDefaults;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.PartitionNotFoundException;
import org.springframework.integration.kafka.core.Result;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import com.gs.collections.impl.factory.Maps;

import kafka.client.ClientUtils$;
import kafka.common.ErrorMapping;
import kafka.common.OffsetAndMetadata;
import kafka.network.BlockingChannel;
import kafka.utils.ZKStringSerializer$;

/**
 * Implementation of an {@link OffsetManager} that uses kafka native 'topic' offset storage.
 *
 * @author Chris Lemper
 * @author Marius Bogoevici
 * @since 1.3
 */
public class KafkaNativeOffsetManager extends AbstractOffsetManager implements InitializingBean {

	private static final String PARTITION_ATTRIBUTE = "partition";

	private final Map<Partition, BrokerAddress> offsetManagerBrokerAddressCache = new ConcurrentHashMap<>();

	private final ZkClient zkClient;

	private RetryTemplate retryTemplate;

	/**
	 * @param connectionFactory a Kafka connection factory
	 * @param zookeeperConnect the zookeeper connection information
	 */
	public KafkaNativeOffsetManager(ConnectionFactory connectionFactory, ZookeeperConnect zookeeperConnect) {
		this(connectionFactory, zookeeperConnect, Collections.<Partition, Long>emptyMap());
	}

	/**
	 * @param connectionFactory a Kafka connection factory
	 * @param zookeeperConnect zookeeper connection for retrieving
	 * @param initialOffsets a map of partitions to initial offsets
	 */
	public KafkaNativeOffsetManager(ConnectionFactory connectionFactory, ZookeeperConnect zookeeperConnect,
									Map<Partition, Long> initialOffsets) {
		super(connectionFactory, initialOffsets);
		Assert.notNull(zookeeperConnect, "'zookeeperConnect' must not be null.");
		this.zkClient = new ZkClient(zookeeperConnect.getZkConnect(),
				Integer.parseInt(zookeeperConnect.getZkSessionTimeout()),
				Integer.parseInt(zookeeperConnect.getZkConnectionTimeout()),
				ZKStringSerializer$.MODULE$);
	}

	public void setRetryTemplate(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.retryTemplate == null) {
			this.retryTemplate = new RetryTemplate();
			this.retryTemplate.registerListener(new ResetOffsetManagerBrokerAddressRetryListener());
			final SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
			retryPolicy.setMaxAttempts(5);
			this.retryTemplate.setRetryPolicy(retryPolicy);
			final ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
			backOffPolicy.setInitialInterval(125L);
			backOffPolicy.setMaxInterval(5000L);
			backOffPolicy.setMultiplier(2);
			this.retryTemplate.setBackOffPolicy(backOffPolicy);
		}
	}

	@Override
	protected Long doGetOffset(final Partition partition) {
		final Long offset = this.retryTemplate.execute(new RetryCallback<Long, RuntimeException>() {

			@Override
			public Long doWithRetry(RetryContext context) throws RuntimeException {
				context.setAttribute(PARTITION_ATTRIBUTE, partition);
				Result<Long> result = getOffsetManagerConnection(partition).fetchStoredOffsetsForConsumer(
						getConsumerId(), partition);
				checkResultForErrors(result, partition);
				return result.getResult(partition);
			}

			@Override
			public String toString() {
				return String.format("fetchStoredOffsetsForConsumer(%s, %s)", getConsumerId(), partition);
			}

		});

		if (offset != null && offset < 0) {
			return null;
		}

		return offset;
	}

	@Override
	protected void doUpdateOffset(final Partition partition, final long offset) {
		this.retryTemplate.execute(new RetryCallback<Void, RuntimeException>() {

			@Override
			public Void doWithRetry(RetryContext context) throws RuntimeException {
				context.setAttribute(PARTITION_ATTRIBUTE, partition);
				Result<Void> result = getOffsetManagerConnection(partition).commitOffsetsForConsumer(
						getConsumerId(), Maps.immutable.of(partition, offset).castToMap());
				checkResultForErrors(result, partition);
				return null;
			}

			@Override
			public String toString() {
				return String.format("commitOffsetsForConsumer(%s, %s, %s)", getConsumerId(), partition, offset);
			}

		});
	}

	@Override
	protected void doRemoveOffset(Partition partition) {
		doUpdateOffset(partition, OffsetAndMetadata.InvalidOffset());
	}

	@Override
	public void close() throws IOException {
		this.zkClient.close();
	}

	@Override
	public void flush() throws IOException {
		// this function left intentionally blank
	}

	private BrokerAddress getOffsetManagerBrokerAddress(final Partition partition) {
		BrokerAddress brokerAddress = this.offsetManagerBrokerAddressCache.get(partition);
		if (brokerAddress == null) {
			int socketTimeoutMs = KafkaConsumerDefaults.SOCKET_TIMEOUT_INT;
			int retryBackOffMs = KafkaConsumerDefaults.BACKOFF_INCREMENT_INT;
			if (this.connectionFactory instanceof DefaultConnectionFactory) {
				Configuration configuration = ((DefaultConnectionFactory) connectionFactory).getConfiguration();
				socketTimeoutMs = configuration.getSocketTimeout();
				retryBackOffMs = configuration.getBackOff();
			}
			final BlockingChannel channel = ClientUtils$.MODULE$.channelToOffsetManager(
					getConsumerId(), zkClient, socketTimeoutMs, retryBackOffMs);
			brokerAddress = new BrokerAddress(channel.host(), channel.port());
			if (log.isDebugEnabled()) {
				log.debug(String.format("Offset manager for [%s] is at [%s].", partition,
						brokerAddress));
			}
			this.offsetManagerBrokerAddressCache.put(partition, brokerAddress);
			channel.disconnect();
		}
		return brokerAddress;
	}

	private Connection getOffsetManagerConnection(final Partition partition) {
		return this.connectionFactory.connect(getOffsetManagerBrokerAddress(partition));
	}

	private void checkResultForErrors(final Result<?> result, final Partition partition) {
		if (result.getErrors().containsKey(partition)) {
			final short errorCode = result.getError(partition);
			if (errorCode == ErrorMapping.UnknownTopicOrPartitionCode()) {
				throw new PartitionNotFoundException(partition);
			}
			else if (errorCode != ErrorMapping.NoError()) {
				throw new ConsumerException(ErrorMapping.exceptionFor(errorCode));
			}
		}
	}

	private class ResetOffsetManagerBrokerAddressRetryListener extends RetryListenerSupport {

		@Override
		@SuppressWarnings("unchecked")
		public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable t) {
			if (log.isWarnEnabled()) {
				log.warn("Retrying kafka operation [" + callback + "] due to [" + t + "]", t);
			}
			Partition partition = (Partition) context.getAttribute(PARTITION_ATTRIBUTE);
			if (partition != null) {
				offsetManagerBrokerAddressCache.remove(partition);
			}
		}
	}

}

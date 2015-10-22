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

package org.springframework.integration.kafka.util;

import java.util.List;
import java.util.Properties;

import kafka.common.LeaderNotAvailableException;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.kafka.core.TopicNotFoundException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import kafka.admin.AdminUtils;
import kafka.api.TopicMetadata;
import kafka.common.ErrorMapping;
import kafka.javaapi.PartitionMetadata;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import scala.collection.Map;
import scala.collection.Seq;

/**
 * Utilities for interacting with Kafka topics
 *
 * @author Marius Bogoevici
 */
public class TopicUtils {

	private static Log log = LogFactory.getLog(TopicUtils.class);

	public static final int METADATA_VERIFICATION_TIMEOUT = 5000;

	public static final int METADATA_VERIFICATION_RETRY_ATTEMPTS = 10;

	public static final double METADATA_VERIFICATION_RETRY_BACKOFF_MULTIPLIER = 1.5;

	public static final int METADATA_VERIFICATION_RETRY_INITIAL_INTERVAL = 100;

	public static final int METADATA_VERIFICATION_MAX_INTERVAL = 1000;

	/**
	 * Creates a topic in Kafka or validates that it exists with the requested number of partitions,
	 * and returns only after the topic has been fully created
	 * @param zkAddress the address of the Kafka ZooKeeper instance
	 * @param topicName the name of the topic
	 * @param numPartitions the number of partitions
	 * @param replicationFactor the replication factor
	 * @return {@link TopicMetadata} information for the topic
	 */
	public static TopicMetadata ensureTopicCreated(final String zkAddress, final String topicName,
			final int numPartitions, int replicationFactor) {

		final int sessionTimeoutMs = 10000;
		final int connectionTimeoutMs = 10000;
		final ZkClient zkClient = new ZkClient(zkAddress, sessionTimeoutMs, connectionTimeoutMs,
				ZKStringSerializer$.MODULE$);
		try {
			// The following is basically copy/paste from AdminUtils.createTopic() with
			// createOrUpdateTopicPartitionAssignmentPathInZK(..., update=true)
			Properties topicConfig = new Properties();
			Seq<Object> brokerList = ZkUtils.getSortedBrokerList(zkClient);
			scala.collection.Map<Object, Seq<Object>> replicaAssignment =
					AdminUtils.assignReplicasToBrokers(brokerList, numPartitions, replicationFactor, -1, -1);
			return ensureTopicCreated(zkClient, topicName, numPartitions, topicConfig, replicaAssignment);

		}
		finally {
			zkClient.close();
		}
	}

	/**
	 * Creates a topic in Kafka and returns only after the topic has been fully and an produce metadata.
	 * @param zkClient an open {@link ZkClient} connection to Zookeeper
	 * @param topicName the name of the topic
	 * @param numPartitions the number of partitions for the topic
	 * @param topicConfig additional topic configuration properties
	 * @param replicaAssignment the mapping of partitions to broker
	 * @return {@link TopicMetadata} information for the topic
	 */
	public static TopicMetadata ensureTopicCreated(final ZkClient zkClient, final String topicName,
			final int numPartitions, Properties topicConfig, Map<Object, Seq<Object>> replicaAssignment) {
		AdminUtils.createOrUpdateTopicPartitionAssignmentPathInZK(zkClient, topicName, replicaAssignment, topicConfig,
				true);

		RetryTemplate retryTemplate = new RetryTemplate();

		CompositeRetryPolicy policy = new CompositeRetryPolicy();
		TimeoutRetryPolicy timeoutRetryPolicy = new TimeoutRetryPolicy();
		timeoutRetryPolicy.setTimeout(METADATA_VERIFICATION_TIMEOUT);
		SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
		simpleRetryPolicy.setMaxAttempts(METADATA_VERIFICATION_RETRY_ATTEMPTS);
		policy.setPolicies(new RetryPolicy[] {timeoutRetryPolicy, simpleRetryPolicy});
		retryTemplate.setRetryPolicy(policy);

		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		backOffPolicy.setInitialInterval(METADATA_VERIFICATION_RETRY_INITIAL_INTERVAL);
		backOffPolicy.setMultiplier(METADATA_VERIFICATION_RETRY_BACKOFF_MULTIPLIER);
		backOffPolicy.setMaxInterval(METADATA_VERIFICATION_MAX_INTERVAL);
		retryTemplate.setBackOffPolicy(backOffPolicy);

		try {
			return retryTemplate.execute(new RetryCallback<TopicMetadata, Exception>() {
				@Override
				public TopicMetadata doWithRetry(RetryContext context) throws Exception {
					TopicMetadata topicMetadata = AdminUtils.fetchTopicMetadataFromZk(topicName, zkClient);
					if (topicMetadata.errorCode() != ErrorMapping.NoError() ||
							!topicName.equals(topicMetadata.topic())) {
						// downcast to Exception because that's what the error throws
						throw (Exception) ErrorMapping.exceptionFor(topicMetadata.errorCode());
					}
					List<PartitionMetadata> partitionMetadatas =
							new kafka.javaapi.TopicMetadata(topicMetadata).partitionsMetadata();
					if (partitionMetadatas.size() != numPartitions) {
						throw new IllegalStateException("The number of expected partitions was: " +
								numPartitions + ", but " + partitionMetadatas.size() + " have been found instead");
					}
					for (PartitionMetadata partitionMetadata : partitionMetadatas) {
						if (partitionMetadata.errorCode() != ErrorMapping.NoError()) {
							throw (Exception) ErrorMapping.exceptionFor(partitionMetadata.errorCode());
						}
						if (partitionMetadata.leader() == null) {
							throw new LeaderNotAvailableException();
						}
					}
					return topicMetadata;
				}
			});
		}
		catch (Exception e) {
			log.error(String.format("Cannot retrieve metadata for topic '%s'", topicName), e);
			throw new TopicNotFoundException(topicName);
		}
	}

}

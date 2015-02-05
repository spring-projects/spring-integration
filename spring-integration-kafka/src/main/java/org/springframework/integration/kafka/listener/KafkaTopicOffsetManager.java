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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkInterruptedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.kafka.core.BrokerAddress;
import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.FetchRequest;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.KafkaMessageBatch;
import org.springframework.integration.kafka.core.KafkaTemplate;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.Result;
import org.springframework.integration.kafka.core.TopicNotFoundException;
import org.springframework.integration.kafka.core.ZookeeperConfiguration;
import org.springframework.integration.kafka.support.ProducerFactoryBean;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.integration.kafka.util.LoggingUtils;
import org.springframework.integration.kafka.util.MessageUtils;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import kafka.admin.AdminUtils$;
import kafka.api.OffsetRequest;
import kafka.common.ErrorMapping$;
import kafka.common.TopicExistsException;
import kafka.javaapi.producer.Producer;
import kafka.producer.DefaultPartitioner;
import kafka.producer.KeyedMessage;
import kafka.serializer.Decoder;
import kafka.serializer.Encoder;
import kafka.utils.ZKStringSerializer$;

/**
 * Implementation of an {@link OffsetManager} that uses a Kafka topic as the underlying support.
 * For its proper functioning, the Kafka server(s) must set {@code log.cleaner.enable=true}. It relies on the property
 * {@code cleanup.policy=compact} to be set on the target topic, and if the topic is not found,
 * it will create a topic with the appropriate settings.
 *
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
public class KafkaTopicOffsetManager extends AbstractOffsetManager implements InitializingBean {

	private static final KeyEncoderDecoder KEY_CODEC = new KeyEncoderDecoder();

	private static final LongEncoderDecoder VALUE_CODEC = new LongEncoderDecoder();

	public static final String CLEANUP_POLICY = "cleanup.policy";

	public static final String CLEANUP_POLICY_COMPACT = "compact";

	public static final String DELETE_RETENTION = "delete.retention.ms";

	public static final String SEGMENT_BYTES = "segment.bytes";

	private final ZookeeperConnect zookeeperConnect;

	private final String topic;

	private final KafkaTemplate kafkaTemplate;

	private final ConcurrentMap<Partition, Long> data = new ConcurrentHashMap<Partition, Long>();

	private String compressionCodec = "default";

	private Producer<Key, Long> producer;

	private int maxSize = 10 * 1024;

	private int maxQueueBufferingTime = 1000;

	private int segmentSize = 25 * 1024;

	private int retentionTime = 60000;

	private int replicationFactor;

	private int maxBatchSize = 200;

	private boolean batchWrites = true;

	private int requiredAcks = 1;

	public KafkaTopicOffsetManager(ZookeeperConnect zookeeperConnect, String topic) {
		this(zookeeperConnect, topic, new HashMap<Partition, Long>());
	}

	public KafkaTopicOffsetManager(ZookeeperConnect zookeeperConnect, String topic,
			Map<Partition, Long> initialOffsets) {
		super(new DefaultConnectionFactory(new ZookeeperConfiguration(zookeeperConnect)), initialOffsets);
		Assert.notNull(zookeeperConnect);
		this.zookeeperConnect = zookeeperConnect;
		this.kafkaTemplate = new KafkaTemplate(connectionFactory);
		this.topic = topic;
	}

	/**
	 * Sets the maximum size of a fetch request, allowing to tune the initialization process.
	 *
	 * @param maxSize the maximum amount of data to be brought on a fetch
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * The compression codec for writing to the offset topic
	 *
	 * @param compressionCodec the compression codec
	 */
	public void setCompressionCodec(String compressionCodec) {
		this.compressionCodec = compressionCodec;
	}

	/**
	 * For how long will producers buffer data before writing to the topic
	 *
	 * @param maxQueueBufferingTime the maximum buffering window (in milliseconds)
	 */
	public void setMaxQueueBufferingTime(int maxQueueBufferingTime) {
		this.maxQueueBufferingTime = maxQueueBufferingTime;
	}

	/**
	 * The size of a segment in the offset topic
	 *
	 * @param segmentSize the segment size of an offset topic
	 */
	public void setSegmentSize(int segmentSize) {
		this.segmentSize = segmentSize;
	}

	/**
	 * How long are dead records retained in the offset topic
	 *
	 * @param retentionTime the retention time for dead records (in seconds)
	 */
	public void setRetentionTime(int retentionTime) {
		this.retentionTime = retentionTime;
	}

	/**
	 * The replication factor of the offset topic
	 *
	 * @param replicationFactor the replication factor
	 */
	public void setReplicationFactor(int replicationFactor) {
		this.replicationFactor = replicationFactor;
	}

	/**
	 * The maximum batch size for offset writes
	 *
	 * @param maxBatchSize maximum batching window
	 */
	public void setMaxBatchSize(int maxBatchSize) {
		this.maxBatchSize = maxBatchSize;
	}

	/**
	 * Whether offset writes should be batched or not
	 *
	 * @param batchWrites true if writes are batched
	 */
	public void setBatchWrites(boolean batchWrites) {
		this.batchWrites = batchWrites;
	}

	/**
	 * The number of required acks on write operations
	 *
	 * @param requiredAcks the number of required acks
	 */
	public void setRequiredAcks(int requiredAcks) {
		this.requiredAcks = requiredAcks;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		((DefaultConnectionFactory) this.connectionFactory).afterPropertiesSet();
		ZkClient zkClient = new ZkClient(this.zookeeperConnect.getZkConnect(),
				Integer.parseInt(this.zookeeperConnect.getZkSessionTimeout()),
				Integer.parseInt(this.zookeeperConnect.getZkConnectionTimeout()),
				ZKStringSerializer$.MODULE$);

		try {
			createCompactedTopicIfNotFound(zkClient);
			validateOffsetTopic(zkClient);
			Partition offsetPartition = new Partition(this.topic, 0);
			BrokerAddress offsetPartitionLeader = this.connectionFactory.getLeader(offsetPartition);
			readOffsetData(offsetPartition, offsetPartitionLeader);
			initializeProducer(offsetPartitionLeader);
		}
		finally {
			try {
				zkClient.close();
			}
			catch (ZkInterruptedException e) {
				log.error("Error while closing Zookeeper client", e);
			}
		}
	}

	@Override
	protected void doUpdateOffset(Partition partition, long offset) {
		this.data.put(partition, offset);
		this.producer.send(new KeyedMessage<Key, Long>(this.topic, new Key(this.consumerId, partition), offset));
	}

	@Override
	protected void doRemoveOffset(Partition partition) {
		this.data.remove(partition);
		this.producer.send(new KeyedMessage<Key, Long>(this.topic, new Key(this.consumerId, partition), null));
	}

	@Override
	protected Long doGetOffset(Partition partition) {
		return this.data.get(partition);
	}

	@Override
	public void flush() throws IOException {
		// not supported
	}

	@Override
	public void close() throws IOException {
		this.producer.close();
		try {
			((DefaultConnectionFactory) this.connectionFactory).destroy();
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}


	private void createCompactedTopicIfNotFound(ZkClient zkClient) {
		Properties topicConfig = new Properties();
		topicConfig.setProperty(CLEANUP_POLICY, CLEANUP_POLICY_COMPACT);
		topicConfig.setProperty(DELETE_RETENTION, String.valueOf(this.retentionTime));
		topicConfig.setProperty(SEGMENT_BYTES, String.valueOf(this.segmentSize));
		try {
			this.replicationFactor = 1;
			AdminUtils$.MODULE$.createTopic(zkClient, this.topic, 1, this.replicationFactor, topicConfig);
		}
		catch (TopicExistsException e) {
			log.debug("Topic already exists", e);
		}
	}

	private void validateOffsetTopic(ZkClient zkClient) throws Exception {
		//validate that the topic exists, but also prevent working with the topic until it's fully initialized
		// set a retry template, since operations may fail
		RetryTemplate retryValidateTopic = new RetryTemplate();
		retryValidateTopic.setRetryPolicy(new SimpleRetryPolicy(10,
				Collections.<Class<? extends Throwable>, Boolean>singletonMap(TopicNotFoundException.class, true)));
		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		backOffPolicy.setInitialInterval(100L);
		backOffPolicy.setMaxInterval(1000L);
		backOffPolicy.setMultiplier(2);
		retryValidateTopic.setBackOffPolicy(backOffPolicy);

		Collection<Partition> partitions =
				retryValidateTopic.execute(new RetryCallback<Collection<Partition>, Exception>() {

					@Override
					public Collection<Partition> doWithRetry(RetryContext context) throws Exception {
						return connectionFactory.getPartitions(topic);
					}

				});

		if (partitions.size() > 1) {
			throw new BeanInitializationException("Offset management topic cannot have more than one partition");
		}

		Properties properties = AdminUtils$.MODULE$.fetchTopicConfig(zkClient, this.topic);
		if (!properties.containsKey(CLEANUP_POLICY)
				|| !CLEANUP_POLICY_COMPACT.equals(properties.getProperty(CLEANUP_POLICY))) {
			// we set the property to compact, but if using an already created topic,
			// we must check if it is set up correctly
			throw new BeanInitializationException("Property 'cleanup.policy' must be set to 'compact' on offset topic");
		}
	}

	private void readOffsetData(Partition offsetManagementPartition, BrokerAddress leader) {
		Result<Long> earliestOffsetResult = this.connectionFactory.connect(leader)
				.fetchInitialOffset(OffsetRequest.EarliestTime(), offsetManagementPartition);
		if (earliestOffsetResult.getErrors().size() > 0) {
			throw new BeanInitializationException("Cannot initialize offset manager, unable to read earliest offset",
					ErrorMapping$.MODULE$.exceptionFor(earliestOffsetResult.getError(offsetManagementPartition)));
		}
		Result<Long> latestOffsetResult = this.connectionFactory.connect(leader)
				.fetchInitialOffset(OffsetRequest.LatestTime(), offsetManagementPartition);
		if (latestOffsetResult.getErrors().size() > 0) {
			throw new BeanInitializationException("Cannot initialize offset manager, unable to read latest offset");
		}

		long initialOffset = earliestOffsetResult.getResult(offsetManagementPartition);
		long finalOffset = latestOffsetResult.getResult(offsetManagementPartition);

		// read repeatedly until we drain the topic and add messages to the data map
		long readingOffset = initialOffset;
		while (readingOffset < finalOffset) {
			FetchRequest fetchRequest = new FetchRequest(offsetManagementPartition, readingOffset, maxSize);
			Result<KafkaMessageBatch> receive = this.kafkaTemplate.receive(Collections.singleton(fetchRequest));
			if (receive.getErrors().size() > 0) {
				throw new BeanInitializationException("Error while fetching initial offsets:",
						ErrorMapping$.MODULE$.exceptionFor(receive.getError(offsetManagementPartition)));
			}
			KafkaMessageBatch result = receive.getResult(offsetManagementPartition);
			for (KafkaMessage kafkaMessage : result.getMessages()) {
				checkAndAddData(kafkaMessage);
				readingOffset = kafkaMessage.getMetadata().getNextOffset();
			}
			if (log.isDebugEnabled()) {
				log.debug(data.size() + " entries in the final map");
			}
			if (log.isTraceEnabled()) {
				for (Map.Entry<Partition, Long> dataEntry : data.entrySet()) {
					log.trace(String.format("Final value for %s : %s", dataEntry.getKey().toString(),
							String.valueOf(dataEntry.getValue())));
				}
			}
		}
	}

	private void checkAndAddData(KafkaMessage kafkaMessage) {
		Key key = MessageUtils.decodeKey(kafkaMessage, KEY_CODEC);
		Long value = MessageUtils.decodePayload(kafkaMessage, VALUE_CODEC);
		if (log.isTraceEnabled()) {
			log.trace("Loading key " + key + " with value " + value);
		}
		// we are only interested for messages that are intended for this consumer id
		if (key != null && ObjectUtils.nullSafeEquals(this.consumerId, key.getConsumerId())) {
			if (null != value) {
				// write data in the cache, overwriting the older values
				this.data.put(key.getPartition(), value);
			}
			else {
				// a null value means that the data has been deleted, but not compacted yet
				if (this.data.containsKey(key.getPartition())) {
					this.data.remove(key.getPartition());
				}
			}
		}
	}


	private void initializeProducer(BrokerAddress leader) throws Exception {
		ProducerMetadata<Key, Long> producerMetadata = new ProducerMetadata<Key, Long>(this.topic);

		producerMetadata.setValueEncoder(VALUE_CODEC);
		producerMetadata.setValueClassType(Long.class);
		producerMetadata.setKeyEncoder(KEY_CODEC);
		producerMetadata.setKeyClassType(Key.class);
		producerMetadata.setPartitioner(new DefaultPartitioner(null));

		Properties additionalProps = new Properties();

		producerMetadata.setAsync(this.batchWrites);
		if (this.batchWrites) {
			producerMetadata.setBatchNumMessages(String.valueOf(this.maxBatchSize));
			producerMetadata.setCompressionCodec(this.compressionCodec);
			additionalProps.put("request.required.acks", String.valueOf(this.requiredAcks));
			additionalProps.put("queue.buffering.max.ms", String.valueOf(this.maxQueueBufferingTime));
		}

		ProducerFactoryBean<Key, Long> producerFB
				= new ProducerFactoryBean<Key, Long>(producerMetadata, leader.toString(), additionalProps);

		this.producer = producerFB.getObject();
	}

	private static int intFromBytes(byte[] bytes, int start) {
		return bytes[start] << 24 | (bytes[start + 1] & 0xFF) << 16
				| (bytes[start + 2] & 0xFF) << 8 | (bytes[start + 3] & 0xFF);
	}

	private static byte[] intToBytes(Integer message) {
		int value = message;
		return new byte[] {
				(byte) (value >>> 24),
				(byte) (value >>> 16),
				(byte) (value >>> 8),
				(byte) value
		};
	}

	/**
	 * Wraps the partition and consumer information and will be used as a key on the Kafka topic
	 */
	public static class Key {

		String consumerId;

		Partition partition;

		public Key(String consumerID, Partition partition) {
			Assert.notNull(consumerID, "Consumer Id cannot be null");
			Assert.notNull(partition, "Partition cannot be null");
			this.consumerId = consumerID;
			this.partition = partition;
		}

		public String getConsumerId() {
			return this.consumerId;
		}

		public Partition getPartition() {
			return this.partition;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Key key = (Key) o;

			return this.consumerId.equals(key.consumerId) && this.partition.equals(key.partition);

		}

		@Override
		public int hashCode() {
			int result = this.consumerId.hashCode();
			result = 31 * result + this.partition.hashCode();
			return result;
		}

	}

	public static class KeyEncoderDecoder implements Encoder<Key>, Decoder<Key> {

		private static final Log log = LogFactory.getLog(KeyEncoderDecoder.class);

		@Override
		public Key fromBytes(byte[] bytes) {
			if (bytes == null || bytes.length <= 0) {
				return null;
			}
			try {
				// calculate the offsets in the key array
				int consumerIdSize = intFromBytes(bytes, 0);
				int topicIdSize = intFromBytes(bytes, consumerIdSize + 4);
				// reconstruct the key
				return new Key(new String(bytes, 4, consumerIdSize),
						new Partition(new String(bytes, consumerIdSize + 8, topicIdSize),
								intFromBytes(bytes, consumerIdSize + topicIdSize + 8)));
			}
			catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug("Cannot decode key:" + LoggingUtils.asCommaSeparatedHexDump(bytes));
				}
				return null;
			}
		}

		@Override
		public byte[] toBytes(Key key) {
			if (key == null) {
				return null;
			}
			try {
				byte[] consumerIdBytes = key.consumerId.getBytes("UTF-8");
				byte[] topicNameBytes = key.partition.getTopic().getBytes("UTF-8");
				byte[] partitionIdBytes = intToBytes(key.partition.getId());
				byte[] result = new byte[4 + consumerIdBytes.length + 4 + topicNameBytes.length + 4];
				System.arraycopy(intToBytes(consumerIdBytes.length), 0, result, 0, 4);
				System.arraycopy(consumerIdBytes, 0, result, 4, consumerIdBytes.length);
				System.arraycopy(intToBytes(topicNameBytes.length), 0, result, consumerIdBytes.length + 4, 4);
				System.arraycopy(topicNameBytes, 0, result, consumerIdBytes.length + 8, topicNameBytes.length);
				System.arraycopy(partitionIdBytes, 0, result, consumerIdBytes.length + topicNameBytes.length + 8, 4);
				return result;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

}

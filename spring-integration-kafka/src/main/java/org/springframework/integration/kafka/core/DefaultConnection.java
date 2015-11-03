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

package org.springframework.integration.kafka.core;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.gs.collections.api.block.function.Function;
import com.gs.collections.api.block.function.Function2;
import com.gs.collections.api.tuple.Pair;
import com.gs.collections.impl.list.mutable.FastList;
import com.gs.collections.impl.tuple.Tuples;
import com.gs.collections.impl.utility.LazyIterate;
import com.gs.collections.impl.utility.MapIterate;
import kafka.api.FetchRequestBuilder;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.cluster.Broker;
import kafka.common.ErrorMapping;
import kafka.common.OffsetAndMetadata;
import kafka.common.OffsetMetadataAndError;
import kafka.common.TopicAndPartition;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.OffsetCommitRequest;
import kafka.javaapi.OffsetCommitResponse;
import kafka.javaapi.OffsetFetchRequest;
import kafka.javaapi.OffsetFetchResponse;
import kafka.javaapi.OffsetRequest;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadata;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.TopicMetadataResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.message.MessageAndOffset;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * A connection to a Kafka broker.
 *
 * @author Marius Bogoevici
 */
public class DefaultConnection implements Connection {

	private static Log log = LogFactory.getLog(DefaultConnection.class);

	private final AtomicInteger correlationIdCounter = new AtomicInteger(new Random(new Date().getTime()).nextInt());

	private final SimpleConsumer simpleConsumer;

	private final BrokerAddress brokerAddress;

	private int minBytes;

	private int maxWait;

	public DefaultConnection(BrokerAddress brokerAddress, String clientId, int bufferSize, int soTimeout,
			int minBytes, int maxWait) {
		this.brokerAddress = brokerAddress;
		this.minBytes = minBytes;
		this.maxWait = maxWait;
		this.simpleConsumer =
				new SimpleConsumer(brokerAddress.getHost(), brokerAddress.getPort(), soTimeout, bufferSize, clientId);
	}

	/**
	 * @see Connection#getBrokerAddress()
	 */
	@Override
	public BrokerAddress getBrokerAddress() {
		return brokerAddress;
	}

	@Override
	public void close() {
		this.simpleConsumer.close();
	}

	/**
	 * Fetche data from Kafka.
	 * @return a combination of messages and errors, depending on whether the invocation was successful or not
	 * @throws ConsumerException the ConsumerException if any underlying error
	 */
	@Override
	public Result<KafkaMessageBatch> fetch(FetchRequest... requests) throws ConsumerException {
		FetchRequestBuilder fetchRequestBuilder = new FetchRequestBuilder();
		for (FetchRequest request : requests) {
			Partition partition = request.getPartition();
			long offset = request.getOffset();
			int maxSize = request.getMaxSizeInBytes();
			fetchRequestBuilder.addFetch(partition.getTopic(), partition.getId(), offset, maxSize);
		}
		FetchResponse fetchResponse;
		try {
			fetchResponse = this.simpleConsumer.fetch(fetchRequestBuilder.maxWait(maxWait).minBytes(minBytes).build());
		}
		catch (Exception e) {
			throw new ConsumerException(e);
		}
		ResultBuilder<KafkaMessageBatch> resultBuilder = new ResultBuilder<KafkaMessageBatch>();
		for (final FetchRequest request : requests) {
			Partition partition = request.getPartition();
			if (log.isTraceEnabled()) {
				log.trace("Reading from " + partition + "@" + request.getOffset());
			}
			short errorCode = fetchResponse.errorCode(partition.getTopic(), partition.getId());
			if (ErrorMapping.NoError() == errorCode) {
				ByteBufferMessageSet messageSet = fetchResponse.messageSet(partition.getTopic(), partition.getId());
				List<KafkaMessage> kafkaMessages = LazyIterate.collect(messageSet,
						new ConvertToKafkaMessageFunction(request)).toList();
				long highWatermark = fetchResponse.highWatermark(partition.getTopic(), partition.getId());
				resultBuilder.add(partition).withResult(new KafkaMessageBatch(partition, kafkaMessages, highWatermark));
			}
			else {
				resultBuilder.add(partition).withError(errorCode);
			}
		}
		return resultBuilder.build();
	}

	@Override
	public Result<Long> fetchStoredOffsetsForConsumer(String consumerId, Partition... partitions) throws
			ConsumerException {
		FastList<TopicAndPartition> topicsAndPartitions = FastList.newList(Arrays.asList(partitions))
				.collect(new ConvertToTopicAndPartitionFunction());
		OffsetFetchRequest offsetFetchRequest = new OffsetFetchRequest(consumerId, topicsAndPartitions,
				kafka.api.OffsetFetchRequest.CurrentVersion(), createCorrelationId(), simpleConsumer.clientId());
		OffsetFetchResponse offsetFetchResponse = null;
		try {
			offsetFetchResponse = simpleConsumer.fetchOffsets(offsetFetchRequest);
		}
		catch (Exception e) {
			throw new ConsumerException(e);
		}
		ResultBuilder<Long> resultBuilder = new ResultBuilder<Long>();
		for (Partition partition : partitions) {
			OffsetMetadataAndError offsetMetadataAndError
					= offsetFetchResponse.offsets().get(new TopicAndPartition(partition.getTopic(), partition.getId()));
			short errorCode = offsetMetadataAndError.error();
			if (ErrorMapping.NoError() == errorCode) {
				resultBuilder.add(partition).withResult(offsetMetadataAndError.offset());
			}
			else {
				resultBuilder.add(partition).withError(errorCode);
			}
		}
		return resultBuilder.build();
	}

	/**
	 * @see Connection#fetchInitialOffset(long, Partition...)
	 */
	@Override
	public Result<Long> fetchInitialOffset(long referenceTime, Partition... partitions) throws
			ConsumerException {
		Assert.isTrue(partitions.length > 0, "Must provide at least one partition");
		Map<TopicAndPartition, PartitionOffsetRequestInfo> infoMap = new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
		for (Partition partition : partitions) {
			infoMap.put(new TopicAndPartition(partition.getTopic(), partition.getId()),
					new PartitionOffsetRequestInfo(referenceTime, 1));
		}
		OffsetRequest offsetRequest =
				new OffsetRequest(infoMap, kafka.api.OffsetRequest.CurrentVersion(), simpleConsumer.clientId());
		OffsetResponse offsetResponse = null;
		try {
			offsetResponse = simpleConsumer.getOffsetsBefore(offsetRequest);
		}
		catch (Exception e) {
			throw new ConsumerException(e);
		}
		ResultBuilder<Long> resultBuilder = new ResultBuilder<Long>();
		for (Partition partition : partitions) {
			short errorCode = offsetResponse.errorCode(partition.getTopic(), partition.getId());
			if (ErrorMapping.NoError() == errorCode) {
				long[] offsets = offsetResponse.offsets(partition.getTopic(), partition.getId());
				if (offsets.length == 0) {
					// normally, we shouldn't get here - either an offset has been returned,
					// or an error. However, in case something went wrong, the check protects against an
					// ArrayIndexOutOfBoundsException
					throw new ConsumerException("Inconsistent response: no error has been returned, " +
							"but no offsets either");
				}
				resultBuilder.add(partition).withResult(offsets[0]);
			}
			else {
				resultBuilder.add(partition).withError(errorCode);
			}
		}
		return resultBuilder.build();
	}

	@Override
	public Result<Void> commitOffsetsForConsumer(String consumerId, Map<Partition, Long> offsets)
			throws ConsumerException {
		Map<TopicAndPartition, OffsetAndMetadata> requestInfo =
				MapIterate.collect(offsets, new CreateRequestInfoMapEntryFunction());
		OffsetCommitResponse offsetCommitResponse = null;
		try {
			offsetCommitResponse = simpleConsumer.commitOffsets(
					new OffsetCommitRequest(consumerId, requestInfo, createCorrelationId(),
							simpleConsumer.clientId(), kafka.api.OffsetCommitRequest.CurrentVersion()));
		}
		catch (Exception e) {
			throw new ConsumerException(e);
		}
		ResultBuilder<Void> resultBuilder = new ResultBuilder<Void>();
		for (TopicAndPartition topicAndPartition : requestInfo.keySet()) {
			if (offsetCommitResponse.errors().containsKey(topicAndPartition)) {
				Partition partition = new Partition(topicAndPartition.topic(), topicAndPartition.partition());
				resultBuilder.add(partition).withError((Short) offsetCommitResponse.errors().get(topicAndPartition));
			}
		}
		return resultBuilder.build();
	}

	/**
	 * @see Connection#findLeaders(String...)
	 */
	@Override
	@Deprecated
	public Result<BrokerAddress> findLeaders(String... topics) throws ConsumerException {
		TopicMetadataRequest topicMetadataRequest =
				new TopicMetadataRequest(Arrays.asList(topics), createCorrelationId());
		TopicMetadataResponse topicMetadataResponse = null;
		try {
			topicMetadataResponse = simpleConsumer.send(topicMetadataRequest);
		}
		catch (Exception e) {
			throw new ConsumerException(e);
		}
		ResultBuilder<BrokerAddress> resultBuilder = new ResultBuilder<BrokerAddress>();
		for (TopicMetadata topicMetadata : topicMetadataResponse.topicsMetadata()) {
			if (topicMetadata.errorCode() != ErrorMapping.NoError()) {
				resultBuilder.add(new Partition(topicMetadata.topic(), -1)).withError(topicMetadata.errorCode());
			}
			else {
				for (PartitionMetadata partitionMetadata : topicMetadata.partitionsMetadata()) {
					Partition partition = new Partition(topicMetadata.topic(), partitionMetadata.partitionId());
					if (ErrorMapping.NoError() == partitionMetadata.errorCode()) {
						Broker leader = partitionMetadata.leader();
						BrokerAddress result = new BrokerAddress(leader.host(), leader.port());
						resultBuilder.add(partition).withResult(result);
					}
					else {
						resultBuilder.add(partition).withError(partitionMetadata.errorCode());
					}
				}

			}
		}
		return resultBuilder.build();
	}



	/**
	 * Creat a pseudo-unique correlation id for requests and responses
	 * @return correlation id
	 */
	private Integer createCorrelationId() {
		return correlationIdCounter.incrementAndGet();
	}

	@SuppressWarnings("serial")
	private static class ConvertToKafkaMessageFunction implements Function<MessageAndOffset, KafkaMessage> {
		private final FetchRequest request;

		public ConvertToKafkaMessageFunction(FetchRequest request) {
			this.request = request;
		}

		@Override
		public KafkaMessage valueOf(MessageAndOffset messageAndOffset) {
			return new KafkaMessage(messageAndOffset.message(),
					new KafkaMessageMetadata(request.getPartition(), messageAndOffset.offset(),
							messageAndOffset.nextOffset()));
		}
	}

	@SuppressWarnings("serial")
	private static class ConvertToTopicAndPartitionFunction implements Function<Partition, TopicAndPartition> {

		@Override
		public TopicAndPartition valueOf(Partition partition) {
			return new TopicAndPartition(partition.getTopic(), partition.getId());
		}

	}

	@SuppressWarnings("serial")
	private static class CreateRequestInfoMapEntryFunction
			implements Function2<Partition, Long, Pair<TopicAndPartition, OffsetAndMetadata>> {

		@Override
		public Pair<TopicAndPartition, OffsetAndMetadata> value(Partition partition, Long offset) {
			return Tuples.pair(new TopicAndPartition(partition.getTopic(), partition.getId()),
					new OffsetAndMetadata(offset, OffsetAndMetadata.NoMetadata(), OffsetAndMetadata.InvalidTime()));
		}

	}

}

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.gs.collections.api.RichIterable;
import com.gs.collections.api.block.function.Function;
import com.gs.collections.api.block.predicate.Predicate;
import com.gs.collections.api.list.ImmutableList;
import com.gs.collections.api.list.MutableList;
import com.gs.collections.api.multimap.list.ImmutableListMultimap;
import com.gs.collections.api.multimap.set.MutableSetMultimap;
import com.gs.collections.api.partition.PartitionIterable;
import com.gs.collections.api.set.MutableSet;
import com.gs.collections.api.tuple.Pair;
import com.gs.collections.impl.block.factory.Functions;
import com.gs.collections.impl.block.function.checked.CheckedFunction;
import com.gs.collections.impl.factory.Lists;
import com.gs.collections.impl.factory.Sets;
import com.gs.collections.impl.list.mutable.FastList;
import com.gs.collections.impl.map.mutable.UnifiedMap;
import com.gs.collections.impl.utility.ArrayIterate;
import com.gs.collections.impl.utility.Iterate;
import kafka.common.ErrorMapping;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.kafka.core.BrokerAddress;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.ConsumerException;
import org.springframework.integration.kafka.core.FetchRequest;
import org.springframework.integration.kafka.core.KafkaConsumerDefaults;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.KafkaMessageBatch;
import org.springframework.integration.kafka.core.KafkaTemplate;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.Result;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.util.Assert;

/**
 * @author Marius Bogoevici
 */
public class KafkaMessageListenerContainer implements SmartLifecycle {

	public static final int DEFAULT_WAIT_FOR_LEADER_REFRESH_RETRY = 5000;

	private static final int DEFAULT_STOP_TIMEOUT = 1000;

	private static final Log log = LogFactory.getLog(KafkaMessageListenerContainer.class);

	private final GetOffsetForPartitionFunction getOffset = new GetOffsetForPartitionFunction();

	private final PartitionToLeaderFunction getLeader = new PartitionToLeaderFunction();

	private final Function<Partition, Partition> passThru = Functions.getPassThru();

	private final Object lifecycleMonitor = new Object();

	private final KafkaTemplate kafkaTemplate;

	private final String[] topics;

	private Partition[] partitions;

	public boolean autoStartup = true;

	private Executor fetchTaskExecutor;

	private Executor adminTaskExecutor;

	private Executor dispatcherTaskExecutor;

	private int concurrency = 1;

	private volatile boolean running = false;

	private int maxFetch = KafkaConsumerDefaults.FETCH_SIZE_INT;

	private int queueSize = 1024;

	private int stopTimeout = DEFAULT_STOP_TIMEOUT;

	private Object messageListener;

	private ErrorHandler errorHandler = new LoggingErrorHandler();

	private volatile OffsetManager offsetManager;

	private ConcurrentMap<Partition, Long> fetchOffsets;

	private ConcurrentMessageListenerDispatcher messageDispatcher;

	private final ConcurrentMap<BrokerAddress, FetchTask> fetchTasksByBroker = new ConcurrentHashMap<>();

	private boolean autoCommitOnError = false;

	public KafkaMessageListenerContainer(ConnectionFactory connectionFactory, Partition... partitions) {
		Assert.notNull(connectionFactory, "A connection factory must be supplied");
		Assert.notEmpty(partitions, "A list of partitions must be provided");
		Assert.noNullElements(partitions, "The list of partitions cannot contain null elements");
		this.kafkaTemplate = new KafkaTemplate(connectionFactory);
		this.partitions = partitions;
		this.topics = null;
	}

	public KafkaMessageListenerContainer(final ConnectionFactory connectionFactory, String... topics) {
		Assert.notNull(connectionFactory, "A connection factory must be supplied");
		Assert.notNull(topics, "A list of topics must be provided");
		Assert.noNullElements(topics, "The list of topics cannot contain null elements");
		this.kafkaTemplate = new KafkaTemplate(connectionFactory);
		this.topics = topics;
	}

	public OffsetManager getOffsetManager() {
		return offsetManager;
	}

	public void setOffsetManager(OffsetManager offsetManager) {
		this.offsetManager = offsetManager;
	}

	public Object getMessageListener() {
		return messageListener;
	}

	public void setMessageListener(Object messageListener) {
		Assert.isTrue(
				messageListener instanceof MessageListener || messageListener instanceof AcknowledgingMessageListener,
				"Either a " + MessageListener.class.getName() + " or a " + AcknowledgingMessageListener.class.getName()
						+ " must be provided");
		this.messageListener = messageListener;
	}

	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public int getConcurrency() {
		return concurrency;
	}

	/**
	 * The maximum number of concurrent {@link MessageListener}s running. Messages from
	 * within the same partition will be processed sequentially.
	 * @param concurrency the concurrency maximum number
	 */
	public void setConcurrency(int concurrency) {
		this.concurrency = concurrency;
	}

	/**
	 * The timeout for waiting for each concurrent {@link MessageListener} to finish on
	 * stopping.
	 * @param stopTimeout timeout in milliseconds
	 * @since 1.1
	 */
	public void setStopTimeout(int stopTimeout) {
		this.stopTimeout = stopTimeout;
	}

	public int getStopTimeout() {
		return stopTimeout;
	}

	public Executor getFetchTaskExecutor() {
		return fetchTaskExecutor;
	}

	/**
	 * The task executor for fetch operations.
	 * @param fetchTaskExecutor the Executor for fetch operations
	 */
	public void setFetchTaskExecutor(Executor fetchTaskExecutor) {
		this.fetchTaskExecutor = fetchTaskExecutor;
	}

	public Executor getAdminTaskExecutor() {
		return adminTaskExecutor;
	}

	/**
	 * The task executor for leader, offset, and partition reassignment updates.
	 * @param adminTaskExecutor the task executor for leader, offset and partition reassignment updates
	 */
	public void setAdminTaskExecutor(Executor adminTaskExecutor) {
		this.adminTaskExecutor = adminTaskExecutor;
	}

	/**
	 * The task executor for invoking the MessageListener
	 * @param dispatcherTaskExecutor the task executor for invoking the MessageListener
	 */
	public void setDispatcherTaskExecutor(Executor dispatcherTaskExecutor) {
		this.dispatcherTaskExecutor = dispatcherTaskExecutor;
	}

	/**
	 * @return the maximum amount of data (in bytes) that pollers will fetch in one round
	 */
	public int getMaxFetch() {
		return maxFetch;
	}

	public int getQueueSize() {
		return queueSize;
	}

	/**
	 * The maximum number of messages that are buffered by each concurrent
	 * {@link MessageListener} runner. Increasing the value may increase throughput, but
	 * also increases the memory consumption. Must be a positive number and a power of 2.
	 * @param queueSize the queue size
	 */
	public void setQueueSize(int queueSize) {
		Assert.isTrue(queueSize > 0 && Integer.bitCount(queueSize) == 1,
				"'queueSize' must be a positive number and a power of 2");
		this.queueSize = queueSize;
	}

	public void setMaxFetch(int maxFetch) {
		this.maxFetch = maxFetch;
	}

	/**
	 * Whether offsets should be auto acknowledged even when exceptions are thrown during processing. This setting
	 * is effective only in auto acknowledged mode. When set to true, all received messages will be acknowledged,
	 * and when set to false only the offset of the last successfully processed message is persisted, even if the
	 * component will try to continue processing incoming messages. In the latter case, it is possible that
	 * a successful message will commit an offset after a series of failures, so the component should rely on
	 * the `errorHandler` to capture failures.
	 * @param autoCommitOnError false if offsets should be committed only for successful messages
	 * @since 1.3
	 */
	public void setAutoCommitOnError(boolean autoCommitOnError) {
		this.autoCommitOnError = autoCommitOnError;
	}

	public boolean isAutoCommitOnError() {
		return autoCommitOnError;
	}

	@Override
	public boolean isAutoStartup() {
		return autoStartup;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public void stop(Runnable callback) {
		synchronized (lifecycleMonitor) {
			if (running) {
				this.running = false;
				try {
					this.offsetManager.flush();
				}
				catch (IOException e) {
					log.error("Error while flushing:", e);
				}
				this.messageDispatcher.stop(stopTimeout);
			}
		}
		if (callback != null) {
			callback.run();
		}
	}

	@Override
	public void start() {
		synchronized (lifecycleMonitor) {
			if (!running) {
				if (partitions == null) {
					partitions = getPartitionsForTopics(kafkaTemplate.getConnectionFactory(), topics);
				}
				this.running = true;
				if (this.offsetManager == null) {
					this.offsetManager = new MetadataStoreOffsetManager(kafkaTemplate.getConnectionFactory());
				}
				// initialize the fetch offset table - defer to OffsetManager for retrieving them
				ImmutableList<Partition> partitionsAsList = Lists.immutable.with(partitions);
				this.fetchOffsets = new ConcurrentHashMap<Partition, Long>(partitionsAsList.toMap(passThru, getOffset));
				this.messageDispatcher = new ConcurrentMessageListenerDispatcher(messageListener, errorHandler,
						Arrays.asList(partitions), offsetManager, concurrency, queueSize, dispatcherTaskExecutor,
						autoCommitOnError);
				this.messageDispatcher.start();
				fetchTasksByBroker.clear();
				ImmutableListMultimap<BrokerAddress, Partition> partitionsByLeader = partitionsAsList
						.groupBy(getLeader);
				if (fetchTaskExecutor == null) {
					fetchTaskExecutor = new SimpleAsyncTaskExecutor("kafka-fetch-");
				}
				if (adminTaskExecutor == null) {
					adminTaskExecutor = Executors.newSingleThreadExecutor();
				}
				for (Pair<BrokerAddress, RichIterable<Partition>> entry : partitionsByLeader.keyMultiValuePairsView()) {
					FetchTask fetchTask = new FetchTask(entry.getOne(), entry.getTwo());
					fetchTaskExecutor.execute(fetchTask);
					fetchTasksByBroker.put(entry.getOne(), fetchTask);
				}
			}
		}
	}

	@Override
	public void stop() {
		this.stop(null);
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	private static Partition[] getPartitionsForTopics(final ConnectionFactory connectionFactory, String[] topics) {
		MutableList<Partition> partitionList =
				ArrayIterate.flatCollect(topics, new GetPartitionsForTopic(connectionFactory));
		return partitionList.toArray(new Partition[partitionList.size()]);
	}

	/**
	 * Fetches data from Kafka for a group of partitions, located on the same broker.
	 */
	public class FetchTask implements SchedulingAwareRunnable {

		private final BrokerAddress brokerAddress;

		private final MutableSet<Partition> listenedPartitions = Sets.mutable.<Partition>of().asSynchronized();

		private volatile boolean active;

		private final PartitionToFetchRequestFunction partitionToFetchRequestFunction =
				new PartitionToFetchRequestFunction();

		private final IsLeaderErrorPredicate isLeaderPredicate = new IsLeaderErrorPredicate();

		private final IsOffsetOutOfRangePredicate offsetOutOfRangePredicate = new IsOffsetOutOfRangePredicate();

		public FetchTask(BrokerAddress brokerAddress, RichIterable<Partition> initialPartitions) {
			this.brokerAddress = brokerAddress;
			this.active = true;
			this.listenedPartitions.addAll(initialPartitions.toSet());
		}

		@Override
		public boolean isLongLived() {
			return true;
		}

		public boolean addListenedPartitionsIfActive(Iterable<Partition> partitions) {
			synchronized (listenedPartitions) {
				if (active) {
					listenedPartitions.addAllIterable(partitions);
				}
				return active;
			}
		}

		@Override
		public void run() {
			try {
				while (active && isRunning()) {
					synchronized (listenedPartitions) {
						try {
							if (!listenedPartitions.isEmpty()) {
								Result<KafkaMessageBatch> result = fetchAvailableData();
								handleSuccessful(result);
								if (result.getErrors().size() > 0) {
									handleErrors(result);
								}
							}
							else {
								active = false;
							}
						}
						catch (ConsumerException e) {
							active = false;
							// the connection is broken, terminate the task
							kafkaTemplate.getConnectionFactory().disconnect(brokerAddress);
							resetLeaders(listenedPartitions.toImmutable());
						}
					}
				}
			}
			finally {
				active = false;
				synchronized (fetchTasksByBroker) {
					if (fetchTasksByBroker.get(brokerAddress) == this) {
						fetchTasksByBroker.remove(brokerAddress);
					}
				}
			}
		}

		private Result<KafkaMessageBatch> fetchAvailableData() {
			return kafkaTemplate.receive(listenedPartitions.collect(partitionToFetchRequestFunction));
		}

		private void handleSuccessful(Result<KafkaMessageBatch> result) {
			Iterable<KafkaMessageBatch> batches = result.getResults().values();
			for (KafkaMessageBatch batch : batches) {
				if (!batch.getMessages().isEmpty()) {
					long highestFetchedOffset = 0;
					for (KafkaMessage kafkaMessage : batch.getMessages()) {
						// fetch operations may return entire blocks of compressed messages,
						// which may have lower offsets than the ones requested
						// thus a batch may contain messages that have been processed already
						if (kafkaMessage.getMetadata().getOffset() >= fetchOffsets.get(batch.getPartition())) {
							messageDispatcher.dispatch(kafkaMessage);
						}
						highestFetchedOffset = Math.max(highestFetchedOffset, kafkaMessage.getMetadata().getNextOffset());
					}
					fetchOffsets.replace(batch.getPartition(), highestFetchedOffset);
				}
			}
		}

		private void handleErrors(Result<KafkaMessageBatch> result) {
			Map<Partition, Short> errors = result.getErrors();
			PartitionIterable<Map.Entry<Partition, Short>> splitByLeaderError =
					Iterate.partition(errors.entrySet(), isLeaderPredicate);
			RichIterable<Partition> partitionsWithLeaderErrors = splitByLeaderError.getSelected()
					.collect(Functions.<Partition>getKeyFunction());
			resetLeaders(partitionsWithLeaderErrors);
			PartitionIterable<Map.Entry<Partition, Short>> splitByOffsetError =
					splitByLeaderError.getRejected().partition(offsetOutOfRangePredicate);
			RichIterable<Partition> partitionsWithWrongOffsets =
					splitByOffsetError.getSelected().collect(Functions.<Partition>getKeyFunction());
			resetOffsets(partitionsWithWrongOffsets.toSet());
			// it's not a leader issue, remove everything else
			RichIterable<Partition> remainingPartitionsWithErrors
					= splitByOffsetError.getRejected().collect(Functions.<Partition>getKeyFunction());
			listenedPartitions.removeAllIterable(remainingPartitionsWithErrors);
		}

		private void resetLeaders(final Iterable<Partition> partitionsToReset) {
			listenedPartitions.removeAllIterable(partitionsToReset);
			adminTaskExecutor.execute(new UpdateLeadersTask(partitionsToReset));
		}

		private void resetOffsets(final Collection<Partition> partitionsToResetOffsets) {
			listenedPartitions.removeAllIterable(partitionsToResetOffsets);
			adminTaskExecutor.execute(new UpdateOffsetsTask(partitionsToResetOffsets));
		}

		private class UpdateLeadersTask implements SchedulingAwareRunnable {

			private final Iterable<Partition> partitionsToReset;

			public UpdateLeadersTask(Iterable<Partition> partitionsToReset) {
				this.partitionsToReset = partitionsToReset;
			}

			@Override
			public boolean isLongLived() {
				return true;
			}

			@Override
			public void run() {
				// fetch can complete successfully or unsuccessfully
				boolean fetchCompleted = false;
				while (!fetchCompleted && isRunning()) {
					try {
						FastList<Partition> partitionsAsList = FastList.newList(partitionsToReset);
						FastList<String> topics = partitionsAsList.collect(new PartitionToTopicFunction()).distinct();
						kafkaTemplate.getConnectionFactory().refreshMetadata(topics);

						MutableSetMultimap<BrokerAddress, Partition> partitionsByBroker = UnifiedMap
								.newMap(kafkaTemplate.getConnectionFactory().getLeaders(partitionsToReset)).flip();
						for (Pair<BrokerAddress, RichIterable<Partition>> pair : partitionsByBroker
								.keyMultiValuePairsView()) {
							synchronized (fetchTasksByBroker) {
								boolean addedSuccessfully = false;
								FetchTask fetchTask = fetchTasksByBroker.get(pair.getOne());
								if (fetchTask != null) {
									addedSuccessfully = fetchTask.addListenedPartitionsIfActive(pair.getTwo());
								}
								if (!addedSuccessfully) {
									fetchTask = new FetchTask(pair.getOne(), pair.getTwo());
									fetchTaskExecutor.execute(fetchTask);
									fetchTasksByBroker.put(pair.getOne(), fetchTask);
								}
							}
						}
						fetchCompleted = true;
					}
					catch (Exception e) {
						if (isRunning()) {
							try {
								Thread.sleep(DEFAULT_WAIT_FOR_LEADER_REFRESH_RETRY);
							}
							catch (InterruptedException e1) {
								Thread.currentThread().interrupt();
								log.error("Interrupted after refresh leaders failure for: " + Iterate
										.makeString(partitionsToReset, ","));
								fetchCompleted = true;
							}
						}
					}
				}
			}

		}

		private class UpdateOffsetsTask implements Runnable {

			private final Collection<Partition> partitionsToResetOffsets;

			public UpdateOffsetsTask(Collection<Partition> partitionsToResetOffsets) {
				this.partitionsToResetOffsets = partitionsToResetOffsets;
			}

			@Override
			public void run() {
				offsetManager.resetOffsets(partitionsToResetOffsets);
				for (Partition partition : partitionsToResetOffsets) {
					fetchOffsets.replace(partition, offsetManager.getOffset(partition));
				}
				synchronized (fetchTasksByBroker) {
					boolean addedSuccessfully = false;
					FetchTask fetchTask = fetchTasksByBroker.get(brokerAddress);
					if (fetchTask != null) {
						addedSuccessfully = fetchTask.addListenedPartitionsIfActive(partitionsToResetOffsets);
					}
					if (!addedSuccessfully) {
						fetchTask = new FetchTask(brokerAddress, Sets.immutable.ofAll(partitionsToResetOffsets));
						fetchTaskExecutor.execute(fetchTask);
						fetchTasksByBroker.put(brokerAddress, fetchTask);
					}
				}
			}

		}

		@SuppressWarnings("serial")
		private class IsLeaderErrorPredicate implements Predicate<Map.Entry<Partition, Short>> {

			@Override
			public boolean accept(Map.Entry<Partition, Short> each) {
				return each.getValue() == ErrorMapping.NotLeaderForPartitionCode() || each.getValue() == ErrorMapping
						.UnknownTopicOrPartitionCode();
			}

		}

		@SuppressWarnings("serial")
		private class IsOffsetOutOfRangePredicate implements Predicate<Map.Entry<Partition, Short>> {

			@Override
			public boolean accept(Map.Entry<Partition, Short> each) {
				return each.getValue() == ErrorMapping.OffsetOutOfRangeCode();
			}

		}
	}

	@SuppressWarnings("serial")
	class GetOffsetForPartitionFunction extends CheckedFunction<Partition, Long> {

		@Override
		public Long safeValueOf(Partition object) throws Exception {
			try {
				return offsetManager.getOffset(object);
			}
			catch (Exception e) {
				log.error(e);
				throw e;
			}
		}

	}

	@SuppressWarnings("serial")
	private class PartitionToLeaderFunction implements Function<Partition, BrokerAddress> {

		@Override
		public BrokerAddress valueOf(Partition partition) {
			return kafkaTemplate.getConnectionFactory().getLeader(partition);
		}

	}

	@SuppressWarnings("serial")
	private class PartitionToFetchRequestFunction implements Function<Partition, FetchRequest> {

		@Override
		public FetchRequest valueOf(Partition partition) {
			return new FetchRequest(partition, fetchOffsets.get(partition), maxFetch);
		}

	}

	@SuppressWarnings("serial")
	static class GetPartitionsForTopic extends CheckedFunction<String, Iterable<Partition>> {

		private final ConnectionFactory connectionFactory;

		public GetPartitionsForTopic(ConnectionFactory connectionFactory) {
			this.connectionFactory = connectionFactory;
		}

		@Override
		public Iterable<Partition> safeValueOf(String topic) throws Exception {
			return connectionFactory.getPartitions(topic);
		}

	}

	@SuppressWarnings("serial")
	private class PartitionToTopicFunction implements Function<Partition, String> {

		@Override
		public String valueOf(Partition object) {
			return object.getTopic();
		}

	}

}

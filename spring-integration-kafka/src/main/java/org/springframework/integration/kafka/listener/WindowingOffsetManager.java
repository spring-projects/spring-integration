/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.util.Assert;

import reactor.Environment;
import reactor.core.processor.RingBufferProcessor;
import reactor.fn.BiFunction;
import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.rx.Stream;
import reactor.rx.Streams;
import reactor.rx.stream.GroupedStream;

/**
 * An {@link OffsetManager} that aggregates writes over a time or count window, using an underlying delegate to
 * do the actual operations. Its purpose is to reduce the performance impact of writing operations
 * wherever this is desirable.
 * <p>
 * A time window or a number of writes can be specified, or both.
 * Defaults to 10 seconds window with {@link Integer#MAX_VALUE} buffer.
 * @author Marius Bogoevici
 * @author Artem Bilan
 * @since 1.3.1
 */
public class WindowingOffsetManager implements OffsetManager, InitializingBean, DisposableBean {

	static {
		Environment.initializeIfEmpty();
	}

	private static final BiFunction<Long, Long, Long> maxFunction = new BiFunction<Long, Long, Long>() {

		@Override
		public Long apply(Long aLong, Long bLong) {
			return Math.max(aLong, bLong);
		}

	};

	private static final Function<PartitionAndOffset, Long> offsetFunction
			= new Function<PartitionAndOffset, Long>() {

		@Override
		public Long apply(PartitionAndOffset partitionAndOffset) {
			return partitionAndOffset.getOffset();
		}

	};

	private static final ComputeMaximumOffsetByPartitionFunction findHighestOffsetInPartitionGroup
			= new ComputeMaximumOffsetByPartitionFunction();

	private static final Function<PartitionAndOffset, Partition> getPartitionFunction
			= new Function<PartitionAndOffset, Partition>() {

		@Override
		public Partition apply(PartitionAndOffset partitionAndOffset) {
			return partitionAndOffset.getPartition();
		}

	};

	private static final FindHighestOffsetsByPartitionFunction findHighestOffsetsByPartition
			= new FindHighestOffsetsByPartitionFunction();

	private final Consumer<PartitionAndOffset> delegateUpdateOffset = new Consumer<PartitionAndOffset>() {

		@Override
		public void accept(PartitionAndOffset partitionAndOffset) {
			delegate.updateOffset(partitionAndOffset.getPartition(), partitionAndOffset.getOffset());
		}

	};

	private final Consumer<Void> offsetComplete = new Consumer<Void>() {

		@Override
		public void accept(Void aVoid) {
			createOffsetsStream();
		}

	};

	private final ReadWriteLock offsetsLock = new ReentrantReadWriteLock();

	private final OffsetManager delegate;

	private long timespan = 10 * 1000;

	private int count = Integer.MAX_VALUE;

	private int shutdownTimeout = 2000;

	private volatile RingBufferProcessor<PartitionAndOffset> offsets;

	private volatile boolean closed;

	public WindowingOffsetManager(OffsetManager offsetManager) {
		this.delegate = offsetManager;
	}

	/**
	 * The timespan for aggregating write operations, before invoking the underlying {@link OffsetManager}.
	 * @param timespan duration in milliseconds
	 */
	public void setTimespan(long timespan) {
		Assert.isTrue(timespan >= 0, "Timespan must be a positive value");
		this.timespan = timespan;
	}

	/**
	 * How many writes should be aggregated, before invoking the underlying {@link OffsetManager}. Setting this value
	 * to 1 effectively disables windowing.
	 * @param count number of writes
	 */
	public void setCount(int count) {
		Assert.isTrue(count >= 0, "Count must be a positive value");
		this.count = count;
	}

	/**
	 * The timeout that {@link #close()} and {@link #destroy()}
	 * operations will wait for receiving a confirmation that
	 * the underlying writes have been processed.
	 * @param shutdownTimeout duration in milliseconds
	 */
	public void setShutdownTimeout(int shutdownTimeout) {
		this.shutdownTimeout = shutdownTimeout;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.count != 1) {
			createOffsetsStream();
		}
	}

	private void createOffsetsStream() {
		if (!this.closed) {
			this.offsetsLock.writeLock().lock();
			try {
				this.offsets = RingBufferProcessor.share("spring-integration-kafka-offset", 1024);
			}
			finally {
				this.offsetsLock.writeLock().unlock();
			}
			Streams.wrap(this.offsets)
					.window(this.count, timespan, TimeUnit.MILLISECONDS)
					.flatMap(findHighestOffsetsByPartition)
					.consume(this.delegateUpdateOffset, null, this.offsetComplete);

		}
	}

	@Override
	public void destroy() throws Exception {
		flush();
		close();
		if (this.delegate instanceof DisposableBean) {
			((DisposableBean) this.delegate).destroy();
		}
	}

	@Override
	public void updateOffset(Partition partition, long offset) {
		if (this.offsets != null) {
			this.offsetsLock.readLock().lock();
			try {
				this.offsets.onNext(new PartitionAndOffset(partition, offset));
			}
			finally {
				this.offsetsLock.readLock().unlock();
			}
		}
		else {
			this.delegate.updateOffset(partition, offset);
		}
	}

	@Override
	public long getOffset(Partition partition) {
		doFlush();
		return this.delegate.getOffset(partition);
	}

	@Override
	public void deleteOffset(Partition partition) {
		doFlush();
		this.delegate.deleteOffset(partition);
	}

	@Override
	public void resetOffsets(Collection<Partition> partition) {
		doFlush();
		this.delegate.resetOffsets(partition);
	}

	@Override
	public void close() throws IOException {
		this.closed = true;
		this.delegate.close();
	}

	@Override
	public void flush() throws IOException {
		if (this.offsets != null) {
			this.offsets.awaitAndShutdown(this.shutdownTimeout, TimeUnit.MILLISECONDS);
		}
		this.delegate.flush();
	}

	private void doFlush() {
		try {
			flush();
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}


	private static class PartitionAndOffset {

		private final Partition partition;

		private final Long offset;

		public PartitionAndOffset(Partition partition, Long offset) {
			this.partition = partition;
			this.offset = offset;
		}

		public Partition getPartition() {
			return partition;
		}

		public Long getOffset() {
			return offset;
		}

		@Override
		public String toString() {
			return "PartitionAndOffset{" +
					"partition=" + partition +
					", offset=" + offset +
					'}';
		}

	}


	private static class ComputeMaximumOffsetByPartitionFunction
			implements Function<GroupedStream<Partition, PartitionAndOffset>, Stream<PartitionAndOffset>> {

		@Override
		public Stream<PartitionAndOffset> apply(final GroupedStream<Partition, PartitionAndOffset> group) {
			return group
					.map(offsetFunction)
					.reduce(maxFunction)
					.map(new Function<Long, PartitionAndOffset>() {

						@Override
						public PartitionAndOffset apply(Long offset) {
							return new PartitionAndOffset(group.key(), offset);
						}

					});
		}

	}

	private static class FindHighestOffsetsByPartitionFunction
			implements Function<Stream<PartitionAndOffset>, Stream<PartitionAndOffset>> {

		@Override
		public Stream<PartitionAndOffset> apply(Stream<PartitionAndOffset> windowBuffer) {
			return windowBuffer
					.groupBy(getPartitionFunction)
					.flatMap(findHighestOffsetInPartitionGroup);
		}

	}

}


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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.util.Assert;

import com.gs.collections.api.block.procedure.Procedure;
import com.gs.collections.api.block.procedure.Procedure2;
import com.gs.collections.api.map.MutableMap;
import com.gs.collections.impl.factory.Maps;

/**
 * Dispatches {@link KafkaMessage}s to a {@link MessageListener}. Messages may be
 * processed concurrently, according to the {@code concurrency} settings, but messages
 * from the same partition are being processed in their original order.
 *
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
class ConcurrentMessageListenerDispatcher {

	private static final Log log = LogFactory.getLog(ConcurrentMessageListenerDispatcher.class);

	private static final StartDelegateProcedure startDelegateProcedure = new StartDelegateProcedure();

	private static final StopDelegateProcedure stopDelegateProcedure = new StopDelegateProcedure();

	private final Object lifecycleMonitor = new Object();

	private final Collection<Partition> partitions;

	private final int consumers;

	private final Object delegateListener;

	private final ErrorHandler errorHandler;

	private final OffsetManager offsetManager;

	private final int queueSize;

	private final Executor taskExecutor;

	private volatile boolean running;

	private MutableMap<Partition, QueueingMessageListenerInvoker> delegates;

	public ConcurrentMessageListenerDispatcher(Object delegateListener, ErrorHandler errorHandler,
											   Collection<Partition> partitions, OffsetManager offsetManager,
											   int consumers, int queueSize, Executor taskExecutor) {
		Assert.isTrue
				(delegateListener instanceof MessageListener
								|| delegateListener instanceof AcknowledgingMessageListener,
						"Either a " + MessageListener.class.getName() + " or a "
								+ AcknowledgingMessageListener.class.getName() + " must be provided");
		Assert.notEmpty(partitions, "A set of partitions must be provided");
		Assert.isTrue(consumers <= partitions.size(),
				"The number of consumers must be smaller or equal to the number of partitions");
		Assert.notNull(delegateListener, "A delegate must be provided");
		this.delegateListener = delegateListener;
		this.errorHandler = errorHandler;
		this.partitions = partitions;
		this.offsetManager = offsetManager;
		this.consumers = Math.min(partitions.size(), consumers);
		this.queueSize = queueSize;
		this.taskExecutor = taskExecutor;
	}

	public void start() {
		synchronized (lifecycleMonitor) {
			if (!this.running) {
				initializeAndStartDispatching();
				this.running = true;
			}
		}
	}

	public void stop(int stopTimeout) {
		synchronized (lifecycleMonitor) {
			if (this.running) {
				this.running = false;
				delegates.flip().keyBag().toSet().forEachWith(stopDelegateProcedure, stopTimeout);
			}
			this.delegates = null;
		}
	}

	public void dispatch(KafkaMessage message) {
		if (this.running) {
			delegates.get(message.getMetadata().getPartition()).enqueue(message);
		}
	}

	private void initializeAndStartDispatching() {
		// allocate delegate instances index them
		List<QueueingMessageListenerInvoker> delegateList = new ArrayList<QueueingMessageListenerInvoker>(consumers);
		for (int i = 0; i < consumers; i++) {
			QueueingMessageListenerInvoker queueingMessageListenerInvoker =
					new QueueingMessageListenerInvoker(queueSize, offsetManager, delegateListener, errorHandler,
							taskExecutor);
			delegateList.add(queueingMessageListenerInvoker);
		}

		// evenly distribute partitions across delegates
		delegates = Maps.mutable.of();
		int i = 0;
		for (Partition partition : partitions) {
			delegates.put(partition, delegateList.get((i++) % consumers));
		}

		// start dispatchers
		delegates.flip().keyBag().toSet().forEach(startDelegateProcedure);
	}

	@SuppressWarnings("serial")
	private static class StopDelegateProcedure implements Procedure2<QueueingMessageListenerInvoker, Integer> {

		@Override
		public void value(QueueingMessageListenerInvoker delegate, Integer stopTimeout) {
			try {
				delegate.stop(stopTimeout);
			}
			catch (Exception e) {
				// ignore the exception, but log it
				if (log.isInfoEnabled()) {
					log.info("Exception thrown while stopping dispatcher:", e);
				}
			}
		}

	}

	@SuppressWarnings("serial")
	private static class StartDelegateProcedure implements Procedure<QueueingMessageListenerInvoker> {

		@Override
		public void value(QueueingMessageListenerInvoker delegate) {
			delegate.start();
		}

	}

}

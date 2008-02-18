/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.dispatcher;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.scheduling.MessagingTask;
import org.springframework.integration.scheduling.MessagingTaskScheduler;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link MessageDispatcher}. For a non-broadcasting
 * {@link MessageChannel} (point-to-point), each handler can be registered with
 * a {@link Schedule}. If the channel is broadcasting (publish-subscribe), the
 * handlers will all be scheduled together according to the dispatcher's
 * {@link #defaultSchedule}.
 * 
 * @author Mark Fisher
 */
public class DefaultMessageDispatcher implements SchedulingMessageDispatcher {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final MessageChannel channel;

	private final MessageRetriever retriever;

	private final MessagingTaskScheduler scheduler;

	private volatile Schedule defaultSchedule = new PollingSchedule(5);

	private final ConcurrentMap<Schedule, List<MessageHandler>> scheduledHandlers = new ConcurrentHashMap<Schedule, List<MessageHandler>>();

	private final AtomicLong totalMessagesProcessed = new AtomicLong();

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	public DefaultMessageDispatcher(MessageChannel channel, MessagingTaskScheduler scheduler) {
		Assert.notNull(channel, "'channel' must not be null");
		Assert.notNull(scheduler, "'scheduler' must not be null");
		this.channel = channel;
		this.scheduler = scheduler;
		this.retriever = new ChannelPollingMessageRetriever(this.channel);
	}


	public void setDefaultSchedule(Schedule defaultSchedule) {
		Assert.notNull(defaultSchedule, "'defaultSchedule' must not be null");
		this.defaultSchedule = defaultSchedule;
	}

	public void addHandler(MessageHandler handler) {
		this.addHandler(handler, null);
	}

	public void addHandler(MessageHandler handler, Schedule schedule) {
		Assert.notNull(handler, "'handler' must not be null");
		if (schedule == null) {
			schedule = this.defaultSchedule;
		}
		else if (this.channel.getDispatcherPolicy().isPublishSubscribe()) {
			if (logger.isInfoEnabled()) {
				logger.info("This dispatcher broadcasts messages for a publish-subscribe channel. " + 
						"Therefore all handlers are scheduled with its 'defaultSchedule', " +
						"and the provided schedule will be ignored.");
			}
			schedule = this.defaultSchedule;
		}
		if (this.isRunning() && handler instanceof Lifecycle) {
			((Lifecycle) handler).start();
		}
		List<MessageHandler> handlers = this.scheduledHandlers.get(schedule);
		if (handlers == null) {
			handlers = this.scheduledHandlers.putIfAbsent(schedule, new CopyOnWriteArrayList<MessageHandler>());
		}
		this.scheduledHandlers.get(schedule).add(handler);
		if (handlers == null && this.isRunning()) {
			this.scheduleDispatcherTask(schedule);
		}
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				return;
			}
			if (this.scheduler == null) {
				throw new MessagingConfigurationException("'scheduler' is required");
			}
			if (!this.scheduler.isRunning()) {
				this.scheduler.start();
			}
			for (Schedule schedule : this.scheduledHandlers.keySet()) {
				scheduleDispatcherTask(schedule);
			}
			this.running = true;
		}
	}

	private void scheduleDispatcherTask(Schedule schedule) {
		List<MessageHandler> handlers = this.scheduledHandlers.get(schedule);
		for (MessageHandler handler : handlers) {
			if (handler instanceof Lifecycle) {
				((Lifecycle) handler).start();
			}
		}
		this.scheduler.schedule(new DispatcherTask(schedule));
	}

	public void stop() {
		if (!this.running) {
			return;
		}
		synchronized (this.lifecycleMonitor) {
			for (List<MessageHandler> handlerList : scheduledHandlers.values()) {
				for (MessageHandler handler : handlerList) {
					if (handler instanceof Lifecycle) {
						((Lifecycle) handler).stop();
					}
				}
			}
			this.running = false;
		}
	}

	public int dispatch() {
		MessageDistributor distributor = this.getDistributor(this.defaultSchedule);
		return this.doDispatch(distributor);
	}

	private int doDispatch(MessageDistributor distributor) {
		int messagesProcessed = 0;
		Collection<Message<?>> messages = this.retriever.retrieveMessages();
		if (messages == null) {
			return 0;
		}
		for (Message<?> message : messages) {
			if (distributor.distribute(message)) {
				messagesProcessed++;
			}
		}
		totalMessagesProcessed.addAndGet(messagesProcessed);
		return messagesProcessed;
	}

	private MessageDistributor getDistributor(Schedule schedule) {
		if (schedule == null) {
			schedule = this.defaultSchedule;
		}
		MessageDistributor distributor = new DefaultMessageDistributor(this.channel.getDispatcherPolicy());
		for (MessageHandler handler : this.scheduledHandlers.get(schedule)) {
			distributor.addHandler(handler);
		}
		return distributor;
	}


	private class DispatcherTask implements MessagingTask {

		private Schedule schedule;


		public DispatcherTask(Schedule schedule) {
			this.schedule = (schedule != null) ? schedule : defaultSchedule;
		}

		public Schedule getSchedule() {
			return this.schedule;
		}

		public void run() {
			doDispatch(getDistributor(this.schedule));
		}
	}

}

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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.scheduling.MessagingTaskScheduler;
import org.springframework.integration.scheduling.MessagingTaskSchedulerAware;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.integration.scheduling.SimpleMessagingTaskScheduler;
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
public class DefaultMessageDispatcher implements MessageDispatcher, MessagingTaskSchedulerAware {

	protected Log logger = LogFactory.getLog(this.getClass());

	private MessageChannel channel;

	private MessagingTaskScheduler scheduler;

	private Schedule defaultSchedule = new PollingSchedule(5);

	private Map<Schedule, List<MessageHandler>> scheduledHandlers = new ConcurrentHashMap<Schedule, List<MessageHandler>>();

	private List<ScheduledFuture<?>> futures = new CopyOnWriteArrayList<ScheduledFuture<?>>();

	private volatile boolean running;

	private Object lifecycleMonitor = new Object();


	public DefaultMessageDispatcher(MessageChannel channel) {
		Assert.notNull(channel, "'channel' must not be null");
		this.channel = channel;
	}


	public void setMessagingTaskScheduler(MessagingTaskScheduler scheduler) {
		Assert.notNull(scheduler, "'scheduler' must not be null");
		this.scheduler = scheduler;
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
		if (this.scheduledHandlers.containsKey(schedule)) {
			this.scheduledHandlers.get(schedule).add(handler);
		}
		else {
			List<MessageHandler> handlerList = new CopyOnWriteArrayList<MessageHandler>();
			handlerList.add(handler);
			this.scheduledHandlers.put(schedule, handlerList);
		}
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		if (this.scheduler == null) {
			if (logger.isInfoEnabled()) {
				logger.info("no scheduler was provided, will create one");
			}
			this.scheduler = new SimpleMessagingTaskScheduler();
		}
		if (!this.scheduler.isRunning()) {
			this.scheduler.start();
		}
		if (this.isRunning()) {
			return;
		}
		synchronized (this.lifecycleMonitor) {
			for (Map.Entry<Schedule, List<MessageHandler>> entry : this.scheduledHandlers.entrySet()) {
				Schedule schedule = entry.getKey();
				List<MessageHandler> handlers = entry.getValue();
				DispatcherTask task = new DispatcherTask(channel);
				task.setSchedule(schedule);
				for (MessageHandler handler : handlers) {
					if (handler instanceof Lifecycle) {
						((Lifecycle) handler).start();
					}
					task.addHandler(handler);
				}
				ScheduledFuture<?> future = this.scheduler.schedule(task);
				if (future != null) {
					futures.add(future);
				}
			}
			this.running = true;
		}
	}

	public void stop() {
		if (!this.isRunning()) {
			return;
		}
		synchronized (this.lifecycleMonitor) {
			for (ScheduledFuture<?> future : this.futures) {
				future.cancel(true);
				for (List<MessageHandler> handlerList : scheduledHandlers.values()) {
					for (MessageHandler handler : handlerList) {
						if (handler instanceof Lifecycle) {
							((Lifecycle) handler).stop();
						}
					}
				}
			}
			this.running = false;
		}
	}

}

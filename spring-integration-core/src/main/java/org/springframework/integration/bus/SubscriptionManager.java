/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.bus;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.dispatcher.PollingDispatcher;
import org.springframework.integration.message.Target;
import org.springframework.integration.scheduling.MessagingTaskScheduler;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * Manages subscriptions for {@link Target Targets} to a {@link MessageChannel}
 * including the creation, scheduling, and lifecycle management of dispatchers.
 * 
 * @author Mark Fisher
 */
public class SubscriptionManager {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final MessageChannel channel;

	private final MessagingTaskScheduler scheduler;

	private volatile Schedule defaultSchedule = new PollingSchedule(5);

	private final ConcurrentMap<Schedule, PollingDispatcher> dispatchers = new ConcurrentHashMap<Schedule, PollingDispatcher>();

	private final List<Lifecycle> lifecycleTargets = new CopyOnWriteArrayList<Lifecycle>();

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	public SubscriptionManager(MessageChannel channel, MessagingTaskScheduler scheduler) {
		Assert.notNull(channel, "channel must not be null");
		Assert.notNull(scheduler, "scheduler must not be null");
		this.channel = channel;
		this.scheduler = scheduler;
	}


	public void setDefaultSchedule(Schedule defaultSchedule) {
		Assert.notNull(defaultSchedule, "'defaultSchedule' must not be null");
		this.defaultSchedule = defaultSchedule;
	}

	public void addTarget(Target target) {
		this.addTarget(target, null);
	}

	public void addTarget(Target target, Schedule schedule) {
		Assert.notNull(target, "'target' must not be null");
		if (schedule == null) {
			schedule = this.defaultSchedule;
		}
		else if (this.channel.getDispatcherPolicy().isPublishSubscribe()) {
			if (logger.isInfoEnabled()) {
				logger.info("This dispatcher broadcasts messages for a publish-subscribe channel. " +
						"Therefore all targets are scheduled with its 'defaultSchedule', " +
						"and the provided schedule will be ignored.");
			}
			schedule = this.defaultSchedule;
		}
		if (target instanceof Lifecycle) {
			this.lifecycleTargets.add((Lifecycle) target);
			if (this.isRunning()) {
				((Lifecycle) target).start();
			}
		}
		PollingDispatcher dispatcher = this.dispatchers.get(schedule);
		if (dispatcher == null) {
			dispatcher = this.dispatchers.putIfAbsent(schedule, new PollingDispatcher(this.channel, schedule));
		}
		this.dispatchers.get(schedule).subscribe(target);
		if (dispatcher == null && this.isRunning()) {
			this.scheduleDispatcherTask(schedule);
		}
	}

	public boolean removeTarget(Target target) {
		boolean removed = false;
		Collection<PollingDispatcher> dispatcherValues = this.dispatchers.values();
		for (PollingDispatcher dispatcher : dispatcherValues) {
			removed = (removed || dispatcher.unsubscribe(target));
		}
		return removed;
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
				throw new ConfigurationException("scheduler is required");
			}
			if (!this.scheduler.isRunning()) {
				this.scheduler.start();
			}
			for (Lifecycle target : lifecycleTargets) {
				target.start();
			}
			for (Schedule schedule : this.dispatchers.keySet()) {
				this.scheduleDispatcherTask(schedule);
			}
			this.running = true;
		}
	}

	private void scheduleDispatcherTask(Schedule schedule) {
		PollingDispatcher dispatcher = this.dispatchers.get(schedule);
		if (dispatcher != null) {
			this.scheduler.schedule(dispatcher);
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				return;
			}
			for (Lifecycle target : lifecycleTargets) {
				target.stop();
			}
			this.running = false;
		}
	}

}

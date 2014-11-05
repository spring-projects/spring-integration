/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.integration.channel;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.SmartLifecycle;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.support.channel.HeaderChannelRegistry;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * Converts a channel to a name, retaining a reference to the channel keyed by the name.
 * Allows a downstream {@link BeanFactoryChannelResolver} to find the channel by name
 * in the event that the flow serialized the message at some point.
 * Channels are expired after a configurable delay (60 seconds by default).
 * The actual average expiry time will be 1.5x the delay.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public class DefaultHeaderChannelRegistry extends IntegrationObjectSupport
		implements HeaderChannelRegistry, SmartLifecycle, Runnable {

	private static final int DEFAULT_REAPER_DELAY = 60000;

	private final Map<String, MessageChannelWrapper> channels = new ConcurrentHashMap<String, DefaultHeaderChannelRegistry.MessageChannelWrapper>();

	private static final AtomicLong id = new AtomicLong();

	private final String uuid = UUID.randomUUID().toString() + ":";

	private volatile long reaperDelay;

	private volatile ScheduledFuture<?> reaperScheduledFuture;

	private volatile boolean running;

	private volatile int phase;

	private volatile boolean autoStartup = false;

	private volatile boolean explicitlyStopped;

	/**
	 * Constructs a registry with the default delay for channel expiry.
	 */
	public DefaultHeaderChannelRegistry() {
		this(DEFAULT_REAPER_DELAY);
	}

	/**
	 * Constructs a registry with the provided delay (milliseconds) for
	 * channel expiry.
	 *
	 * @param reaperDelay the delay in milliseconds.
	 */
	public DefaultHeaderChannelRegistry(long reaperDelay) {
		this.setReaperDelay(reaperDelay);
	}

	/**
	 * Set the reaper delay.
	 *
	 * @param reaperDelay the delay in milliseconds.
	 */
	public final void setReaperDelay(long reaperDelay) {
		Assert.isTrue(reaperDelay > 0, "'reaperDelay' must be > 0");
		this.reaperDelay = reaperDelay;
	}

	public final long getReaperDelay() {
		return reaperDelay;
	}

	@Override
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		super.setTaskScheduler(taskScheduler);
	}

	/**
	 * @deprecated - this class will not implement {@link SmartLifecycle} in 4.2, just {@code Lifecycle}.
	 */
	@Deprecated
	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * @deprecated - this class will not implement {@link SmartLifecycle} in 4.2, just {@code Lifecycle}.
	 */
	@Deprecated
	public final void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * @deprecated - this class will not implement {@link SmartLifecycle} in 4.2, just {@code Lifecycle}.
	 */
	@Deprecated
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * @deprecated - this class will not implement {@link SmartLifecycle} in 4.2, just {@code Lifecycle}.
	 */
	@Deprecated
	public final void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public final int size() {
		return this.channels.size();
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		Assert.notNull(this.getTaskScheduler(), "a task scheduler is required");
	}

	@Override
	public synchronized void start() {
		if (!this.running) {
			Assert.notNull(this.getTaskScheduler(), "a task scheduler is required");
			this.reaperScheduledFuture = this.getTaskScheduler().schedule(this,
					new Date(System.currentTimeMillis() + this.reaperDelay));
			this.running = true;
		}
	}

	@Override
	public synchronized void stop() {
		this.running = false;
		if (this.reaperScheduledFuture != null) {
			this.reaperScheduledFuture.cancel(true);
		}
		this.explicitlyStopped = true;
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public Object channelToChannelName(Object channel) {
		if (!this.running && !this.explicitlyStopped && this.getTaskScheduler() != null) {
			start();
		}
		if (channel != null && channel instanceof MessageChannel) {
			String name = this.uuid + DefaultHeaderChannelRegistry.id.incrementAndGet();
			channels.put(name, new MessageChannelWrapper((MessageChannel) channel));
			if (logger.isDebugEnabled()) {
				logger.debug("Registered " + channel + " as " + name);
			}
			return name;
		}
		else {
			return channel;
		}
	}

	@Override
	public MessageChannel channelNameToChannel(String name) {
		if (name != null) {
			MessageChannelWrapper messageChannelWrapper = this.channels.get(name);
			if (logger.isDebugEnabled() && messageChannelWrapper != null) {
				logger.debug("Retrieved " + messageChannelWrapper.getChannel() + " with " + name);
			}
			return messageChannelWrapper == null ? null : messageChannelWrapper.getChannel();
		}
		return null;
	}

	/**
	 * Cancel the scheduled reap task and run immediately; then reschedule.
	 */
	@Override
	public void runReaper() {
		synchronized(this) {
			this.reaperScheduledFuture.cancel(false);
			this.reaperScheduledFuture = null;
		}
		this.run();
	}

	@Override
	public void run() {
		this.reaperScheduledFuture = null;
		if (logger.isTraceEnabled()) {
			logger.trace("Reaper started; channels size=" + this.channels.size());
		}
		Iterator<Entry<String, MessageChannelWrapper>> iterator = this.channels.entrySet().iterator();
		long threshold = System.currentTimeMillis() - this.reaperDelay;
		while (iterator.hasNext()) {
			Entry<String, MessageChannelWrapper> entry = iterator.next();
			if (entry.getValue().getCreated() < threshold) {
				if (logger.isDebugEnabled()) {
					logger.debug("Expiring " + entry.getKey() + " (" + entry.getValue().getChannel() + ")");
				}
				iterator.remove();
			}
		}
		synchronized (this) {
			if (this.reaperScheduledFuture == null) {
				this.reaperScheduledFuture = this.getTaskScheduler().schedule(this,
						new Date(System.currentTimeMillis() + this.reaperDelay));
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Reaper completed; channels size=" + this.channels.size());
		}
	}


	private class MessageChannelWrapper {

		private final MessageChannel channel;

		private final long created;

		private MessageChannelWrapper(MessageChannel channel) {
			this.channel = channel;
			this.created = System.currentTimeMillis();
		}

		public final long getCreated() {
			return created;
		}

		public final MessageChannel getChannel() {
			return channel;
		}

	}

}

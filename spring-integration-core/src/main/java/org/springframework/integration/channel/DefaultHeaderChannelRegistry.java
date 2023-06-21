/*
 * Copyright 2013-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.channel;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.support.channel.HeaderChannelRegistry;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * Converts a channel to a name, retaining a reference to the channel keyed by the name.
 * Allows a downstream
 * {@link org.springframework.integration.support.channel.BeanFactoryChannelResolver}
 * to find the channel by name
 * in the event that the flow serialized the message at some point.
 * Channels are expired after a configurable delay (60 seconds by default).
 * The actual average expiry time will be 1.5x the delay.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Trung Pham
 * @author Christian Tzolov
 *
 * @since 3.0
 *
 */
public class DefaultHeaderChannelRegistry extends IntegrationObjectSupport
		implements HeaderChannelRegistry, ManageableLifecycle, Runnable {

	private static final int DEFAULT_REAPER_DELAY = 60000;

	protected static final AtomicLong id = new AtomicLong(); // NOSONAR

	protected final Map<String, MessageChannelWrapper> channels = new ConcurrentHashMap<>(); // NOSONAR

	protected final String uuid = UUID.randomUUID() + ":"; // NOSONAR

	private boolean removeOnGet;

	private long reaperDelay;

	private volatile ScheduledFuture<?> reaperScheduledFuture;

	private volatile boolean running;

	private volatile boolean explicitlyStopped;

	private final Lock lock = new ReentrantLock();

	/**
	 * Construct a registry with the default delay for channel expiry.
	 */
	public DefaultHeaderChannelRegistry() {
		this(DEFAULT_REAPER_DELAY);
	}

	/**
	 * Construct a registry with the provided delay (milliseconds) for
	 * channel expiry.
	 * @param reaperDelay the delay in milliseconds.
	 */
	public DefaultHeaderChannelRegistry(long reaperDelay) {
		this.setReaperDelay(reaperDelay);
	}

	/**
	 * Set the reaper delay.
	 * @param reaperDelay the delay in milliseconds.
	 */
	public final void setReaperDelay(long reaperDelay) {
		Assert.isTrue(reaperDelay > 0, "'reaperDelay' must be > 0");
		this.reaperDelay = reaperDelay;
	}

	public final long getReaperDelay() {
		return this.reaperDelay;
	}

	/**
	 * Set to true to immediately remove the channel mapping when
	 * {@link #channelNameToChannel(String)} is invoked.
	 * @param removeOnGet true to remove immediately, default false.
	 * @since 4.1
	 */
	public void setRemoveOnGet(boolean removeOnGet) {
		this.removeOnGet = removeOnGet;
	}

	@Override
	public final int size() {
		return this.channels.size();
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.notNull(getTaskScheduler(), "a task scheduler is required");
	}

	@Override
	public void start() {
		this.lock.lock();
		try {
			if (!this.running) {
				Assert.notNull(getTaskScheduler(), "a task scheduler is required");
				this.reaperScheduledFuture = getTaskScheduler()
						.schedule(this, Instant.now().plusMillis(this.reaperDelay));

				this.running = true;
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void stop() {
		this.lock.lock();
		try {
			this.running = false;
			if (this.reaperScheduledFuture != null) {
				this.reaperScheduledFuture.cancel(true);
				this.reaperScheduledFuture = null;
			}
			this.explicitlyStopped = true;
		}
		finally {
			this.lock.unlock();
		}

	}

	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	@Nullable
	public Object channelToChannelName(@Nullable Object channel) {
		return channelToChannelName(channel, this.reaperDelay);
	}

	@Override
	@Nullable
	public Object channelToChannelName(@Nullable Object channel, long timeToLive) {
		if (!this.running && !this.explicitlyStopped && this.getTaskScheduler() != null) {
			start();
		}
		if (channel instanceof MessageChannel) {
			String name = this.uuid + id.incrementAndGet();
			this.channels.put(name, new MessageChannelWrapper((MessageChannel) channel,
					System.currentTimeMillis() + timeToLive));
			logger.debug(() -> "Registered " + channel + " as " + name);
			return name;
		}
		else {
			return channel;
		}
	}

	@Override
	@Nullable
	public MessageChannel channelNameToChannel(@Nullable String name) {
		if (name != null) {
			MessageChannelWrapper messageChannelWrapper;
			if (this.removeOnGet) {
				messageChannelWrapper = this.channels.remove(name);
			}
			else {
				messageChannelWrapper = this.channels.get(name);
			}

			if (messageChannelWrapper != null) {
				MessageChannel channel = messageChannelWrapper.channel();
				logger.debug(() -> "Retrieved " + channel + " with " + name);
				return channel;
			}
		}
		return null;
	}

	/**
	 * Cancel the scheduled reap task and run immediately; then reschedule.
	 */
	@Override
	public void runReaper() {
		this.lock.lock();
		try {
			if (this.reaperScheduledFuture != null) {
				this.reaperScheduledFuture.cancel(true);
				this.reaperScheduledFuture = null;
			}

			run();
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void run() {
		this.lock.lock();
		try {
			logger.trace(() -> "Reaper started; channels size=" + this.channels.size());
			Iterator<Entry<String, MessageChannelWrapper>> iterator = this.channels.entrySet().iterator();
			long now = System.currentTimeMillis();
			while (iterator.hasNext()) {
				Entry<String, MessageChannelWrapper> entry = iterator.next();
				if (entry.getValue().expireAt() < now) {
					logger.debug(() -> "Expiring " + entry.getKey() + " (" + entry.getValue().channel() + ")");
					iterator.remove();
				}
			}
			this.reaperScheduledFuture = getTaskScheduler()
					.schedule(this, Instant.now().plusMillis(this.reaperDelay));

			logger.trace(() -> "Reaper completed; channels size=" + this.channels.size());
		}
		finally {
			this.lock.unlock();
		}
	}

	protected record MessageChannelWrapper(MessageChannel channel, long expireAt) {

	}

}

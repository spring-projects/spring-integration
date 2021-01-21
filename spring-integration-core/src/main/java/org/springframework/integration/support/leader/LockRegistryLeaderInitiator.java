/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.support.leader;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.leader.Candidate;
import org.springframework.integration.leader.Context;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.leader.event.DefaultLeaderEventPublisher;
import org.springframework.integration.leader.event.LeaderEventPublisher;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

/**
 * Component that initiates leader election based on holding a lock. If the lock has the
 * right properties (global with expiry), there will never be more than one leader, but
 * there may occasionally be no leader for short periods. If the lock has stronger
 * guarantees, and it interrupts the holder's thread when it expires or is stolen, then
 * you can adjust the parameters to reduce the leaderless period to be limited only by
 * latency to the lock provider. The election process ties up a thread perpetually while
 * we hold and try to acquire the lock, so a native leader initiator (not based on a lock)
 * is likely to be more efficient. If there is no native leader initiator available, but
 * there is a lock registry (e.g. on a shared database), this implementation is likely to
 * be useful.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Vedran Pavic
 * @author Glenn Renfro
 * @author Kiel Boatman
 * @author Gary Russell
 *
 * @since 4.3.1
 */
public class LockRegistryLeaderInitiator implements SmartLifecycle, DisposableBean,
		ApplicationEventPublisherAware {

	public static final long DEFAULT_HEART_BEAT_TIME = 500L;

	public static final long DEFAULT_BUSY_WAIT_TIME = 50L;

	private static final Log LOGGER = LogFactory.getLog(LockRegistryLeaderInitiator.class);

	private final Object lifecycleMonitor = new Object();

	/**
	 * A lock registry. The locks it manages should be global (whatever that means for the
	 * system) and expiring, in case the holder dies without notifying anyone.
	 */
	private final LockRegistry locks;

	/**
	 * Candidate for leader election. User injects this to receive callbacks on leadership
	 * events. Alternatively applications can listen for the
	 * {@link org.springframework.integration.leader.event.OnGrantedEvent} and
	 * {@link org.springframework.integration.leader.event.OnRevokedEvent}, as long as the
	 * {@link #setLeaderEventPublisher(LeaderEventPublisher) leaderEventPublisher} is set.
	 */
	private final Candidate candidate;

	private final Context nullContext = new Context() {

		@Override
		public boolean isLeader() {
			return false;
		}

		@Override
		public String getRole() {
			return LockRegistryLeaderInitiator.this.candidate.getRole();
		}

	};

	/**
	 * Executor service for running leadership daemon.
	 */
	private ExecutorService executorService =
			Executors.newSingleThreadExecutor(new CustomizableThreadFactory("lock-leadership-"));

	/**
	 * Flag to denote whether the {@link ExecutorService} was provided via the setter and
	 * thus should not be shutdown when {@link #destroy()} is called
	 */
	private boolean executorServiceExplicitlySet;

	/**
	 * Time in milliseconds to wait in between attempts to re-acquire the lock, once it is
	 * held. The heartbeat time has to be less than the remote lock expiry period, if
	 * there is one, otherwise other nodes can steal the lock while we are sleeping here.
	 * If the remote lock does not expire, or if you know it interrupts the current thread
	 * when it expires or is broken, then you can extend the heartbeat to Long.MAX_VALUE.
	 */
	private long heartBeatMillis = DEFAULT_HEART_BEAT_TIME;

	private boolean publishFailedEvents = false;

	private LeaderSelector leaderSelector;

	private ApplicationEventPublisher applicationEventPublisher;

	/**
	 * Leader event publisher if set.
	 */
	private LeaderEventPublisher leaderEventPublisher;

	/**
	 * @see SmartLifecycle
	 */
	private boolean autoStartup = true;

	/**
	 * @see SmartLifecycle which is an extension of org.springframework.context.Phased
	 */
	private int phase = Integer.MAX_VALUE - 1000;

	/**
	 * Time in milliseconds to wait in between attempts to acquire the lock, if it is not
	 * held. The longer this is, the longer the system can be leaderless, if the leader
	 * dies. If a leader dies without releasing its lock, the system might still have to
	 * wait for the old lock to expire, but after that it should not have to wait longer
	 * than the busy wait time to get a new leader. If the remote lock does not expire, or
	 * if you know it interrupts the current thread when it expires or is broken, then you
	 * can reduce the busy wait to zero.
	 */
	private volatile long busyWaitMillis = DEFAULT_BUSY_WAIT_TIME;

	/**
	 * Flag that indicates whether the leadership election for this {@link #candidate} is
	 * running.
	 */
	private volatile boolean running;

	/**
	 * Future returned by submitting an {@link LeaderSelector} to
	 * {@link #executorService}. This is used to cancel leadership.
	 */
	private volatile Future<?> future;

	/**
	 * Create a new leader initiator with the provided lock registry and a default
	 * candidate (which just logs the leadership events).
	 * @param locks lock registry
	 */
	public LockRegistryLeaderInitiator(LockRegistry locks) {
		this(locks, new DefaultCandidate());
	}

	/**
	 * Create a new leader initiator. The candidate implementation is provided by the user
	 * to listen for leadership events and carry out business actions.
	 * @param locks lock registry
	 * @param candidate leadership election candidate
	 */
	public LockRegistryLeaderInitiator(LockRegistry locks, Candidate candidate) {
		Assert.notNull(locks, "'locks' must not be null");
		Assert.notNull(candidate, "'candidate' must not be null");
		this.locks = locks;
		this.candidate = candidate;
	}

	/**
	 * Set the {@link ExecutorService}, where is not provided then a default of
	 * single thread Executor will be used.
	 * @param executorService the executor service
	 * @since 5.0.2
	 */
	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
		this.executorServiceExplicitlySet = true;
	}

	public void setHeartBeatMillis(long heartBeatMillis) {
		this.heartBeatMillis = heartBeatMillis;
	}

	public void setBusyWaitMillis(long busyWaitMillis) {
		this.busyWaitMillis = busyWaitMillis;
	}

	/**
	 * Set the {@link LeaderEventPublisher}.
	 * @param leaderEventPublisher the event publisher
	 */
	public void setLeaderEventPublisher(LeaderEventPublisher leaderEventPublisher) {
		this.leaderEventPublisher = leaderEventPublisher;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * @return true if leadership election for this {@link #candidate} is running.
	 */
	@Override
	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * @param phase the phase
	 * @see SmartLifecycle
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * @param autoStartup true to start automatically
	 * @see SmartLifecycle
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * @return the context.
	 */
	public Context getContext() {
		if (this.leaderSelector == null) {
			return this.nullContext;
		}
		return this.leaderSelector.context;
	}

	public boolean isPublishFailedEvents() {
		return this.publishFailedEvents;
	}

	/**
	 * Enable or disable the publishing of failed events to the
	 * specified applicationEventPublisher. Because of the large
	 * number of failure events that can be published while attempting to get a
	 * mutex during leader election (in the case that another instance is
	 * holding the mutex), the default is set to false.
	 * @param publishFailedEvents boolean that if true, failed events will
	 * be published. If false, no failures will be published. Default is false.
	 * @since 5.0
	 */
	public void setPublishFailedEvents(boolean publishFailedEvents) {
		this.publishFailedEvents = publishFailedEvents;
	}

	/**
	 * Start the registration of the {@link #candidate} for leader election.
	 */
	@Override
	public void start() {
		if (this.leaderEventPublisher == null && this.applicationEventPublisher != null) {
			this.leaderEventPublisher = new DefaultLeaderEventPublisher(this.applicationEventPublisher);
		}
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				this.leaderSelector = new LeaderSelector(buildLeaderPath());
				this.running = true;
				this.future = this.executorService.submit(this.leaderSelector);
				LOGGER.debug("Started LeaderInitiator");
			}
		}
	}

	@Override
	public void destroy() {
		stop();
		if (!this.executorServiceExplicitlySet) {
			this.executorService.shutdown();
		}
	}

	/**
	 * Stop the registration of the {@link #candidate} for leader election. If the
	 * candidate is currently leader, its leadership will be revoked.
	 */
	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				this.running = false;
				if (this.future != null) {
					this.future.cancel(true);
				}
				this.future = null;
				LOGGER.debug("Stopped LeaderInitiator for " + getContext());
			}
		}
	}

	/**
	 * @return the lock key used by leader election
	 */
	private String buildLeaderPath() {
		return this.candidate.getRole();
	}

	protected class LeaderSelector implements Callable<Void> {

		private final Lock lock;

		private final String lockKey;

		private final LockContext context = new LockContext();

		private volatile boolean locked = false;

		LeaderSelector(String lockKey) {
			this.lock = LockRegistryLeaderInitiator.this.locks.obtain(lockKey);
			this.lockKey = lockKey;
		}

		@Override
		public Void call() {
			try {
				while (isRunning()) {
					try {
						tryAcquireLock();
					}
					catch (Exception e) {
						if (handleLockException(e)) {
							return null;
						}
					}
				}
			}
			finally {
				if (this.locked) {
					this.locked = false;
					try {
						this.lock.unlock();
					}
					catch (Exception e) {
						LOGGER.debug("Could not unlock during stop for " + this.context
								+ " - treat as broken. Revoking...", e);
					}
					// We are stopping, therefore not leading any more
					handleRevoked();
				}
			}
			return null;
		}

		private void tryAcquireLock() throws InterruptedException {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Acquiring the lock for " + this.context);
			}
			// We always try to acquire the lock, in case it expired
			boolean acquired = this.lock.tryLock(LockRegistryLeaderInitiator.this.heartBeatMillis,
					TimeUnit.MILLISECONDS);
			if (!this.locked) {
				if (acquired) {
					// Success: we are now leader
					this.locked = true;
					handleGranted();
				}
				else if (isPublishFailedEvents()) {
					publishFailedToAcquire();
				}
			}
			else if (acquired) {
				// If we were able to acquire it but we were already locked we
				// should release it
				this.lock.unlock();
				if (isRunning()) {
					// Give it a chance to expire.
					Thread.sleep(LockRegistryLeaderInitiator.this.heartBeatMillis);
				}
			}
			else {
				this.locked = false;
				// We were not able to acquire it, therefore not leading any more
				handleRevoked();
				if (isRunning()) {
					// Try again quickly in case the lock holder dropped it
					Thread.sleep(LockRegistryLeaderInitiator.this.busyWaitMillis);
				}
			}
		}

		private boolean handleLockException(Exception ex) { // NOSONAR
			if (this.locked) {
				this.locked = false;
				try {
					this.lock.unlock();
				}
				catch (Exception e1) {
					LOGGER.debug("Could not unlock - treat as broken " + this.context +
							". Revoking " + (isRunning() ? " and retrying..." : "..."), e1);

				}
				// The lock was broken and we are no longer leader
				handleRevoked();
			}

			if (ex instanceof InterruptedException || Thread.currentThread().isInterrupted()) {
				Thread.currentThread().interrupt();
				if (isRunning()) {
					restartSelectorBecauseOfError(ex);
				}
				return true;
			}
			else {
				if (isRunning()) {
					// Give it a chance to elect some other leader.
					try {
						Thread.sleep(LockRegistryLeaderInitiator.this.busyWaitMillis);
					}
					catch (InterruptedException e1) {
						// Ignore interruption and let it to be caught on the next cycle.
						Thread.currentThread().interrupt();
					}
				}
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Error acquiring the lock for " + this.context +
							". " + (isRunning() ? "Retrying..." : ""), ex);
				}
			}
			return false;
		}

		private void restartSelectorBecauseOfError(Exception ex) {
			LOGGER.warn("Restarting LeaderSelector for " + this.context + " because of error.", ex);
			LockRegistryLeaderInitiator.this.future =
					LockRegistryLeaderInitiator.this.executorService.submit(
							() -> {
								// Give it a chance to elect some other leader.
								Thread.sleep(LockRegistryLeaderInitiator.this.busyWaitMillis);
								return call();
							});
		}

		public boolean isLeader() {
			return this.locked;
		}

		private void handleGranted() throws InterruptedException {
			LockRegistryLeaderInitiator.this.candidate.onGranted(this.context);
			if (LockRegistryLeaderInitiator.this.leaderEventPublisher != null) {
				try {
					LockRegistryLeaderInitiator.this.leaderEventPublisher.publishOnGranted(
							LockRegistryLeaderInitiator.this, this.context, this.lockKey);
				}
				catch (Exception e) {
					LOGGER.warn("Error publishing OnGranted event.", e);
				}
			}
		}

		private void handleRevoked() {
			LockRegistryLeaderInitiator.this.candidate.onRevoked(this.context);
			if (LockRegistryLeaderInitiator.this.leaderEventPublisher != null) {
				try {
					LockRegistryLeaderInitiator.this.leaderEventPublisher.publishOnRevoked(
							LockRegistryLeaderInitiator.this, this.context,
							LockRegistryLeaderInitiator.this.candidate.getRole());
				}
				catch (Exception e) {
					LOGGER.warn("Error publishing OnRevoked event.", e);
				}
			}
		}

		private void publishFailedToAcquire() {
			if (LockRegistryLeaderInitiator.this.leaderEventPublisher != null) {
				try {
					LockRegistryLeaderInitiator.this.leaderEventPublisher.publishOnFailedToAcquire(
							LockRegistryLeaderInitiator.this,
							this.context,
							LockRegistryLeaderInitiator.this.candidate.getRole());
				}
				catch (Exception e) {
					LOGGER.warn("Error publishing OnFailedToAcquire event.", e);
				}
			}
		}

	}

	/**
	 * Implementation of leadership context backed by lock registry.
	 */
	private class LockContext implements Context {

		LockContext() {
		}

		@Override
		public boolean isLeader() {
			return LockRegistryLeaderInitiator.this.leaderSelector.isLeader();
		}

		@Override
		public void yield() {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Yielding leadership from " + this);
			}
			if (LockRegistryLeaderInitiator.this.future != null) {
				LockRegistryLeaderInitiator.this.future.cancel(true);
			}
		}

		@Override
		public String getRole() {
			return LockRegistryLeaderInitiator.this.candidate.getRole();
		}

		@Override
		public String toString() {
			return "LockContext{role=" + LockRegistryLeaderInitiator.this.candidate.getRole() +
					", id=" + LockRegistryLeaderInitiator.this.candidate.getId() +
					", isLeader=" + isLeader() + "}";
		}

	}

}

/*
 * Copyright 2015-2024 the original author or authors.
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

package org.springframework.integration.hazelcast.leader;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.cp.lock.FencedLock;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.log.LogAccessor;
import org.springframework.core.log.LogMessage;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.leader.Candidate;
import org.springframework.integration.leader.Context;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.leader.event.DefaultLeaderEventPublisher;
import org.springframework.integration.leader.event.LeaderEventPublisher;
import org.springframework.integration.support.leader.LockRegistryLeaderInitiator;
import org.springframework.util.Assert;

/**
 * Bootstrap leadership {@link org.springframework.integration.leader.Candidate candidates}
 * with Hazelcast. Upon construction, {@link #start} must be invoked to
 * register the candidate for leadership election.
 *
 * @author Patrick Peralta
 * @author Gary Russell
 * @author Dave Syer
 * @author Artem Bilan
 * @author Mael Le Guével
 * @author Alexey Tsoy
 * @author Robert Höglund
 * @author Christian Tzolov
 * @author Emil Palm
 */
public class LeaderInitiator implements SmartLifecycle, DisposableBean, ApplicationEventPublisherAware {

	private static final LogAccessor logger = new LogAccessor(LeaderInitiator.class);

	private static final Context NULL_CONTEXT = new NullContext();

	private final Lock lock = new ReentrantLock();

	/*** Hazelcast client.
	 */
	private final HazelcastInstance client;

	/**
	 * Candidate for leader election.
	 */
	private final Candidate candidate;

	/**
	 * Executor service for running leadership daemon.
	 */
	private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("Hazelcast-leadership-");

	private long heartBeatMillis = LockRegistryLeaderInitiator.DEFAULT_HEART_BEAT_TIME;

	private long busyWaitMillis = LockRegistryLeaderInitiator.DEFAULT_BUSY_WAIT_TIME;

	private LeaderSelector leaderSelector;

	/**
	 * Leader event publisher.
	 */
	private LeaderEventPublisher leaderEventPublisher = new DefaultLeaderEventPublisher();

	private boolean autoStartup = true;

	private int phase;

	/**
	 * Future returned by submitting an {@link LeaderSelector} to {@link #taskExecutor}.
	 * This is used to cancel leadership.
	 */
	private volatile Future<Void> future;

	private boolean customPublisher = false;

	private volatile boolean running;

	private final Semaphore yieldSign = new Semaphore(0);

	/**
	 * Construct a {@link LeaderInitiator} with a default candidate.
	 * @param client Hazelcast client
	 */
	public LeaderInitiator(HazelcastInstance client) {
		this(client, new DefaultCandidate());
	}

	/**
	 * Construct a {@link LeaderInitiator}.
	 * @param client Hazelcast client
	 * @param candidate leadership election candidate
	 */
	public LeaderInitiator(HazelcastInstance client, Candidate candidate) {
		Assert.notNull(client, "'client' must not be null");
		Assert.notNull(candidate, "'candidate' must not be null");
		this.client = client;
		this.candidate = candidate;
	}

	/**
	 * Set a {@link AsyncTaskExecutor} for running leadership daemon.
	 * @param taskExecutor the {@link AsyncTaskExecutor} to use.
	 * @since 6.2
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "A 'taskExecutor' must not be null.");
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Set the {@link LeaderEventPublisher}.
	 * @param leaderEventPublisher the event publisher
	 */
	public void setLeaderEventPublisher(LeaderEventPublisher leaderEventPublisher) {
		Assert.notNull(leaderEventPublisher, "'leaderEventPublisher' must not be null");
		this.leaderEventPublisher = leaderEventPublisher;
		this.customPublisher = true;
	}

	/**
	 * Time in milliseconds to wait in between attempts to re-acquire the lock, once it is
	 * held. The heartbeat time has to be less than the remote lock expiry period, if
	 * there is one, otherwise other nodes can steal the lock while we are sleeping here.
	 * @param heartBeatMillis the heart-beat timeout in milliseconds.
	 * Defaults to {@link LockRegistryLeaderInitiator#DEFAULT_HEART_BEAT_TIME}
	 * @since 1.0.1
	 */
	public void setHeartBeatMillis(long heartBeatMillis) {
		this.heartBeatMillis = heartBeatMillis;
	}

	/**
	 * Time in milliseconds to wait in between attempts to acquire the lock, if it is not
	 * held. The longer this is, the longer the system can be leaderless, if the leader
	 * dies. If a leader dies without releasing its lock, the system might still have to
	 * wait for the old lock to expire, but after that it should not have to wait longer
	 * than the busy wait time to get a new leader.
	 * @param busyWaitMillis the busy-wait timeout in milliseconds
	 * Defaults to {@link LockRegistryLeaderInitiator#DEFAULT_BUSY_WAIT_TIME}
	 * @since 1.0.1
	 */
	public void setBusyWaitMillis(long busyWaitMillis) {
		this.busyWaitMillis = busyWaitMillis;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		if (!this.customPublisher) {
			this.leaderEventPublisher = new DefaultLeaderEventPublisher(applicationEventPublisher);
		}
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * The context of the initiator or null if not running.
	 * @return the context (or null if not running)
	 */
	public Context getContext() {
		if (this.leaderSelector == null) {
			return NULL_CONTEXT;
		}
		return this.leaderSelector.context;
	}

	/**
	 * Start the registration of the {@link #candidate} for leader election.
	 */
	@Override
	public void start() {
		this.lock.lock();
		try {
			if (!this.running) {
				this.leaderSelector = new LeaderSelector();
				this.running = true;
				this.future = this.taskExecutor.submit(this.leaderSelector);
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	/**
	 * Stop the registration of the {@link #candidate} for leader election.
	 * If the candidate is currently leader, its leadership will be revoked.
	 */
	@Override
	public void stop() {
		this.lock.lock();
		try {
			if (this.running) {
				this.running = false;
				if (this.future != null) {
					this.future.cancel(true);
				}
				this.future = null;
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	/**
	 * {@code true} if leadership election for this {@link #candidate} is running.
	 * @return true if leadership election for this {@link #candidate} is running
	 */
	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public void destroy() {
		stop();
	}

	FencedLock getLock() {
		CPSubsystem cpSubSystem = this.client.getCPSubsystem();
		FencedLock lock = cpSubSystem.getLock(this.candidate.getRole());
		logger.debug(
				LogMessage.format("Use lock groupId '%s', lock count '%s'", lock.getGroupId(), lock.getLockCount()));
		return lock;
	}

	/**
	 * Callable that manages the acquisition of Hazelcast locks
	 * for leadership election.
	 */
	protected class LeaderSelector implements Callable<Void> {

		protected final HazelcastContext context = new HazelcastContext();

		protected final String role = LeaderInitiator.this.candidate.getRole();

		private volatile boolean leader = false;

		@Override
		public Void call() {
			try {
				while (isRunning()) {
					try {
						logger.trace(() ->
								"Am I the leader (" + LeaderInitiator.this.candidate.getRole() + ")? " + this.leader);
						if (getLock().isLockedByCurrentThread()) {
							if (!this.leader) {
								// Since we have the lock we need to ensure that the leader flag is set
								this.leader = true;
							}
							// Give it a chance to expire.
							if (LeaderInitiator.this.yieldSign
									.tryAcquire(LeaderInitiator.this.heartBeatMillis, TimeUnit.MILLISECONDS)) {

								revokeLeadership();
								// Give it a chance to elect some other leader.
								Thread.sleep(LeaderInitiator.this.busyWaitMillis);
							}
						}
						else {
							// We try to acquire the lock
							boolean acquired = getLock()
									.tryLock(LeaderInitiator.this.heartBeatMillis, TimeUnit.MILLISECONDS);
							if (acquired && !this.leader) {
								// Success: we are now leader
								this.leader = true;
								handleGranted();
							}
							if (!acquired && this.leader) {
								//If we no longer can acquire the lock but still have the leader status
								revokeLeadership();
							}
						}
					}
					catch (Exception ex) {
						// The lock was broken and we are no longer leader
						revokeLeadership();

						if (isRunning()) {
							// Give it a chance to elect some other leader.
							try {
								Thread.sleep(LeaderInitiator.this.busyWaitMillis);
							}
							catch (InterruptedException e1) {
								// Ignore interruption and let it be caught on the next cycle.
								Thread.currentThread().interrupt();
							}
						}
						logger.debug(ex, () -> "Error acquiring the lock for " + this.context +
								". " + (isRunning() ? "Retrying..." : ""));
					}
				}
			}
			finally {
				revokeLeadership();
			}

			return null;
		}

		private void revokeLeadership() {
			if (this.leader) {
				this.leader = false;
				try {
					// Try to unlock
					getLock().unlock();
				}
				catch (Exception e1) {
					logger.warn(e1, () -> "Could not unlock - treat as broken " + this.context + ". Revoking "
							+ (isRunning() ? " and retrying..." : "..."));

				}

				handleRevoked();
			}
		}

		private void handleGranted() throws InterruptedException {
			LeaderInitiator.this.candidate.onGranted(this.context);
			if (LeaderInitiator.this.leaderEventPublisher != null) {
				try {
					LeaderInitiator.this.leaderEventPublisher.publishOnGranted(
							LeaderInitiator.this, this.context, this.role);
				}
				catch (Exception ex) {
					logger.warn(ex, "Error publishing OnGranted event.");
				}
			}
		}

		private void handleRevoked() {
			LeaderInitiator.this.candidate.onRevoked(this.context);
			if (LeaderInitiator.this.leaderEventPublisher != null) {
				try {
					LeaderInitiator.this.leaderEventPublisher.publishOnRevoked(
							LeaderInitiator.this, this.context, this.role);
				}
				catch (Exception ex) {
					logger.warn(ex, "Error publishing OnRevoked event.");
				}
			}
		}

	}

	/**
	 * Implementation of leadership context backed by Hazelcast.
	 */
	protected class HazelcastContext implements Context {

		@Override
		public boolean isLeader() {
			return LeaderInitiator.this.leaderSelector.leader;
		}

		@Override
		public void yield() {
			if (isLeader()) {
				LeaderInitiator.this.yieldSign.release();
			}
		}

		@Override
		public String getRole() {
			return LeaderInitiator.this.candidate.getRole();
		}

		@Override
		public String toString() {
			return "HazelcastContext{role=" + LeaderInitiator.this.candidate.getRole() +
					", id=" + LeaderInitiator.this.candidate.getId() +
					", isLeader=" + isLeader() + "}";
		}

	}

	private static final class NullContext implements Context {

		@Override
		public boolean isLeader() {
			return false;
		}

		@Override
		public void yield() {
			// No-op
		}

	}

}

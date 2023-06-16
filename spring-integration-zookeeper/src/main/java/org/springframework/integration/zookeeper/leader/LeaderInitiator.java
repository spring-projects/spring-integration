/*
 * Copyright 2014-2023 the original author or authors.
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

package org.springframework.integration.zookeeper.leader;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.framework.recipes.leader.Participant;

import org.springframework.context.SmartLifecycle;
import org.springframework.integration.leader.Candidate;
import org.springframework.integration.leader.Context;
import org.springframework.integration.leader.event.LeaderEventPublisher;
import org.springframework.util.StringUtils;

/**
 * Bootstrap leadership {@link Candidate candidates}
 * with ZooKeeper/Curator. Upon construction, {@link #start} must be invoked to
 * register the candidate for leadership election.
 *
 * @author Patrick Peralta
 * @author Janne Valkealahti
 * @author Gary Russell
 * @author Artem Bilan
 * @author Ivan Zaitsev
 * @author Christian Tzolov
 *
 * @since 4.2
 */
public class LeaderInitiator implements SmartLifecycle {

	private static final Log LOGGER = LogFactory.getLog(LeaderInitiator.class);

	private static final String DEFAULT_NAMESPACE = "/spring-integration/leader/";

	private final CuratorContext context = new CuratorContext();

	private final CuratorContext nullContext = new NullCuratorContext();

	/**
	 * Curator client.
	 */
	private final CuratorFramework client;

	/**
	 * Candidate for leader election.
	 */
	private final Candidate candidate;

	private final Lock lifecycleMonitor = new ReentrantLock();

	/**
	 * Base path in a zookeeper
	 */
	private final String namespace;

	/**
	 * Leader event publisher if set
	 */
	private LeaderEventPublisher leaderEventPublisher;

	/**
	 * @see SmartLifecycle
	 */
	private boolean autoStartup = true;

	/**
	 * @see SmartLifecycle which is an extension of org.springframework.context.Phased
	 */
	private int phase = Integer.MAX_VALUE - 1000; // NOSONAR

	/**
	 * Curator utility for selecting leaders.
	 */
	private volatile LeaderSelector leaderSelector;

	/**
	 * Flag that indicates whether the leadership election for
	 * this {@link #candidate} is running.
	 */
	private volatile boolean running;

	/**
	 * Construct a {@link LeaderInitiator}.
	 * @param client     Curator client
	 * @param candidate  leadership election candidate
	 */
	public LeaderInitiator(CuratorFramework client, Candidate candidate) {
		this(client, candidate, DEFAULT_NAMESPACE);
	}

	/**
	 * Construct a {@link LeaderInitiator}.
	 * @param client     Curator client
	 * @param candidate  leadership election candidate
	 * @param namespace  namespace base path in zookeeper
	 */
	public LeaderInitiator(CuratorFramework client, Candidate candidate, String namespace) {
		this.client = client;
		this.candidate = candidate;
		this.namespace = namespace;
	}

	/**
	 * @return true if leadership election for this {@link #candidate} is running
	 */
	@Override
	public boolean isRunning() {
		return this.running;
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
	 * Start the registration of the {@link #candidate} for leader election.
	 */
	@Override
	public void start() {
		this.lifecycleMonitor.lock();
		try {
			if (!this.running) {
				if (this.client.getState() != CuratorFrameworkState.STARTED) {
					// we want to do curator start here because it needs to
					// be started before leader selector and it gets a little
					// complicated to control ordering via beans so that
					// curator is fully started.
					this.client.start();
				}
				this.leaderSelector = new LeaderSelector(this.client, buildLeaderPath(), new LeaderListener());
				this.leaderSelector.setId(this.candidate.getId());
				this.leaderSelector.autoRequeue();
				this.leaderSelector.start();

				this.running = true;
				LOGGER.debug("Started LeaderInitiator");
			}
		}
		finally {
			this.lifecycleMonitor.unlock();
		}
	}

	/**
	 * Stop the registration of the {@link #candidate} for leader election.
	 * If the candidate is currently leader, its leadership will be revoked.
	 */
	@Override
	public void stop() {
		this.lifecycleMonitor.lock();
		try {
			if (this.running) {
				this.leaderSelector.close();
				this.running = false;
				LOGGER.debug("Stopped LeaderInitiator");
			}
		}
		finally {
			this.lifecycleMonitor.unlock();
		}
	}

	/**
	 * Sets the {@link LeaderEventPublisher}.
	 * @param leaderEventPublisher the event publisher
	 */
	public void setLeaderEventPublisher(LeaderEventPublisher leaderEventPublisher) {
		this.leaderEventPublisher = leaderEventPublisher;
	}

	/**
	 * The context of the initiator.
	 * @return the context.
	 * @since 5.0
	 */
	public CuratorContext getContext() {
		if (this.leaderSelector == null) {
			return this.nullContext;
		}
		return this.context;
	}

	/**
	 * @return the ZooKeeper path used for leadership election by Curator
	 */
	private String buildLeaderPath() {
		String ns = StringUtils.hasText(this.namespace) ? this.namespace : DEFAULT_NAMESPACE;
		if (ns.charAt(0) != '/') {
			ns = '/' + ns;
		}
		if (!ns.endsWith("/")) {
			ns = ns + '/';
		}
		return ns + this.candidate.getRole();
	}

	/**
	 * Implementation of Curator leadership election listener.
	 */
	protected class LeaderListener extends LeaderSelectorListenerAdapter {

		@Override
		public void takeLeadership(CuratorFramework framework) {
			try {
				LeaderInitiator.this.candidate.onGranted(LeaderInitiator.this.context);
				if (LeaderInitiator.this.leaderEventPublisher != null) {
					try {
						LeaderInitiator.this.leaderEventPublisher.publishOnGranted(LeaderInitiator.this,
								LeaderInitiator.this.context, LeaderInitiator.this.candidate.getRole());
					}
					catch (Exception e) {
						LOGGER.warn("Error publishing OnGranted event.", e);
					}
				}

				// when this method exits, the leadership will be revoked;
				// therefore this thread needs to be held up until the
				// candidate is no longer leader
				Thread.sleep(Long.MAX_VALUE);
			}
			catch (@SuppressWarnings("unused") InterruptedException e) {
				// InterruptedException, like any other runtime exception,
				// is handled by the finally block below. No need to
				// reset the interrupt flag as the interrupt is handled.
			}
			finally {
				LeaderInitiator.this.candidate.onRevoked(LeaderInitiator.this.context);
				if (LeaderInitiator.this.leaderEventPublisher != null) {
					try {
						LeaderInitiator.this.leaderEventPublisher.publishOnRevoked(LeaderInitiator.this,
								LeaderInitiator.this.context, LeaderInitiator.this.candidate.getRole());
					}
					catch (Exception e) {
						LOGGER.warn("Error publishing OnRevoked event.", e);
					}
				}
			}
		}

	}

	/**
	 * Implementation of leadership context backed by Curator.
	 */
	public class CuratorContext implements Context {

		CuratorContext() {
		}

		@Override
		public boolean isLeader() {
			return LeaderInitiator.this.leaderSelector.hasLeadership();
		}

		@Override
		public void yield() {
			LeaderInitiator.this.leaderSelector.interruptLeadership();
		}

		@Override
		public String getRole() {
			return LeaderInitiator.this.candidate.getRole();
		}

		/**
		 * Get the leader
		 * @return the leader.
		 * @since 6.0.3
		 */
		public Participant getLeader() {
			try {
				return LeaderInitiator.this.leaderSelector.getLeader();
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

		/**
		 * Get the list of participants
		 * @return list of participants.
		 * @since 6.0.3
		 */
		public Collection<Participant> getParticipants() {
			try {
				return LeaderInitiator.this.leaderSelector.getParticipants();
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

		@Override
		public String toString() {
			return "CuratorContext{role=" + LeaderInitiator.this.candidate.getRole() +
					", id=" + LeaderInitiator.this.candidate.getId() +
					", isLeader=" + isLeader() + "}";
		}

	}

	private class NullCuratorContext extends CuratorContext {

		@Override
		public boolean isLeader() {
			return false;
		}

		@Override
		public String getRole() {
			return LeaderInitiator.this.candidate.getRole();
		}

		@Override
		public Participant getLeader() {
			return null;
		}

		@Override
		public Collection<Participant> getParticipants() {
			return List.of();
		}

	}

}

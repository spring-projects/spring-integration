/*
 * Copyright 2014-2015 the original author or authors.
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
package org.springframework.integration.zookeeper.leader;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.leader.Candidate;
import org.springframework.integration.leader.Context;
import org.springframework.integration.leader.event.LeaderEventPublisher;
import org.springframework.util.StringUtils;

/**
 * Bootstrap leadership {@link org.springframework.cloud.cluster.leader.Candidate candidates}
 * with ZooKeeper/Curator. Upon construction, {@link #start} must be invoked to
 * register the candidate for leadership election.
 *
 * @author Patrick Peralta
 * @author Janne Valkealahti
 * @author Gary Russell
 *
 */
public class LeaderInitiator implements Lifecycle, InitializingBean, DisposableBean {

	private static final String DEFAULT_NAMESPACE = "/spring-cloud/leader/";

	/**
	 * Curator client.
	 */
	private final CuratorFramework client;

	/**
	 * Candidate for leader election.
	 */
	private final Candidate candidate;

	/**
	 * Curator utility for selecting leaders.
	 */
	private volatile LeaderSelector leaderSelector;

	/**
	 * Flag that indicates whether the leadership election for
	 * this {@link #candidate} is running.
	 */
	private volatile boolean running;

	/** Base path in a zookeeper */
	private final String namespace;

	/** Leader event publisher if set */
	private volatile LeaderEventPublisher leaderEventPublisher;

	/**
	 * Construct a {@link LeaderInitiator}.
	 *
	 * @param client     Curator client
	 * @param candidate  leadership election candidate
	 */
	public LeaderInitiator(CuratorFramework client, Candidate candidate) {
		this(client, candidate, DEFAULT_NAMESPACE);
	}

	/**
	 * Construct a {@link LeaderInitiator}.
	 *
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
	 * Start the registration of the {@link #candidate} for leader election.
	 */
	@Override
	public synchronized void start() {
		if (!running) {
			if (client.getState() != CuratorFrameworkState.STARTED) {
				// we want to do curator start here because it needs to
				// be started before leader selector and it gets a little
				// complicated to control ordering via beans so that
				// curator is fully started.
				client.start();
			}
			leaderSelector = new LeaderSelector(client, buildLeaderPath(), new LeaderListener());
			leaderSelector.setId(candidate.getId());
			leaderSelector.autoRequeue();
			leaderSelector.start();

			running = true;
		}
	}

	/**
	 * Stop the registration of the {@link #candidate} for leader election.
	 * If the candidate is currently leader, its leadership will be revoked.
	 */
	@Override
	public synchronized void stop() {
		if (running) {
			leaderSelector.close();
			running = false;
		}
	}

	/**
	 * @return true if leadership election for this {@link #candidate} is running
	 */
	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		start();
	}

	@Override
	public void destroy() throws Exception {
		stop();
	}

	/**
	 * Sets the {@link LeaderEventPublisher}.
	 *
	 * @param leaderEventPublisher the event publisher
	 */
	public void setLeaderEventPublisher(LeaderEventPublisher leaderEventPublisher) {
		this.leaderEventPublisher = leaderEventPublisher;
	}

	/**
	 * @return the ZooKeeper path used for leadership election by Curator
	 */
	private String buildLeaderPath() {

		String ns = StringUtils.hasText(namespace) ? namespace : DEFAULT_NAMESPACE;
		if (!ns.startsWith("/")) {
			ns = "/" + ns;
		}
		if (!ns.endsWith("/")) {
			ns = ns + "/";
		}
		return String.format(ns + "%s", candidate.getRole());
	}

	/**
	 * Implementation of Curator leadership election listener.
	 */
	class LeaderListener extends LeaderSelectorListenerAdapter {

		@Override
		public void takeLeadership(CuratorFramework framework) throws Exception {
			CuratorContext context = new CuratorContext();

			try {
				candidate.onGranted(context);
				if (leaderEventPublisher != null) {
					leaderEventPublisher.publishOnGranted(LeaderInitiator.this, context, candidate.getRole());
				}

				// when this method exits, the leadership will be revoked;
				// therefore this thread needs to be held up until the
				// candidate is no longer leader
				Thread.sleep(Long.MAX_VALUE);
			}
			catch (InterruptedException e) {
				// InterruptedException, like any other runtime exception,
				// is handled by the finally block below. No need to
				// reset the interrupt flag as the interrupt is handled.
			}
			finally {
				candidate.onRevoked(context);
				if (leaderEventPublisher != null) {
					leaderEventPublisher.publishOnRevoked(LeaderInitiator.this, context, candidate.getRole());
				}
			}
		}
	}

	/**
	 * Implementation of leadership context backed by Curator.
	 */
	class CuratorContext implements Context {

		@Override
		public boolean isLeader() {
			return leaderSelector.hasLeadership();
		}

		@Override
		public void yield() {
			leaderSelector.interruptLeadership();
		}

		@Override
		public String toString() {
			return String.format("CuratorContext{role=%s, id=%s, isLeader=%s}",
					candidate.getRole(), candidate.getId(), isLeader());
		}

	}
}

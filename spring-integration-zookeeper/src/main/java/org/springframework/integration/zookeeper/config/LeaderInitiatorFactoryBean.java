/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.zookeeper.config;

import java.util.UUID;

import org.apache.curator.framework.CuratorFramework;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.leader.Candidate;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.leader.event.DefaultLeaderEventPublisher;
import org.springframework.integration.leader.event.LeaderEventPublisher;
import org.springframework.integration.zookeeper.leader.LeaderInitiator;
import org.springframework.util.Assert;

/**
 * Creates a {@link LeaderInitiator}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class LeaderInitiatorFactoryBean
		implements FactoryBean<LeaderInitiator>, SmartLifecycle, InitializingBean, ApplicationEventPublisherAware {

	private CuratorFramework client;

	private Candidate candidate;

	private String path;

	private LeaderInitiator leaderInitiator;

	private boolean autoStartup = true;

	private int phase = Integer.MAX_VALUE - 1000;

	private ApplicationEventPublisher applicationEventPublisher;

	private LeaderEventPublisher leaderEventPublisher;

	public LeaderInitiatorFactoryBean() {
	}

	public LeaderInitiatorFactoryBean setClient(CuratorFramework client) {
		this.client = client;
		return this;
	}

	public LeaderInitiatorFactoryBean setPath(String path) {
		this.path = path;
		return this;
	}

	/**
	 * Configure a role for {@link DefaultCandidate}.
	 * Or this or {@link #setCandidate(Candidate)} can be configured, but not both.
	 * @param role the role for candidate
	 * @return this instance
	 */
	public LeaderInitiatorFactoryBean setRole(String role) {
		Assert.isNull(this.candidate,
				"Or 'role' for an internal 'DefaultCandidate' or 'candidate' option must be provided, but not both.");
		return setCandidate(new DefaultCandidate(UUID.randomUUID().toString(), role));
	}

	/**
	 * Configure a {@link Candidate} for leader election.
	 * Or this or {@link #setRole(String)} can be configured, but not both.
	 * @param candidate the {@link Candidate} to use
	 * @return this instance
	 * @since 5.3
	 */
	public LeaderInitiatorFactoryBean setCandidate(Candidate candidate) {
		this.candidate = candidate;
		return this;
	}

	/**
	 * A {@link LeaderEventPublisher} option for events from the {@link LeaderInitiator}.
	 * @param leaderEventPublisher the {@link LeaderEventPublisher} to use.
	 * @since 4.3.2
	 */
	public void setLeaderEventPublisher(LeaderEventPublisher leaderEventPublisher) {
		this.leaderEventPublisher = leaderEventPublisher;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public boolean isAutoStartup() {
		return this.leaderInitiator != null && this.leaderInitiator.isAutoStartup();
	}

	@Override
	public void start() {
		if (this.leaderInitiator != null) {
			this.leaderInitiator.start();
		}
	}

	@Override
	public void stop() {
		if (this.leaderInitiator != null) {
			this.leaderInitiator.stop();
		}
	}

	@Override
	public void stop(Runnable callback) {
		if (this.leaderInitiator != null) {
			this.leaderInitiator.stop(callback);
		}
		else {
			callback.run();
		}
	}

	@Override
	public boolean isRunning() {
		return this.leaderInitiator != null && this.leaderInitiator.isRunning();
	}

	@Override
	public int getPhase() {
		if (this.leaderInitiator != null) {
			return this.leaderInitiator.getPhase();
		}
		return this.phase;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.leaderInitiator == null) {
			this.leaderInitiator = new LeaderInitiator(this.client, this.candidate, this.path);
			this.leaderInitiator.setPhase(this.phase);
			this.leaderInitiator.setAutoStartup(this.autoStartup);
			if (this.leaderEventPublisher != null) {
				this.leaderInitiator.setLeaderEventPublisher(this.leaderEventPublisher);
			}
			else if (this.applicationEventPublisher != null) {
				this.leaderInitiator.setLeaderEventPublisher(
						new DefaultLeaderEventPublisher(this.applicationEventPublisher));
			}
		}
	}

	@Override
	public synchronized LeaderInitiator getObject() {
		return this.leaderInitiator;
	}

	@Override
	public Class<?> getObjectType() {
		return LeaderInitiator.class;
	}

}

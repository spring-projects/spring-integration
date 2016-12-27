/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.integration.support.leader;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.integration.leader.Context;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.leader.event.LeaderEventPublisher;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.test.rule.Log4jLevelAdjuster;

/**
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 4.3.1
 */
public class LockRegistryLeaderInitiatorTests {

	@Rule
	public Log4jLevelAdjuster adjuster = new Log4jLevelAdjuster(Level.TRACE, "org.springframework.integration");

	private CountDownLatch granted;

	private CountDownLatch revoked;

	private LockRegistry registry = new DefaultLockRegistry();

	private LockRegistryLeaderInitiator initiator =
			new LockRegistryLeaderInitiator(this.registry, new DefaultCandidate());

	@Before
	public void init() {
		this.granted = new CountDownLatch(1);
		this.revoked = new CountDownLatch(1);
		this.initiator.setLeaderEventPublisher(new CountingPublisher(this.granted, this.revoked));
	}

	@Test
	public void startAndStop() throws Exception {
		assertThat(this.initiator.getContext().isLeader(), is(false));
		this.initiator.start();
		assertThat(this.initiator.isRunning(), is(true));
		this.granted.await(10, TimeUnit.SECONDS);
		assertThat(this.initiator.getContext().isLeader(), is(true));
		Thread.sleep(200L);
		assertThat(this.initiator.getContext().isLeader(), is(true));
		this.initiator.stop();
		this.revoked.await(10, TimeUnit.SECONDS);
		assertThat(this.initiator.getContext().isLeader(), is(false));
	}

	@Test
	public void yield() throws Exception {
		assertThat(this.initiator.getContext().isLeader(), is(false));
		this.initiator.start();
		assertThat(this.initiator.isRunning(), is(true));
		this.granted.await(10, TimeUnit.SECONDS);
		assertThat(this.initiator.getContext().isLeader(), is(true));
		this.initiator.getContext().yield();
		assertThat(this.revoked.await(10, TimeUnit.SECONDS), is(true));
	}

	@Test
	public void competing() throws Exception {
		LockRegistryLeaderInitiator another =
				new LockRegistryLeaderInitiator(this.registry, new DefaultCandidate());
		CountDownLatch other = new CountDownLatch(1);
		another.setLeaderEventPublisher(new CountingPublisher(other));
		this.initiator.start();
		assertThat(this.granted.await(10, TimeUnit.SECONDS), is(true));
		another.start();
		this.initiator.stop();
		assertThat(other.await(10, TimeUnit.SECONDS), is(true));
		assertThat(another.getContext().isLeader(), is(true));
	}

	private static class CountingPublisher implements LeaderEventPublisher {

		private final CountDownLatch granted;

		private final CountDownLatch revoked;

		CountingPublisher(CountDownLatch granted, CountDownLatch revoked) {
			this.granted = granted;
			this.revoked = revoked;
		}

		CountingPublisher(CountDownLatch granted) {
			this(granted, new CountDownLatch(1));
		}

		@Override
		public void publishOnRevoked(Object source, Context context, String role) {
			this.revoked.countDown();
		}

		@Override
		public void publishOnGranted(Object source, Context context, String role) {
			this.granted.countDown();
		}

	}

}

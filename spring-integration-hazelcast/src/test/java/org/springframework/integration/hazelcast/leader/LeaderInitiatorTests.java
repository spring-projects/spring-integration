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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.CPGroupId;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.cp.lock.FencedLock;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.leader.Candidate;
import org.springframework.integration.leader.Context;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.leader.event.AbstractLeaderEvent;
import org.springframework.integration.leader.event.DefaultLeaderEventPublisher;
import org.springframework.integration.leader.event.LeaderEventPublisher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for hazelcast leader election.
 *
 * @author Janne Valkealahti
 * @author Patrick Peralta
 * @author Dave Syer
 * @author Artem Bilan
 * @author Mael Le Guével
 * @author Emil Palm
 */
@SpringJUnitConfig
@DirtiesContext
public class LeaderInitiatorTests {

	@Autowired
	private HazelcastInstance hazelcastInstance;

	@Autowired
	private TestCandidate candidate;

	@Autowired
	private TestEventListener listener;

	@Autowired
	private LeaderInitiator initiator;

	@Test
	public void testLeaderElections() throws Exception {
		assertThat(this.candidate.onGrantedLatch.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(this.listener.onEventLatch.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(this.listener.events.size()).isEqualTo(1);

		this.initiator.destroy();

		CountDownLatch granted = new CountDownLatch(1);
		CountingPublisher countingPublisher = new CountingPublisher(granted);
		List<LeaderInitiator> initiators = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			LeaderInitiator initiator = new LeaderInitiator(this.hazelcastInstance, new DefaultCandidate());
			initiator.setLeaderEventPublisher(countingPublisher);
			initiators.add(initiator);
		}

		for (LeaderInitiator initiator : initiators) {
			initiator.start();
		}

		assertThat(granted.await(10, TimeUnit.SECONDS)).isTrue();

		LeaderInitiator initiator1 = countingPublisher.initiator;

		LeaderInitiator initiator2 = null;

		for (LeaderInitiator initiator : initiators) {
			if (initiator != initiator1) {
				initiator2 = initiator;
				break;
			}
		}

		assertThat(initiator2).isNotNull();

		assertThat(initiator1.getContext().isLeader()).isTrue();
		assertThat(initiator2.getContext().isLeader()).isFalse();

		final CountDownLatch granted1 = new CountDownLatch(1);
		final CountDownLatch granted2 = new CountDownLatch(1);
		CountDownLatch revoked1 = new CountDownLatch(1);
		CountDownLatch revoked2 = new CountDownLatch(1);
		initiator1.setLeaderEventPublisher(new CountingPublisher(granted1, revoked1) {

			@Override
			public void publishOnRevoked(Object source, Context context, String role) {
				try {
					// It's difficult to see round-robin election, so block one initiator until the second is elected.
					assertThat(granted2.await(10, TimeUnit.SECONDS)).isTrue();
				}
				catch (InterruptedException e) {
					// No op
				}
				super.publishOnRevoked(source, context, role);
			}

		});

		initiator2.setLeaderEventPublisher(new CountingPublisher(granted2, revoked2) {

			@Override
			public void publishOnRevoked(Object source, Context context, String role) {
				try {
					// It's difficult to see round-robin election, so block one initiator until the second is elected.
					assertThat(granted1.await(10, TimeUnit.SECONDS)).isTrue();
				}
				catch (InterruptedException e) {
					// No op
				}
				super.publishOnRevoked(source, context, role);
			}

		});

		initiator1.getContext().yield();

		assertThat(revoked1.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(initiator2.getContext().isLeader()).isTrue();
		assertThat(initiator1.getContext().isLeader()).isFalse();

		initiator2.getContext().yield();

		assertThat(revoked2.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(initiator1.getContext().isLeader()).isTrue();
		assertThat(initiator2.getContext().isLeader()).isFalse();

		initiator2.destroy();

		CountDownLatch revoked11 = new CountDownLatch(1);
		initiator1.setLeaderEventPublisher(new CountingPublisher(new CountDownLatch(1), revoked11));

		initiator1.getContext().yield();

		assertThat(revoked11.await(10, TimeUnit.SECONDS)).isTrue();

		initiator1.destroy();

		CountDownLatch onGranted = new CountDownLatch(1);

		DefaultCandidate candidate = spy(new DefaultCandidate());
		willAnswer(invocation -> {
			try {
				return invocation.callRealMethod();
			}
			finally {
				onGranted.countDown();
			}
		})
				.given(candidate).onGranted(any(Context.class));

		LeaderInitiator initiator = new LeaderInitiator(this.hazelcastInstance, candidate);

		initiator.setLeaderEventPublisher(new DefaultLeaderEventPublisher() {

			@Override
			public void publishOnGranted(Object source, Context context, String role) {
				throw new RuntimeException("intentional");
			}

		});

		initiator.start();

		assertThat(onGranted.await(5, TimeUnit.SECONDS)).isTrue();

		assertThat(initiator.getContext().isLeader()).isTrue();

		initiator.destroy();
	}

	@Test
	public void testRevokeLeadershipCalledWhenLockNotAcquiredButStillLeader() throws Exception {
		// Initialize mocks and objects needed for the revoke leadership when fenced lock is no longer acquired
		HazelcastInstance hazelcastInstance = mock();
		Candidate candidate = mock();
		FencedLock fencedLock = mock();
		LeaderEventPublisher leaderEventPublisher = mock();

		CPSubsystem cpSubsystem = mock(CPSubsystem.class);
		given(candidate.getRole()).willReturn("role");
		given(hazelcastInstance.getCPSubsystem()).willReturn(cpSubsystem);
		given(cpSubsystem.getLock(anyString())).willReturn(fencedLock);
		given(fencedLock.getGroupId())
				.willReturn(new CPGroupId() {

					@Override
					public String getName() {
						return "";
					}

					@Override
					public long getId() {
						return 0;
					}
				});

		LeaderInitiator leaderInitiator = new LeaderInitiator(hazelcastInstance, candidate);
		leaderInitiator.setLeaderEventPublisher(leaderEventPublisher);

		// Simulate that the lock is currently held by this thread
		given(fencedLock.isLockedByCurrentThread()).willReturn(true, false);
		given(fencedLock.tryLock(anyLong(), any(TimeUnit.class))).willReturn(false); // Lock acquisition fails

		// Start the LeaderInitiator to trigger the leader election process
		leaderInitiator.start();

		// Simulate the lock acquisition check process
		Thread.sleep(1000);  // Give time for the async task to run

		// Verify that revokeLeadership was called due to lock not being acquired
		// unlock is part of revokeLeadership
		verify(fencedLock).unlock();
		// verify revoke event is published
		verify(leaderEventPublisher).publishOnRevoked(any(Object.class), any(Context.class), anyString());

		leaderInitiator.destroy();
	}

	@Configuration
	public static class TestConfig {

		@Bean
		TestCandidate candidate() {
			return new TestCandidate();
		}

		@Bean
		Config hazelcastConfig() {
			Config config = new Config();
			config.getCPSubsystemConfig()
					.setSessionHeartbeatIntervalSeconds(1);
			return config;
		}

		@Bean(destroyMethod = "shutdown")
		HazelcastInstance hazelcastInstance() {
			return Hazelcast.newHazelcastInstance(hazelcastConfig());
		}

		@Bean
		LeaderInitiator initiator() {
			return new LeaderInitiator(hazelcastInstance(), candidate());
		}

		@Bean
		TestEventListener testEventListener() {
			return new TestEventListener();
		}

	}

	static class TestCandidate extends DefaultCandidate {

		CountDownLatch onGrantedLatch = new CountDownLatch(1);

		@Override
		public void onGranted(Context ctx) {
			this.onGrantedLatch.countDown();
			super.onGranted(ctx);
		}

	}

	static class TestEventListener implements ApplicationListener<AbstractLeaderEvent> {

		CountDownLatch onEventLatch = new CountDownLatch(1);

		ArrayList<AbstractLeaderEvent> events = new ArrayList<>();

		@Override
		public void onApplicationEvent(AbstractLeaderEvent event) {
			this.events.add(event);
			this.onEventLatch.countDown();
		}

	}

	private static class CountingPublisher implements LeaderEventPublisher {

		private CountDownLatch granted;

		private CountDownLatch revoked;

		private volatile LeaderInitiator initiator;

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
		public void publishOnFailedToAcquire(Object source, Context context, String role) {

		}

		@Override
		public void publishOnGranted(Object source, Context context, String role) {
			this.initiator = (LeaderInitiator) source;
			this.granted.countDown();
		}

	}

}

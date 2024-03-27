/*
 * Copyright 2012-2024 the original author or authors.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.integration.leader.Context;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.leader.event.DefaultLeaderEventPublisher;
import org.springframework.integration.leader.event.LeaderEventPublisher;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author Dave Syer
 * @author Artem Bilan
 * @author Vedran Pavic
 * @author Glenn Renfro
 * @author Kiel Boatman
 *
 * @since 4.3.1
 */
public class LockRegistryLeaderInitiatorTests {

	private CountDownLatch granted = new CountDownLatch(1);

	private CountDownLatch revoked = new CountDownLatch(1);

	private final LockRegistry registry = new DefaultLockRegistry();

	private final LockRegistryLeaderInitiator initiator =
			new LockRegistryLeaderInitiator(this.registry, new DefaultCandidate());

	@BeforeEach
	public void init() {
		this.initiator.setLeaderEventPublisher(new CountingPublisher(this.granted, this.revoked));
	}

	@Test
	public void startAndStop() throws Exception {
		assertThat(this.initiator.getContext().isLeader()).isFalse();
		this.initiator.start();
		assertThat(this.initiator.isRunning()).isTrue();
		assertThat(this.granted.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.initiator.getContext().isLeader()).isTrue();
		Thread.sleep(200L);
		assertThat(this.initiator.getContext().isLeader()).isTrue();
		this.initiator.stop();
		assertThat(this.revoked.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.initiator.getContext().isLeader()).isFalse();
	}

	@Test
	public void yield() throws Exception {
		assertThat(this.initiator.getContext().isLeader()).isFalse();
		this.initiator.start();
		assertThat(this.initiator.isRunning()).isTrue();
		assertThat(this.granted.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.initiator.getContext().isLeader()).isTrue();
		this.initiator.getContext().yield();
		assertThat(this.revoked.await(10, TimeUnit.SECONDS)).isTrue();
		this.initiator.stop();
	}

	@Test
	public void competing() throws Exception {
		LockRegistryLeaderInitiator another =
				new LockRegistryLeaderInitiator(this.registry, new DefaultCandidate());

		CountDownLatch other = new CountDownLatch(1);
		another.setLeaderEventPublisher(new CountingPublisher(other));
		this.initiator.start();
		assertThat(this.granted.await(20, TimeUnit.SECONDS)).isTrue();
		another.start();
		this.initiator.stop();
		assertThat(other.await(20, TimeUnit.SECONDS)).isTrue();
		assertThat(another.getContext().isLeader()).isTrue();
		another.stop();
	}

	@Test
	public void competingWithErrorPublish() throws Exception {
		LockRegistryLeaderInitiator another =
				new LockRegistryLeaderInitiator(this.registry, new DefaultCandidate());

		CountDownLatch other = new CountDownLatch(1);
		CountDownLatch failedAcquireLatch = new CountDownLatch(1);
		another.setLeaderEventPublisher(new CountingPublisher(other, new CountDownLatch(1), failedAcquireLatch));
		another.setPublishFailedEvents(true);
		this.initiator.start();
		assertThat(this.granted.await(20, TimeUnit.SECONDS)).isTrue();
		another.start();
		assertThat(failedAcquireLatch.await(20, TimeUnit.SECONDS)).isTrue();
		this.initiator.stop();
		assertThat(other.await(20, TimeUnit.SECONDS)).isTrue();
		assertThat(another.getContext().isLeader()).isTrue();
		another.stop();
	}

	@Test
	public void testExceptionFromEvent() throws Exception {
		CountDownLatch onGranted = new CountDownLatch(1);

		this.initiator.setLeaderEventPublisher(new DefaultLeaderEventPublisher() {

			@Override
			public void publishOnGranted(Object source, Context context, String role) {
				try {
					throw new RuntimeException("intentional");
				}
				finally {
					onGranted.countDown();
				}
			}

		});

		this.initiator.start();

		assertThat(onGranted.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(initiator.getContext().isLeader()).isTrue();

		this.initiator.stop();
	}

	@Test
	public void competingWithLock() throws Exception {
		// switch used to toggle which registry obtains lock
		AtomicBoolean firstLocked = new AtomicBoolean(true);

		// set up first registry instance - this one will be able to obtain lock initially
		LockRegistry firstRegistry = mock(LockRegistry.class);
		Lock firstLock = mock(Lock.class);
		given(firstRegistry.obtain(anyString())).willReturn(firstLock);
		given(firstLock.tryLock(anyLong(), any(TimeUnit.class))).willAnswer(i -> firstLocked.get());

		// set up first initiator instance using first LockRegistry
		LockRegistryLeaderInitiator first =
				new LockRegistryLeaderInitiator(firstRegistry, new DefaultCandidate());

		CountDownLatch firstGranted = new CountDownLatch(1);
		CountDownLatch firstRevoked = new CountDownLatch(1);
		CountDownLatch firstAquireLockFailed = new CountDownLatch(1);
		first.setHeartBeatMillis(10);
		first.setBusyWaitMillis(1);
		first.setLeaderEventPublisher(new CountingPublisher(firstGranted, firstRevoked, firstAquireLockFailed));

		// set up second registry instance - this one will NOT be able to obtain lock initially
		LockRegistry secondRegistry = mock(LockRegistry.class);
		Lock secondLock = mock(Lock.class);
		given(secondRegistry.obtain(anyString())).willReturn(secondLock);
		given(secondLock.tryLock(anyLong(), any(TimeUnit.class))).willAnswer(i -> !firstLocked.get());

		// set up second initiator instance using second LockRegistry
		LockRegistryLeaderInitiator second =
				new LockRegistryLeaderInitiator(secondRegistry, new DefaultCandidate());

		CountDownLatch secondGranted = new CountDownLatch(1);
		CountDownLatch secondRevoked = new CountDownLatch(1);
		CountDownLatch secondAquireLockFailed = new CountDownLatch(1);
		second.setHeartBeatMillis(10);
		second.setBusyWaitMillis(1);
		second.setLeaderEventPublisher(new CountingPublisher(secondGranted, secondRevoked, secondAquireLockFailed));

		// start initiators
		first.start();
		second.start();

		// first initiator should lead and publish granted event
		assertThat(firstGranted.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(first.getContext().isLeader()).isTrue();
		assertThat(second.getContext().isLeader()).isFalse();

		// simulate first registry instance unable to obtain lock, for example due to lock timeout
		firstLocked.set(false);

		// second initiator should take lead and publish granted event, first initiator should publish revoked event
		assertThat(secondGranted.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(firstRevoked.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(second.getContext().isLeader()).isTrue();
		assertThat(first.getContext().isLeader()).isFalse();

		first.stop();
		second.stop();
	}

	@Test
	public void testGracefulLeaderSelectorExit() throws Exception {
		AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();

		LockRegistry registry = mock(LockRegistry.class);

		Lock lock = spy(new ReentrantLock());

		willAnswer(invocation -> {
			try {
				return invocation.callRealMethod();
			}
			catch (Throwable e) {
				throwableAtomicReference.set(e);
				throw e;
			}
		})
				.given(lock)
				.unlock();

		given(registry.obtain(anyString()))
				.willReturn(lock);

		LockRegistryLeaderInitiator another = new LockRegistryLeaderInitiator(registry);

		willAnswer(invocation -> {
			another.stop();
			return false;
		})
				.given(lock)
				.tryLock(anyLong(), eq(TimeUnit.MILLISECONDS));

		new DirectFieldAccessor(another).setPropertyValue("taskExecutor",
				new TaskExecutorAdapter(new SyncTaskExecutor()));

		another.start();

		Throwable throwable = throwableAtomicReference.get();
		assertThat(throwable).isNull();
	}

	@Test
	public void testExceptionFromLock() throws Exception {
		Lock mockLock = mock(Lock.class);

		AtomicBoolean exceptionThrown = new AtomicBoolean();

		willAnswer(invocation -> {
			if (!exceptionThrown.getAndSet(true)) {
				throw new RuntimeException("lock is broken");
			}
			else {
				return true;
			}
		}).given(mockLock).tryLock(anyLong(), any(TimeUnit.class));

		LockRegistry registry = lockKey -> mockLock;

		CountDownLatch onGranted = new CountDownLatch(1);

		LockRegistryLeaderInitiator another = new LockRegistryLeaderInitiator(registry);

		another.setLeaderEventPublisher(new CountingPublisher(onGranted));

		another.start();

		assertThat(onGranted.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(another.getContext().isLeader()).isTrue();
		assertThat(exceptionThrown.get()).isTrue();

		another.stop();
	}

	private static class CountingPublisher implements LeaderEventPublisher {

		private final CountDownLatch granted;

		private final CountDownLatch revoked;

		private final CountDownLatch acquireFailed;

		CountingPublisher(CountDownLatch granted) {
			this(granted, new CountDownLatch(1), new CountDownLatch(1));
		}

		CountingPublisher(CountDownLatch granted, CountDownLatch revoked) {
			this(granted, revoked, new CountDownLatch(1));
		}

		CountingPublisher(CountDownLatch granted, CountDownLatch revoked, CountDownLatch acquireFailed) {
			this.granted = granted;
			this.revoked = revoked;
			this.acquireFailed = acquireFailed;
		}

		@Override
		public void publishOnRevoked(Object source, Context context, String role) {
			this.revoked.countDown();
		}

		@Override
		public void publishOnFailedToAcquire(Object source, Context context, String role) {
			this.acquireFailed.countDown();
		}

		@Override
		public void publishOnGranted(Object source, Context context, String role) {
			this.granted.countDown();
		}

	}

}

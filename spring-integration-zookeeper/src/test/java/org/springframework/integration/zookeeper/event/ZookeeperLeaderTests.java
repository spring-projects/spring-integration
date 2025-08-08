/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.zookeeper.event;

import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.leader.event.AbstractLeaderEvent;
import org.springframework.integration.leader.event.DefaultLeaderEventPublisher;
import org.springframework.integration.leader.event.LeaderEventPublisher;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.zookeeper.ZookeeperTestSupport;
import org.springframework.integration.zookeeper.leader.LeaderInitiator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public class ZookeeperLeaderTests extends ZookeeperTestSupport {

	private final BlockingQueue<AbstractLeaderEvent> events = new LinkedBlockingQueue<>();

	private final SourcePollingChannelAdapter adapter = buildChannelAdapter();

	private final SmartLifecycleRoleController controller = new SmartLifecycleRoleController(
			Collections.singletonList("sitest"), Collections.singletonList(this.adapter));

	private final CountDownLatch yieldBarrier = new CountDownLatch(1);

	@Test
	public void testLeader() throws Exception {
		assertThat(this.adapter.isRunning()).isFalse();
		LeaderEventPublisher publisher = publisher();
		DefaultCandidate candidate1 = new DefaultCandidate("foo", "sitest");
		LeaderInitiator initiator1 = new LeaderInitiator(this.client, candidate1, "/sitest");
		initiator1.setLeaderEventPublisher(publisher);
		initiator1.start();
		DefaultCandidate candidate2 = new DefaultCandidate("bar", "sitest");
		LeaderInitiator initiator2 = new LeaderInitiator(this.client, candidate2, "/sitest");
		initiator2.setLeaderEventPublisher(publisher);
		initiator2.start();
		AbstractLeaderEvent event = this.events.poll(30, TimeUnit.SECONDS);
		assertThat(event).isNotNull();
		assertThat(event).isInstanceOf(OnGrantedEvent.class);

		assertThat(this.adapter.isRunning()).isTrue();

		event.getContext().yield();
		event = this.events.poll(30, TimeUnit.SECONDS);
		assertThat(event).isNotNull();
		assertThat(event).isInstanceOf(OnRevokedEvent.class);

		assertThat(this.adapter.isRunning()).isFalse();

		this.yieldBarrier.countDown();

		event = this.events.poll(30, TimeUnit.SECONDS);
		assertThat(event).isNotNull();
		assertThat(event).isInstanceOf(OnGrantedEvent.class);

		assertThat(this.adapter.isRunning()).isTrue();

		initiator1.stop();
		initiator2.stop();
		event = this.events.poll(30, TimeUnit.SECONDS);
		assertThat(event).isNotNull();
		assertThat(event).isInstanceOf(OnRevokedEvent.class);

		assertThat(this.adapter.isRunning()).isFalse();
	}

	private LeaderEventPublisher publisher() {
		return new DefaultLeaderEventPublisher(new ApplicationEventPublisher() {

			volatile boolean onRevokedEventHappened;

			@Override
			public void publishEvent(Object event) {
			}

			@Override
			public void publishEvent(ApplicationEvent event) {
				AbstractLeaderEvent leadershipEvent = (AbstractLeaderEvent) event;
				if (this.onRevokedEventHappened) {
					try {
						yieldBarrier.await(10, TimeUnit.SECONDS);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new RuntimeException(e);
					}
				}
				onRevokedEventHappened = event instanceof OnRevokedEvent;
				controller.onApplicationEvent((AbstractLeaderEvent) event);
				events.add(leadershipEvent);
			}

		});
	}

	private SourcePollingChannelAdapter buildChannelAdapter() {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		adapter.setSource(mock(MessageSource.class));
		adapter.setOutputChannel(new QueueChannel());
		adapter.setTrigger(mock(Trigger.class));
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.setTaskScheduler(mock(TaskScheduler.class));
		adapter.afterPropertiesSet();
		return adapter;
	}

}

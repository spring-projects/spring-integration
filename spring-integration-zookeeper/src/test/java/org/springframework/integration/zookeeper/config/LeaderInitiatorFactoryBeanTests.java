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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.leader.Context;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.leader.event.AbstractLeaderEvent;
import org.springframework.integration.leader.event.DefaultLeaderEventPublisher;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;
import org.springframework.integration.zookeeper.ZookeeperTestSupport;
import org.springframework.integration.zookeeper.leader.LeaderInitiator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class LeaderInitiatorFactoryBeanTests extends ZookeeperTestSupport {

	private static CuratorFramework client;

	@Autowired
	private LeaderInitiator leaderInitiator;

	@Autowired
	private Config config;

	@BeforeAll
	public static void getClient() {
		client = createNewClient();
	}

	@AfterAll
	public static void closeClient() {
		if (client != null) {
			client.close();
		}
	}

	@Test
	public void test() throws Exception {
		assertThat(this.config.latch1.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.config.events.get(0)).isInstanceOf(OnGrantedEvent.class);
		this.leaderInitiator.stop();
		assertThat(this.config.latch2.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.config.events.get(1)).isInstanceOf(OnRevokedEvent.class);
	}

	@Test
	public void testExceptionFromEvent() throws Exception {
		CountDownLatch onGranted = new CountDownLatch(1);

		LeaderInitiator initiator = new LeaderInitiator(client, new DefaultCandidate("foo", "bar"));

		initiator.setLeaderEventPublisher(new DefaultLeaderEventPublisher() {

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

		assertThat(initiator.getContext().getRole()).isEqualTo("bar");
		initiator.start();

		assertThat(onGranted.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(initiator.getContext().isLeader()).isTrue();
		assertThat(initiator.getContext().getRole()).isEqualTo("bar");

		initiator.stop();
	}

	@Configuration
	public static class Config {

		private final List<AbstractLeaderEvent> events = new ArrayList<>();

		private final CountDownLatch latch1 = new CountDownLatch(1);

		private final CountDownLatch latch2 = new CountDownLatch(2);

		@Bean
		public LeaderInitiatorFactoryBean leaderInitiator(CuratorFramework client) {
			return new LeaderInitiatorFactoryBean()
					.setClient(client)
					.setPath("/siTest/")
					.setCandidate(new DefaultCandidate(UUID.randomUUID().toString(), "foo"));
		}

		@Bean
		public CuratorFramework client() {
			return LeaderInitiatorFactoryBeanTests.client;
		}

		@Bean
		public ApplicationListener<?> listener() {
			return event -> {
				events.add((AbstractLeaderEvent) event);
				latch1.countDown();
				latch2.countDown();
			};
		}

	}

}

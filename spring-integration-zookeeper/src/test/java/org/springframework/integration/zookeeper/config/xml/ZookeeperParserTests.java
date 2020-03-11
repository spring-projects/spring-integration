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

package org.springframework.integration.zookeeper.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
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
public class ZookeeperParserTests extends ZookeeperTestSupport {

	@Autowired
	private LeaderInitiator initiator;

	@Autowired
	private SourcePollingChannelAdapter adapter;

	@Autowired
	private CuratorFramework client;

	@Test
	public void test() throws Exception {
		assertThat(this.initiator.isAutoStartup()).isFalse();
		assertThat(this.initiator.getPhase()).isEqualTo(1234);
		assertThat(TestUtils.getPropertyValue(this.initiator, "namespace")).isEqualTo("/siNamespaceTest");
		assertThat(TestUtils.getPropertyValue(this.initiator, "candidate.role")).isEqualTo("cluster");
		assertThat(TestUtils.getPropertyValue(this.initiator, "client")).isSameAs(this.client);

		this.initiator.start();
		int n = 0;
		while (n++ < 100 && !this.adapter.isRunning()) {
			Thread.sleep(100);
		}
		assertThat(this.adapter.isRunning()).isTrue();
		this.initiator.stop();
		n = 0;
		while (n++ < 100 && this.adapter.isRunning()) {
			Thread.sleep(100);
		}
		assertThat(this.adapter.isRunning()).isFalse();
	}

	public static class Config {

		@Bean
		public CuratorFramework client() {
			return createNewClient();
		}

	}

}

/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.zookeeper.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.zookeeper.ZookeeperTestSupport;
import org.springframework.integration.zookeeper.leader.LeaderInitiator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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
		assertFalse(this.initiator.isAutoStartup());
		assertEquals(1234, this.initiator.getPhase());
		assertEquals("/siNamespaceTest", TestUtils.getPropertyValue(this.initiator, "namespace"));
		assertEquals("cluster", TestUtils.getPropertyValue(this.initiator, "candidate.role"));
		assertSame(this.client, TestUtils.getPropertyValue(this.initiator, "client"));

		this.initiator.start();
		int n = 0;
		while (n++ < 100 && !this.adapter.isRunning()) {
			Thread.sleep(100);
		}
		assertTrue(this.adapter.isRunning());
		this.initiator.stop();
		n = 0;
		while (n++ < 100 && this.adapter.isRunning()) {
			Thread.sleep(100);
		}
		assertFalse(this.adapter.isRunning());
	}

	public static class Config {

		@Bean
		public CuratorFramework client() throws Exception {
			return createNewClient();
		}
	}

}

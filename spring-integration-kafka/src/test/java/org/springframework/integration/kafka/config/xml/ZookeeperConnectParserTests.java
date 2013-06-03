/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.kafka.config.xml;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.kafka.core.ZookeeperConnectDefaults;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Soby Chacko
 * @since 0.5
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ZookeeperConnectParserTests {

	@Autowired
	private ApplicationContext appContext;

	@Test
	public void testCustomKafkaBrokerConfiguration() {
		final ZookeeperConnect broker = appContext.getBean("zookeeperConnect", ZookeeperConnect.class);

		Assert.assertEquals("localhost:2181", broker.getZkConnect());
		Assert.assertEquals("10000", broker.getZkConnectionTimeout());
		Assert.assertEquals("10000", broker.getZkSessionTimeout());
		Assert.assertEquals("200", broker.getZkSyncTime());
	}

	@Test
	public void testDefaultKafkaBrokerConfiguration() {
		final ZookeeperConnect broker = appContext.getBean("defaultZookeeperConnect", ZookeeperConnect.class);

		Assert.assertEquals(ZookeeperConnectDefaults.ZK_CONNECT, broker.getZkConnect());
		Assert.assertEquals(ZookeeperConnectDefaults.ZK_CONNECTION_TIMEOUT, broker.getZkConnectionTimeout());
		Assert.assertEquals(ZookeeperConnectDefaults.ZK_SESSION_TIMEOUT, broker.getZkSessionTimeout());
		Assert.assertEquals(ZookeeperConnectDefaults.ZK_SYNC_TIME, broker.getZkSyncTime());
	}
}

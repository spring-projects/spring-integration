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

package org.springframework.integration.zookeeper.config;

import static org.junit.Assert.assertTrue;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.test.TestingServer;
import org.junit.Test;

/**
 * @author Gary Russell
 *
 */
public class CuratorFrameworkFactoryBeanTests {

	@Test
	public void test() throws Exception {
		TestingServer testingServer = new TestingServer();
		CuratorFrameworkFactoryBean fb = new CuratorFrameworkFactoryBean(testingServer.getConnectString());
		CuratorFramework client = fb.getObject();
		fb.start();
		assertTrue(client.getState().equals(CuratorFrameworkState.STARTED));
		fb.stop();
		assertTrue(client.getState().equals(CuratorFrameworkState.STOPPED));
		testingServer.close();
	}

}

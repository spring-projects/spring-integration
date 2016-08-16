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

package org.springframework.integration.zookeeper;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.CloseableUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * @author Marius Bogoevici
 * @author Gary Russell
 * @since 4.2
 *
 */
public class ZookeeperTestSupport {

	private static final Log logger = LogFactory.getLog(ZookeeperTestSupport.class);

	protected final Log log = LogFactory.getLog(this.getClass());

	protected static TestingServer testingServer;

	protected CuratorFramework client;

	@BeforeClass
	public static void setUpClass() throws Exception {
		testingServer = new TestingServer(true);
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		try {
			testingServer.stop();
		}
		catch (IOException e) {
			logger.warn("Exception thrown while shutting down ZooKeeper: ", e);
		}
		testingServer.getTempDirectory().delete();
	}

	@Before
	public void setUp() throws Exception {
		client = createNewClient();
	}

	@After
	public void tearDown() throws Exception {
		CloseableUtils.closeQuietly(this.client);
	}

	protected static CuratorFramework createNewClient() throws InterruptedException {
		CuratorFramework client = CuratorFrameworkFactory.newClient(testingServer.getConnectString(),
				new BoundedExponentialBackoffRetry(100, 1000, 3));
		client.start();
		return client;
	}

	protected void closeClient(CuratorFramework client) {
		try {
			CloseableUtils.closeQuietly(client);
		}
		catch (Exception e) {
			log.warn("Exception thrown while closing client: ", e);
		}
	}

}

/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Marius Bogoevici
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public class ZookeeperTestSupport {

	private static final Log logger = LogFactory.getLog(ZookeeperTestSupport.class);

	protected final Log log = LogFactory.getLog(this.getClass());

	protected static TestingServer testingServer;

	protected CuratorFramework client;

	@BeforeAll
	public static void setUpClass() throws Exception {
		testingServer = new TestingServer(true);
	}

	@AfterAll
	public static void tearDownClass() {
		try {
			testingServer.stop();
		}
		catch (IOException e) {
			logger.warn("Exception thrown while shutting down ZooKeeper: ", e);
		}
		testingServer.getTempDirectory().delete();
	}

	@BeforeEach
	public void setUp() {
		client = createNewClient();
	}

	@AfterEach
	public void tearDown() throws Exception {
		CloseableUtils.closeQuietly(this.client);
	}

	protected static CuratorFramework createNewClient() {
		CuratorFramework client = CuratorFrameworkFactory.newClient(testingServer.getConnectString(),
				new BoundedExponentialBackoffRetry(100, 1000, 3));
		client.start();
		return client;
	}

}

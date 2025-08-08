/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.zookeeper.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(client.getState().equals(CuratorFrameworkState.STARTED)).isTrue();
		fb.stop();
		assertThat(client.getState().equals(CuratorFrameworkState.STOPPED)).isTrue();
		testingServer.close();
	}

}

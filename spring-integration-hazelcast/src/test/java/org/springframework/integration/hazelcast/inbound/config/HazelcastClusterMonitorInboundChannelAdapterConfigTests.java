/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast.inbound.config;

import com.hazelcast.core.HazelcastInstance;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.hazelcast.inbound.util.HazelcastInboundChannelAdapterTestUtils;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Hazelcast Cluster Monitor Inbound Channel Adapter JavaConfig driven Unit Test Class
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = HazelcastIntegrationInboundTestConfiguration.class)
@DirtiesContext
public class HazelcastClusterMonitorInboundChannelAdapterConfigTests {

	@Autowired
	private PollableChannel cmonChannel;

	@Autowired
	private PollableChannel cmonChannel2;

	@Autowired
	private HazelcastInstance testHazelcastInstance;

	@Test
	public void testConfigDrivenDistributedObjectEvent() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedObjectEventByChannelAndHazelcastInstance(cmonChannel2,
						testHazelcastInstance, "Test_Distributed_Map3");
	}

}

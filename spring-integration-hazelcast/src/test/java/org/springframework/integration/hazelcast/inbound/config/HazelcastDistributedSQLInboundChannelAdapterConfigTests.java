/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast.inbound.config;

import com.hazelcast.map.IMap;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.hazelcast.HazelcastIntegrationTestUser;
import org.springframework.integration.hazelcast.inbound.util.HazelcastInboundChannelAdapterTestUtils;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Hazelcast Distributed SQL Inbound Channel Adapter JavaConfig driven Unit Test Class
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HazelcastIntegrationInboundTestConfiguration.class,
		loader = AnnotationConfigContextLoader.class)
@DirtiesContext
public class HazelcastDistributedSQLInboundChannelAdapterConfigTests {

	@Autowired
	private PollableChannel dsDistributedMapChannel;

	@Autowired
	private PollableChannel dsDistributedMapChannel2;

	@Autowired
	private PollableChannel dsDistributedMapChannel3;

	@Autowired
	private PollableChannel dsDistributedMapChannel4;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> testDSDistributedMap;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> testDSDistributedMap2;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> testDSDistributedMap3;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> testDSDistributedMap4;

	@Test
	public void testDistributedSQLForENTRYIterationType() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedSQLForENTRYIterationType(testDSDistributedMap,
						dsDistributedMapChannel);
	}

	@Test
	public void testDistributedSQLForKEYIterationType() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedSQLForKEYIterationType(testDSDistributedMap2,
						dsDistributedMapChannel2);
	}

	@Test
	public void testDistributedSQLForLOCAL_KEYIterationType() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedSQLForLOCAL_KEYIterationType(testDSDistributedMap3,
						dsDistributedMapChannel3);
	}

	@Test
	public void testDistributedSQLForVALUEIterationType() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedSQLForVALUEIterationType(testDSDistributedMap4,
						dsDistributedMapChannel4);
	}

}

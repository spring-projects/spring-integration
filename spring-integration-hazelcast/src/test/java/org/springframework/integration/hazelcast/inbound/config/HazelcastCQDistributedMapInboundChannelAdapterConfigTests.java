/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast.inbound.config;

import com.hazelcast.map.IMap;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.hazelcast.HazelcastIntegrationTestUser;
import org.springframework.integration.hazelcast.inbound.util.HazelcastInboundChannelAdapterTestUtils;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Hazelcast Continuous Query Inbound Channel Adapter JavaConfig driven Unit Test Class
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
@SpringJUnitConfig(classes = HazelcastIntegrationInboundTestConfiguration.class,
		loader = AnnotationConfigContextLoader.class)
@DirtiesContext
public class HazelcastCQDistributedMapInboundChannelAdapterConfigTests {

	@Autowired
	private PollableChannel cqDistributedMapChannel1;

	@Autowired
	private PollableChannel cqDistributedMapChannel2;

	@Autowired
	private PollableChannel cqDistributedMapChannel3;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> testCQDistributedMap1;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> testCQDistributedMap2;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> testCQDistributedMap3;

	@Test
	public void testContinuousQueryForADDEDEntryEvent() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForADDEDDistributedMapEntryEvent(testCQDistributedMap1,
						cqDistributedMapChannel1, "Test_CQ_Distributed_Map1");
	}

	@Test
	public void testContinuousQueryForALLEntryEvent() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForDistributedMapEntryEvents(testCQDistributedMap2,
						cqDistributedMapChannel2, "Test_CQ_Distributed_Map2");
	}

	@Test
	public void testContinuousQueryForUPDATEDEntryEventWhenIncludeValueIsFalse() {
		HazelcastInboundChannelAdapterTestUtils
				.testContinuousQueryForUPDATEDEntryEventWhenIncludeValueIsFalse(
						testCQDistributedMap3, cqDistributedMapChannel3,
						"Test_CQ_Distributed_Map3");
	}

}

/*
 * Copyright 2015-2022 the original author or authors.
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
 * Hazelcast Continuous Query Inbound Channel Adapter JavaConfig driven Unit Test Class
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HazelcastIntegrationInboundTestConfiguration.class,
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

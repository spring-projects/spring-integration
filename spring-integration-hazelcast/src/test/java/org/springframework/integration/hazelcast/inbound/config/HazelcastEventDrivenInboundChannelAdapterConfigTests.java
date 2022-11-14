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

import com.hazelcast.collection.IList;
import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ISet;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.topic.ITopic;
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
 * Hazelcast Event Driven Inbound Channel Adapter JavaConfig driven Unit Test Class
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HazelcastIntegrationInboundTestConfiguration.class,
		loader = AnnotationConfigContextLoader.class)
@DirtiesContext
public class HazelcastEventDrivenInboundChannelAdapterConfigTests {

	@Autowired
	private PollableChannel distributedMapChannel;

	@Autowired
	private PollableChannel distributedMapChannel2;

	@Autowired
	private PollableChannel distributedListChannel;

	@Autowired
	private PollableChannel distributedSetChannel;

	@Autowired
	private PollableChannel distributedQueueChannel;

	@Autowired
	private PollableChannel topicChannel;

	@Autowired
	private PollableChannel replicatedMapChannel;

	@Autowired
	private PollableChannel multiMapChannel;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> testDistributedMap;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> testDistributedMap2;

	@Autowired
	private IList<HazelcastIntegrationTestUser> testDistributedList;

	@Autowired
	private ISet<HazelcastIntegrationTestUser> testDistributedSet;

	@Autowired
	private IQueue<HazelcastIntegrationTestUser> testDistributedQueue;

	@Autowired
	private ITopic<HazelcastIntegrationTestUser> testTopic;

	@Autowired
	private ReplicatedMap<Integer, HazelcastIntegrationTestUser> testReplicatedMap;

	@Autowired
	private MultiMap<Integer, HazelcastIntegrationTestUser> testMultiMap;

	@Test
	public void testEventDrivenForADDEDEntryEvent() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForADDEDDistributedMapEntryEvent(testDistributedMap,
						distributedMapChannel, "Test_Distributed_Map");
	}

	@Test
	public void testEventDrivenForEntryEvents() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForDistributedMapEntryEvents(testDistributedMap2,
						distributedMapChannel2, "Test_Distributed_Map2");
	}

	@Test
	public void testEventDrivenForDistributedListItemEvents() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForDistributedCollectionItemEvents(testDistributedList,
						distributedListChannel);
	}

	@Test
	public void testEventDrivenForDistributedSetItemEvents() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForDistributedCollectionItemEvents(testDistributedSet,
						distributedSetChannel);
	}

	@Test
	public void testEventDrivenForDistributedQueueItemEvents() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForDistributedCollectionItemEvents(testDistributedQueue,
						distributedQueueChannel);
	}

	@Test
	public void testEventDrivenForADDEDMessageEvent() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForTopicMessageEvent(testTopic, topicChannel);
	}

	@Test
	public void testEventDrivenForReplicatedMapEntryEvents() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForReplicatedMapEntryEvents(testReplicatedMap,
						replicatedMapChannel, "Test_Replicated_Map");
	}

	@Test
	public void testEventDrivenForMultiMapEntryEvents() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForMultiMapEntryEvents(testMultiMap, multiMapChannel,
						"Test_Multi_Map");
	}

}

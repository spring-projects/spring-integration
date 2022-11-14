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

package org.springframework.integration.hazelcast.outbound.config;

import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.hazelcast.instance.impl.HazelcastInstanceFactory;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.topic.ITopic;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.hazelcast.HazelcastIntegrationTestUser;
import org.springframework.integration.hazelcast.HazelcastTestRequestHandlerAdvice;
import org.springframework.integration.hazelcast.outbound.util.HazelcastOutboundChannelAdapterTestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Hazelcast Outbound Channel Adapter JavaConfig driven Unit Test Class
 *
 * @author Eren Avsarogullari
 * @author Atem Bilan
 *
 * @since 6.0
 */
@SpringJUnitConfig(classes = HazelcastIntegrationOutboundTestConfiguration.class)
@DirtiesContext
public class HazelcastOutboundChannelAdapterConfigTests {

	@Autowired
	@Qualifier("distMapChannel")
	private MessageChannel distMapChannel;

	@Autowired
	@Qualifier("distMapBulkChannel")
	private MessageChannel distMapBulkChannel;

	@Autowired
	@Qualifier("distListChannel")
	private MessageChannel distListChannel;

	@Autowired
	@Qualifier("distSetChannel")
	private MessageChannel distSetChannel;

	@Autowired
	@Qualifier("distQueueChannel")
	private MessageChannel distQueueChannel;

	@Autowired
	@Qualifier("topicChannel2")
	private MessageChannel topicChannel2;

	@Autowired
	@Qualifier("multiMapChannel2")
	private MessageChannel multiMapChannel2;

	@Autowired
	@Qualifier("replicatedMapChannel2")
	private MessageChannel replicatedMapChannel2;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> distMap;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> distBulkMap;

	@Autowired
	private List<HazelcastIntegrationTestUser> distList;

	@Autowired
	private Set<HazelcastIntegrationTestUser> distSet;

	@Autowired
	private Queue<HazelcastIntegrationTestUser> distQueue;

	@Autowired
	private ITopic<HazelcastIntegrationTestUser> topic;

	@Autowired
	private MultiMap<Integer, HazelcastIntegrationTestUser> multiMap;

	@Autowired
	private ReplicatedMap<Integer, HazelcastIntegrationTestUser> replicatedMap;

	@Autowired
	@Qualifier("distMapRequestHandlerAdvice")
	private HazelcastTestRequestHandlerAdvice distMapRequestHandlerAdvice;

	@Autowired
	@Qualifier("distBulkMapRequestHandlerAdvice")
	private HazelcastTestRequestHandlerAdvice distBulkMapRequestHandlerAdvice;

	@Autowired
	@Qualifier("distListRequestHandlerAdvice")
	private HazelcastTestRequestHandlerAdvice distListRequestHandlerAdvice;

	@Autowired
	@Qualifier("distSetRequestHandlerAdvice")
	private HazelcastTestRequestHandlerAdvice distSetRequestHandlerAdvice;

	@Autowired
	@Qualifier("distQueueRequestHandlerAdvice")
	private HazelcastTestRequestHandlerAdvice distQueueRequestHandlerAdvice;

	@Autowired
	@Qualifier("topicRequestHandlerAdvice")
	private HazelcastTestRequestHandlerAdvice topicRequestHandlerAdvice;

	@Autowired
	@Qualifier("multiMapRequestHandlerAdvice")
	private HazelcastTestRequestHandlerAdvice multiMapRequestHandlerAdvice;

	@Autowired
	@Qualifier("replicatedMapRequestHandlerAdvice")
	private HazelcastTestRequestHandlerAdvice replicatedMapRequestHandlerAdvice;

	@AfterAll
	public static void shutdown() {
		HazelcastInstanceFactory.terminateAll();
	}

	@Test
	public void testWriteToDistributedMap() {
		HazelcastOutboundChannelAdapterTestUtils
				.testWriteToDistributedMap(this.distMapChannel, this.distMap,
						this.distMapRequestHandlerAdvice);
	}

	@Test
	public void testBulkWriteToDistributedMap() {
		HazelcastOutboundChannelAdapterTestUtils
				.testBulkWriteToDistributedMap(this.distMapBulkChannel, this.distBulkMap,
						this.distBulkMapRequestHandlerAdvice);
	}

	@Test
	public void testWriteToDistributedList() {
		HazelcastOutboundChannelAdapterTestUtils
				.testWriteToDistributedList(this.distListChannel, this.distList,
						this.distListRequestHandlerAdvice);
	}

	@Test
	public void testWriteToDistributedSet() {
		HazelcastOutboundChannelAdapterTestUtils
				.testWriteToDistributedSet(this.distSetChannel, this.distSet,
						this.distSetRequestHandlerAdvice);
	}

	@Test
	public void testWriteToDistributedQueue() {
		HazelcastOutboundChannelAdapterTestUtils
				.testWriteToDistributedQueue(this.distQueueChannel, this.distQueue,
						this.distQueueRequestHandlerAdvice);
	}

	@Test
	public void testWriteToTopic() {
		HazelcastOutboundChannelAdapterTestUtils
				.testWriteToTopic(this.topicChannel2, this.topic, this.topicRequestHandlerAdvice);
	}

	@Test
	public void testWriteToMultiMap() {
		HazelcastOutboundChannelAdapterTestUtils
				.testWriteToMultiMap(this.multiMapChannel2, this.multiMap,
						this.multiMapRequestHandlerAdvice);
	}

	@Test
	public void testWriteToReplicatedMap() {
		HazelcastOutboundChannelAdapterTestUtils
				.testWriteToReplicatedMap(this.replicatedMapChannel2, this.replicatedMap,
						this.replicatedMapRequestHandlerAdvice);
	}

}

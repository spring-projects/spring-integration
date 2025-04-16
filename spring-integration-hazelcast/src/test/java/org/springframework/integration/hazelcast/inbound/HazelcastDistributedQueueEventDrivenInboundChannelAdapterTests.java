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

package org.springframework.integration.hazelcast.inbound;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.EntryEventType;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.hazelcast.HazelcastHeaders;
import org.springframework.integration.hazelcast.HazelcastIntegrationTestUser;
import org.springframework.integration.hazelcast.inbound.util.HazelcastInboundChannelAdapterTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hazelcast Distributed Queue Event Driven Inbound Channel Adapter Test
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@DirtiesContext
@SuppressWarnings({"unchecked", "rawtypes"})
public class HazelcastDistributedQueueEventDrivenInboundChannelAdapterTests {

	@Autowired
	private PollableChannel edQueueChannel1;

	@Autowired
	private PollableChannel edQueueChannel2;

	@Autowired
	private PollableChannel edQueueChannel3;

	@Autowired
	private IQueue edDistributedQueue1;

	@Autowired
	private IQueue edDistributedQueue2;

	@Autowired
	private IQueue edDistributedQueue3;

	@Test
	public void testEventDrivenForOnlyADDEDEntryEvent() {
		edDistributedQueue1
				.add(new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1"));
		Message<?> msg =
				edQueueChannel1.receive(HazelcastInboundChannelAdapterTestUtils.TIMEOUT);
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.MEMBER)).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.EVENT_TYPE).toString()).isEqualTo(EntryEventType.ADDED.toString());
		assertThat((((HazelcastIntegrationTestUser) msg.getPayload()).getId())).isEqualTo(1);
		assertThat((((HazelcastIntegrationTestUser) msg.getPayload()).getName())).isEqualTo("TestName1");
		assertThat((((HazelcastIntegrationTestUser) msg.getPayload()).getSurname())).isEqualTo("TestSurname1");
	}

	@Test
	public void testEventDrivenForOnlyREMOVEDEntryEvent() {
		HazelcastIntegrationTestUser user =
				new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2");
		edDistributedQueue2.add(user);
		edDistributedQueue2.remove(user);
		Message<?> msg =
				edQueueChannel2.receive(HazelcastInboundChannelAdapterTestUtils.TIMEOUT);
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.MEMBER)).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.EVENT_TYPE).toString()).isEqualTo(EntryEventType.REMOVED.toString());
		assertThat((((HazelcastIntegrationTestUser) msg.getPayload()).getId())).isEqualTo(2);
		assertThat((((HazelcastIntegrationTestUser) msg.getPayload()).getName())).isEqualTo("TestName2");
		assertThat((((HazelcastIntegrationTestUser) msg.getPayload()).getSurname())).isEqualTo("TestSurname2");
	}

	@Test
	public void testEventDrivenForALLEntryEvent() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForDistributedCollectionItemEvents(edDistributedQueue3,
						edQueueChannel3);
	}

}

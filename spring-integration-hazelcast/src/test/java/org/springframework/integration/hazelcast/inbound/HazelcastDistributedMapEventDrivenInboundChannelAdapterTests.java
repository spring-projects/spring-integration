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

import com.hazelcast.core.EntryEventType;
import com.hazelcast.map.IMap;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.hazelcast.HazelcastHeaders;
import org.springframework.integration.hazelcast.HazelcastIntegrationTestUser;
import org.springframework.integration.hazelcast.inbound.util.HazelcastInboundChannelAdapterTestUtils;
import org.springframework.integration.hazelcast.message.EntryEventMessagePayload;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hazelcast Distributed Map Event Driven Inbound Channel Adapter Test
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@DirtiesContext
@SuppressWarnings({"unchecked", "rawtypes"})
public class HazelcastDistributedMapEventDrivenInboundChannelAdapterTests {

	@Autowired
	private PollableChannel edMapChannel1;

	@Autowired
	private PollableChannel edMapChannel2;

	@Autowired
	private PollableChannel edMapChannel3;

	@Autowired
	private PollableChannel edMapChannel4;

	@Autowired
	private IMap edDistributedMap1;

	@Autowired
	private IMap edDistributedMap2;

	@Autowired
	private IMap edDistributedMap3;

	@Autowired
	private IMap edDistributedMap4;

	@Test
	public void testEventDrivenForOnlyADDEDEntryEvent() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForADDEDDistributedMapEntryEvent(edDistributedMap1,
						edMapChannel1, "edDistributedMap1");
	}

	@Test
	public void testEventDrivenForOnlyUPDATEDEntryEvent() {
		edDistributedMap2
				.put(2, new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1"));
		edDistributedMap2
				.put(2, new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2"));
		Message<?> msg =
				edMapChannel2.receive(HazelcastInboundChannelAdapterTestUtils.TIMEOUT);
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getPayload() instanceof EntryEventMessagePayload).isTrue();
		assertThat(msg.getHeaders().get(HazelcastHeaders.MEMBER)).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.EVENT_TYPE)).isEqualTo(EntryEventType.UPDATED.name());
		assertThat(msg.getHeaders().get(HazelcastHeaders.CACHE_NAME)).isEqualTo("edDistributedMap2");

		assertThat(((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).key).isEqualTo(Integer.valueOf(2));
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).oldValue).getId()).isEqualTo(1);
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).oldValue).getName()).isEqualTo("TestName1");
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).oldValue).getSurname()).isEqualTo("TestSurname1");
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).value).getId()).isEqualTo(2);
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).value).getName()).isEqualTo("TestName2");
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).value).getSurname()).isEqualTo("TestSurname2");
	}

	@Test
	public void testEventDrivenForOnlyREMOVEDEntryEvent() {
		edDistributedMap3
				.put(1, new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1"));
		edDistributedMap3
				.put(2, new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2"));
		edDistributedMap3.remove(2);
		Message<?> msg =
				edMapChannel3.receive(HazelcastInboundChannelAdapterTestUtils.TIMEOUT);
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getPayload() instanceof EntryEventMessagePayload).isTrue();
		assertThat(msg.getHeaders().get(HazelcastHeaders.MEMBER)).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.EVENT_TYPE)).isEqualTo(EntryEventType.REMOVED.name());
		assertThat(msg.getHeaders().get(HazelcastHeaders.CACHE_NAME)).isEqualTo("edDistributedMap3");

		assertThat(((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).key).isEqualTo(Integer.valueOf(2));
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).oldValue).getId()).isEqualTo(2);
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).oldValue).getName()).isEqualTo("TestName2");
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).oldValue).getSurname()).isEqualTo("TestSurname2");
	}

	@Test
	public void testEventDrivenForALLEntryEvent() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForDistributedMapEntryEvents(edDistributedMap4, edMapChannel4,
						"edDistributedMap4");
	}

}

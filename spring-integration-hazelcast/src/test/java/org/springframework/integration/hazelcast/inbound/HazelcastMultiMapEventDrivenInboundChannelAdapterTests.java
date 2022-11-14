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
import com.hazelcast.multimap.MultiMap;
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
 * Hazelcast MultiMap Event Driven Inbound Channel Adapter Test
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 * @since 6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@DirtiesContext
@SuppressWarnings({"unchecked", "rawtypes"})
public class HazelcastMultiMapEventDrivenInboundChannelAdapterTests {

	@Autowired
	private PollableChannel edMultiMapChannel1;

	@Autowired
	private PollableChannel edMultiMapChannel2;

	@Autowired
	private PollableChannel edMultiMapChannel3;

	@Autowired
	private MultiMap edMultiMap1;

	@Autowired
	private MultiMap edMultiMap2;

	@Autowired
	private MultiMap edMultiMap3;

	@Test
	public void testEventDrivenForOnlyADDEDEntryEvent() {
		edMultiMap1
				.put(1, new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1"));
		Message<?> msg =
				edMultiMapChannel1.receive(HazelcastInboundChannelAdapterTestUtils.TIMEOUT);
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getPayload() instanceof EntryEventMessagePayload).isTrue();
		assertThat(msg.getHeaders().get(HazelcastHeaders.MEMBER)).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.EVENT_TYPE)).isEqualTo(EntryEventType.ADDED.name());
		assertThat(msg.getHeaders().get(HazelcastHeaders.CACHE_NAME)).isEqualTo("edMultiMap1");

		assertThat(((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).key).isEqualTo(Integer.valueOf(1));
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).value).getId()).isEqualTo(1);
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).value).getName()).isEqualTo("TestName1");
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).value).getSurname()).isEqualTo("TestSurname1");
	}

	@Test
	public void testEventDrivenForOnlyREMOVEDEntryEvent() {
		edMultiMap2
				.put(1, new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1"));
		edMultiMap2
				.put(2, new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2"));
		edMultiMap2.remove(2);
		Message<?> msg =
				edMultiMapChannel2.receive(HazelcastInboundChannelAdapterTestUtils.TIMEOUT);
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getPayload() instanceof EntryEventMessagePayload).isTrue();
		assertThat(msg.getHeaders().get(HazelcastHeaders.MEMBER)).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.EVENT_TYPE)).isEqualTo(EntryEventType.REMOVED.name());
		assertThat(msg.getHeaders().get(HazelcastHeaders.CACHE_NAME)).isEqualTo("edMultiMap2");

		assertThat(((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).key).isEqualTo(Integer.valueOf(2));
		assertThat(((EntryEventMessagePayload<?, ?>) msg.getPayload()).value).isNull();
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
				.testEventDrivenForMultiMapEntryEvents(edMultiMap3, edMultiMapChannel3,
						"edMultiMap3");
	}

}

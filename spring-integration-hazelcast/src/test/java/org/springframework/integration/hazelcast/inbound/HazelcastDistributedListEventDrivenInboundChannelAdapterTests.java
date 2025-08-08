/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast.inbound;

import com.hazelcast.collection.IList;
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
 * Hazelcast Distributed List Event Driven Inbound Channel Adapter Test Class
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@DirtiesContext
@SuppressWarnings({"unchecked", "rawtypes"})
public class HazelcastDistributedListEventDrivenInboundChannelAdapterTests {

	@Autowired
	private PollableChannel edListChannel1;

	@Autowired
	private PollableChannel edListChannel2;

	@Autowired
	private PollableChannel edListChannel3;

	@Autowired
	private IList edDistributedList1;

	@Autowired
	private IList edDistributedList2;

	@Autowired
	private IList edDistributedList3;

	@Test
	public void testEventDrivenForOnlyADDEDEntryEvent() {
		edDistributedList1.add(new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1"));
		Message<?> msg = edListChannel1.receive(HazelcastInboundChannelAdapterTestUtils.TIMEOUT);
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.MEMBER)).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.EVENT_TYPE).toString()).isEqualTo(EntryEventType.ADDED.toString());
		assertThat(((HazelcastIntegrationTestUser) msg.getPayload()).getId()).isEqualTo(1);
		assertThat(((HazelcastIntegrationTestUser) msg.getPayload()).getName()).isEqualTo("TestName1");
		assertThat(((HazelcastIntegrationTestUser) msg.getPayload()).getSurname()).isEqualTo("TestSurname1");
	}

	@Test
	public void testEventDrivenForOnlyREMOVEDEntryEvent() {
		HazelcastIntegrationTestUser user = new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2");
		edDistributedList2.add(user);
		edDistributedList2.remove(user);
		Message<?> msg = edListChannel2.receive(HazelcastInboundChannelAdapterTestUtils.TIMEOUT);
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.MEMBER)).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.EVENT_TYPE).toString()).isEqualTo(EntryEventType.REMOVED.toString());
		assertThat(((HazelcastIntegrationTestUser) msg.getPayload()).getId()).isEqualTo(2);
		assertThat(((HazelcastIntegrationTestUser) msg.getPayload()).getName()).isEqualTo("TestName2");
		assertThat(((HazelcastIntegrationTestUser) msg.getPayload()).getSurname()).isEqualTo("TestSurname2");
	}

	@Test
	public void testEventDrivenForALLEntryEvent() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForDistributedCollectionItemEvents(edDistributedList3, edListChannel3);
	}

}

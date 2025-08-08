/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast.inbound;

import com.hazelcast.topic.ITopic;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.hazelcast.inbound.util.HazelcastInboundChannelAdapterTestUtils;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Hazelcast Distributed Topic Event Driven Inbound Channel Adapter Test
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@DirtiesContext
@SuppressWarnings({"unchecked", "rawtypes"})
public class HazelcastDistributedTopicEventDrivenInboundChannelAdapterTests {

	@Autowired
	private PollableChannel edTopicChannel1;

	@Autowired
	private ITopic edDistributedTopic1;

	@Test
	public void testEventDrivenForOnlyADDEDEntryEvent() {
		HazelcastInboundChannelAdapterTestUtils
				.testEventDrivenForTopicMessageEvent(edDistributedTopic1, edTopicChannel1);
	}

}

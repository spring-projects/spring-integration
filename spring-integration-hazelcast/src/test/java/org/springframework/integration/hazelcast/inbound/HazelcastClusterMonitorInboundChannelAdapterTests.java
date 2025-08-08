/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast.inbound;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleEvent.LifecycleState;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.hazelcast.inbound.util.HazelcastInboundChannelAdapterTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hazelcast Cluster Monitor Inbound Channel Adapter Unit Test Class
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 */
@RunWith(SpringRunner.class)
@SpringJUnitConfig
@DirtiesContext
@Ignore("Hard to reach CP consensus with limited number of members in two clusters")
public class HazelcastClusterMonitorInboundChannelAdapterTests {

	private static final String TEST_GROUP_NAME1 = "Test_Group_Name1";

	@Autowired
	private PollableChannel cmChannel1;

	@Autowired
	private PollableChannel cmChannel2;

	@Autowired
	private PollableChannel cmChannel3;

	@Autowired
	private PollableChannel cmChannel4;

	@Autowired
	private PollableChannel cmChannel5;

	@Autowired
	private PollableChannel cmChannel6;

	@Autowired
	private HazelcastInstance hazelcastInstance;

	@Autowired
	private HazelcastInstance hazelcastInstance2;

	@Autowired
	private HazelcastInstance hazelcastInstance3;

	@AfterClass
	public static void shutdown() {
		HazelcastInstanceFactory.terminateAll();
	}

	@Test
	public void testDistributedObjectEvent() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedObjectEventByChannelAndHazelcastInstance(cmChannel2,
						hazelcastInstance, "Test_Distributed_Map4");
	}

	@Test
	public void testLifecycleEvent() {
		hazelcastInstance2.getLifecycleService().terminate();

		Message<?> msg =
				cmChannel4.receive(HazelcastInboundChannelAdapterTestUtils.TIMEOUT);
		verifyLifecycleEvent(msg, LifecycleState.SHUTTING_DOWN);

		msg = cmChannel4.receive(HazelcastInboundChannelAdapterTestUtils.TIMEOUT);
		verifyLifecycleEvent(msg, LifecycleState.SHUTDOWN);
	}

	private void verifyLifecycleEvent(final Message<?> msg,
			final LifecycleState lifecycleState) {
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getPayload() instanceof LifecycleEvent).isTrue();
		assertThat(((LifecycleEvent) msg.getPayload()).getState()).isEqualTo(lifecycleState);
	}

}

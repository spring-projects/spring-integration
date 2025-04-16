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

import com.hazelcast.core.HazelcastInstance;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.hazelcast.inbound.util.HazelcastInboundChannelAdapterTestUtils;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Hazelcast Cluster Monitor Inbound Channel Adapter JavaConfig driven Unit Test Class
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = HazelcastIntegrationInboundTestConfiguration.class)
@DirtiesContext
public class HazelcastClusterMonitorInboundChannelAdapterConfigTests {

	@Autowired
	private PollableChannel cmonChannel;

	@Autowired
	private PollableChannel cmonChannel2;

	@Autowired
	private HazelcastInstance testHazelcastInstance;

	@Test
	public void testConfigDrivenDistributedObjectEvent() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedObjectEventByChannelAndHazelcastInstance(cmonChannel2,
						testHazelcastInstance, "Test_Distributed_Map3");
	}

}

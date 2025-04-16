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
 * Hazelcast Distributed SQL Inbound Channel Adapter JavaConfig driven Unit Test Class
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HazelcastIntegrationInboundTestConfiguration.class,
		loader = AnnotationConfigContextLoader.class)
@DirtiesContext
public class HazelcastDistributedSQLInboundChannelAdapterConfigTests {

	@Autowired
	private PollableChannel dsDistributedMapChannel;

	@Autowired
	private PollableChannel dsDistributedMapChannel2;

	@Autowired
	private PollableChannel dsDistributedMapChannel3;

	@Autowired
	private PollableChannel dsDistributedMapChannel4;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> testDSDistributedMap;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> testDSDistributedMap2;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> testDSDistributedMap3;

	@Autowired
	private IMap<Integer, HazelcastIntegrationTestUser> testDSDistributedMap4;

	@Test
	public void testDistributedSQLForENTRYIterationType() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedSQLForENTRYIterationType(testDSDistributedMap,
						dsDistributedMapChannel);
	}

	@Test
	public void testDistributedSQLForKEYIterationType() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedSQLForKEYIterationType(testDSDistributedMap2,
						dsDistributedMapChannel2);
	}

	@Test
	public void testDistributedSQLForLOCAL_KEYIterationType() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedSQLForLOCAL_KEYIterationType(testDSDistributedMap3,
						dsDistributedMapChannel3);
	}

	@Test
	public void testDistributedSQLForVALUEIterationType() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedSQLForVALUEIterationType(testDSDistributedMap4,
						dsDistributedMapChannel4);
	}

}

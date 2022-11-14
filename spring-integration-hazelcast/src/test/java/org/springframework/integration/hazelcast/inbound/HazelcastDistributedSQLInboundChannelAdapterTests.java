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

import com.hazelcast.map.IMap;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.hazelcast.inbound.util.HazelcastInboundChannelAdapterTestUtils;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Hazelcast Distributed SQL Inbound Channel Adapter Test
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@DirtiesContext
@SuppressWarnings({"unchecked", "rawtypes"})
public class HazelcastDistributedSQLInboundChannelAdapterTests {

	@Autowired
	private PollableChannel dsMapChannel1;

	@Autowired
	private PollableChannel dsMapChannel2;

	@Autowired
	private PollableChannel dsMapChannel3;

	@Autowired
	private PollableChannel dsMapChannel4;

	@Autowired
	private IMap dsDistributedMap1;

	@Autowired
	private IMap dsDistributedMap2;

	@Autowired
	private IMap dsDistributedMap3;

	@Autowired
	private IMap dsDistributedMap4;

	@Test
	public void testDistributedSQLForOnlyENTRYIterationType() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedSQLForENTRYIterationType(dsDistributedMap1, dsMapChannel1);
	}

	@Test
	public void testDistributedSQLForOnlyKEYIterationType() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedSQLForKEYIterationType(dsDistributedMap2, dsMapChannel2);
	}

	@Test
	public void testDistributedSQLForOnlyLOCAL_KEYIterationType() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedSQLForLOCAL_KEYIterationType(dsDistributedMap3, dsMapChannel3);
	}

	@Test
	public void testDistributedSQLForOnlyVALUEIterationType() {
		HazelcastInboundChannelAdapterTestUtils
				.testDistributedSQLForVALUEIterationType(dsDistributedMap4, dsMapChannel4);
	}

}

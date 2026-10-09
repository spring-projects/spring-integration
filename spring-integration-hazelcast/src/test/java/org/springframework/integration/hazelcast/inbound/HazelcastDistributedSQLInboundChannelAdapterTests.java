/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast.inbound;

import com.hazelcast.map.IMap;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.hazelcast.inbound.util.HazelcastInboundChannelAdapterTestUtils;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Hazelcast Distributed SQL Inbound Channel Adapter Test
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
@SpringJUnitConfig
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

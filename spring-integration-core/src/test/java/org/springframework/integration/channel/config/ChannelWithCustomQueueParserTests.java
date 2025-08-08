/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testcases for detailed namespace support for &lt;queue/> element under
 * &lt;channel/>
 *
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Gary Russell
 *
 * @see ChannelWithCustomQueueParserTests
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ChannelWithCustomQueueParserTests {

	@Qualifier("customQueueChannel")
	@Autowired
	QueueChannel customQueueChannel;

	@Test
	public void parseConfig() throws Exception {
		assertThat(customQueueChannel).isNotNull();
	}

	@Test
	public void queueTypeSet() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(customQueueChannel);
		Object queue = accessor.getPropertyValue("queue");
		assertThat(queue).isNotNull();
		assertThat(queue).isInstanceOf(ArrayBlockingQueue.class);
		assertThat(((BlockingQueue<?>) queue).remainingCapacity()).isEqualTo(2);
	}

}

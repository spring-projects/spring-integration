/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testcases for detailed namespace support for &lt;queue/> element under
 * &lt;channel/>
 *
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @see ChannelWithCustomQueueParserTests
 */
@SpringJUnitConfig
@DirtiesContext
public class ChannelWithCustomQueueParserTests {

	@Qualifier("customQueueChannel")
	@Autowired
	QueueChannel customQueueChannel;

	@Test
	public void parseConfig() {
		assertThat(customQueueChannel).isNotNull();
	}

	@Test
	public void queueTypeSet() {
		DirectFieldAccessor accessor = new DirectFieldAccessor(customQueueChannel);
		Object queue = accessor.getPropertyValue("queue");
		assertThat(queue).isNotNull();
		assertThat(queue).isInstanceOf(ArrayBlockingQueue.class);
		assertThat(((BlockingQueue<?>) queue).remainingCapacity()).isEqualTo(2);
	}

}

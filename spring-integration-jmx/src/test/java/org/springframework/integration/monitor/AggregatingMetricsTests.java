/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.monitor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class AggregatingMetricsTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private QueueChannel output;

	@Autowired
	private BridgeHandler handler;

	@Test
	public void test() {
		Message<?> message = new GenericMessage<String>("foo");
		int count = 2000;
		for (int i = 0; i < count; i++) {
			input.send(message);
		}
		assertEquals(count, this.output.getQueueSize());
		assertEquals(count, this.output.getSendCount());
		assertEquals(Long.valueOf(count / 1000).longValue(), this.output.getSendDuration().getCountLong());
		assertEquals(count, this.handler.getHandleCount());
		assertEquals(Long.valueOf(count / 1000).longValue(), this.handler.getDuration().getCountLong());
	}

}

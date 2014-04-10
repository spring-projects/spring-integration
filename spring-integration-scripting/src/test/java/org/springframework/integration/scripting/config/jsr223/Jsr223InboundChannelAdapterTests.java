/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.scripting.config.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class Jsr223InboundChannelAdapterTests {

	@Autowired
	@Qualifier("inbound-channel-adapter-channel")
	private PollableChannel inboundChannelAdapterChannel;

	@Test
	public void testInt2867InboundChannelAdapter() throws Exception {
		Message<?> message = this.inboundChannelAdapterChannel.receive(20000);
		assertNotNull(message);
		Object payload = message.getPayload();
		assertThat(payload, Matchers.instanceOf(Date.class));
		assertTrue(((Date) payload).before(new Date()));
		assertEquals("bar", message.getHeaders().get("foo"));

		message = this.inboundChannelAdapterChannel.receive(20000);
		assertNotNull(message);

		message = this.inboundChannelAdapterChannel.receive(10);
		assertNull(message);
	}

}

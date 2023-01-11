/*
 * Copyright 2013-2023 the original author or authors.
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

package org.springframework.integration.scripting.config.jsr223;

import java.util.Date;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class Jsr223InboundChannelAdapterTests {

	@Autowired
	@Qualifier("inbound-channel-adapter-channel")
	private PollableChannel inboundChannelAdapterChannel;

	@Test
	public void testInt2867InboundChannelAdapter() throws Exception {
		Message<?> message = this.inboundChannelAdapterChannel.receive(20000);
		assertThat(message).isNotNull();
		Object payload = message.getPayload();
		Thread.sleep(2);
		assertThat(payload).isInstanceOf(Date.class);
		assertThat(((Date) payload).before(new Date())).isTrue();
		assertThat(message.getHeaders().get("foo")).isEqualTo("bar");

		message = this.inboundChannelAdapterChannel.receive(20000);
		assertThat(message).isNotNull();
	}

}

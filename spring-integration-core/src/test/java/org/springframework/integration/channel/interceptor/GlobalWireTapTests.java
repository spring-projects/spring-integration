/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.channel.interceptor;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class GlobalWireTapTests {

	@Autowired
	@Qualifier("channel")
	DirectChannel channel;

	@Autowired
	@Qualifier("random-channel")
	MessageChannel anotherChannel;

	@Autowired
	@Qualifier("wiretap-single")
	PollableChannel wiretapSingle;

	@Autowired
	@Qualifier("wiretap-all")
	PollableChannel wiretapAll;

	@Autowired
	@Qualifier("wiretap-all2")
	PollableChannel wiretapAll2;

	@Test
	public void testWireTapsOnTargetedChannel() {
		Message<?> message = new GenericMessage<String>("hello");
		this.channel.send(message);
		Message<?> wireTapMessage = this.wiretapSingle.receive(100);
		assertThat(wireTapMessage).isNotNull();

		// There should be 5 messages on this channel:
		// 'channel', 'output', 'wiretapSingle', and too for 'unnamedGlobalWireTaps'.
		wireTapMessage = this.wiretapAll.receive(100);
		int msgCount = 0;
		while (wireTapMessage != null) {
			msgCount++;
			assertThat(message.getPayload()).isEqualTo(wireTapMessage.getPayload());
			wireTapMessage = this.wiretapAll.receive(100);
		}

		assertThat(msgCount).isEqualTo(5);

		assertThat(this.wiretapAll2.receive(1)).isNull();

		assertThat(this.channel.getInterceptors().size()).isEqualTo(4);
	}

	@Test
	public void testWireTapsOnRandomChannel() {
		Message<?> message = new GenericMessage<String>("hello");
		anotherChannel.send(message);

		//This time no message on wiretapSingle
		Message<?> wireTapMessage = wiretapSingle.receive(100);
		assertThat(wireTapMessage).isNull();

		//There should be two messages on this channel. One for 'channel' and one for 'output'
		wireTapMessage = wiretapAll.receive(100);
		int msgCount = 0;
		while (wireTapMessage != null) {
			msgCount++;
			assertThat(message.getPayload()).isEqualTo(wireTapMessage.getPayload());
			wireTapMessage = wiretapAll.receive(100);
		}

		assertThat(msgCount).isEqualTo(2);
	}

}

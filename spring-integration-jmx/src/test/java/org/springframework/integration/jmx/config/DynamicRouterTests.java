/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.jmx.config;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class DynamicRouterTests {

	@Autowired
	@Qualifier("controlChannel")
	private MessageChannel controlChannel;

	@Autowired
	@Qualifier("routingChannel")
	private MessageChannel routingChannel;

	@Autowired
	@Qualifier("processAChannel")
	private PollableChannel processAChannel;

	@Autowired
	@Qualifier("processBChannel")
	private PollableChannel processBChannel;

	@Autowired
	@Qualifier("processCChannel")
	private PollableChannel processCChannel;

	@Autowired
	private NullChannel nullChannel;

	@Test
	@DirtiesContext
	public void testRouteChange() throws Exception {
		routingChannel.send(new GenericMessage<String>("123"));
		assertThat(processAChannel.receive(0).getPayload()).isEqualTo("123");
		routingChannel.send(MessageBuilder.withPayload(123).build());
		assertThat(processBChannel.receive(0).getPayload()).isEqualTo(123);

		controlChannel.send(MessageBuilder.withPayload(new String[] {"java.lang.String", "processCChannel"}).build());

		routingChannel.send(new GenericMessage<String>("123"));
		assertThat(processCChannel.receive(0).getPayload()).isEqualTo("123");
	}

	@Test
	@DirtiesContext
	public void testRouteChangeMap() throws Exception {
		routingChannel.send(new GenericMessage<String>("123"));
		assertThat(processAChannel.receive(0).getPayload()).isEqualTo("123");
		routingChannel.send(MessageBuilder.withPayload(123).build());
		assertThat(processBChannel.receive(0).getPayload()).isEqualTo(123);
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("p1", "java.lang.String");
		args.put("p2", "processCChannel");

		controlChannel.send(MessageBuilder.withPayload(args).build());

		routingChannel.send(new GenericMessage<String>("123"));
		assertThat(processCChannel.receive(0).getPayload()).isEqualTo("123");
	}

	@Test
	@DirtiesContext
	public void testRouteChangeMapNamedArgs() throws Exception {
		routingChannel.send(new GenericMessage<String>("123"));
		assertThat(processAChannel.receive(0).getPayload()).isEqualTo("123");
		routingChannel.send(MessageBuilder.withPayload(123).build());
		assertThat(processBChannel.receive(0).getPayload()).isEqualTo(123);
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("key", "java.lang.String");
		args.put("channelName", "processCChannel");

		controlChannel.send(MessageBuilder.withPayload(args).build());

		routingChannel.send(new GenericMessage<String>("123"));
		assertThat(processCChannel.receive(0).getPayload()).isEqualTo("123");
	}

	@Test
	@DirtiesContext
	@Disabled
	public void testPerf() throws Exception {
//		this.nullChannel.enableStats(false);
		for (int i = 0; i < 1000000000; i++) {
			this.nullChannel.send(null);
		}
	}

}

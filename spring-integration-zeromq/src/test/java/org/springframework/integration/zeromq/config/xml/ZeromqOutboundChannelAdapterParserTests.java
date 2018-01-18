/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.zeromq.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Subhobrata Dey
 * @since 5.1
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class ZeromqOutboundChannelAdapterParserTests {

	@Autowired @Qualifier("withConverter")
	private EventDrivenConsumer withConverterEndpoint;

	@Autowired @Qualifier("withConverter.handler")
	private MessageHandler withConverterHandler;

	@Autowired @Qualifier("withDefaultConverter.handler")
	private org.springframework.integration.zeromq.outbound.ZeromqMessageHandler withDefaultConverterHandler;

	@Autowired
	private org.springframework.integration.zeromq.support.ZeromqMessageConverter converter;

	@Autowired
	private org.springframework.integration.zeromq.core.DefaultZeromqClientFactory clientFactory;

	@Test
	public void testWithConverter() {
		assertThat("tcp://*:5559").isEqualTo(TestUtils.getPropertyValue(withConverterHandler, "url"));
		assertThat("serverId1").isEqualTo(TestUtils.getPropertyValue(withConverterHandler, "clientId"));
		assertThat("zmq-foo").isEqualTo(TestUtils.getPropertyValue(withConverterHandler, "topic"));
		assertThat(converter).isEqualToComparingFieldByField(TestUtils.getPropertyValue(withConverterHandler, "converter"));
		assertThat(clientFactory).isEqualToComparingFieldByField(TestUtils.getPropertyValue(withConverterHandler, "clientFactory"));
	}

	@Test
	public void testWithDefaultConverter() {
		assertThat("tcp://*:5560").isEqualTo(TestUtils.getPropertyValue(withDefaultConverterHandler, "url"));
		assertThat("serverId2").isEqualTo(TestUtils.getPropertyValue(withDefaultConverterHandler, "clientId"));
		assertThat("zmq-foo").isEqualTo(TestUtils.getPropertyValue(withDefaultConverterHandler, "topic"));
		org.springframework.integration.zeromq.support.DefaultZeromqMessageConverter defaultConverter = TestUtils.getPropertyValue(withDefaultConverterHandler,
				"converter", org.springframework.integration.zeromq.support.DefaultZeromqMessageConverter.class);
		assertThat(defaultConverter).isNotNull();
		assertThat(clientFactory).isEqualTo(TestUtils.getPropertyValue(withDefaultConverterHandler, "clientFactory"));
	}
}

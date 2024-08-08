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

package org.springframework.integration.jmx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 *
 */
@SpringJUnitConfig
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class UpdateMappingsTests {

	@Autowired
	private MessageChannel control;

	@Autowired
	private MessageChannel in;

	@Autowired
	private PollableChannel qux;

	@Autowired
	private MBeanServer server;

	@Test
	public void test() {
		control.send(
				MessageBuilder.withPayload("myRouter.setChannelMapping")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of("baz", "qux"))
						.build());
		Message<?> message = MessageBuilder.withPayload("Hello, world!")
				.setHeader("routing.header", "baz").build();
		in.send(message);
		assertThat(qux.receive()).isNotNull();
	}

	@Test
	public void testChangeRouterMappings() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		Properties newMapping = new Properties();
		newMapping.setProperty("foo", "bar");
		newMapping.setProperty("baz", "qux");
		messagingTemplate.send(control,
				MessageBuilder.withPayload("'router.handler'.replaceChannelMappings")
						.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of(newMapping))
						.build());
		Map<?, ?> mappings =
				messagingTemplate.convertSendAndReceive(control, "@'router.handler'.getChannelMappings()", Map.class);
		assertThat(mappings).isNotNull();
		assertThat(mappings.size()).isEqualTo(2);
		assertThat(mappings.get("foo")).isEqualTo("bar");
		assertThat(mappings.get("baz")).isEqualTo("qux");

		newMapping = new Properties();
		newMapping.setProperty("foo", "qux");
		newMapping.setProperty("baz", "bar");
		messagingTemplate
				.send(control,
						MessageBuilder.withPayload("'router.handler'.replaceChannelMappings")
								.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of(newMapping))
								.build());
		mappings = messagingTemplate.convertSendAndReceive(control, "@'router.handler'.getChannelMappings()", Map.class);
		assertThat(mappings.size()).isEqualTo(2);
		assertThat(mappings.get("baz")).isEqualTo("bar");
		assertThat(mappings.get("foo")).isEqualTo("qux");
	}

	@Test
	public void testJmx() throws Exception {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		Set<ObjectName> names = this.server.queryNames(ObjectName
						.getInstance("update.mapping.domain:type=MessageHandler,name=router,bean=endpoint"),
				null);
		assertThat(names.size()).isEqualTo(1);
		Map<String, String> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("baz", "qux");
		Object[] params = new Object[] {map};
		this.server.invoke(names.iterator().next(), "setChannelMappings", params,
				new String[] {"java.util.Map"});
		Map<?, ?> mappings =
				messagingTemplate.convertSendAndReceive(control, "@'router.handler'.getChannelMappings()", Map.class);
		assertThat(mappings).isNotNull();
		assertThat(mappings.size()).isEqualTo(2);
		assertThat(mappings.get("foo")).isEqualTo("bar");
		assertThat(mappings.get("baz")).isEqualTo("qux");
	}

}

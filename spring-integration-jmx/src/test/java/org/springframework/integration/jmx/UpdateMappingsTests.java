/*
 * Copyright 2002-present the original author or authors.
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
import org.springframework.integration.test.support.TestApplicationContextAware;
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
public class UpdateMappingsTests implements TestApplicationContextAware {

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
		messagingTemplate.setBeanFactory(TEST_INTEGRATION_CONTEXT);
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
		messagingTemplate.setBeanFactory(TEST_INTEGRATION_CONTEXT);
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

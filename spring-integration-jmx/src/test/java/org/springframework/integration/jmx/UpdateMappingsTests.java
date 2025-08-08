/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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
		control.send(new GenericMessage<String>("@myRouter.setChannelMapping('baz', 'qux')"));
		Message<?> message = MessageBuilder.withPayload("Hello, world!")
				.setHeader("routing.header", "baz").build();
		in.send(message);
		assertThat(qux.receive()).isNotNull();
	}

	@Test
	public void testChangeRouterMappings() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		messagingTemplate.convertAndSend(control,
				"@'router.handler'.replaceChannelMappings('foo=bar \n baz=qux')");
		Map<?, ?> mappings = messagingTemplate.convertSendAndReceive(control, "@'router.handler'.getChannelMappings()", Map.class);
		assertThat(mappings).isNotNull();
		assertThat(mappings.size()).isEqualTo(2);
		assertThat(mappings.get("foo")).isEqualTo("bar");
		assertThat(mappings.get("baz")).isEqualTo("qux");
		messagingTemplate.convertAndSend(control,
				"@'router.handler'.replaceChannelMappings('foo=qux \n baz=bar')");
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
		Map<String, String> map = new HashMap<String, String>();
		map.put("foo", "bar");
		map.put("baz", "qux");
		Object[] params = new Object[] {map};
		this.server.invoke(names.iterator().next(), "setChannelMappings", params,
				new String[] {"java.util.Map"});
		Map<?, ?> mappings = messagingTemplate.convertSendAndReceive(control, "@'router.handler'.getChannelMappings()", Map.class);
		assertThat(mappings).isNotNull();
		assertThat(mappings.size()).isEqualTo(2);
		assertThat(mappings.get("foo")).isEqualTo("bar");
		assertThat(mappings.get("baz")).isEqualTo("qux");
	}

}

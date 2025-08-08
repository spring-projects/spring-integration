/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class HeaderEnricherMethodInvokingTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void replyChannelExplicitOverwriteTrue() {
		MessageChannel inputChannel = context.getBean("input", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test");
		assertThat(result.getHeaders().getReplyChannel()).isEqualTo(replyChannel);
		assertThat(result.getHeaders().get("foo")).isEqualTo(123);
		assertThat(result.getHeaders().get("bar")).isEqualTo("ABC");
		assertThat(result.getHeaders().get("other")).isEqualTo("zzz");
	}

	public static class TestBean {

		public String echo(String text) {
			return text.toUpperCase();
		}

		public Map<String, Object> enrich() {
			Map<String, Object> headers = new HashMap<>();
			headers.put("foo", 123);
			headers.put("bar", "ABC");
			return headers;
		}

	}

}

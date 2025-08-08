/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aop;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class MessagePublishingAnnotationUsageTests {

	@Autowired
	private TestBean testBean;

	@Autowired
	private QueueChannel channel;

	@Test
	public void headerWithExplicitName() {
		String name = this.testBean.defaultPayload("John", "Doe");
		assertThat(name).isNotNull();
		Message<?> message = this.channel.receive(1000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("John Doe");
		assertThat(message.getHeaders().get("last")).isEqualTo("Doe");
	}

	@Test
	public void headerWithImplicitName() {
		String name = this.testBean.defaultPayloadButExplicitAnnotation("John", "Doe");
		assertThat(name).isNotNull();
		Message<?> message = this.channel.receive(1000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("John Doe");
		assertThat(message.getHeaders().get("lname")).isEqualTo("Doe");
	}

	@Test
	public void payloadAsArgument() {
		String name = this.testBean.argumentAsPayload("John", "Doe");
		assertThat(name).isNotNull();
		assertThat(name).isEqualTo("John Doe");
		Message<?> message = this.channel.receive(1000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("John");
		assertThat(message.getHeaders().get("lname")).isEqualTo("Doe");
	}

	public static class TestBean {

		@Publisher(channel = "messagePublishingAnnotationUsageTestChannel")
		public String defaultPayload(String fname, @Header("last") String lname) {
			return fname + " " + lname;
		}

		@Publisher(channel = "messagePublishingAnnotationUsageTestChannel")
		@Payload
		public String defaultPayloadButExplicitAnnotation(String fname, @Header String lname) {
			return fname + " " + lname;
		}

		@Publisher(channel = "messagePublishingAnnotationUsageTestChannel")
		public String argumentAsPayload(@Payload String fname, @Header String lname) {
			return fname + " " + lname;
		}

	}

}

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
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class AnnotationConfigRegistrationTests {

	@Autowired
	private TestBean testBean;

	@Autowired
	private QueueChannel annotationConfigRegistrationTest;

	@Autowired
	private QueueChannel defaultChannel;

	@Test // INT-1200
	public void verifyInterception() {
		String name = this.testBean.setName("John", "Doe", 123);
		assertThat(name).isNotNull();
		Message<?> message = this.annotationConfigRegistrationTest.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("John DoeDoe");
		assertThat(message.getHeaders().get("x")).isEqualTo(123);
	}

	@Test
	public void defaultChannel() {
		String result = this.testBean.exclaim("hello");
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo("HELLO!!!");
		Message<?> message = this.defaultChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("HELLO!!!");
	}

	public static class TestBean {

		@Publisher(channel = "annotationConfigRegistrationTest")
		@Payload("#return + #args.lname")
		public String setName(String fname, String lname, @Header("x") int num) {
			return fname + " " + lname;
		}

		@Publisher
		public String exclaim(String s) {
			return s.toUpperCase() + "!!!";
		}

	}

}

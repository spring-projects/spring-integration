/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aop;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class MessagePublishingInterceptorUsageTests {

	@Autowired
	private TestBean testBean;

	@Autowired
	private QueueChannel channel;

	@Test
	public void demoMessagePublishingInterceptor() {
		String name = this.testBean.setName("John", "Doe");
		assertThat(name).isNotNull();
		Message<?> message = this.channel.receive(1000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("John Doe");
		assertThat(message.getHeaders().get("foo")).isEqualTo("bar");
	}

	public static class TestBean {

		public String setName(String fname, String lname) {
			return fname + " " + lname;
		}

	}

}

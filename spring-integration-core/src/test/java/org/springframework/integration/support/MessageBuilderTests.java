/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.support;

import org.junit.Test;

import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 4.3.10
 *
 */
public class MessageBuilderTests {

	@Test
	public void testReadOnlyHeaders() {
		DefaultMessageBuilderFactory factory = new DefaultMessageBuilderFactory();
		Message<?> message = factory.withPayload("bar").setHeader("foo", "baz").setHeader("qux", "fiz").build();
		assertThat(message.getHeaders().get("foo")).isEqualTo("baz");
		assertThat(message.getHeaders().get("qux")).isEqualTo("fiz");
		factory.setReadOnlyHeaders("foo");
		message = factory.fromMessage(message).build();
		assertThat(message.getHeaders().get("foo")).isNull();
		assertThat(message.getHeaders().get("qux")).isEqualTo("fiz");
		factory.addReadOnlyHeaders("qux");
		message = factory.fromMessage(message).build();
		assertThat(message.getHeaders().get("foo")).isNull();
		assertThat(message.getHeaders().get("qux")).isNull();
	}

}

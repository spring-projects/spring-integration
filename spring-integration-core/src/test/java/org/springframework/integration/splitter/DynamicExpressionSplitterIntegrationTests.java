/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.splitter;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class DynamicExpressionSplitterIntegrationTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Test
	public void simple() {
		Message<?> message = MessageBuilder.withPayload(new TestBean()).setHeader("foo", "foo").build();
		this.input.send(message);
		Message<?> one = output.receive(0);
		Message<?> two = output.receive(0);
		Message<?> three = output.receive(0);
		Message<?> four = output.receive(0);
		assertThat(one.getPayload()).isEqualTo(1);
		assertThat(one.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(two.getPayload()).isEqualTo(2);
		assertThat(two.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(three.getPayload()).isEqualTo(3);
		assertThat(three.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(four.getPayload()).isEqualTo(4);
		assertThat(four.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(output.receive(0)).isNull();
	}

	static class TestBean {

		private final List<Integer> numbers = new ArrayList<>();

		TestBean() {
			for (int i = 1; i <= 10; i++) {
				this.numbers.add(i);
			}
		}

		public List<Integer> getNumbers() {
			return this.numbers;
		}

		public String[] split(String s) {
			return s.split(",");
		}

	}

}

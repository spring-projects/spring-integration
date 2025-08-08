/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class SplitterAggregatorTests {

	private final AtomicInteger count = new AtomicInteger();

	@Test
	public void testSplitterAndAggregator() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"splitterAggregatorTests.xml", this.getClass());
		MessageChannel inputChannel = (MessageChannel) context.getBean("numbers");
		PollableChannel outputChannel = (PollableChannel) context.getBean("results");
		inputChannel.send(new GenericMessage<Numbers>(this.nextTen()));
		Message<?> result1 = outputChannel.receive(1000);
		assertThat(result1).isNotNull();
		assertThat(result1.getPayload().getClass()).isEqualTo(Integer.class);
		assertThat(result1.getPayload()).isEqualTo(55);
		inputChannel.send(new GenericMessage<Numbers>(this.nextTen()));
		Message<?> result2 = outputChannel.receive(1000);
		assertThat(result2).isNotNull();
		assertThat(result2.getPayload().getClass()).isEqualTo(Integer.class);
		assertThat(result2.getPayload()).isEqualTo(155);
		context.close();
	}

	private Numbers nextTen() {
		List<Integer> values = new ArrayList<Integer>();
		for (int i = 0; i < 10; i++) {
			values.add(this.count.incrementAndGet());
		}
		return new Numbers(values);
	}

}

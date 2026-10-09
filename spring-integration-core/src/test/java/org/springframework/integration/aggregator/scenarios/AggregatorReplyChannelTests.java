/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator.scenarios;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
public class AggregatorReplyChannelTests {

	@Autowired
	private volatile MessageChannel input;

	@Autowired
	private volatile PollableChannel output;

	private final List<String> list = new ArrayList<>();

	@BeforeEach
	public void setupList() {
		this.list.add("foo");
		this.list.add("bar");
	}

	@Test
	public void replyChannelHeader() {
		verifyReply(MessageBuilder.withPayload(list).setReplyChannel(output).build());
	}

	@Test // INT-1095
	public void replyChannelNameHeader() {
		verifyReply(MessageBuilder.withPayload(list).setReplyChannelName("output").build());
	}

	private void verifyReply(Message<?> message) {
		assertThat(this.output.receive(0)).isNull();
		this.input.send(message);
		Message<?> result = this.output.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload() instanceof List).isTrue();
		List<?> resultList = (List<?>) result.getPayload();
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.contains("foo")).isTrue();
		assertThat(resultList.contains("bar")).isTrue();
	}

}

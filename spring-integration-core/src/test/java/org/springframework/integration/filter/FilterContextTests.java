/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.filter;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class FilterContextTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Autowired
	private AbstractEndpoint pojoFilter;

	@Autowired
	private TestBean testBean;

	@Test
	public void methodInvokingFilterRejects() {
		this.input.send(new GenericMessage<>("foo"));
		Message<?> reply = this.output.receive(0);
		assertThat(reply).isNull();

		assertThat(this.testBean.isRunning()).isTrue();
		this.pojoFilter.stop();
		assertThat(this.testBean.isRunning()).isFalse();
		this.pojoFilter.start();
		assertThat(this.testBean.isRunning()).isTrue();
	}

	@Test
	public void methodInvokingFilterAccepts() {
		this.input.send(new GenericMessage<>("foobar"));
		Message<?> reply = this.output.receive(0);
		assertThat(reply.getPayload()).isEqualTo("foobar");
	}

}

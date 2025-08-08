/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel.interceptor;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 * @author Artem Bilan
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class GlobalChannelInterceptorSubElementTests {

	@Autowired
	@Qualifier("inputA")
	MessageChannel inputA;

	@Autowired
	@Qualifier("wiretap")
	PollableChannel wiretapChannel;

	@Autowired
	@Qualifier("wiretap1")
	PollableChannel wiretap1;

	@Test
	public void testWiretapSubElement() {
		this.inputA.send(new GenericMessage<String>("hello"));
		Message<?> result = this.wiretapChannel.receive(100);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("hello");
		assertThat(this.wiretapChannel.receive(1)).isNull();
		assertThat(this.wiretap1.receive(1)).isNull();
	}

}

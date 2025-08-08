/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class ErrorMessageExceptionTypeRouterParserTests {

	@Autowired
	private MessageChannel inputChannel;

	@Autowired
	private QueueChannel defaultChannel;

	@Autowired
	private QueueChannel illegalChannel;

	@Autowired
	private QueueChannel npeChannel;

	@Test
	public void validateExceptionTypeRouterConfig() {

		inputChannel.send(new ErrorMessage(new NullPointerException()));
		assertThat(npeChannel.receive(1000).getPayload()).isInstanceOf(NullPointerException.class);

		inputChannel.send(new ErrorMessage(new IllegalArgumentException()));
		assertThat(illegalChannel.receive(1000).getPayload()).isInstanceOf(IllegalArgumentException.class);

		inputChannel.send(new ErrorMessage(new RuntimeException()));
		assertThat(defaultChannel.receive(1000).getPayload()).isInstanceOf(RuntimeException.class);
	}

}

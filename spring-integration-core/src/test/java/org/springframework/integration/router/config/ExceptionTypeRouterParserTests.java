/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class ExceptionTypeRouterParserTests {

	@Autowired
	private ApplicationContext context;

	@SuppressWarnings("unchecked")
	@Test
	public void testExceptionTypeRouterConfig() {
		MessageChannel inputChannel = this.context.getBean("inChannel", MessageChannel.class);

		inputChannel.send(new GenericMessage<Throwable>(new NullPointerException()));
		QueueChannel nullPointerChannel = this.context.getBean("nullPointerChannel", QueueChannel.class);
		Message<Throwable> npeMessage = (Message<Throwable>) nullPointerChannel.receive(1000);
		assertThat(npeMessage).isNotNull();
		assertThat(npeMessage.getPayload() instanceof NullPointerException).isTrue();

		inputChannel.send(new GenericMessage<Throwable>(new IllegalArgumentException()));
		QueueChannel illegalArgumentChannel = this.context.getBean("illegalArgumentChannel", QueueChannel.class);
		Message<Throwable> iaMessage = (Message<Throwable>) illegalArgumentChannel.receive(1000);
		assertThat(iaMessage).isNotNull();
		assertThat(iaMessage.getPayload() instanceof IllegalArgumentException).isTrue();

		inputChannel.send(new GenericMessage<>("Hello"));
		QueueChannel outputChannel = this.context.getBean("outputChannel", QueueChannel.class);
		assertThat(outputChannel.receive(1000)).isNotNull();
	}

}

/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class EndpointParserTests {

	@Test
	public void testSimpleEndpoint() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleEndpointTests.xml", this.getClass());
		context.start();
		MessageChannel channel = (MessageChannel) context.getBean("endpointParserTestInput");
		TestHandler handler = (TestHandler) context.getBean("testHandler");
		assertThat(handler.getMessageString()).isNull();
		channel.send(new GenericMessage<>("test"));
		assertThat(handler.getLatch().await(10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(handler.getMessageString()).isEqualTo("test");
		context.close();
	}

}

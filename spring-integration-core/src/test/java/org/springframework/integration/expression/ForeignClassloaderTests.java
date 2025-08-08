/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.expression;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 3.0.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class ForeignClassloaderTests {

	@Autowired
	private MessageChannel foo;

	@Autowired
	private PollableChannel bar;

	/**
	 * Sends a message on a thread that has a ClassLoader that doesn't have visibility to Foo.
	 * Fails without a custom TypeLocator in the evaluation context factory bean.
	 */
	@Test
	public void testThreadHasWrongClassLoader() {
		Thread t = new Thread(() -> {
			try {
				foo.send(new GenericMessage<>("foo"));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		});
		t.setContextClassLoader(new ClassLoader() {

			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				throw new ClassNotFoundException("Foo not found");
			}
		});
		t.start();
		Message<?> reply = bar.receive(10000);
		assertThat(reply.getPayload()).isInstanceOf(Foo.class);
	}

	public static class Foo {

	}

}

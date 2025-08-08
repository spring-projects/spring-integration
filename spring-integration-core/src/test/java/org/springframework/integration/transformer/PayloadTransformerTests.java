/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import java.util.Date;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 */
public class PayloadTransformerTests {

	@Test
	public void testSuccessfulTransformation() {
		TestPayloadTransformer transformer = new TestPayloadTransformer();
		Message<?> message = new GenericMessage<String>("foo");
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo(3);
	}

	@Test(expected = MessagingException.class)
	public void testExceptionThrownByTransformer() {
		TestPayloadTransformer transformer = new TestPayloadTransformer();
		Message<?> message = new GenericMessage<String>("bad");
		transformer.transform(message);
	}

	@Test(expected = MessagingException.class)
	public void testWrongPayloadType() {
		TestPayloadTransformer transformer = new TestPayloadTransformer();
		Message<?> message = new GenericMessage<Date>(new Date());
		transformer.transform(message);
	}

	private static class TestPayloadTransformer extends AbstractPayloadTransformer<String, Integer> {

		TestPayloadTransformer() {
			super();
		}

		@Override
		public Integer transformPayload(String s) {
			if (s.equals("bad")) {
				throw new IllegalStateException("bad input!");
			}
			return s.length();
		}

	}

}

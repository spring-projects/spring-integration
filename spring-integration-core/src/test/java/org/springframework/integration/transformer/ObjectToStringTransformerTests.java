/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import java.nio.charset.Charset;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Andrew Cowlin
 * @author Gary Russell
 */
public class ObjectToStringTransformerTests {

	@Test
	public void stringPayload() {
		Transformer transformer = new ObjectToStringTransformer();
		Message<?> result = transformer.transform(new GenericMessage<String>("foo"));
		assertThat(result.getPayload()).isEqualTo("foo");
	}

	@Test
	public void objectPayload() {
		Transformer transformer = new ObjectToStringTransformer();
		Message<?> result = transformer.transform(new GenericMessage<TestBean>(new TestBean()));
		assertThat(result.getPayload()).isEqualTo("test");
	}

	@Test
	public void byteArrayPayload() throws Exception {
		Transformer transformer = new ObjectToStringTransformer();
		Message<?> result = transformer.transform(new GenericMessage<byte[]>(("foo" + '\u0fff').getBytes("UTF-8")));
		assertThat(result.getPayload()).isEqualTo("foo" + '\u0fff');
	}

	@Test
	public void byteArrayPayloadCharset() throws Exception {
		String defaultCharsetName = Charset.defaultCharset().toString();
		Transformer transformer = new ObjectToStringTransformer(defaultCharsetName);
		Message<?> result = transformer.transform(new GenericMessage<byte[]>("foo".getBytes(defaultCharsetName)));
		assertThat(result.getPayload()).isEqualTo("foo");
	}

	@Test
	public void charArrayPayload() {
		Transformer transformer = new ObjectToStringTransformer();
		Message<?> result = transformer.transform(new GenericMessage<char[]>("foo".toCharArray()));
		assertThat(result.getPayload()).isEqualTo("foo");
	}

	private static class TestBean {

		TestBean() {
			super();
		}

		@Override
		public String toString() {
			return "test";
		}

	}

}

/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.message;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 */
public class GenericMessageTests {

	@Test
	public void testMessageHeadersCopiedFromMap() {
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put("testAttribute", Integer.valueOf(123));
		headerMap.put("testProperty", "foo");
		headerMap.put(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, 42);
		headerMap.put(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, 24);
		GenericMessage<String> message = new GenericMessage<String>("test", headerMap);
		assertThat(message.getHeaders().get("testAttribute")).isEqualTo(123);
		assertThat(message.getHeaders().get("testProperty", String.class)).isEqualTo("foo");
		assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceSize()).isEqualTo(42);
		assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceNumber()).isEqualTo(24);
	}

}

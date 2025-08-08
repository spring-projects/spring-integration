/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import java.util.Collections;

import org.junit.Test;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.acks.AcknowledgmentCallback.Status;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Gary Russell
 *
 * @since 5.0.1
 *
 */
public class MessageSourcePollingTemplateTests {

	@Test
	public void testAckNack() {
		AcknowledgmentCallback callback = mock(AcknowledgmentCallback.class);
		given(callback.isAutoAck()).willReturn(true);
		MessageSource<?> source = () -> new GenericMessage<>("foo",
				Collections.singletonMap(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, callback));
		MessageSourcePollingTemplate template = new MessageSourcePollingTemplate(source);
		template.poll(h -> {
		});
		verify(callback).acknowledge(Status.ACCEPT);
		try {
			template.poll(h -> {
				throw new RuntimeException("expected");
			});
			fail("expected exception");
		}
		catch (MessageHandlingException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("expected");
		}
		verify(callback).acknowledge(Status.REJECT);
	}

	@Test
	public void testNoAutoAck() {
		AcknowledgmentCallback callback = mock(AcknowledgmentCallback.class);
		given(callback.isAutoAck()).willReturn(false);
		MessageSource<?> source = () -> new GenericMessage<>("foo",
				Collections.singletonMap(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, callback));
		MessageSourcePollingTemplate template = new MessageSourcePollingTemplate(source);
		template.poll(h -> {
		});
		verify(callback, never()).acknowledge(Status.ACCEPT);
		verify(callback, never()).acknowledge(Status.REJECT);
	}

}

/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.endpoint;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.Test;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.acks.AcknowledgmentCallback.Status;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;

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
		template.poll(h -> { });
		verify(callback).acknowledge(Status.ACCEPT);
		try {
			template.poll(h -> {
				throw new RuntimeException("expected");
			});
			fail("expected exception");
		}
		catch (MessageHandlingException e) {
			assertThat(e.getCause().getMessage(), equalTo("expected"));
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
		template.poll(h -> { });
		verify(callback, never()).acknowledge(Status.ACCEPT);
		verify(callback, never()).acknowledge(Status.REJECT);
	}

}

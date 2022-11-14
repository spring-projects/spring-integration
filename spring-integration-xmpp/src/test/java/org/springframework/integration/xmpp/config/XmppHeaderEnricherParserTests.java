/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xmpp.config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Mark Fisher
 * @author Josh Long
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class XmppHeaderEnricherParserTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private DirectChannel output;

	@Test
	public void to() throws InterruptedException {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		CountDownLatch callLatch = new CountDownLatch(1);
		MessageHandler handler = mock(MessageHandler.class);
		willAnswer(invocation -> {
			Message<?> message = invocation.getArgument(0);
			String chatToUser = (String) message.getHeaders().get(XmppHeaders.TO);
			assertThat(chatToUser).isNotNull();
			assertThat(chatToUser).isEqualTo("test1@example.org");
			callLatch.countDown();
			return null;
		})
				.given(handler)
				.handleMessage(Mockito.any(Message.class));
		this.output.subscribe(handler);
		messagingTemplate.send(this.input, MessageBuilder.withPayload("foo").build());
		assertThat(callLatch.await(10, TimeUnit.SECONDS)).isTrue();
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}

}

/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.jmx.config;

import javax.management.Notification;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.jmx.inbound.NotificationListeningMessageProducer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class NotificationListeningChannelAdapterParserTests {

	@Autowired
	private PollableChannel channel;

	@Autowired
	private PollableChannel patternChannel;

	@Autowired
	private PollableChannel multiChannel;

	@Autowired
	private TestPublisher testPublisher;

	@Autowired
	private TestPublisher testPublisher2;

	@Autowired
	private MessageChannel autoChannel;

	@Autowired @Qualifier("autoChannel.adapter")
	private NotificationListeningMessageProducer autoChannelAdapter;

	@Autowired @Qualifier("multiAdapter")
	private NotificationListeningMessageProducer multiAdapter;

	@Test
	public void receiveNotification() throws Exception {
		this.multiAdapter.start();
		assertThat(channel.receive(0)).isNull();
		testPublisher.send("ABC");
		verifyReceipt(channel, "testPublisher");
		verifyReceipt(patternChannel, "testPublisher");
		// multiChannel should see 2 copies
		verifyReceipt(multiChannel, "testPublisher");
		verifyReceipt(multiChannel, "testPublisher");

		testPublisher2.send("ABC");
		assertThat(channel.receive(0)).isNull();
		assertThat(patternChannel.receive(0)).isNull();
		// multiChannel should see only 1 copy
		verifyReceipt(multiChannel, "testPublisher2");
		assertThat(multiChannel.receive(0)).isNull();
	}

	private void verifyReceipt(PollableChannel channel, String beanName) {
		Message<?> message = channel.receive(1000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().getClass()).isEqualTo(Notification.class);
		assertThat(((Notification) message.getPayload()).getMessage()).isEqualTo("ABC");
		assertThat(((String) ((Notification) message.getPayload()).getSource()).endsWith(beanName)).isTrue();
	}

	@Test
	public void testAutoChannel() {
		assertThat(TestUtils.<Object>getPropertyValue(autoChannelAdapter, "outputChannel"))
				.isSameAs(autoChannel);
	}

}

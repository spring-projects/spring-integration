/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.dispatcher;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class UnicastingDispatcherTests {

	@Autowired
	ApplicationContext applicationContext;

	@Test
	public void withInboundGatewayAsyncRequestChannelAndExplicitErrorChannel() {
		SubscribableChannel errorChannel = this.applicationContext.getBean("errorChannel", SubscribableChannel.class);
		MessageHandler errorHandler = message -> {
			MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
			assertThat(message.getPayload() instanceof MessageDeliveryException).isTrue();
			replyChannel.send(new GenericMessage<>("reply"));
		};
		errorChannel.subscribe(errorHandler);

		RequestReplyExchanger exchanger = this.applicationContext.getBean(RequestReplyExchanger.class);
		Message<?> reply = exchanger.exchange(new GenericMessage<>("Hello"));
		assertThat(reply.getPayload()).isEqualTo("reply");
	}

}

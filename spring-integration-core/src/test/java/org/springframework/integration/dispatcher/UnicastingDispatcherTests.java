/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.dispatcher;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Oleg Zhurakousky
 *
 */
public class UnicastingDispatcherTests {

	@SuppressWarnings("unchecked")
	@Test
	public void withInboundGatewayAsyncRequestChannelAndExplicitErrorChannel() throws Exception{
		ApplicationContext context = new ClassPathXmlApplicationContext("unicasting-with-async.xml", this.getClass());
		SubscribableChannel errorChannel = context.getBean("errorChannel", SubscribableChannel.class);
		MessageHandler errorHandler = new MessageHandler() {
			
			public void handleMessage(Message<?> message) throws MessagingException {
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				assertTrue(message.getPayload() instanceof MessageDeliveryException);
				replyChannel.send(new GenericMessage<String>("reply"));
			}
		};
		errorChannel.subscribe(errorHandler);
		
		RequestReplyExchanger exchanger = context.getBean(RequestReplyExchanger.class);
		Message<String> reply = (Message<String>) exchanger.exchange(new GenericMessage<String>("Hello"));
		assertEquals("reply", reply.getPayload());
	}
	
}

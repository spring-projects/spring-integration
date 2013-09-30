/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.jpa.outbound;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * The test cases for testing out a complete flow of the JPA gateways/adapters with all
 * the components integrated.
 *
 * @author Amol Nayak
 * @since 3.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JpaOutboundGatewayIntegrationTests {

	@Autowired
	@Qualifier("in")
	private SubscribableChannel requestChannel;

	@Autowired
	@Qualifier("out")
	private SubscribableChannel responseChannel;

	/**
	 * Sends a message with the payload as a integer representing the start number in the result
	 * set and a header with value maxResults to get the max number of results
	 * @throws Exception
	 */
	@Test
	public void retrieveFromSecondRecordAndMaximumOneRecord() throws Exception {
		responseChannel.subscribe(new MessageHandler() {
			@SuppressWarnings("rawtypes")
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertEquals(1, ((List)message.getPayload()).size());
			}
		});
		Message<Integer> message = MessageBuilder
						.withPayload(1)
						.setHeader("maxResults", "1")
						.build();
		requestChannel.send(message);
	}
}

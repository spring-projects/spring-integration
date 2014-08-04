/*
 * Copyright 2002-2014 the original author or authors.
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
/**
 * @author David Liu
 * since 4.1
 */
package org.springframework.integration.aggregator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * @author David Liu
 * since 4.1
 */
public class INT3460Tests {

	@Test
	public void validateUnboundedResequencerLight(){
		ApplicationContext context = new ClassPathXmlApplicationContext("INT3460Tests-context.xml",  INT3460Tests.class);
		MessageChannel inputChannel = context.getBean("inputChannel", MessageChannel.class);
		QueueChannel outputChannel = context.getBean("outputChannel", QueueChannel.class);
		Message<?> message1 = MessageBuilder.withPayload("1").setCorrelationId("A").setSequenceNumber(1).build();
		inputChannel.send(message1);
		message1 = outputChannel.receive(0);
		assertNotNull(message1);
		assertEquals((Integer)1, new IntegrationMessageHeaderAccessor(message1).getSequenceNumber());
	}
}

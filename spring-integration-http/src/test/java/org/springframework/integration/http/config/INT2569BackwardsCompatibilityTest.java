/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.http.config;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
/**
 * This test is to handle specific incompatibility with Spring 3.1 that resulted in
 * breaking change. For more details see https://jira.springsource.org/browse/INT-2569
 * 
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class INT2569BackwardsCompatibilityTest{


	@Test
	public void testValidUrlVariable() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("INT2569BackwardsCompatibilityTest-context.xml", this.getClass());
		MessageChannel channel = context.getBean("inputChannel", MessageChannel.class);
		QueueChannel replyChannel = context.getBean("replyChannel", QueueChannel.class);
		channel.send(new GenericMessage<String>("vmw"));
		// asserting that reply came back and no exception was thrown
		assertNotNull(replyChannel.receive(5000)); 
		channel.send(new GenericMessage<String>("ibm"));
		assertNotNull(replyChannel.receive(5000));
		
		channel.send(new GenericMessage<String>("orcl"));
		assertNotNull(replyChannel.receive(5000));
		
		channel.send(new GenericMessage<String>("ctxs"));
		assertNotNull(replyChannel.receive(5000));
	}
}

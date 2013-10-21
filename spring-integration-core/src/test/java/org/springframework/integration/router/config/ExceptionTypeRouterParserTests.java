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
package org.springframework.integration.router.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 */
public class ExceptionTypeRouterParserTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testExceptionTypeRouterConfig(){
		ApplicationContext context = new ClassPathXmlApplicationContext("ExceptionTypeRouterParserTests-context.xml", this.getClass());
		MessageChannel inputChannel = context.getBean("inChannel", MessageChannel.class);

		inputChannel.send(new GenericMessage<Throwable>(new NullPointerException()));
		QueueChannel nullPointerChannel = context.getBean("nullPointerChannel", QueueChannel.class);
		Message<Throwable> npeMessage = (Message<Throwable>) nullPointerChannel.receive(1000);
		assertNotNull(npeMessage);
		assertTrue(npeMessage.getPayload() instanceof NullPointerException);

		inputChannel.send(new GenericMessage<Throwable>(new IllegalArgumentException()));
		QueueChannel illegalArgumentChannel = context.getBean("illegalArgumentChannel", QueueChannel.class);
		Message<Throwable> iaMessage = (Message<Throwable>) illegalArgumentChannel.receive(1000);
		assertNotNull(iaMessage);
		assertTrue(iaMessage.getPayload() instanceof IllegalArgumentException);

		inputChannel.send(new GenericMessage<String>("Hello"));
		QueueChannel outputChannel = context.getBean("outputChannel", QueueChannel.class);
		assertNotNull(outputChannel.receive(1000));
	}
}

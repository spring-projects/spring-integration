/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.config.annotation;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 */

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MessagingAnnotationPostProcessorAopIntegrationTests {

	@Qualifier("input")
	@Autowired
	private SubscribableChannel input;

	@Qualifier("output")
	@Autowired
	private PollableChannel output;


	@Test
	public void parseConfig() throws Exception {
		assertThat(input, notNullValue());
	}

	@Test
	public void sendMessage() throws Exception {
		input.send(MessageBuilder.withPayload(new AtomicInteger(0)).build());
		Message<?> reply = output.receive(1000);
		assertEquals(111, ((Integer) reply.getPayload()).intValue());
	}


	@Aspect
	public static class HandlerAspect {

		@Before("execution(* org.springframework.messaging.MessageHandler+.*(..)) && args(message)")
		public void addOneHundred(Message<?> message) {
			((AtomicInteger) message.getPayload()).addAndGet(100);
		}
	}


	@Aspect
	public static class ServiceAspect {

		@Before("execution(* addOne(*)) && args(n)")
		public void addTen(AtomicInteger n) {
			n.addAndGet(10);
		}
	}


	@MessageEndpoint
	public static class AnnotatedService {

		@ServiceActivator(inputChannel = "input", outputChannel="output")
		public int addOne(AtomicInteger n) {
			return n.addAndGet(1);
		}
	}

}

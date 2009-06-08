/*
 * Copyright 2002-2008 the original author or authors.
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
import static org.junit.Assert.assertThat;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Iwein Fuld
 */

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MessagingAnnotationPostProcessorAopIntegrationTests {
	@Qualifier("input")
	@Autowired
	SubscribableChannel input;

	@Test
	public void parseConfig() throws Exception {
		assertThat(input, notNullValue());
	}

	@Test
	public void sendMessage() throws Exception {
		input.send(MessageBuilder.withPayload("A test message").build());
	}

	@Aspect
	public static class HandlerAspect {

		@Before("execution(* org.springframework.integration.message.MessageHandler+.*(..)) && args(message)")
		public void printMessage(Message<?> message) {
			System.out.println(message + " hit handler wrapping aspect");
		}
	}

	@Aspect
	public static class ServiceAspect {

		@Before("execution(* printMessage(*)) && args(message)")
		public void printMessage(Message<?> message) {
			System.out.println(message + " hit service wrapping aspect");
		}
	}

	@MessageEndpoint
	public static class AnnotatedService {
		@ServiceActivator(inputChannel = "input")
		public void printMessage(Message<?> m) {
			System.out.println(m + " hit service");
		}
	}
}

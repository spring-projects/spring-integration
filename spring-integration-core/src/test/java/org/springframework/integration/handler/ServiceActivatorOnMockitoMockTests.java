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

package org.springframework.integration.handler;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.Mockito.verify;

/**
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ServiceActivatorOnMockitoMockTests {

	@Autowired
	@Qualifier("in")
	MessageChannel in;

	@Autowired
	@Qualifier("out")
	PollableChannel out;

	public static class SingleAnnotatedMethodOnClass {

		@ServiceActivator
		public String move(String s) {
			return s;
		}

	}

	@Autowired
	SingleAnnotatedMethodOnClass singleAnnotatedMethodOnClass;

	@Test
	public void shouldInvokeMockedSingleAnnotatedMethodOnClass() {
		in.send(MessageBuilder.withPayload("singleAnnotatedMethodOnClass").build());
		verify(singleAnnotatedMethodOnClass).move("singleAnnotatedMethodOnClass");
	}

	public static class SingleMethodOnClass {

		public String move(String s) {
			return s;
		}

	}

	@Autowired
	SingleMethodOnClass singleMethodOnClass;

	@Test
	public void shouldInvokeMockedSingleMethodOnClass() {
		in.send(MessageBuilder.withPayload("SingleMethodOnClass").build());
		verify(singleMethodOnClass).move("SingleMethodOnClass");
	}

	public static class SingleMethodAcceptingHeaderOnClass {

		public String move(@Header("s") String s) {
			return s;
		}

	}

	@Autowired
	SingleMethodAcceptingHeaderOnClass singleMethodAcceptingHeaderOnClass;

	@Test
	public void shouldInvokeMockedSingleMethodAcceptingHeaderOnClass() {
		in.send(MessageBuilder.withPayload("SingleMethodAcceptingHeaderOnClass")
				.setHeader("s", "SingleMethodAcceptingHeaderOnClass")
				.build());
		verify(singleMethodAcceptingHeaderOnClass).move("SingleMethodAcceptingHeaderOnClass");
	}

}

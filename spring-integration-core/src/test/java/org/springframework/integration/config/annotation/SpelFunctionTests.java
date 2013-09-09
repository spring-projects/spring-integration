/*
 * Copyright 2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.SpelFunction;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 *
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SpelFunctionTests {

	@Autowired
	private DirectChannel toUpperCaseChannel;

	@Autowired
	private DirectChannel toLowerCaseChannel;

	@Autowired
	private QueueChannel outputChannel;

	@Test
	public void testSpelFunctionAnnotation() throws InterruptedException {
		this.toUpperCaseChannel.send(new GenericMessage<String>("TEst"));
		Message<?> receive = this.outputChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("TEST", receive.getPayload());

		this.toLowerCaseChannel.send(new GenericMessage<String>("TEst"));
		receive = this.outputChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("test", receive.getPayload());
	}

	public static class Foo {

		@SpelFunction
		public static String toUpperCase(String s) {
			return s.toUpperCase();
		}

		@SpelFunction("lower")
		public static String toLoweCase(String s) {
			return s.toLowerCase();
		}
	}

}

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

package org.springframework.integration.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SpelTransformerIntegrationTests {

	@Autowired
	private MessageChannel simpleInput;

	@Autowired
	private MessageChannel beanResolvingInput;

	@Autowired @Qualifier("output")
	private PollableChannel output;

	@Autowired
	private MessageChannel transformerChainInput;


	@Test
	public void simple() {
		Message<?> message = MessageBuilder.withPayload(new TestBean()).setHeader("bar", 123).build();
		this.simpleInput.send(message);
		Message<?> result = output.receive(0);
		assertEquals("test123", result.getPayload());
	}

	@Test
	public void beanResolving() {
		Message<?> message = MessageBuilder.withPayload("foo").build();
		this.beanResolvingInput.send(message);
		Message<?> result = output.receive(0);
		assertEquals("testFOO", result.getPayload());
	}

	@Test
	public void testInt2755ChainChildIdWithinExceptionMessage() {
		try {
			this.transformerChainInput.send(new GenericMessage<String>("foo"));
		}
		catch (ReplyRequiredException e) {
			assertThat(e.getMessage(), Matchers.containsString("No reply produced by handler 'transformerChain$child#0'"));
		}
	}


	static class TestBean {

		public String getFoo() {
			return "test";
		}
	}

}

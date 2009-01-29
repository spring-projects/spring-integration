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

package org.springframework.integration.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ReplyMessageHolder;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ChainParserTests  {

	@Autowired
	@Qualifier("filterInput")
	private MessageChannel filterInput;

	@Autowired
	@Qualifier("pollableInput")
	private MessageChannel pollableInput;

	@Autowired
	@Qualifier("headerEnricherInput")
	private MessageChannel headerEnricherInput;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Autowired
	@Qualifier("replyOutput")
	private PollableChannel replyOutput;

	
	@Autowired
	@Qualifier("beanInput")
	private MessageChannel beanInput;

	public static Message successMessage = MessageBuilder.withPayload("success").build();


	@Test
	public void chainWithAcceptingFilter() {	
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.filterInput.send(message);
		Message<?> reply = this.output.receive(0);
		assertNotNull(reply);
		assertEquals("foo", reply.getPayload());
	}

	@Test
	public void chainWithRejectingFilter() {
		Message<?> message = MessageBuilder.withPayload(123).build();
		this.filterInput.send(message);
		Message<?> reply = this.output.receive(0);
		assertNull(reply);
	}

	@Test
	public void chainWithHeaderEnricher() {
		Message<?> message = MessageBuilder.withPayload(123).build();
		this.headerEnricherInput.send(message);
		Message<?> reply = this.replyOutput.receive(0);
		assertNotNull(reply);
		assertEquals("foo", reply.getPayload());
		assertEquals("ABC", reply.getHeaders().getCorrelationId());
		assertEquals("XYZ", reply.getHeaders().get("testValue"));
		assertEquals(123, reply.getHeaders().get("testRef"));
	}

	@Test
	public void chainWithPollableInput() {	
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.pollableInput.send(message);
		Message<?> reply = this.output.receive(3000);
		assertNotNull(reply);
		assertEquals("foo", reply.getPayload());
	}
	
	@Test
	public void chainHandlerBean() throws Exception {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.beanInput.send(message);
		Message<?> reply = this.output.receive(3000);
		assertNotNull(reply);
		assertThat(reply, is(successMessage));
	}
	
	public static class StubHandler extends AbstractReplyProducingMessageHandler {
		@Override
		protected void handleRequestMessage(Message<?> requestMessage, ReplyMessageHolder replyMessageHolder) {
			replyMessageHolder.add(successMessage);
		}
		
	}

}

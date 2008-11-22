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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
public class ChainParserTests extends AbstractJUnit4SpringContextTests {

	@Autowired
	@Qualifier("filterInput")
	private MessageChannel filterInput;

	@Autowired
	@Qualifier("headerEnricherInput")
	private MessageChannel headerEnricherInput;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Autowired
	@Qualifier("replyOutput")
	private PollableChannel replyOutput;


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

}

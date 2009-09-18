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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.integration.test.util.TestUtils.getPropertyValue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.CorrelationStrategyAdapter;
import org.springframework.integration.aggregator.Resequencer;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class ResequencerParserTests {

	private ApplicationContext context;


	@Before
	public void setUp() {
		this.context = new ClassPathXmlApplicationContext("resequencerParserTests.xml", this.getClass());
	}


	@Test
	public void testResequencing() {
		MessageChannel inputChannel = (MessageChannel) context.getBean("inputChannel");
		PollableChannel outputChannel = (PollableChannel) context.getBean("outputChannel");
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		outboundMessages.add(createMessage("123", "id1", 3, 3, outputChannel));
		outboundMessages.add(createMessage("789", "id1", 3, 1, outputChannel));
		outboundMessages.add(createMessage("456", "id1", 3, 2, outputChannel));
		for (Message<?> message : outboundMessages) {
			inputChannel.send(message);
		}
		Message<?> message1 = outputChannel.receive(500);
		Message<?> message2 = outputChannel.receive(500);
		Message<?> message3 = outputChannel.receive(500);
		assertNotNull(message1);
		assertEquals(new Integer(1), message1.getHeaders().getSequenceNumber());
		assertNotNull(message2);
		assertEquals(new Integer(2), message2.getHeaders().getSequenceNumber());
		assertNotNull(message3);
		assertEquals(new Integer(3), message3.getHeaders().getSequenceNumber());
	}

	@Test
	public void testDefaultResequencerProperties() {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("defaultResequencer");
		Resequencer resequencer = (Resequencer) new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertNull(getPropertyValue(resequencer, "outputChannel"));
		assertNull(getPropertyValue(resequencer, "discardChannel"));
		assertEquals("The ResequencerEndpoint is not set with the appropriate timeout value",
				1000l, getPropertyValue(resequencer, "channelTemplate.sendTimeout"));
		assertEquals("The ResequencerEndpoint is not configured with the appropriate 'send partial results on timeout' flag",
				false, getPropertyValue(resequencer, "sendPartialResultOnTimeout"));
		assertEquals("The ResequencerEndpoint is not configured with the appropriate reaper interval",
				1000l, getPropertyValue(resequencer, "reaperInterval"));
		assertEquals("The ResequencerEndpoint is not configured with the appropriate tracked correlationId capacity",
				1000, getPropertyValue(resequencer, "trackedCorrelationIdCapacity"));
		assertEquals("The ResequencerEndpoint is not configured with the appropriate timeout",
				60000l, getPropertyValue(resequencer, "timeout"));
		assertEquals("The ResequencerEndpoint is not configured with the appropriate 'release partial sequences' flag",
				true, getPropertyValue(resequencer, "releasePartialSequences"));		
	}

	@Test
	public void testPropertyAssignment() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("completelyDefinedResequencer");
		MessageChannel outputChannel = (MessageChannel) context.getBean("outputChannel");
		MessageChannel discardChannel = (MessageChannel) context.getBean("discardChannel");
		Resequencer resequencer = (Resequencer) new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals("The ResequencerEndpoint is not injected with the appropriate output channel",
				outputChannel, getPropertyValue(resequencer, "outputChannel"));
		assertEquals("The ResequencerEndpoint is not injected with the appropriate discard channel",
				discardChannel, getPropertyValue(resequencer, "discardChannel"));
		assertEquals("The ResequencerEndpoint is not set with the appropriate timeout value",
				86420000l, getPropertyValue(resequencer, "channelTemplate.sendTimeout"));
		assertEquals("The ResequencerEndpoint is not configured with the appropriate 'send partial results on timeout' flag",
				true, getPropertyValue(resequencer, "sendPartialResultOnTimeout"));
		assertEquals("The ResequencerEndpoint is not configured with the appropriate reaper interval",
				135l, getPropertyValue(resequencer, "reaperInterval"));
		assertEquals("The ResequencerEndpoint is not configured with the appropriate tracked correlationId capacity",
				99, getPropertyValue(resequencer, "trackedCorrelationIdCapacity"));
		assertEquals("The ResequencerEndpoint is not configured with the appropriate timeout",
				42l, getPropertyValue(resequencer, "timeout"));
		assertEquals("The ResequencerEndpoint is not configured with the appropriate 'release partial sequences' flag",
				false, getPropertyValue(resequencer, "releasePartialSequences"));
	}

	@Test
	public void testCorrelationStrategyRefOnly() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("resequencerWithCorrelationStrategyRefOnly");
		Resequencer resequencer = (Resequencer) new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals("The ResequencerEndpoint is not configured with the appropriate CorrelationStrategy",
				context.getBean("testCorrelationStrategy"), getPropertyValue(resequencer, "correlationStrategy"));
	}

	@Test
	public void testCorrelationStrategyRefAndMethod() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("resequencerWithCorrelationStrategyRefAndMethod");
		Resequencer resequencer = (Resequencer) new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		Object correlationStrategy = getPropertyValue(resequencer, "correlationStrategy");
		assertEquals("The ResequencerEndpoint is not configured with a CorrelationStrategy adapter",
				CorrelationStrategyAdapter.class, correlationStrategy.getClass());
		CorrelationStrategyAdapter adapter = (CorrelationStrategyAdapter) correlationStrategy;
		assertEquals("foo", adapter.getCorrelationKey(MessageBuilder.withPayload("not important").build()));
	}


	private static <T> Message<T> createMessage(T payload, Object correlationId,
			int sequenceSize, int sequenceNumber, MessageChannel outputChannel) {
		return MessageBuilder.withPayload(payload)
				.setCorrelationId(correlationId)
				.setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber)
				.setReplyChannel(outputChannel)
				.build();
	}


	static class TestCorrelationStrategy implements CorrelationStrategy {

		public Object getCorrelationKey(Message<?> message) {
			return "test";
		}
	}


	static class TestCorrelationStrategyPojo {

		public Object foo(Object o) {
			return "foo";
		}
	}

}

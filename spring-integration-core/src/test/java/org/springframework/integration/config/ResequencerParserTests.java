/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.config;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.aggregator.*;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;

import java.util.Comparator;

import static org.junit.Assert.*;
import static org.springframework.integration.test.util.TestUtils.getPropertyValue;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Dave Syer
 */
public class ResequencerParserTests {

	private ApplicationContext context;

	@Before
	public void setUp() {
		this.context = new ClassPathXmlApplicationContext("resequencerParserTests.xml", this.getClass());
	}

	@Test
	public void testDefaultResequencerProperties() {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("defaultResequencer");
		ResequensingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
				ResequensingMessageHandler.class);
		assertNull(getPropertyValue(resequencer, "outputChannel"));
		assertTrue(getPropertyValue(resequencer, "discardChannel") instanceof NullChannel);
		assertEquals("The ResequencerEndpoint is not set with the appropriate timeout value", 1000l, getPropertyValue(
				resequencer, "messagingTemplate.sendTimeout"));
		assertEquals(
				"The ResequencerEndpoint is not configured with the appropriate 'send partial results on timeout' flag",
				false, getPropertyValue(resequencer, "sendPartialResultOnExpiry"));
		assertEquals("The ResequencerEndpoint is not configured with the appropriate 'release partial sequences' flag",
				false, getPropertyValue(getPropertyValue(resequencer, "releaseStrategy"), "releasePartialSequences"));
	}

	@Test
	public void testPropertyAssignment() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("completelyDefinedResequencer");
		MessageChannel outputChannel = (MessageChannel) context.getBean("outputChannel");
		MessageChannel discardChannel = (MessageChannel) context.getBean("discardChannel");
		ResequensingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
				ResequensingMessageHandler.class);
		assertEquals("The ResequencerEndpoint is not injected with the appropriate output channel", outputChannel,
				getPropertyValue(resequencer, "outputChannel"));
		assertEquals("The ResequencerEndpoint is not injected with the appropriate discard channel", discardChannel,
				getPropertyValue(resequencer, "discardChannel"));
		assertEquals("The ResequencerEndpoint is not set with the appropriate timeout value", 86420000l,
				getPropertyValue(resequencer, "messagingTemplate.sendTimeout"));
		assertEquals(
				"The ResequencerEndpoint is not configured with the appropriate 'send partial results on timeout' flag",
				true, getPropertyValue(resequencer, "sendPartialResultOnExpiry"));
		assertEquals("The ResequencerEndpoint is not configured with the appropriate 'release partial sequences' flag",
				true, getPropertyValue(getPropertyValue(resequencer, "releaseStrategy"), "releasePartialSequences"));
	}

	@Test
	public void testCorrelationStrategyRefOnly() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context
				.getBean("resequencerWithCorrelationStrategyRefOnly");
		ResequensingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
				ResequensingMessageHandler.class);
		assertEquals("The ResequencerEndpoint is not configured with the appropriate CorrelationStrategy", context
				.getBean("testCorrelationStrategy"), getPropertyValue(resequencer, "correlationStrategy"));
	}

	@Test
	public void shouldSetReleasePartialSequencesFlag(){
				EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("completelyDefinedResequencer");
				ResequensingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
						ResequensingMessageHandler.class);
		assertEquals("The ResequencerEndpoint is not configured with the appropriate 'release partial sequences' flag",
				true, getPropertyValue(getPropertyValue(resequencer, "releaseStrategy"), "releasePartialSequences"));
	}

	@Test
	public void testCorrelationStrategyRefAndMethod() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context
				.getBean("resequencerWithCorrelationStrategyRefAndMethod");
		ResequensingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
				ResequensingMessageHandler.class);
		Object correlationStrategy = getPropertyValue(resequencer, "correlationStrategy");
		assertEquals("The ResequencerEndpoint is not configured with a CorrelationStrategy adapter",
				MethodInvokingCorrelationStrategy.class, correlationStrategy.getClass());
		MethodInvokingCorrelationStrategy adapter = (MethodInvokingCorrelationStrategy) correlationStrategy;
		assertEquals("foo", adapter.getCorrelationKey(MessageBuilder.withPayload("not important").build()));
	}

	@Test
	public void testComparator() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("resequencerWithComparator");
		ResequensingMessageHandler handler = TestUtils.getPropertyValue(endpoint, "handler",
				ResequensingMessageHandler.class);
		ResequencingMessageGroupProcessor resequencer = TestUtils.getPropertyValue(handler, "outputProcessor",
				ResequencingMessageGroupProcessor.class);
		Object comparator = getPropertyValue(resequencer, "comparator");
		assertEquals("The Resequencer is not configured with a TestComparator", TestComparator.class, comparator
				.getClass());
	}

	@Test
	public void testReleaseStrategy() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("resequencerWithReleaseStrategy");
		ResequensingMessageHandler handler = TestUtils.getPropertyValue(endpoint, "handler",
				ResequensingMessageHandler.class);
		Object releaseStrategy = getPropertyValue(handler, "releaseStrategy");
		assertEquals("The Resequencer is not configured with an adapter", MethodInvokingReleaseStrategy.class, releaseStrategy
				.getClass());
	}

	@SuppressWarnings("unused")
	private static <T> Message<T> createMessage(T payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel outputChannel) {
		return MessageBuilder.withPayload(payload).setCorrelationId(correlationId).setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber).setReplyChannel(outputChannel).build();
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

	static class TestComparator implements Comparator<Message<?>> {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public int compare(Message<?> o1, Message<?> o2) {
			return ((Comparable) o1.getPayload()).compareTo(o2.getPayload());
		}
	}

}

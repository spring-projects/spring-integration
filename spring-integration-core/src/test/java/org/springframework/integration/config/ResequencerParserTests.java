/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.integration.test.util.TestUtils.getPropertyValue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy;
import org.springframework.integration.aggregator.MethodInvokingReleaseStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.aggregator.ResequencingMessageHandler;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Stefan Ferstl
 * @author Artem Bilan
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
		ResequencingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
				ResequencingMessageHandler.class);
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
		ResequencingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
				ResequencingMessageHandler.class);
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
		assertEquals(60000L, getPropertyValue(resequencer, "minimumTimeoutForEmptyGroups", Long.class).longValue());
	}

	@Test
	public void testCorrelationStrategyRefOnly() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context
				.getBean("resequencerWithCorrelationStrategyRefOnly");
		ResequencingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
				ResequencingMessageHandler.class);
		assertEquals("The ResequencerEndpoint is not configured with the appropriate CorrelationStrategy", context
				.getBean("testCorrelationStrategy"), getPropertyValue(resequencer, "correlationStrategy"));
	}

	@Test
	public void testReleaseStrategyRefOnly() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("resequencerWithReleaseStrategyRefOnly");
		ResequencingMessageHandler resequencer = getPropertyValue(endpoint, "handler", ResequencingMessageHandler.class);
		assertEquals("The ResequencerEndpoint is not configured with the appropriate ReleaseStrategy",
				context.getBean("testReleaseStrategy"), getPropertyValue(resequencer, "releaseStrategy"));
	}

	@Test
	public void testReleaseStrategyRefAndMethod() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context
				.getBean("resequencerWithReleaseStrategyRefAndMethod");
		ResequencingMessageHandler resequencer = getPropertyValue(endpoint, "handler", ResequencingMessageHandler.class);

		Object releaseStrategyBean = context.getBean("testReleaseStrategyPojo");
		assertTrue("Release strategy is not of the expected type",
				releaseStrategyBean instanceof TestReleaseStrategyPojo);
		TestReleaseStrategyPojo expectedReleaseStrategy = (TestReleaseStrategyPojo) releaseStrategyBean;

		int currentInvocationCount = expectedReleaseStrategy.invocationCount;
		ReleaseStrategy effectiveReleaseStrategy = (ReleaseStrategy) getPropertyValue(resequencer, "releaseStrategy");
		assertTrue("The release strategy is expected to be a MethodInvokingReleaseStrategy",
				effectiveReleaseStrategy instanceof MethodInvokingReleaseStrategy);
		effectiveReleaseStrategy.canRelease(new SimpleMessageGroup("test"));
		assertEquals("The ResequencerEndpoint was not invoked the expected number of times;",
				currentInvocationCount + 1, expectedReleaseStrategy.invocationCount);
	}

	@Test
	public void shouldSetReleasePartialSequencesFlag(){
				EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("completelyDefinedResequencer");
				ResequencingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
						ResequencingMessageHandler.class);
		assertEquals("The ResequencerEndpoint is not configured with the appropriate 'release partial sequences' flag",
				true, getPropertyValue(getPropertyValue(resequencer, "releaseStrategy"), "releasePartialSequences"));
	}

	@Test
	public void testCorrelationStrategyRefAndMethod() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context
				.getBean("resequencerWithCorrelationStrategyRefAndMethod");
		ResequencingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
				ResequencingMessageHandler.class);
		Object correlationStrategy = getPropertyValue(resequencer, "correlationStrategy");
		assertEquals("The ResequencerEndpoint is not configured with a CorrelationStrategy adapter",
				MethodInvokingCorrelationStrategy.class, correlationStrategy.getClass());
		MethodInvokingCorrelationStrategy adapter = (MethodInvokingCorrelationStrategy) correlationStrategy;
		assertEquals("foo", adapter.getCorrelationKey(MessageBuilder.withPayload("not important").build()));
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

	static class TestReleaseStrategy implements ReleaseStrategy {
		public boolean canRelease(MessageGroup group) {
			return true;
		}
	}

	static class TestReleaseStrategyPojo {
		private int invocationCount = 0;

		public boolean bar(List<Message<?>> messages) {
			invocationCount++;
			return true;
		}
	}

}

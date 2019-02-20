/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.integration.test.util.TestUtils.getPropertyValue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Stefan Ferstl
 * @author Artem Bilan
 * @author Gary Russell
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
		assertThat(getPropertyValue(resequencer, "outputChannel")).isNull();
		assertThat(getPropertyValue(resequencer, "discardChannel") instanceof NullChannel).isTrue();
		assertThat(getPropertyValue(
				resequencer, "messagingTemplate.sendTimeout"))
				.as("The ResequencerEndpoint is not set with the appropriate timeout value").isEqualTo(-1L);
		assertThat(getPropertyValue(resequencer, "sendPartialResultOnExpiry"))
				.as("The ResequencerEndpoint is not configured with the appropriate 'send partial results on timeout' " +
						"flag")
				.isEqualTo(false);
		assertThat(getPropertyValue(resequencer, "releasePartialSequences"))
				.as("The ResequencerEndpoint is not configured with the appropriate 'release partial sequences' flag")
				.isEqualTo(false);
	}

	@Test
	public void testPropertyAssignment() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("completelyDefinedResequencer");
		MessageChannel outputChannel = (MessageChannel) context.getBean("outputChannel");
		MessageChannel discardChannel = (MessageChannel) context.getBean("discardChannel");
		ResequencingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
				ResequencingMessageHandler.class);
		assertThat(getPropertyValue(resequencer, "outputChannel"))
				.as("The ResequencerEndpoint is not injected with the appropriate output channel")
				.isEqualTo(outputChannel);
		assertThat(getPropertyValue(resequencer, "discardChannel"))
				.as("The ResequencerEndpoint is not injected with the appropriate discard channel")
				.isEqualTo(discardChannel);
		assertThat(getPropertyValue(resequencer, "messagingTemplate.sendTimeout"))
				.as("The ResequencerEndpoint is not set with the appropriate timeout value").isEqualTo(86420000L);
		assertThat(getPropertyValue(resequencer, "sendPartialResultOnExpiry"))
				.as("The ResequencerEndpoint is not configured with the appropriate 'send partial results on timeout' " +
						"flag")
				.isEqualTo(true);
		assertThat(getPropertyValue(getPropertyValue(resequencer, "releaseStrategy"), "releasePartialSequences"))
				.as("The ResequencerEndpoint is not configured with the appropriate 'release partial sequences' flag")
				.isEqualTo(true);
		assertThat(getPropertyValue(resequencer, "minimumTimeoutForEmptyGroups", Long.class).longValue())
				.isEqualTo(60000L);
	}

	@Test
	public void testCorrelationStrategyRefOnly() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context
				.getBean("resequencerWithCorrelationStrategyRefOnly");
		ResequencingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
				ResequencingMessageHandler.class);
		assertThat(getPropertyValue(resequencer, "correlationStrategy"))
				.as("The ResequencerEndpoint is not configured with the appropriate CorrelationStrategy")
				.isEqualTo(context
						.getBean("testCorrelationStrategy"));
	}

	@Test
	public void testReleaseStrategyRefOnly() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("resequencerWithReleaseStrategyRefOnly");
		ResequencingMessageHandler resequencer = getPropertyValue(endpoint, "handler",
				ResequencingMessageHandler.class);
		assertThat(getPropertyValue(resequencer, "releaseStrategy"))
				.as("The ResequencerEndpoint is not configured with the appropriate ReleaseStrategy")
				.isEqualTo(context.getBean("testReleaseStrategy"));
		assertThat(TestUtils.getPropertyValue(resequencer, "expireGroupsUponTimeout", Boolean.class)).isFalse();
	}

	@Test
	public void testReleaseStrategyRefAndMethod() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context
				.getBean("resequencerWithReleaseStrategyRefAndMethod");
		ResequencingMessageHandler resequencer = getPropertyValue(endpoint, "handler",
				ResequencingMessageHandler.class);

		Object releaseStrategyBean = context.getBean("testReleaseStrategyPojo");
		assertThat(releaseStrategyBean instanceof TestReleaseStrategyPojo)
				.as("Release strategy is not of the expected type").isTrue();
		TestReleaseStrategyPojo expectedReleaseStrategy = (TestReleaseStrategyPojo) releaseStrategyBean;

		int currentInvocationCount = expectedReleaseStrategy.invocationCount;
		ReleaseStrategy effectiveReleaseStrategy = (ReleaseStrategy) getPropertyValue(resequencer, "releaseStrategy");
		assertThat(effectiveReleaseStrategy instanceof MethodInvokingReleaseStrategy)
				.as("The release strategy is expected to be a MethodInvokingReleaseStrategy").isTrue();
		effectiveReleaseStrategy.canRelease(new SimpleMessageGroup("test"));
		assertThat(expectedReleaseStrategy.invocationCount)
				.as("The ResequencerEndpoint was not invoked the expected number of times;")
				.isEqualTo(currentInvocationCount + 1);
		assertThat(TestUtils.getPropertyValue(resequencer, "expireGroupsUponTimeout", Boolean.class)).isTrue();
	}

	@Test
	public void shouldSetReleasePartialSequencesFlag() {
				EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("completelyDefinedResequencer");
				ResequencingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
						ResequencingMessageHandler.class);
		assertThat(getPropertyValue(getPropertyValue(resequencer, "releaseStrategy"), "releasePartialSequences"))
				.as("The ResequencerEndpoint is not configured with the appropriate 'release partial sequences' flag")
				.isEqualTo(true);
	}

	@Test
	public void testCorrelationStrategyRefAndMethod() throws Exception {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context
				.getBean("resequencerWithCorrelationStrategyRefAndMethod");
		ResequencingMessageHandler resequencer = TestUtils.getPropertyValue(endpoint, "handler",
				ResequencingMessageHandler.class);
		Object correlationStrategy = getPropertyValue(resequencer, "correlationStrategy");
		assertThat(correlationStrategy.getClass())
				.as("The ResequencerEndpoint is not configured with a CorrelationStrategy adapter")
				.isEqualTo(MethodInvokingCorrelationStrategy.class);
		MethodInvokingCorrelationStrategy adapter = (MethodInvokingCorrelationStrategy) correlationStrategy;
		assertThat(adapter.getCorrelationKey(MessageBuilder.withPayload("not important").build())).isEqualTo("foo");
	}

	@SuppressWarnings("unused")
	private static <T> Message<T> createMessage(T payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel outputChannel) {
		return MessageBuilder.withPayload(payload).setCorrelationId(correlationId).setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber).setReplyChannel(outputChannel).build();
	}

	static class TestCorrelationStrategy implements CorrelationStrategy {

		@Override
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
		@Override
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

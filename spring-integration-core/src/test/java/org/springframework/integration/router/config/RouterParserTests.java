/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.router.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Jonas Partner
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
public class RouterParserTests {

	@Autowired
	private PollableChannel output1;

	@Autowired
	private PollableChannel output2;

	@Autowired
	private MessageChannel input;

	@Autowired
	private MessageChannel inputForRouterWithDefaultOutput;

	@Autowired
	private PollableChannel defaultOutput;

	@Autowired
	private MessageChannel inputForAbstractMessageRouterImplementation;

	@Autowired
	private PollableChannel output3;

	@Autowired
	private MessageChannel inputForAnnotatedRouter;

	@Autowired
	private PollableChannel output4;

	@Autowired
	private MessageChannel inputForRouterRequiringResolution;

	@Autowired
	private MessageChannel resolutionRequiredIsFalseInput;

	@Autowired
	@Qualifier("routerWithTimeout.handler")
	private MessageHandler routerWithTimeout;

	@Autowired
	private MessageChannel sequenceRouter;

	@Autowired
	private PollableChannel sequenceOut1;

	@Autowired
	private PollableChannel sequenceOut2;

	@Autowired
	private PollableChannel sequenceOut3;

	@Autowired
	private MessageChannel routerNestedBeanChannel;

	@Autowired
	private MessageChannel chainRouterNestedBeanChannel;

	@Autowired
	private MessageChannel routerAndErrorChannelInputChannel;

	@Autowired
	private SubscribableChannel errorChannel;

	@Test
	public void testRouter() {
		this.input.send(new GenericMessage<>("1"));
		Message<?> result1 = this.output1.receive(1000);
		assertThat(result1.getPayload()).isEqualTo("1");
		assertThat(output2.receive(0)).isNull();
		input.send(new GenericMessage<>("2"));
		Message<?> result2 = this.output2.receive(1000);
		assertThat(result2.getPayload()).isEqualTo("2");
		assertThat(output1.receive(0)).isNull();
	}

	@Test
	public void testRouterWithDefaultOutputChannel() {
		this.inputForRouterWithDefaultOutput.send(new GenericMessage<>("99"));
		assertThat(this.output1.receive(0)).isNull();
		assertThat(this.output2.receive(0)).isNull();
		Message<?> result = this.defaultOutput.receive(0);
		assertThat(result.getPayload()).isEqualTo("99");
	}

	@Test
	public void refOnlyForAbstractMessageRouterImplementation() {
		this.inputForAbstractMessageRouterImplementation.send(new GenericMessage<>("test-implementation"));
		Message<?> result = this.output3.receive(1000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test-implementation");
	}

	@Test
	public void refOnlyForAnnotatedObject() {
		this.inputForAnnotatedRouter.send(new GenericMessage<>("test-annotation"));
		Message<?> result = this.output4.receive(1000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test-annotation");
	}

	@Test
	public void testResolutionRequired() {
		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> this.inputForRouterRequiringResolution.send(new GenericMessage<>(3)))
				.withCauseInstanceOf(DestinationResolutionException.class);
	}

	@Test
	public void testResolutionRequiredIsFalse() {
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() ->
						this.resolutionRequiredIsFalseInput.send(new GenericMessage<>("channelThatDoesNotExist")));
	}

	@Test
	public void timeoutValueConfigured() {
		assertThat(this.routerWithTimeout instanceof MethodInvokingRouter).isTrue();
		MessagingTemplate template =
				TestUtils.getPropertyValue(this.routerWithTimeout, "messagingTemplate", MessagingTemplate.class);
		Long timeout = TestUtils.getPropertyValue(template, "sendTimeout", Long.class);
		assertThat(timeout).isEqualTo(1234L);
	}

	@Test
	public void sequence() {
		Message<?> originalMessage = new GenericMessage<>("test");
		this.sequenceRouter.send(originalMessage);
		Message<?> message1 = this.sequenceOut1.receive(1000);
		Message<?> message2 = this.sequenceOut2.receive(1000);
		Message<?> message3 = this.sequenceOut3.receive(1000);
		assertThat(new IntegrationMessageHeaderAccessor(message1).getCorrelationId())
				.isEqualTo(originalMessage.getHeaders().getId());
		assertThat(new IntegrationMessageHeaderAccessor(message2).getCorrelationId())
				.isEqualTo(originalMessage.getHeaders().getId());
		assertThat(new IntegrationMessageHeaderAccessor(message3).getCorrelationId())
				.isEqualTo(originalMessage.getHeaders().getId());
		assertThat(new IntegrationMessageHeaderAccessor(message1).getSequenceNumber()).isEqualTo(1);
		assertThat(new IntegrationMessageHeaderAccessor(message1).getSequenceSize()).isEqualTo(3);
		assertThat(new IntegrationMessageHeaderAccessor(message2).getSequenceNumber()).isEqualTo(2);
		assertThat(new IntegrationMessageHeaderAccessor(message2).getSequenceSize()).isEqualTo(3);
		assertThat(new IntegrationMessageHeaderAccessor(message3).getSequenceNumber()).isEqualTo(3);
		assertThat(new IntegrationMessageHeaderAccessor(message3).getSequenceSize()).isEqualTo(3);
	}

	@Test
	public void testInt2893RouterNestedBean() {
		this.routerNestedBeanChannel.send(new GenericMessage<>("1"));
		Message<?> result1 = this.output1.receive(1000);
		assertThat(result1.getPayload()).isEqualTo("1");
		assertThat(this.output2.receive(0)).isNull();
		this.routerNestedBeanChannel.send(new GenericMessage<>("2"));
		Message<?> result2 = this.output2.receive(1000);
		assertThat(result2.getPayload()).isEqualTo("2");
		assertThat(this.output1.receive(0)).isNull();
	}

	@Test
	public void testInt2893RouterNestedBeanWithinChain() {
		this.chainRouterNestedBeanChannel.send(new GenericMessage<>("1"));
		Message<?> result1 = this.output1.receive(1000);
		assertThat(result1.getPayload()).isEqualTo("1");
		assertThat(this.output2.receive(0)).isNull();
		this.chainRouterNestedBeanChannel.send(new GenericMessage<>("2"));
		Message<?> result2 = this.output2.receive(1000);
		assertThat(result2.getPayload()).isEqualTo("2");
		assertThat(this.output1.receive(0)).isNull();
	}

	@Test
	public void testErrorChannel() {
		MessageHandler handler = mock(MessageHandler.class);
		this.errorChannel.subscribe(handler);
		this.routerAndErrorChannelInputChannel.send(new GenericMessage<>("fail"));
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}

	@Test // should not fail
	public void routerFactoryBeanTest() {
		new ClassPathXmlApplicationContext("rfb-fix-config.xml", this.getClass()).close();
	}


	public static class NonExistingChannelRouter {

		public String route(String payload) {
			return "foo";
		}

	}

	public static class TestRouterImplementation extends AbstractMappingMessageRouter {

		private final MessageChannel channel;

		public TestRouterImplementation(MessageChannel channel) {
			this.channel = channel;
		}


		@Override
		protected List<Object> getChannelKeys(Message<?> message) {
			return Collections.singletonList((Object) this.channel);
		}

	}


	public static class AnnotatedTestRouterBean {

		private final MessageChannel channel;

		public AnnotatedTestRouterBean(MessageChannel channel) {
			this.channel = channel;
		}

		@Router
		public MessageChannel test(String payload) {
			return this.channel;
		}

	}

	public static class ReturnStringPassedInAsChannelNameRouter {

		@Router
		public String route(Message<?> message) {
			return (String) message.getPayload();
		}


	}


	static class TestChannelResolver implements DestinationResolver<MessageChannel> {

		@Override
		public MessageChannel resolveDestination(String channelName) {
			return null;
		}

	}

}

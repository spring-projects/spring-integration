/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.ServiceActivatorFactoryBean;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class DelegatingConsumerParserTests {

	@Autowired
	@Qualifier("directFilter.handler")
	private MessageHandler directFilter;

	@Autowired
	@Qualifier("refFilter.handler")
	private MessageHandler refFilter;

	@Autowired
	@Qualifier("filterWithMessageSelectorThatsAlsoAnARPMH.handler")
	private MessageHandler filterWithMessageSelectorThatsAlsoAnARPMH;

	@Autowired
	@Qualifier("directRouter.handler")
	private MessageHandler directRouter;

	@Autowired
	@Qualifier("refRouter.handler")
	private MessageHandler refRouter;

	@Autowired
	@Qualifier("directRouterMH.handler")
	private MessageHandler directRouterMH;

	@Autowired
	@Qualifier("refRouterMH.handler")
	private MessageHandler refRouterMH;

	@Autowired
	@Qualifier("directRouterARPMH.handler")
	private MessageHandler directRouterARPMH;

	@Autowired
	@Qualifier("refRouterARPMH.handler")
	private MessageHandler refRouterARPMH;

	@Autowired
	@Qualifier("directServiceARPMH.handler")
	private MessageHandler directServiceARPMH;

	@Autowired
	@Qualifier("refServiceARPMH.handler")
	private MessageHandler refServiceARPMH;

	@Autowired
	@Qualifier("directSplitter.handler")
	private MessageHandler directSplitter;

	@Autowired
	@Qualifier("refSplitter.handler")
	private MessageHandler refSplitter;

	@Autowired
	@Qualifier("splitterWithARPMH.handler")
	private MessageHandler splitterWithARPMH;

	@Autowired
	@Qualifier("splitterWithARPMHWithAtts.handler")
	private MessageHandler splitterWithARPMHWithAtts;

	@Autowired
	@Qualifier("directTransformer.handler")
	private MessageHandler directTransformer;

	@Autowired
	@Qualifier("refTransformer.handler")
	private MessageHandler refTransformer;

	@Autowired
	@Qualifier("directTransformerARPMH.handler")
	private MessageHandler directTransformerARPMH;

	@Autowired
	@Qualifier("refTransformerARPMH.handler")
	private MessageHandler refTransformerARPMH;

	private static QueueChannel replyChannel = new QueueChannel();

	@Test
	public void testDelegates() {
		assertThat(directFilter instanceof MyFilter).isTrue();
		testHandler(directFilter);
		assertThat(refFilter instanceof MyFilter).isTrue();
		testHandler(refFilter);
		// MessageSelector (wrapped in MessageFilter) wins here
		assertThat(filterWithMessageSelectorThatsAlsoAnARPMH instanceof MessageFilter).isTrue();
		testHandler(filterWithMessageSelectorThatsAlsoAnARPMH);

		assertThat(directRouter instanceof MyRouter).isTrue();
		testHandler(directRouter);
		assertThat(refRouter instanceof MyRouter).isTrue();
		testHandler(refRouter);
		assertThat(directRouterMH instanceof MyRouterMH).isTrue();
		testHandler(directRouterMH);
		assertThat(refRouterMH instanceof MyRouterMH).isTrue();
		testHandler(refRouterMH);
		assertThat(directRouterARPMH instanceof MyRouterARPMH).isTrue();
		testHandler(directRouterARPMH);
		assertThat(refRouterARPMH instanceof MyRouterARPMH).isTrue();
		testHandler(refRouterARPMH);

		assertThat(directServiceARPMH instanceof MyServiceARPMH).isTrue();
		testHandler(directServiceARPMH);
		assertThat(refServiceARPMH instanceof MyServiceARPMH).isTrue();
		testHandler(refServiceARPMH);

		assertThat(directSplitter instanceof MySplitter).isTrue();
		testHandler(directSplitter);
		assertThat(refSplitter instanceof MySplitter).isTrue();
		testHandler(refSplitter);
		assertThat(splitterWithARPMH instanceof MySplitterThatsAnARPMH).isTrue();
		testHandler(splitterWithARPMH);
		assertThat(splitterWithARPMHWithAtts instanceof MySplitterThatsAnARPMH).isTrue();
		assertThat(TestUtils.getPropertyValue(splitterWithARPMHWithAtts, "messagingTemplate.sendTimeout", Long.class))
				.isEqualTo(Long.valueOf(123));
		testHandler(splitterWithARPMHWithAtts);

		assertThat(directTransformer instanceof MessageTransformingHandler).isTrue();
		assertThat(TestUtils.getPropertyValue(directTransformer, "transformer") instanceof MyTransformer).isTrue();
		testHandler(directTransformer);
		assertThat(refTransformer instanceof MessageTransformingHandler).isTrue();
		assertThat(TestUtils.getPropertyValue(refTransformer, "transformer") instanceof MyTransformer).isTrue();
		testHandler(refTransformer);
		assertThat(directTransformerARPMH instanceof MyTransformerARPMH).isTrue();
		testHandler(directTransformerARPMH);
		assertThat(refTransformerARPMH instanceof MyTransformerARPMH).isTrue();
		testHandler(refTransformerARPMH);

	}

	@Test
	public void testOneRefOnly() {
		ServiceActivatorFactoryBean fb = new ServiceActivatorFactoryBean();
		fb.setBeanFactory(mock(BeanFactory.class));
		MyServiceARPMH service = new MyServiceARPMH();
		service.setBeanName("foo");
		fb.setTargetObject(service);
		fb.getObject();

		assertThat(TestUtils.getPropertyValue(fb, "REFERENCED_REPLY_PRODUCERS", Set.class).contains(service)).isTrue();

		ServiceActivatorFactoryBean fb2 = new ServiceActivatorFactoryBean();
		fb2.setBeanFactory(mock(BeanFactory.class));
		fb2.setTargetObject(service);
		assertThatExceptionOfType(Exception.class)
				.isThrownBy(fb2::getObject)
				.withMessage("An AbstractMessageProducingMessageHandler may only be referenced once (foo) - "
						+ "use scope=\"prototype\"");

		fb.destroy();
		assertThat(TestUtils.getPropertyValue(fb, "REFERENCED_REPLY_PRODUCERS", Set.class).contains(service)).isFalse();
	}

	private void testHandler(MessageHandler handler) {
		Message<?> message = MessageBuilder.withPayload("foo")
				.setReplyChannel(replyChannel)
				.build();
		handler.handleMessage(message);
		assertThat(replyChannel.receive(0)).isNotNull();

	}

	public static class MyFilter extends MessageFilter {

		public MyFilter() {
			super(message -> true);
		}

	}

	public static class MySelectorShouldntBeUsedAsTheHandler extends AbstractReplyProducingMessageHandler
			implements MessageSelector {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			return null;
		}

		@Override
		public boolean accept(Message<?> message) {
			return true;
		}

	}

	public static class MyRouter extends AbstractMessageRouter {

		@Override
		protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
			List<MessageChannel> channels = new ArrayList<>();
			channels.add(replyChannel);
			return channels;
		}

	}

	public static class MyRouterMH implements MessageHandler {

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			replyChannel.send(message);
		}

	}

	public static class MyRouterARPMH extends AbstractReplyProducingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			replyChannel.send(requestMessage);
			return null;
		}

	}

	public static class MyServiceARPMH extends AbstractReplyProducingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			return requestMessage;
		}

	}

	public static class MyTransformer extends AbstractTransformer {

		@Override
		protected Object doTransform(Message<?> message) {
			return message;
		}

	}

	public static class MyTransformerARPMH extends AbstractReplyProducingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			return requestMessage;
		}

	}

	public static class MySplitter extends AbstractMessageSplitter {

		@Override
		protected Object splitMessage(Message<?> message) {
			return message;
		}

	}

	public static class MySplitterThatsAnARPMH extends AbstractReplyProducingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			return requestMessage;
		}

	}

}

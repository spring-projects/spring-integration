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
package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class DelegatingConsumerParserTests {

	@Autowired @Qualifier("directFilter.handler")
	private MessageHandler directFilter;

	@Autowired @Qualifier("refFilter.handler")
	private MessageHandler refFilter;

	@Autowired @Qualifier("filterWithMessageSelectorThatsAlsoAnARPMH.handler")
	private MessageHandler filterWithMessageSelectorThatsAlsoAnARPMH;

	@Autowired @Qualifier("directRouter.handler")
	private MessageHandler directRouter;

	@Autowired @Qualifier("refRouter.handler")
	private MessageHandler refRouter;

	@Autowired @Qualifier("directRouterMH.handler")
	private MessageHandler directRouterMH;

	@Autowired @Qualifier("refRouterMH.handler")
	private MessageHandler refRouterMH;

	@Autowired @Qualifier("directRouterARPMH.handler")
	private MessageHandler directRouterARPMH;

	@Autowired @Qualifier("refRouterARPMH.handler")
	private MessageHandler refRouterARPMH;

	@Autowired @Qualifier("directServiceARPMH.handler")
	private MessageHandler directServiceARPMH;

	@Autowired @Qualifier("refServiceARPMH.handler")
	private MessageHandler refServiceARPMH;

	@Autowired @Qualifier("directSplitter.handler")
	private MessageHandler directSplitter;

	@Autowired @Qualifier("refSplitter.handler")
	private MessageHandler refSplitter;

	@Autowired @Qualifier("splitterWithARPMH.handler")
	private MessageHandler splitterWithARPMH;

	@Autowired @Qualifier("splitterWithARPMHWithAtts.handler")
	private MessageHandler splitterWithARPMHWithAtts;

	@Autowired @Qualifier("directTransformer.handler")
	private MessageHandler directTransformer;

	@Autowired @Qualifier("refTransformer.handler")
	private MessageHandler refTransformer;

	@Autowired @Qualifier("directTransformerARPMH.handler")
	private MessageHandler directTransformerARPMH;

	@Autowired @Qualifier("refTransformerARPMH.handler")
	private MessageHandler refTransformerARPMH;

	private static QueueChannel replyChannel = new QueueChannel();

	@Test
	public void testDelegates() {
		assertTrue(directFilter instanceof MyFilter);
		testHandler(directFilter);
		assertTrue(refFilter instanceof MyFilter);
		testHandler(refFilter);
		// MessageSelector (wrapped in MessageFilter) wins here
		assertTrue(filterWithMessageSelectorThatsAlsoAnARPMH instanceof MessageFilter);
		testHandler(filterWithMessageSelectorThatsAlsoAnARPMH);

		assertTrue(directRouter instanceof MyRouter);
		testHandler(directRouter);
		assertTrue(refRouter instanceof MyRouter);
		testHandler(refRouter);
		assertTrue(directRouterMH instanceof MyRouterMH);
		testHandler(directRouterMH);
		assertTrue(refRouterMH instanceof MyRouterMH);
		testHandler(refRouterMH);
		assertTrue(directRouterARPMH instanceof MyRouterARPMH);
		testHandler(directRouterARPMH);
		assertTrue(refRouterARPMH instanceof MyRouterARPMH);
		testHandler(refRouterARPMH);

		assertTrue(directServiceARPMH instanceof MyServiceARPMH);
		testHandler(directServiceARPMH);
		assertTrue(refServiceARPMH instanceof MyServiceARPMH);
		testHandler(refServiceARPMH);

		assertTrue(directSplitter instanceof MySplitter);
		testHandler(directSplitter);
		assertTrue(refSplitter instanceof MySplitter);
		testHandler(refSplitter);
		assertTrue(splitterWithARPMH instanceof MySplitterThatsAnARPMH);
		testHandler(splitterWithARPMH);
		assertTrue(splitterWithARPMHWithAtts instanceof MySplitterThatsAnARPMH);
		assertEquals(Long.valueOf(123), TestUtils.getPropertyValue(splitterWithARPMHWithAtts, "messagingTemplate.sendTimeout", Long.class));
		testHandler(splitterWithARPMHWithAtts);

		assertTrue(directTransformer instanceof MessageTransformingHandler);
		assertTrue(TestUtils.getPropertyValue(directTransformer, "transformer") instanceof MyTransformer);
		testHandler(directTransformer);
		assertTrue(refTransformer instanceof MessageTransformingHandler);
		assertTrue(TestUtils.getPropertyValue(refTransformer, "transformer") instanceof MyTransformer);
		testHandler(refTransformer);
		assertTrue(directTransformerARPMH instanceof MyTransformerARPMH);
		testHandler(directTransformerARPMH);
		assertTrue(refTransformerARPMH instanceof MyTransformerARPMH);
		testHandler(refTransformerARPMH);

	}

	@Test
	public void testOneRefOnly() throws Exception {
		ServiceActivatorFactoryBean fb = new ServiceActivatorFactoryBean();
		fb.setBeanFactory(mock(BeanFactory.class));
		MyServiceARPMH service = new MyServiceARPMH();
		service.setBeanName("foo");
		fb.setTargetObject(service);
		fb.getObject();
		fb = new ServiceActivatorFactoryBean();
		fb.setBeanFactory(mock(BeanFactory.class));
		fb.setTargetObject(service);
		try {
			fb.getObject();
			fail("expected exception");
		}
		catch (Exception e) {
			assertEquals("An AbstractReplyProducingMessageHandler may only be referenced once (foo) - "
					+ "use scope=\"prototype\"", e.getMessage());
		}
	}

	private void testHandler(MessageHandler handler) {
		Message<?> message = MessageBuilder.withPayload("foo")
				.setReplyChannel(replyChannel)
				.build();
		handler.handleMessage(message);
		assertNotNull(replyChannel.receive(0));

	}

	public static class MyFilter extends MessageFilter {

		public MyFilter() {
			super(new MessageSelector() {

				@Override
				public boolean accept(Message<?> message) {
					return true;
				}
			});
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
			List<MessageChannel> channels = new ArrayList<MessageChannel>();
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
		protected Object doTransform(Message<?> message) throws Exception {
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

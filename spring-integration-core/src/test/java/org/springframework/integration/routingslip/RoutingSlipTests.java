/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.integration.routingslip;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.RoutingSlipHeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.GenericXmlContextLoader;

import reactor.rx.Streams;

/**
 * @author Artem Bilan
 * @since 4.1
 */
@ContextConfiguration(loader = GenericXmlContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class RoutingSlipTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private MessageChannel routingSlipHeaderChannel;

	@Autowired
	private PollableChannel resultsChannel;

	@Autowired
	private MessageChannel invalidRoutingSlipChannel;

	@Test
	@SuppressWarnings("unchecked")
	public void testRoutingSlip() {
		PollableChannel replyChannel = new QueueChannel();
		Message<List<String>> request = MessageBuilder.withPayload(Arrays.asList("test1", "test2"))
				.setReplyChannel(replyChannel)
				.setHeader("myRoutingSlipChannel", "channel5").build();
		this.input.send(request);
		Message<?> reply = replyChannel.receive(10000);
		assertNotNull(reply);
		List<Message<?>> messages = (List<Message<?>>) reply.getPayload();
		for (Message<?> message : messages) {
			Map<List<String>, Integer> routingSlip = message.getHeaders()
					.get(IntegrationMessageHeaderAccessor.ROUTING_SLIP, Map.class);
			assertEquals(routingSlip.keySet().iterator().next().size(), routingSlip.values().iterator().next().intValue());
			MessageHistory messageHistory = MessageHistory.read(message);
			List<String> channelNames = Arrays.asList("input", "split", "process", "channel1", "channel2",
					"channel3", "channel4", "channel5", "aggregate");
			int i = 0;
			for (Properties properties : messageHistory) {
				assertTrue(channelNames.contains(properties.getProperty("name")));
			}
		}
	}

	@Test
	public void testDynamicRoutingSlipRoutStrategy() {
		this.routingSlipHeaderChannel.send(new GenericMessage<>("foo"));
		Message<?> result = this.resultsChannel.receive(10000);
		assertNotNull(result);
		assertEquals("FOO", result.getPayload());

		this.routingSlipHeaderChannel.send(new GenericMessage<>(2));
		result = this.resultsChannel.receive(10000);
		assertNotNull(result);
		assertEquals(4, result.getPayload());
	}

	@Test
	public void testInvalidRoutingSlipRoutStrategy() {
		try {
			new RoutingSlipHeaderValueMessageProcessor(new Date());
			fail("IllegalArgumentException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalArgumentException.class));
			assertThat(e.getMessage(),
					containsString("The RoutingSlip can contain " +
							"only bean names of MessageChannel or RoutingSlipRouteStrategy, " +
							"or MessageChannel and RoutingSlipRouteStrategy instances"));
		}
		try {
			this.invalidRoutingSlipChannel.send(new GenericMessage<>("foo"));
			fail("MessagingException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessagingException.class));
			assertThat(e.getMessage(), containsString("replyChannel must be a MessageChannel or String"));
		}
	}

	public static class TestRoutingSlipRoutePojo {

		final String[] channels = {"channel2", "channel3"};

		private int i = 0;

		public String get(Message<?> requestMessage, Object reply) {
			try {
				return this.channels[i++];
			}
			catch (Exception e) {
				return null;
			}
		}

	}

	public static class TestRoutingSlipRouteStrategy implements RoutingSlipRouteStrategy {

		private AtomicBoolean invoked = new AtomicBoolean();

		@Override
		public Object getNextPath(Message<?> requestMessage, Object reply) {
			return !invoked.getAndSet(true) ? "channel4" : null;
		}

	}

	@Configuration
	@EnableIntegration
	public static class RoutingSlipConfiguration {

		@Bean
		public MessagingTemplate messagingTemplate() {
			return new MessagingTemplate();
		}

		@Bean
		public PollableChannel resultsChannel() {
			return new QueueChannel();
		}

		@Bean
		public RoutingSlipRouteStrategy routeStrategy() {
			return (requestMessage, reply) -> requestMessage.getPayload() instanceof String
					? new FixedSubscriberChannel(m ->
					Streams.just((String) m.getPayload())
							.map(String::toUpperCase)
							.consume(v -> messagingTemplate().convertAndSend(resultsChannel(), v)))
					: new FixedSubscriberChannel(m ->
					Streams.just((Integer) m.getPayload())
							.map(v -> v * 2)
							.consume(v -> messagingTemplate().convertAndSend(resultsChannel(), v)));
		}

		@Bean
		public MessageChannel routingSlipHeaderChannel() {
			return new DirectChannel();
		}

		@Bean
		@BridgeTo
		public MessageChannel processChannel() {
			return new DirectChannel();
		}

		@Bean
		@Transformer(inputChannel = "routingSlipHeaderChannel", outputChannel = "processChannel")
		public HeaderEnricher headerEnricher() {
			return new HeaderEnricher(Collections.singletonMap(IntegrationMessageHeaderAccessor.ROUTING_SLIP,
					new RoutingSlipHeaderValueMessageProcessor(routeStrategy())));
		}

		@Bean
		public MessageChannel invalidRoutingSlipChannel() {
			return new DirectChannel();
		}

		@Bean
		@Transformer(inputChannel = "invalidRoutingSlipChannel", outputChannel = "processChannel")
		public HeaderEnricher headerEnricher2() {
			return new HeaderEnricher(Collections.singletonMap(IntegrationMessageHeaderAccessor.ROUTING_SLIP,
					new RoutingSlipHeaderValueMessageProcessor((RoutingSlipRouteStrategy) (message, r) -> new Date())));
		}

	}

}

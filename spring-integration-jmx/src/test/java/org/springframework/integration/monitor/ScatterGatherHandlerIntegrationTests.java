/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.monitor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.ExpressionEvaluatingMessageGroupProcessor;
import org.springframework.integration.aggregator.ExpressionEvaluatingReleaseStrategy;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.MessageCountReleaseStrategy;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.integration.scattergather.ScatterGatherHandler;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.1
 */
@SpringJUnitConfig
@DirtiesContext
public class ScatterGatherHandlerIntegrationTests {

	@Autowired
	private PollableChannel output;

	@Autowired
	private MessageChannel inputAuctionWithoutGatherChannel;

	@Autowired
	private MessageChannel inputAuctionWithGatherChannel;

	@Autowired
	private MessageChannel distributionChannel;

	@Test
	public void testSimpleAuction() {
		Message<String> quoteMessage = MessageBuilder.withPayload("testQuote").build();
		this.inputAuctionWithoutGatherChannel.send(quoteMessage);
		Message<?> bestQuoteMessage = this.output.receive(10000);
		assertThat(bestQuoteMessage).isNotNull();
		Object payload = bestQuoteMessage.getPayload();
		assertThat(payload).isInstanceOf(Double.class);
		assertThat((Double) payload).isLessThan(10D);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testAuctionWithGatherChannel() {
		Message<String> quoteMessage = MessageBuilder.withPayload("testQuote").build();
		this.inputAuctionWithGatherChannel.send(quoteMessage);
		Message<?> bestQuoteMessage = this.output.receive(10000);
		assertThat(bestQuoteMessage).isNotNull();
		Object payload = bestQuoteMessage.getPayload();
		assertThat(payload).isInstanceOf(List.class);
		assertThat(((List) payload).size()).isEqualTo(3);
	}

	@Test
	public void testDistribution() {
		Message<String> quoteMessage = MessageBuilder.withPayload("testQuote").build();
		this.distributionChannel.send(quoteMessage);
		Message<?> bestQuoteMessage = this.output.receive(10000);
		assertThat(bestQuoteMessage).isNotNull();
		Object payload = bestQuoteMessage.getPayload();
		assertThat(payload).isInstanceOf(Double.class);
		assertThat((Double) payload).isLessThan(10D);
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationMBeanExport(server = "mBeanServer")
	public static class ContextConfiguration {

		@Bean
		public static MBeanServerFactoryBean mBeanServer() {
			return new MBeanServerFactoryBean();
		}

		@Bean
		public PollableChannel output() {
			return new QueueChannel();
		}

		@Bean
		public SubscribableChannel scatterAuctionWithoutGatherChannel() {
			PublishSubscribeChannel channel = new PublishSubscribeChannel();
			channel.setApplySequence(true);
			return channel;
		}

		@Bean
		public MessageHandler gatherer1() {
			return new AggregatingMessageHandler(
					new ExpressionEvaluatingMessageGroupProcessor("^[payload gt 5] ?: -1D"),
					new SimpleMessageStore(),
					new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID),
					new ExpressionEvaluatingReleaseStrategy("size() == 2"));
		}

		@Bean
		@ServiceActivator(inputChannel = "inputAuctionWithoutGatherChannel")
		public ScatterGatherHandler scatterGatherAuctionWithoutGatherChannel() {
			ScatterGatherHandler handler = new ScatterGatherHandler(scatterAuctionWithoutGatherChannel(), gatherer1());
			handler.setOutputChannel(output());
			return handler;
		}

		@Bean
		@BridgeFrom("scatterAuctionWithoutGatherChannel")
		@BridgeFrom("scatterAuctionWithoutGatherChannel")
		@BridgeFrom("scatterAuctionWithoutGatherChannel")
		public MessageChannel serviceChannel1() {
			return new DirectChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "serviceChannel1")
		public AbstractReplyProducingMessageHandler service1() {
			return new AbstractReplyProducingMessageHandler() {

				@Override
				protected Object handleRequestMessage(Message<?> requestMessage) {
					return Math.random() * 10;
				}

			};
		}

		@Bean
		public RecipientListRouter distributor() {
			RecipientListRouter router = new RecipientListRouter();
			router.setApplySequence(true);
			router.setChannels(Arrays.asList(distributionChannel1(), distributionChannel2(), distributionChannel3()));
			return router;
		}

		@Bean
		@BridgeTo("serviceChannel1")
		public MessageChannel distributionChannel1() {
			return new DirectChannel();
		}

		@Bean
		@BridgeTo("serviceChannel1")
		public MessageChannel distributionChannel2() {
			return new DirectChannel();
		}

		@Bean
		@BridgeTo("serviceChannel1")
		public MessageChannel distributionChannel3() {
			return new DirectChannel();
		}

		@Bean
		public MessageChannel distributionChannel() {
			return new DirectChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "distributionChannel")
		public ScatterGatherHandler scatterGatherDistribution() {
			ScatterGatherHandler handler = new ScatterGatherHandler(distributor(), gatherer1());
			handler.setOutputChannel(output());
			return handler;
		}

		@Bean
		public MessageChannel inputAuctionWithGatherChannel() {
			return new DirectChannel();
		}

		@Bean
		public MessageChannel gatherChannel() {
			return new DirectChannel();
		}

		@Bean
		public SubscribableChannel scatterAuctionWithGatherChannel(Executor executor) {
			PublishSubscribeChannel channel = new PublishSubscribeChannel(executor);
			channel.setApplySequence(true);
			return channel;
		}

		@Bean
		public MessageHandler gatherer2() {
			return new AggregatingMessageHandler(new DefaultAggregatingMessageGroupProcessor(),
					new SimpleMessageStore(),
					new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID),
					new MessageCountReleaseStrategy(3));
		}

		@Bean
		@ServiceActivator(inputChannel = "inputAuctionWithGatherChannel")
		public ScatterGatherHandler scatterGatherAuctionWithGatherChannel() {
			ScatterGatherHandler handler =
					new ScatterGatherHandler(scatterAuctionWithGatherChannel(null), gatherer2());
			handler.setGatherChannel(gatherChannel());
			handler.setOutputChannel(output());
			return handler;
		}

		@Bean
		@BridgeFrom("scatterAuctionWithGatherChannel")
		@BridgeFrom("scatterAuctionWithGatherChannel")
		@BridgeFrom("scatterAuctionWithGatherChannel")
		@BridgeFrom("scatterAuctionWithGatherChannel")
		public MessageChannel serviceChannel2() {
			return new DirectChannel();
		}

		@ServiceActivator(inputChannel = "serviceChannel2", outputChannel = "gatherChannel")
		public double service2() {
			return Math.random();
		}

	}

}

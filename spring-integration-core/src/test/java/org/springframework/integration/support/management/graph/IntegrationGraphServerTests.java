/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.support.management.graph;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class IntegrationGraphServerTests {

	@Autowired
	private IntegrationGraphServer server;

	@Test
	public void test() throws Exception {
		Graph graph = this.server.getGraph();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.writeValue(baos, graph);
//		System . out . println(new String(baos.toByteArray()));
		Map<?, ?> map = objectMapper.readValue(baos.toByteArray(), Map.class);
		assertThat(map.size(), is(equalTo(3)));
		@SuppressWarnings("unchecked")
		List<Map<?, ?>> nodes = (List<Map<?, ?>>) map.get("nodes");
		assertThat(nodes, is(notNullValue()));
		assertThat(nodes.size(), is(equalTo(13)));
		@SuppressWarnings("unchecked")
		List<Map<?, ?>> links = (List<Map<?, ?>>) map.get("links");
		assertThat(links, is(notNullValue()));
		assertThat(links.size(), is(equalTo(9)));
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationManagement
	public static class Config {

		@Bean
		public IntegrationGraphServer server() {
			IntegrationGraphServer server = new IntegrationGraphServer();
			server.setApplicationName("myAppName:1.0");
			return server;
		}

		@Bean
		public MessageProducer producer() {
			MessageProducerSupport producer = new MessageProducerSupport() {

				@Override
				public String getComponentType() {
					return "test-producer";
				}

			};
			producer.setOutputChannelName("one");
			producer.setErrorChannelName("myErrors");
			return producer;
		}

		@Bean
		public Services services() {
			return new Services();
		}

		@Bean
		public EventDrivenConsumer foreignMessageHandlerNoStats() {
			return new EventDrivenConsumer(three(), new BareHandler());
		}

		@Bean
		public PollingConsumer polling() {
			PollingConsumer pollingConsumer = new PollingConsumer(four(), new BareHandler());
			pollingConsumer.setAutoStartup(false);
			return pollingConsumer;
		}

		@Bean
		public PollableChannel two() {
			return new QueueChannel();
		}

		@Bean
		public SubscribableChannel three() {
			return new DirectChannel();
		}

		@Bean
		public PollableChannel four() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel myErrors() {
			return new QueueChannel();
		}

		@Bean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerMetadata defaultPoller() {
			PollerMetadata poller = new PollerMetadata();
			poller.setTrigger(new PeriodicTrigger(60000));
			MessagePublishingErrorHandler errorHandler = new MessagePublishingErrorHandler();
			errorHandler.setDefaultErrorChannel(myErrors());
			poller.setErrorHandler(errorHandler);
			return poller;
		}

	}

	public static class Services {

		@ServiceActivator(inputChannel = "one", outputChannel = "two")
		public String foo(String foo) {
			return foo.toUpperCase();
		}

		@ServiceActivator(inputChannel = "two")
		public void bar(String foo) {
		}

	}

	public static class BareHandler implements MessageHandler {

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			// empty
		}

	}

}

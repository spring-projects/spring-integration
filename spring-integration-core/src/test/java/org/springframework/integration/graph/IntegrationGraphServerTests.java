/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.json.JsonPathUtils;
import org.springframework.integration.router.ExpressionEvaluatingRouter;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.minidev.json.JSONArray;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class IntegrationGraphServerTests {

	@Autowired
	private IntegrationGraphServer server;

	@Autowired
	private MessageChannel toRouter;

	@Autowired
	private MessageChannel expressionRouterInput;

	@Autowired
	private IntegrationFlowContext flowContext;

	@Autowired
	private MessageSource<String> testSource;

	@SuppressWarnings("unchecked")
	@Test
	void test() throws Exception {
		Graph graph = this.server.getGraph();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.writeValue(baos, graph);

		//		System . out . println(new String(baos.toByteArray()));

		Map<?, ?> map = objectMapper.readValue(baos.toByteArray(), Map.class);
		assertThat(map.size()).isEqualTo(3);
		List<Map<?, ?>> nodes = (List<Map<?, ?>>) map.get("nodes");
		assertThat(nodes).isNotNull();
		assertThat(nodes.size()).isEqualTo(34);

		JSONArray jsonArray =
				JsonPathUtils.evaluate(baos.toByteArray(), "$..nodes[?(@.componentType == 'gateway')]");

		assertThat(jsonArray.size()).isEqualTo(3);

		Map<String, Object> gateway1 = (Map<String, Object>) jsonArray.get(0);

		Map<String, Object> properties = (Map<String, Object>) gateway1.get("properties");

		assertThat(properties).isNotNull();

		assertThat(properties.get("auto-startup")).isEqualTo(Boolean.TRUE);
		assertThat(properties.get("running")).isEqualTo(Boolean.TRUE);

		List<Map<?, ?>> links = (List<Map<?, ?>>) map.get("links");
		assertThat(links).isNotNull();
		assertThat(links.size()).isEqualTo(34);

		jsonArray =
				JsonPathUtils.evaluate(baos.toByteArray(), "$..nodes[?(@.name == 'expressionRouter')]");

		Map<String, Object> expressionRouter = (Map<String, Object>) jsonArray.get(0);
		assertThat(((List<?>) expressionRouter.get("routes")).size()).isEqualTo(0);

		this.toRouter.send(MessageBuilder.withPayload("foo").setHeader("foo", "bar").build());
		this.toRouter.send(MessageBuilder.withPayload("foo").setHeader("foo", "baz").build());
		this.toRouter.send(MessageBuilder.withPayload("foo").setHeader("foo", "quxChannel").build());
		this.toRouter.send(MessageBuilder.withPayload("foo").setHeader("foo", "fizChannel").build());
		this.testSource.receive();
		this.expressionRouterInput.send(MessageBuilder.withPayload("foo").setHeader("foo", "fizChannel").build());

		jsonArray = JsonPathUtils.evaluate(baos.toByteArray(), "$..nodes[?(@.name == 'router')]");
		String routerJson = jsonArray.toJSONString();

		this.server.rebuild();
		graph = this.server.getGraph();
		baos = new ByteArrayOutputStream();
		objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.writeValue(baos, graph);

		//		System . out . println(new String(baos.toByteArray()));

		map = objectMapper.readValue(baos.toByteArray(), Map.class);
		assertThat(map.size()).isEqualTo(3);
		nodes = (List<Map<?, ?>>) map.get("nodes");
		assertThat(nodes).isNotNull();
		assertThat(nodes.size()).isEqualTo(34);
		links = (List<Map<?, ?>>) map.get("links");
		assertThat(links).isNotNull();
		assertThat(links.size()).isEqualTo(37);

		jsonArray = JsonPathUtils.evaluate(baos.toByteArray(), "$..nodes[?(@.name == 'router')]");
		routerJson = jsonArray.toJSONString();
		assertThat(routerJson).contains("\"sendTimers\":{\"successes\":{\"count\":4");
		jsonArray = JsonPathUtils.evaluate(baos.toByteArray(), "$..nodes[?(@.name == 'toRouter')]");
		String toRouterJson = jsonArray.toJSONString();
		assertThat(toRouterJson).contains("\"sendTimers\":{\"successes\":{\"count\":4");
		jsonArray = JsonPathUtils.evaluate(baos.toByteArray(), "$..nodes[?(@.name == 'testSource')]");
		String sourceJson = jsonArray.toJSONString();
		assertThat(sourceJson).contains("\"receiveCounters\":{\"successes\":1,\"failures\":0");

		// stats refresh without rebuild()
		this.testSource.receive();
		baos = new ByteArrayOutputStream();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.writeValue(baos, graph);
		jsonArray = JsonPathUtils.evaluate(baos.toByteArray(), "$..nodes[?(@.name == 'testSource')]");
		sourceJson = jsonArray.toJSONString();
		assertThat(sourceJson).contains("\"receiveCounters\":{\"successes\":2,\"failures\":0");

		assertThatIllegalStateException().isThrownBy(() -> this.testSource.receive());
		baos = new ByteArrayOutputStream();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.writeValue(baos, graph);
		jsonArray = JsonPathUtils.evaluate(baos.toByteArray(), "$..nodes[?(@.name == 'testSource')]");
		sourceJson = jsonArray.toJSONString();
		assertThat(sourceJson).contains("\"receiveCounters\":{\"successes\":2,\"failures\":1");

		jsonArray =
				JsonPathUtils.evaluate(baos.toByteArray(), "$..nodes[?(@.name == 'expressionRouter')]");

		expressionRouter = (Map<String, Object>) jsonArray.get(0);
		JSONArray routes = (JSONArray) expressionRouter.get("routes");
		assertThat(routes).hasSize(1);
		assertThat(routes.get(0)).isEqualTo("fizChannel");

		Object routerNodeId = expressionRouter.get("nodeId");

		Object fizChannelNodeId =
				((JSONArray) JsonPathUtils.evaluate(baos.toByteArray(),
						"$..nodes[?(@.name == 'fizChannel')].nodeId")).get(0);

		jsonArray =
				JsonPathUtils.evaluate(baos.toByteArray(),
						"$..links[?(@.from == " + routerNodeId + "&& @.to == " + fizChannelNodeId + ")]");
		assertThat(jsonArray).hasSize(1);

		jsonArray = JsonPathUtils.evaluate(baos.toByteArray(),
				"$..nodes[?(@.name == 'services.foo.serviceActivator')]");

		assertThat(jsonArray).hasSize(1);

		Map<String, Object> serviceActivator = (Map<String, Object>) jsonArray.get(0);
		assertThat(serviceActivator).containsEntry("integrationPatternType", "service_activator");

		jsonArray = JsonPathUtils.evaluate(baos.toByteArray(),
				"$..nodes[?(@.name == 'polling')]");

		assertThat(jsonArray).hasSize(1);

		serviceActivator = (Map<String, Object>) jsonArray.get(0);
		assertThat(serviceActivator)
				.containsEntry("integrationPatternType", "service_activator")
				.containsEntry("integrationPatternCategory", "messaging_endpoint");
	}

	@Test
	void testIncludesDynamic() {
		Graph graph = this.server.getGraph();
		assertThat(graph.getNodes().size()).isEqualTo(34);
		IntegrationFlow flow = f -> f.handle(m -> {
		});
		IntegrationFlowRegistration reg = this.flowContext.registration(flow).register();
		graph = this.server.rebuild();
		assertThat(graph.getNodes().size()).isEqualTo(36);
		this.flowContext.remove(reg.getId());
		graph = this.server.rebuild();
		assertThat(graph.getNodes().size()).isEqualTo(34);
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationManagement
	@IntegrationComponentScan
	@ImportResource("org/springframework/integration/graph/integration-graph-context.xml")
	public static class Config {

		@Bean
		public static MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

		@Bean
		public IntegrationGraphServer server() {
			IntegrationGraphServer server = new IntegrationGraphServer();
			server.setApplicationName("myAppName:1.0");
			server.setAdditionalPropertiesCallback(namedComponent -> {
				Map<String, Object> properties = null;
				if (namedComponent instanceof SmartLifecycle) {
					SmartLifecycle smartLifecycle = (SmartLifecycle) namedComponent;
					properties = new HashMap<>();
					properties.put("auto-startup", smartLifecycle.isAutoStartup());
					properties.put("running", smartLifecycle.isRunning());
				}
				return properties;
			});
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
		public PollableChannel polledChannel() {
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

		@Bean
		@Router(inputChannel = "toRouter")
		public HeaderValueRouter router() {
			HeaderValueRouter router = new HeaderValueRouter("foo");
			router.setChannelMapping("bar", "barChannel");
			router.setChannelMapping("baz", "bazChannel");
			router.setDefaultOutputChannel(discards());
			return router;
		}

		@Bean
		@Router(inputChannel = "four")
		public RecipientListRouter rlRouter() {
			RecipientListRouter router = new RecipientListRouter();
			router.setChannels(Arrays.asList(barChannel(), bazChannel()));
			router.setDefaultOutputChannel(discards());
			return router;
		}

		@Bean
		@Router(inputChannel = "expressionRouterInput")
		public ExpressionEvaluatingRouter expressionRouter() {
			ExpressionEvaluatingRouter router = new ExpressionEvaluatingRouter(
					new SpelExpressionParser().parseExpression("headers['foo']"));
			router.setDefaultOutputChannel(discards());
			return router;
		}

		@Bean
		public MessageChannel discards() {
			return new DirectChannel();
		}

		@Bean
		public MessageChannel toRouter() {
			return new DirectChannel();
		}

		@Bean
		public MessageChannel barChannel() {
			return new QueueChannel();
		}

		@Bean
		public MessageChannel bazChannel() {
			return new QueueChannel();
		}

		@Bean
		public MessageChannel quxChannel() {
			return new QueueChannel();
		}

		@Bean
		public MessageChannel fizChannel() {
			return new QueueChannel();
		}

		int sourceCount;

		@Bean
		@InboundChannelAdapter(channel = "fizChannel", autoStartup = "false")
		public MessageSource<String> testSource() {
			return new AbstractMessageSource<String>() {

				@Override
				public String getComponentType() {
					return "source";
				}

				@Override
				protected Object doReceive() {
					if (++sourceCount > 2) {
						throw new IllegalStateException();
					}
					return new GenericMessage<>("foo");
				}

			};
		}

	}

	public static class Services {

		@ServiceActivator(inputChannel = "one", outputChannel = "polledChannel")
		public String foo(String foo) {
			return foo.toUpperCase();
		}

		@ServiceActivator(inputChannel = "polledChannel")
		public void bar(@SuppressWarnings("unused") String foo) {
		}

		@Filter(inputChannel = "filterChannel")
		public boolean filter(@SuppressWarnings("unused") String payload) {
			return false;
		}

	}

	public static class BareHandler implements MessageHandler {

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			// empty
		}

	}

	@MessagingGateway(defaultRequestChannel = "four")
	public interface Gate {

		void foo(String foo);

		void foo(Integer foo);

		void bar(String bar);

	}

}

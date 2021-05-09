/*
 * Copyright 2014-2021 the original author or authors.
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

package org.springframework.integration.websocket.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;
import org.springframework.integration.transformer.ExpressionEvaluatingTransformer;
import org.springframework.integration.websocket.ClientWebSocketContainer;
import org.springframework.integration.websocket.IntegrationWebSocketContainer;
import org.springframework.integration.websocket.ServerWebSocketContainer;
import org.springframework.integration.websocket.TomcatWebSocketTestServer;
import org.springframework.integration.websocket.inbound.WebSocketInboundChannelAdapter;
import org.springframework.integration.websocket.outbound.WebSocketOutboundMessageHandler;
import org.springframework.integration.websocket.support.SubProtocolHandlerRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.broker.SubscriptionRegistry;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

/**
 * @author Artem Bilan
 *
 * @since 4.1
 */
@SpringJUnitConfig(classes = WebSocketServerTests.ClientConfig.class)
@DirtiesContext
public class WebSocketServerTests {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	@Autowired
	@Qualifier("webSocketOutputChannel")
	private MessageChannel webSocketOutputChannel;

	@Autowired
	@Qualifier("webSocketInputChannel")
	private PollableChannel webSocketInputChannel;

	@Value("#{server.serverContext.getBean('simpleBrokerMessageHandler')}")
	private SimpleBrokerMessageHandler brokerHandler;

	@Value("#{server.serverContext.getBean('webSocketEvents')}")
	private PollableChannel webSocketEvents;

	@Value("#{server.serverContext.getBean('requestUpgradeStrategy')}")
	private Lifecycle requestUpgradeStrategy;

	@Test
	public void testWebSocketOutboundMessageHandler() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSubscriptionId("subs1");
		headers.setDestination("/queue/foo");
		Message<byte[]> message = MessageBuilder.withPayload(ByteBuffer.allocate(0).array()).setHeaders(headers).build();

		headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setSubscriptionId("subs1");
		Message<String> message2 = MessageBuilder.withPayload("Spring").setHeaders(headers).build();

		this.webSocketOutputChannel.send(message);
		this.webSocketOutputChannel.send(message2);

		Message<?> received = this.webSocketInputChannel.receive(10000);
		assertThat(received).isNotNull();
		StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(received);
		assertThat(stompHeaderAccessor.getMessageType()).isEqualTo(StompCommand.MESSAGE.getMessageType());

		Object receivedPayload = received.getPayload();
		assertThat(receivedPayload).isInstanceOf(String.class);
		assertThat(receivedPayload).isEqualTo("Hello Spring");

		SubscriptionRegistry subscriptionRegistry = this.brokerHandler.getSubscriptionRegistry();
		headers = StompHeaderAccessor.create(StompCommand.MESSAGE);
		headers.setDestination("/queue/foo");
		message = MessageBuilder.withPayload(ByteBuffer.allocate(0).array()).setHeaders(headers).build();
		MultiValueMap<String, String> subscriptions = subscriptionRegistry.findSubscriptions(message);
		assertThat(subscriptions.isEmpty()).isFalse();
		List<String> subscription = subscriptions.values().iterator().next();
		assertThat(subscription.size()).isEqualTo(1);
		assertThat(subscription.get(0)).isEqualTo("subs1");

		Message<?> event = this.webSocketEvents.receive(10000);
		assertThat(event).isNotNull();
		assertThat(event.getPayload()).isInstanceOf(WebSocketSession.class);

		verify(this.requestUpgradeStrategy).start();
	}

	@Test
	public void testBrokerIsNotPresented() {
		WebSocketInboundChannelAdapter webSocketInboundChannelAdapter =
				new WebSocketInboundChannelAdapter(Mockito.mock(ServerWebSocketContainer.class));
		webSocketInboundChannelAdapter.setOutputChannel(new DirectChannel());
		webSocketInboundChannelAdapter.setUseBroker(true);
		webSocketInboundChannelAdapter.setBeanFactory(Mockito.mock(BeanFactory.class));
		webSocketInboundChannelAdapter.setApplicationContext(Mockito.mock(ApplicationContext.class));

		assertThatIllegalStateException()
				.isThrownBy(webSocketInboundChannelAdapter::afterPropertiesSet)
				.withMessageContaining("WebSocket Broker Relay isn't present in the application context;");
	}

	@Configuration
	@EnableIntegration
	public static class ClientConfig {

		@Bean
		public TomcatWebSocketTestServer server() {
			return new TomcatWebSocketTestServer(ServerConfig.class);
		}

		@Bean
		public WebSocketClient webSocketClient() {
			return new SockJsClient(Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient())));
		}

		@Bean
		public IntegrationWebSocketContainer clientWebSocketContainer() {
			ClientWebSocketContainer clientWebSocketContainer =
					new ClientWebSocketContainer(webSocketClient(), server().getWsBaseUrl() + "/ws");
			clientWebSocketContainer.setOrigin("https://www.example.com/");
			return clientWebSocketContainer;
		}

		@Bean
		public SubProtocolHandler stompSubProtocolHandler() {
			return new StompSubProtocolHandler();
		}

		@Bean
		public PollableChannel webSocketInputChannel() {
			return new QueueChannel();
		}

		@Bean
		public MessageChannel webSocketOutputChannel() {
			return new DirectChannel();
		}

		@Bean
		public MessageProducer webSocketInboundChannelAdapter() {
			WebSocketInboundChannelAdapter webSocketInboundChannelAdapter =
					new WebSocketInboundChannelAdapter(clientWebSocketContainer(),
							new SubProtocolHandlerRegistry(stompSubProtocolHandler()));
			webSocketInboundChannelAdapter.setOutputChannel(webSocketInputChannel());
			return webSocketInboundChannelAdapter;
		}

		@Bean
		@ServiceActivator(inputChannel = "webSocketOutputChannel")
		public MessageHandler webSocketOutboundMessageHandler() {
			return new WebSocketOutboundMessageHandler(clientWebSocketContainer(),
					new SubProtocolHandlerRegistry(stompSubProtocolHandler()));
		}

	}

	// WebSocket Server part

	@Configuration
	@EnableIntegration
	@EnableWebSocketMessageBroker
	static class ServerConfig implements WebSocketMessageBrokerConfigurer {

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/foo");
		}

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.setApplicationDestinationPrefixes("/app/")
					.enableSimpleBroker("/queue/", "/topic/");
		}

		@Bean
		public WebSocketHandlerDecoratorFactory testWebSocketHandlerDecoratorFactory() {
			return new TestWebSocketHandlerDecoratorFactory();
		}

		@Bean
		public RequestUpgradeStrategy requestUpgradeStrategy() {
			return spy(new MyTomcatRequestUpgradeStrategy());
		}

		@Bean
		public DefaultHandshakeHandler handshakeHandler() {
			return new DefaultHandshakeHandler(requestUpgradeStrategy());
		}

		@Bean
		public ServerWebSocketContainer serverWebSocketContainer() {
			return new ServerWebSocketContainer("/ws")
					.setHandshakeHandler(handshakeHandler())
					.setDecoratorFactories(testWebSocketHandlerDecoratorFactory())
					.setAllowedOrigins("https://www.example.com")
					.withSockJs();
		}

		@Bean
		public SubProtocolHandler stompSubProtocolHandler() {
			return new StompSubProtocolHandler();
		}

		@Bean
		public MessageChannel webSocketInputChannel() {
			return new DirectChannel();
		}

		@Bean
		public MessageChannel webSocketOutputChannel() {
			return new DirectChannel();
		}

		@Bean
		public MessageProducer webSocketInboundChannelAdapter() {
			WebSocketInboundChannelAdapter webSocketInboundChannelAdapter =
					new WebSocketInboundChannelAdapter(serverWebSocketContainer(),
							new SubProtocolHandlerRegistry(stompSubProtocolHandler()));
			webSocketInboundChannelAdapter.setOutputChannel(webSocketInputChannel());
			webSocketInboundChannelAdapter.setUseBroker(true);
			return webSocketInboundChannelAdapter;
		}

		@Bean
		@Transformer(inputChannel = "webSocketInputChannel", outputChannel = "webSocketOutputChannel")
		public ExpressionEvaluatingTransformer transformer() {
			return new ExpressionEvaluatingTransformer(PARSER.parseExpression("'Hello ' + payload"));
		}

		@Bean
		@ServiceActivator(inputChannel = "webSocketOutputChannel")
		public MessageHandler webSocketOutboundMessageHandler() {
			return new WebSocketOutboundMessageHandler(serverWebSocketContainer(),
					new SubProtocolHandlerRegistry(stompSubProtocolHandler()));
		}

		@Bean
		public PollableChannel webSocketEvents() {
			return new QueueChannel();
		}

		@Bean
		public ApplicationListener<ApplicationEvent> webSocketEventListener() {
			ApplicationEventListeningMessageProducer producer = new ApplicationEventListeningMessageProducer();
			producer.setEventTypes(PayloadApplicationEvent.class);
			producer.setPayloadExpression(new SpelExpressionParser().parseExpression("payload"));
			producer.setOutputChannel(webSocketEvents());
			return producer;
		}

	}

	private static class TestWebSocketHandlerDecoratorFactory
			implements WebSocketHandlerDecoratorFactory, ApplicationEventPublisherAware {

		private ApplicationEventPublisher applicationEventPublisher;

		TestWebSocketHandlerDecoratorFactory() {
			super();
		}

		@Override
		public WebSocketHandler decorate(WebSocketHandler handler) {
			return new TestWebSocketHandler(handler);
		}

		@Override
		public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
			this.applicationEventPublisher = applicationEventPublisher;
		}

		private class TestWebSocketHandler extends WebSocketHandlerDecorator {

			TestWebSocketHandler(WebSocketHandler delegate) {
				super(delegate);
			}

			@Override
			public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
				super.handleMessage(session, message);
			}

			@Override
			public void afterConnectionEstablished(WebSocketSession session) throws Exception {
				super.afterConnectionEstablished(session);
				applicationEventPublisher.publishEvent(session);
			}

		}

	}

	public static class MyTomcatRequestUpgradeStrategy extends TomcatRequestUpgradeStrategy implements Lifecycle {

		@Override
		public void start() {

		}

		@Override
		public void stop() {

		}

		@Override
		public boolean isRunning() {
			return true;
		}

	}

}

/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.integration.websocket.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
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
import org.springframework.integration.websocket.TomcatWebSocketTestServer;
import org.springframework.integration.websocket.event.ReceiptEvent;
import org.springframework.integration.websocket.inbound.WebSocketInboundChannelAdapter;
import org.springframework.integration.websocket.outbound.WebSocketOutboundMessageHandler;
import org.springframework.integration.websocket.support.ClientStompEncoder;
import org.springframework.integration.websocket.support.SubProtocolHandlerRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.broker.SubscriptionRegistry;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

/**
 * @author Artem Bilan
 *
 * @since 4.1
 */
@ContextConfiguration(classes = StompIntegrationTests.ClientConfig.class)
@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StompIntegrationTests {

	@Value("#{server.serverContext}")
	private ApplicationContext serverContext;

	@Autowired
	private IntegrationWebSocketContainer clientWebSocketContainer;

	@Autowired
	@Qualifier("webSocketOutputChannel")
	private MessageChannel webSocketOutputChannel;

	@Autowired
	@Qualifier("webSocketInputChannel")
	private QueueChannel webSocketInputChannel;

	@Autowired
	@Qualifier("webSocketEvents")
	private QueueChannel webSocketEvents;

	@Test
	public void sendMessageToController() throws Exception {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		this.webSocketOutputChannel.send(MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build());

		Message<?> receive = this.webSocketEvents.receive(20000);
		assertThat(receive).isNotNull();
		Object event = receive.getPayload();
		assertThat(event).isInstanceOf(SessionConnectedEvent.class);
		Message<?> connectedMessage = ((SessionConnectedEvent) event).getMessage();
		headers = StompHeaderAccessor.wrap(connectedMessage);
		assertThat(headers.getCommand()).isEqualTo(StompCommand.CONNECTED);

		headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setSubscriptionId("sub1");
		headers.setDestination("/app/simple");
		Message<String> message = MessageBuilder.withPayload("foo").setHeaders(headers).build();

		this.webSocketOutputChannel.send(message);

		SimpleController controller = this.serverContext.getBean(SimpleController.class);
		assertThat(controller.latch.await(20, TimeUnit.SECONDS)).isTrue();
		assertThat(controller.stompCommand).isEqualTo(StompCommand.SEND.name());
	}

	@Test
	public void sendMessageToControllerAndReceiveReplyViaTopic() throws Exception {
		Message<?> receive = this.webSocketEvents.receive(20000);
		assertThat(receive).isNotNull();
		Object event = receive.getPayload();
		assertThat(event).isInstanceOf(SessionConnectedEvent.class);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSubscriptionId("subs1");
		headers.setDestination("/topic/increment");
		headers.setReceipt("myReceipt");
		Message<byte[]> message = MessageBuilder.withPayload(ByteBuffer.allocate(0).array())
				.setHeaders(headers)
				.build();

		this.webSocketOutputChannel.send(message);

		receive = this.webSocketEvents.receive(20000);
		assertThat(receive).isNotNull();
		event = receive.getPayload();
		assertThat(event).isInstanceOf(ReceiptEvent.class);
		Message<?> receiptMessage = ((ReceiptEvent) event).getMessage();
		headers = StompHeaderAccessor.wrap(receiptMessage);
		assertThat(headers.getCommand()).isEqualTo(StompCommand.RECEIPT);
		assertThat(headers.getReceiptId()).isEqualTo("myReceipt");

		waitForSubscribe("/topic/increment");

		headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setSubscriptionId("subs1");
		headers.setDestination("/app/increment");
		Message<Integer> message2 = MessageBuilder.withPayload(5).setHeaders(headers).build();

		this.webSocketOutputChannel.send(message2);

		receive = webSocketInputChannel.receive(20000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("6");
	}

	@Test
	public void sendMessageToBrokerAndReceiveReplyViaTopic() throws Exception {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSubscriptionId("subs1");
		headers.setDestination("/topic/foo");
		Message<byte[]> message = MessageBuilder.withPayload(ByteBuffer.allocate(0).array())
				.setHeaders(headers)
				.build();

		headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setSubscriptionId("subs1");
		headers.setDestination("/topic/foo");
		Message<Integer> message2 = MessageBuilder.withPayload(10).setHeaders(headers).build();

		this.webSocketOutputChannel.send(message);

		waitForSubscribe("/topic/foo");

		this.webSocketOutputChannel.send(message2);

		Message<?> receive = webSocketInputChannel.receive(20000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("10");
	}

	@Test
	public void sendSubscribeToControllerAndReceiveReply() {
		String destHeader = "/app/number";

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSubscriptionId("subs1");
		headers.setDestination(destHeader);
		Message<byte[]> message = MessageBuilder.withPayload(ByteBuffer.allocate(0).array())
				.setHeaders(headers)
				.build();

		this.webSocketOutputChannel.send(message);

		Message<?> receive = webSocketInputChannel.receive(20000);
		assertThat(receive).isNotNull();

		StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(receive);

		assertThat(stompHeaderAccessor.getDestination())
				.as("Expected STOMP destination=/app/number, got " + stompHeaderAccessor).isEqualTo(destHeader);

		Object payload = receive.getPayload();

		assertThat(payload).as("Expected STOMP Payload=42, got " + payload).isEqualTo("42");
	}

	@Test
	public void handleExceptionAndSendToUser() throws Exception {
		String destHeader = "/user/queue/error";

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSubscriptionId("subs1");
		headers.setDestination(destHeader);
		Message<byte[]> message = MessageBuilder.withPayload(ByteBuffer.allocate(0).array())
				.setHeaders(headers)
				.build();

		headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setSubscriptionId("subs1");
		headers.setDestination("/app/exception");
		Message<String> message2 = MessageBuilder.withPayload("foo").setHeaders(headers).build();

		this.webSocketOutputChannel.send(message);

		waitForSubscribe("/queue/error-user" + this.clientWebSocketContainer.getSession(null).getId());

		this.webSocketOutputChannel.send(message2);


		Message<?> receive = webSocketInputChannel.receive(20000);
		assertThat(receive).isNotNull();

		StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(receive);

		assertThat(stompHeaderAccessor.getDestination())
				.as("Expected STOMP destination=/user/queue/error, got " + stompHeaderAccessor).isEqualTo(destHeader);

		assertThat(receive.getPayload()).isEqualTo("Got error: Bad input");
	}

	@Test
	public void sendMessageToGateway() throws Exception {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSubscriptionId("subs1");
		headers.setDestination("/user/queue/answer");
		Message<byte[]> message = MessageBuilder.withPayload(ByteBuffer.allocate(0).array())
				.setHeaders(headers)
				.build();

		headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setSubscriptionId("subs1");
		headers.setDestination("/app/greeting");
		Message<String> message2 = MessageBuilder.withPayload("Bob").setHeaders(headers).build();

		this.webSocketOutputChannel.send(message);

		waitForSubscribe("/queue/answer-user" + this.clientWebSocketContainer.getSession(null).getId());

		this.webSocketOutputChannel.send(message2);

		Message<?> receive = webSocketInputChannel.receive(20000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("Hello Bob");
	}

	private void waitForSubscribe(String destination) throws InterruptedException {
		SimpleBrokerMessageHandler serverBrokerMessageHandler =
				this.serverContext.getBean("simpleBrokerMessageHandler", SimpleBrokerMessageHandler.class);

		SubscriptionRegistry subscriptionRegistry = serverBrokerMessageHandler.getSubscriptionRegistry();

		int n = 0;
		while (!containsDestination(destination, subscriptionRegistry) && n++ < 100) {
			Thread.sleep(100);
		}

		assertThat(n < 100).as("The subscription for the '" + destination + "' destination hasn't been registered")
				.isTrue();
	}

	private boolean containsDestination(String destination, SubscriptionRegistry subscriptionRegistry) {
		StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
		stompHeaderAccessor.setDestination(destination);
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], stompHeaderAccessor.toMessageHeaders());
		MultiValueMap<String, String> subscriptions = subscriptionRegistry.findSubscriptions(message);
		return !subscriptions.isEmpty();
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
			return new SockJsClient(Collections.<Transport>singletonList(new WebSocketTransport(new StandardWebSocketClient())));
		}

		@Bean
		public IntegrationWebSocketContainer clientWebSocketContainer() {
			return new ClientWebSocketContainer(webSocketClient(), server().getWsBaseUrl() + "/ws");
		}

		@Bean
		public SubProtocolHandler stompSubProtocolHandler() {
			StompSubProtocolHandler stompSubProtocolHandler = new StompSubProtocolHandler();
			stompSubProtocolHandler.setEncoder(new ClientStompEncoder());
			return stompSubProtocolHandler;
		}

		@Bean
		public MessageChannel webSocketInputChannel() {
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

		@Bean
		public PollableChannel webSocketEvents() {
			return new QueueChannel();
		}

		@Bean
		public ApplicationListener<ApplicationEvent> webSocketEventListener() {
			ApplicationEventListeningMessageProducer producer = new ApplicationEventListeningMessageProducer();
			producer.setEventTypes(AbstractSubProtocolEvent.class);
			producer.setOutputChannel(webSocketEvents());
			return producer;
		}

	}

	// WebSocket Server part

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Controller
	private @interface IntegrationTestController {

	}

	@IntegrationTestController
	static class SimpleController {

		private final CountDownLatch latch = new CountDownLatch(1);

		private String stompCommand;

		@MessageMapping("/simple")
		public void handle(@Header("stompCommand") String stompCommand) {
			this.stompCommand = stompCommand;
			this.latch.countDown();
		}

		@MessageMapping("/exception")
		public void handleWithError() {
			throw new IllegalArgumentException("Bad input");
		}

		@MessageExceptionHandler
		@SendToUser("/queue/error")
		public String handleException(IllegalArgumentException ex) {
			return "Got error: " + ex.getMessage();
		}

	}

	@IntegrationTestController
	static class IncrementController {

		@MessageMapping("/increment")
		@SendTo
		public int handle(int i) {
			return i + 1;
		}

		@SubscribeMapping("/number")
		public int number() {
			return 42;
		}

	}


	@MessagingGateway
	@Controller
	public interface WebSocketGateway {

		@MessageMapping("/greeting")
		@SendToUser("/queue/answer")
		@Gateway(requestChannel = "greetingChannel")
		String greeting(String payload);

	}

	@Configuration
	@EnableWebSocketMessageBroker
	@EnableIntegration
	@ComponentScan(
			basePackageClasses = StompIntegrationTests.class,
			useDefaultFilters = false,
			includeFilters = @ComponentScan.Filter(IntegrationTestController.class))
	@IntegrationComponentScan
	static class ServerConfig implements WebSocketMessageBrokerConfigurer {

		private static final ExpressionParser expressionParser = new SpelExpressionParser();

		@Bean
		public MessageChannel greetingChannel() {
			return new DirectChannel();
		}

		@Bean
		@Transformer(inputChannel = "greetingChannel")
		public ExpressionEvaluatingTransformer greetingTransformer() {
			return new ExpressionEvaluatingTransformer(expressionParser.parseExpression("'Hello ' + payload"));
		}

		@Bean
		public DefaultHandshakeHandler handshakeHandler() {
			return new DefaultHandshakeHandler(new TomcatRequestUpgradeStrategy());
		}

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/ws").setHandshakeHandler(handshakeHandler()).withSockJS();
		}

		@Override
		public void configureMessageBroker(MessageBrokerRegistry configurer) {
			configurer.setApplicationDestinationPrefixes("/app");
			configurer.enableSimpleBroker("/topic", "/queue");
		}

		//SimpleBrokerMessageHandler doesn't support RECEIPT frame, hence we emulate it this way
		@Bean
		public ApplicationListener<SessionSubscribeEvent> webSocketEventListener(
				final AbstractSubscribableChannel clientOutboundChannel) {
			// Cannot be lambda because Java can't infer generic type from lambdas,
			// therefore we end up with ClassCastException for other event types
			return new ApplicationListener<SessionSubscribeEvent>() {

				@Override
				public void onApplicationEvent(SessionSubscribeEvent event) {
					Message<byte[]> message = event.getMessage();
					StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(message);
					if (stompHeaderAccessor.getReceipt() != null) {
						stompHeaderAccessor.setHeader("stompCommand", StompCommand.RECEIPT);
						stompHeaderAccessor.setReceiptId(stompHeaderAccessor.getReceipt());
						clientOutboundChannel.send(
								MessageBuilder.createMessage(new byte[0], stompHeaderAccessor.getMessageHeaders()));
					}
				}

			};
		}

	}

}

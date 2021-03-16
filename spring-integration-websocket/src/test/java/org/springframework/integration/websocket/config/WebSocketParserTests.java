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

package org.springframework.integration.websocket.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.converter.MapMessageConverter;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.websocket.IntegrationWebSocketContainer;
import org.springframework.integration.websocket.inbound.WebSocketInboundChannelAdapter;
import org.springframework.integration.websocket.outbound.WebSocketOutboundMessageHandler;
import org.springframework.integration.websocket.support.PassThruSubProtocolHandler;
import org.springframework.integration.websocket.support.SubProtocolHandlerRegistry;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * @author Artem Bilan
 *
 * @since 4.1
 */
@SpringJUnitConfig
@DirtiesContext
public class WebSocketParserTests {

	@Autowired
	@Qualifier("integrationWebSocketHandlerMapping")
	private HandlerMapping handlerMapping;

	@Autowired
	@Qualifier("serverWebSocketContainer")
	private IntegrationWebSocketContainer serverWebSocketContainer;

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired
	private HandshakeHandler handshakeHandler;

	@Autowired
	private HandshakeInterceptor handshakeInterceptor;

	@Autowired
	private SockJsMessageCodec sockJsMessageCodec;

	@Autowired
	@Qualifier("defaultInboundAdapter.adapter")
	private WebSocketInboundChannelAdapter defaultInboundAdapter;

	@Autowired
	private AbstractBrokerMessageHandler brokerHandler;

	@Autowired
	@Qualifier("clientWebSocketContainer")
	private IntegrationWebSocketContainer clientWebSocketContainer;

	@Autowired
	@Qualifier("simpleClientWebSocketContainer")
	private IntegrationWebSocketContainer simpleClientWebSocketContainer;

	@Autowired
	@Qualifier("customInboundAdapter")
	private WebSocketInboundChannelAdapter customInboundAdapter;

	@Autowired
	private MessageChannel clientInboundChannel;

	@Autowired
	private MessageChannel errorChannel;

	@Autowired
	private StompSubProtocolHandler stompSubProtocolHandler;

	@Autowired
	private SimpleMessageConverter simpleMessageConverter;

	@Autowired
	private MapMessageConverter mapMessageConverter;

	@Autowired
	private WebSocketClient webSocketClient;

	@Autowired
	@Qualifier("defaultOutboundAdapter.handler")
	private WebSocketOutboundMessageHandler defaultOutboundAdapter;

	@Autowired
	@Qualifier("customOutboundAdapter.handler")
	private WebSocketOutboundMessageHandler customOutboundAdapter;

	@Autowired
	private WebSocketHandlerDecoratorFactory decoratorFactory;

	@Test
	@SuppressWarnings("unckecked")
	public void testDefaultInboundChannelAdapterAndServerContainer() {
		Map<?, ?> urlMap = TestUtils.getPropertyValue(this.handlerMapping, "urlMap", Map.class);
		assertThat(urlMap.size()).isEqualTo(1);
		assertThat(urlMap.containsKey("/ws/**")).isTrue();
		Object mappedHandler = urlMap.get("/ws/**");
		//WebSocketHttpRequestHandler -> ExceptionWebSocketHandlerDecorator - > LoggingWebSocketHandlerDecorator
		// -> IntegrationWebSocketContainer$IntegrationWebSocketHandler
		assertThat(TestUtils.getPropertyValue(mappedHandler, "webSocketHandler.delegate.delegate"))
				.isSameAs(TestUtils.getPropertyValue(this.serverWebSocketContainer, "webSocketHandler"));
		assertThat(TestUtils.getPropertyValue(this.serverWebSocketContainer, "handshakeHandler"))
				.isSameAs(this.handshakeHandler);
		HandshakeInterceptor[] interceptors =
				TestUtils.getPropertyValue(this.serverWebSocketContainer, "interceptors", HandshakeInterceptor[].class);
		assertThat(interceptors).isNotNull();
		assertThat(interceptors.length).isEqualTo(1);
		assertThat(interceptors[0]).isSameAs(this.handshakeInterceptor);
		assertThat(TestUtils.getPropertyValue(this.serverWebSocketContainer, "sendTimeLimit")).isEqualTo(100);
		assertThat(TestUtils.getPropertyValue(this.serverWebSocketContainer, "sendBufferSizeLimit")).isEqualTo(100000);
		assertThat(TestUtils.getPropertyValue(this.serverWebSocketContainer, "origins", String[].class))
				.isEqualTo(new String[]{ "https://foo.com" });

		WebSocketHandlerDecoratorFactory[] decoratorFactories =
				TestUtils.getPropertyValue(this.serverWebSocketContainer, "decoratorFactories",
						WebSocketHandlerDecoratorFactory[].class);
		assertThat(decoratorFactories).isNotNull();
		assertThat(decoratorFactories.length).isEqualTo(1);
		assertThat(decoratorFactories[0]).isSameAs(this.decoratorFactory);

		TransportHandlingSockJsService sockJsService =
				TestUtils.getPropertyValue(mappedHandler, "sockJsService", TransportHandlingSockJsService.class);
		assertThat(sockJsService.getTaskScheduler()).isSameAs(this.taskScheduler);
		assertThat(sockJsService.getMessageCodec()).isSameAs(this.sockJsMessageCodec);
		Map<TransportType, TransportHandler> transportHandlers = sockJsService.getTransportHandlers();

		//If "handshake-handler" is provided, "transport-handlers" isn't allowed
		assertThat(transportHandlers.size()).isEqualTo(6);
		assertThat(TestUtils.getPropertyValue(transportHandlers.get(TransportType.WEBSOCKET), "handshakeHandler"))
				.isSameAs(this.handshakeHandler);
		assertThat(sockJsService.getDisconnectDelay()).isEqualTo(4000L);
		assertThat(sockJsService.getHeartbeatTime()).isEqualTo(30000L);
		assertThat(sockJsService.getHttpMessageCacheSize()).isEqualTo(10000);
		assertThat(sockJsService.getStreamBytesLimit()).isEqualTo(2000);
		assertThat(sockJsService.getSockJsClientLibraryUrl()).isEqualTo("https://foo.sock.js");
		assertThat(sockJsService.isSessionCookieNeeded()).isFalse();
		assertThat(sockJsService.isWebSocketEnabled()).isFalse();
		assertThat(sockJsService.shouldSuppressCors()).isTrue();

		assertThat(TestUtils.getPropertyValue(this.defaultInboundAdapter, "webSocketContainer"))
				.isSameAs(this.serverWebSocketContainer);
		assertThat(TestUtils.getPropertyValue(this.defaultInboundAdapter, "messageConverters")).isNull();
		assertThat(TestUtils.getPropertyValue(this.defaultInboundAdapter, "defaultConverters"))
				.isEqualTo(TestUtils.getPropertyValue(this.defaultInboundAdapter, "messageConverter.converters"));
		assertThat(TestUtils.getPropertyValue(this.defaultInboundAdapter, "payloadType", AtomicReference.class).get())
				.isEqualTo(String.class);
		assertThat(TestUtils.getPropertyValue(this.defaultInboundAdapter, "useBroker", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.defaultInboundAdapter, "brokerHandler"))
				.isSameAs(this.brokerHandler);

		SubProtocolHandlerRegistry subProtocolHandlerRegistry = TestUtils.getPropertyValue(this.defaultInboundAdapter,
				"subProtocolHandlerRegistry", SubProtocolHandlerRegistry.class);
		assertThat(TestUtils.getPropertyValue(subProtocolHandlerRegistry, "defaultProtocolHandler"))
				.isInstanceOf(PassThruSubProtocolHandler.class);
		assertThat(TestUtils.getPropertyValue(subProtocolHandlerRegistry, "protocolHandlers", Map.class).isEmpty())
				.isTrue();
	}

	@Test
	public void testCustomInboundChannelAdapterAndClientContainer() throws URISyntaxException {
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "outputChannel"))
				.isSameAs(this.clientInboundChannel);
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "errorChannel")).isSameAs(this.errorChannel);
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "webSocketContainer"))
				.isSameAs(this.clientWebSocketContainer);
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "messagingTemplate.sendTimeout"))
				.isEqualTo(2000L);
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "phase")).isEqualTo(200);
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "payloadType", AtomicReference.class).get())
				.isEqualTo(Integer.class);
		SubProtocolHandlerRegistry subProtocolHandlerRegistry = TestUtils.getPropertyValue(this.customInboundAdapter,
				"subProtocolHandlerRegistry", SubProtocolHandlerRegistry.class);
		assertThat(TestUtils.getPropertyValue(subProtocolHandlerRegistry,
				"defaultProtocolHandler")).isSameAs(this.stompSubProtocolHandler);
		Map<?, ?> protocolHandlers =
				TestUtils.getPropertyValue(subProtocolHandlerRegistry, "protocolHandlers", Map.class);
		assertThat(protocolHandlers.size()).isEqualTo(3);
		//PassThruSubProtocolHandler is ignored because it doesn't provide any 'protocol' by default.
		//See warn log message.
		for (Object handler : protocolHandlers.values()) {
			assertThat(handler).isSameAs(this.stompSubProtocolHandler);
		}

		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "mergeWithDefaultConverters", Boolean.class))
				.isTrue();
		CompositeMessageConverter compositeMessageConverter = TestUtils.getPropertyValue(this.customInboundAdapter,
				"messageConverter", CompositeMessageConverter.class);
		List<MessageConverter> converters = compositeMessageConverter.getConverters();
		assertThat(converters.size()).isEqualTo(5);
		assertThat(converters.get(0)).isSameAs(this.simpleMessageConverter);
		assertThat(converters.get(1)).isSameAs(this.mapMessageConverter);
		assertThat(converters.get(2)).isInstanceOf(StringMessageConverter.class);

		//Test ClientWebSocketContainer parser
		assertThat(TestUtils.getPropertyValue(this.clientWebSocketContainer, "messageListener"))
				.isSameAs(this.customInboundAdapter);
		assertThat(TestUtils.getPropertyValue(this.clientWebSocketContainer, "sendTimeLimit")).isEqualTo(100);
		assertThat(TestUtils.getPropertyValue(this.clientWebSocketContainer, "sendBufferSizeLimit")).isEqualTo(1000);
		assertThat(TestUtils.getPropertyValue(this.clientWebSocketContainer, "connectionManager.uri", URI.class))
				.isEqualTo(new URI("ws://foo.bar/ws?service=user"));
		assertThat(TestUtils.getPropertyValue(this.clientWebSocketContainer, "connectionManager.client"))
				.isSameAs(this.webSocketClient);
		assertThat(TestUtils.getPropertyValue(this.clientWebSocketContainer, "connectionManager.phase")).isEqualTo(100);
		WebSocketHttpHeaders headers = TestUtils.getPropertyValue(this.clientWebSocketContainer, "headers",
				WebSocketHttpHeaders.class);
		assertThat(headers.getOrigin()).isEqualTo("FOO");
		assertThat(headers.get("FOO")).isEqualTo(Arrays.asList("BAR", "baz"));

		assertThat(TestUtils.getPropertyValue(this.simpleClientWebSocketContainer, "sendTimeLimit"))
				.isEqualTo(10 * 1000);
		assertThat(TestUtils.getPropertyValue(this.simpleClientWebSocketContainer, "sendBufferSizeLimit"))
				.isEqualTo(512 * 1024);
		assertThat(TestUtils.getPropertyValue(this.simpleClientWebSocketContainer, "connectionManager.uri", URI.class))
				.isEqualTo(new URI("ws://foo.bar"));
		assertThat(TestUtils.getPropertyValue(this.simpleClientWebSocketContainer, "connectionManager.client"))
				.isSameAs(this.webSocketClient);
		assertThat(TestUtils.getPropertyValue(this.simpleClientWebSocketContainer, "connectionManager.phase"))
				.isEqualTo(Integer.MAX_VALUE);
		assertThat(TestUtils.getPropertyValue(this.simpleClientWebSocketContainer,
				"connectionManager.autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.simpleClientWebSocketContainer, "headers",
				WebSocketHttpHeaders.class).isEmpty()).isTrue();
	}

	@Test
	public void testDefaultOutboundChannelAdapter() {
		assertThat(TestUtils.getPropertyValue(this.defaultOutboundAdapter, "webSocketContainer"))
				.isSameAs(this.serverWebSocketContainer);
		assertThat(TestUtils.getPropertyValue(this.defaultOutboundAdapter, "messageConverters")).isNull();
		assertThat(TestUtils.getPropertyValue(this.defaultOutboundAdapter, "defaultConverters"))
				.isEqualTo(TestUtils.getPropertyValue(this.defaultOutboundAdapter, "messageConverter.converters"));
		SubProtocolHandlerRegistry subProtocolHandlerRegistry = TestUtils.getPropertyValue(this.defaultOutboundAdapter,
				"subProtocolHandlerRegistry", SubProtocolHandlerRegistry.class);
		assertThat(TestUtils.getPropertyValue(subProtocolHandlerRegistry, "defaultProtocolHandler"))
				.isInstanceOf(PassThruSubProtocolHandler.class);
		assertThat(TestUtils.getPropertyValue(subProtocolHandlerRegistry, "protocolHandlers", Map.class).isEmpty())
				.isTrue();
		assertThat(TestUtils.getPropertyValue(this.defaultOutboundAdapter, "client", Boolean.class)).isFalse();
	}

	@Test
	public void testCustomOutboundChannelAdapter() throws URISyntaxException {
		assertThat(TestUtils.getPropertyValue(this.customOutboundAdapter, "webSocketContainer"))
				.isSameAs(this.clientWebSocketContainer);

		SubProtocolHandlerRegistry subProtocolHandlerRegistry = TestUtils.getPropertyValue(this.customOutboundAdapter,
				"subProtocolHandlerRegistry", SubProtocolHandlerRegistry.class);
		assertThat(TestUtils.getPropertyValue(subProtocolHandlerRegistry,
				"defaultProtocolHandler")).isSameAs(this.stompSubProtocolHandler);
		Map<?, ?> protocolHandlers =
				TestUtils.getPropertyValue(subProtocolHandlerRegistry, "protocolHandlers", Map.class);
		assertThat(protocolHandlers.size()).isEqualTo(3);
		//PassThruSubProtocolHandler is ignored because it doesn't provide any 'protocol' by default.
		//See warn log message.
		for (Object handler : protocolHandlers.values()) {
			assertThat(handler).isSameAs(this.stompSubProtocolHandler);
		}

		assertThat(TestUtils.getPropertyValue(this.customOutboundAdapter, "mergeWithDefaultConverters", Boolean.class))
				.isTrue();
		CompositeMessageConverter compositeMessageConverter = TestUtils.getPropertyValue(this.customOutboundAdapter,
				"messageConverter", CompositeMessageConverter.class);
		List<MessageConverter> converters = compositeMessageConverter.getConverters();
		assertThat(converters.size()).isEqualTo(5);
		assertThat(converters.get(0)).isSameAs(this.simpleMessageConverter);
		assertThat(converters.get(1)).isSameAs(this.mapMessageConverter);
		assertThat(converters.get(2)).isInstanceOf(StringMessageConverter.class);
		assertThat(TestUtils.getPropertyValue(this.customOutboundAdapter, "client", Boolean.class)).isTrue();
	}

	private static class TestWebSocketHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {

		@Override
		public WebSocketHandler decorate(WebSocketHandler handler) {
			return handler;
		}

	}

}

/*
 * Copyright 2014-present the original author or authors.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;
import org.springframework.web.socket.sockjs.transport.TransportType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Julian Koch
 * @author Jooyoung Pyoung
 * @author Glenn Renfro
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
		Map<?, ?> urlMap = TestUtils.getPropertyValue(this.handlerMapping, "urlMap");
		assertThat(urlMap.size()).isEqualTo(1);
		assertThat(urlMap.containsKey("/ws/**")).isTrue();
		Object mappedHandler = urlMap.get("/ws/**");
		//WebSocketHttpRequestHandler -> ExceptionWebSocketHandlerDecorator - > LoggingWebSocketHandlerDecorator
		// -> IntegrationWebSocketContainer$IntegrationWebSocketHandler
		assertThat(TestUtils.<Object>getPropertyValue(mappedHandler, "webSocketHandler.delegate.delegate"))
				.isSameAs(TestUtils.getPropertyValue(this.serverWebSocketContainer, "webSocketHandler"));
		assertThat(TestUtils.<Object>getPropertyValue(this.serverWebSocketContainer, "handshakeHandler"))
				.isSameAs(this.handshakeHandler);
		HandshakeInterceptor[] interceptors =
				TestUtils.getPropertyValue(this.serverWebSocketContainer, "interceptors");
		assertThat(interceptors).isNotNull();
		assertThat(interceptors.length).isEqualTo(1);
		assertThat(interceptors[0]).isSameAs(this.handshakeInterceptor);
		assertThat(TestUtils.<Integer>getPropertyValue(this.serverWebSocketContainer, "sendTimeLimit")).isEqualTo(100);
		assertThat(TestUtils.<Integer>getPropertyValue(this.serverWebSocketContainer, "sendBufferSizeLimit"))
				.isEqualTo(100000);
		assertThat(TestUtils.<Object>getPropertyValue(this.serverWebSocketContainer, "sendBufferOverflowStrategy"))
				.isEqualTo(ConcurrentWebSocketSessionDecorator.OverflowStrategy.DROP);
		assertThat(TestUtils.<String[]>getPropertyValue(this.serverWebSocketContainer, "origins"))
				.isEqualTo(new String[] {"https://foo.com"});

		WebSocketHandlerDecoratorFactory[] decoratorFactories =
				TestUtils.getPropertyValue(this.serverWebSocketContainer, "decoratorFactories");
		assertThat(decoratorFactories).isNotNull();
		assertThat(decoratorFactories.length).isEqualTo(1);
		assertThat(decoratorFactories[0]).isSameAs(this.decoratorFactory);

		TransportHandlingSockJsService sockJsService = TestUtils.getPropertyValue(mappedHandler, "sockJsService");
		assertThat(sockJsService.getTaskScheduler()).isSameAs(this.taskScheduler);
		assertThat(sockJsService.getMessageCodec()).isSameAs(this.sockJsMessageCodec);
		Map<TransportType, TransportHandler> transportHandlers = sockJsService.getTransportHandlers();

		//If "handshake-handler" is provided, "transport-handlers" isn't allowed
		assertThat(transportHandlers.size()).isEqualTo(6);
		assertThat(TestUtils.<Object>getPropertyValue(
				transportHandlers.get(TransportType.WEBSOCKET), "handshakeHandler"))
				.isSameAs(this.handshakeHandler);
		assertThat(sockJsService.getDisconnectDelay()).isEqualTo(4000L);
		assertThat(sockJsService.getHeartbeatTime()).isEqualTo(30000L);
		assertThat(sockJsService.getHttpMessageCacheSize()).isEqualTo(10000);
		assertThat(sockJsService.getStreamBytesLimit()).isEqualTo(2000);
		assertThat(sockJsService.getSockJsClientLibraryUrl()).isEqualTo("https://foo.sock.js");
		assertThat(sockJsService.isSessionCookieNeeded()).isFalse();
		assertThat(sockJsService.isWebSocketEnabled()).isFalse();
		assertThat(sockJsService.shouldSuppressCors()).isTrue();

		assertThat(TestUtils.<Object>getPropertyValue(this.defaultInboundAdapter, "webSocketContainer"))
				.isSameAs(this.serverWebSocketContainer);
		assertThat(TestUtils.<Object>getPropertyValue(this.defaultInboundAdapter, "messageConverters")).isNull();
		assertThat(TestUtils.<List<?>>getPropertyValue(this.defaultInboundAdapter, "defaultConverters"))
				.isEqualTo(TestUtils.getPropertyValue(this.defaultInboundAdapter, "messageConverter.converters"));
		assertThat(TestUtils.<Object>getPropertyValue(this.defaultInboundAdapter, "payloadType"))
				.isEqualTo(String.class);
		assertThat(TestUtils.<Boolean>getPropertyValue(this.defaultInboundAdapter, "useBroker")).isTrue();
		assertThat(TestUtils.<Object>getPropertyValue(this.defaultInboundAdapter, "brokerHandler"))
				.isSameAs(this.brokerHandler);

		SubProtocolHandlerRegistry subProtocolHandlerRegistry =
				TestUtils.getPropertyValue(this.defaultInboundAdapter, "subProtocolHandlerRegistry");
		assertThat(TestUtils.<Object>getPropertyValue(subProtocolHandlerRegistry, "defaultProtocolHandler"))
				.isInstanceOf(PassThruSubProtocolHandler.class);
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(subProtocolHandlerRegistry, "protocolHandlers").isEmpty())
				.isTrue();
	}

	@Test
	public void testCustomInboundChannelAdapterAndClientContainer() throws URISyntaxException {
		assertThat(TestUtils.<MessageChannel>getPropertyValue(this.customInboundAdapter, "outputChannel"))
				.isSameAs(this.clientInboundChannel);
		assertThat(TestUtils.<MessageChannel>getPropertyValue(this.customInboundAdapter, "errorChannel"))
				.isSameAs(this.errorChannel);
		assertThat(TestUtils.<IntegrationWebSocketContainer>getPropertyValue(this.customInboundAdapter,
				"webSocketContainer")).isSameAs(this.clientWebSocketContainer);
		assertThat(TestUtils.<Long>getPropertyValue(this.customInboundAdapter, "messagingTemplate.sendTimeout"))
				.isEqualTo(2000L);
		assertThat(TestUtils.<Integer>getPropertyValue(this.customInboundAdapter, "phase")).isEqualTo(200);
		assertThat(TestUtils.<Boolean>getPropertyValue(this.customInboundAdapter, "autoStartup")).isFalse();
		assertThat(TestUtils.<Object>getPropertyValue(this.customInboundAdapter, "payloadType"))
				.isEqualTo(Integer.class);
		SubProtocolHandlerRegistry subProtocolHandlerRegistry =
				TestUtils.getPropertyValue(this.customInboundAdapter, "subProtocolHandlerRegistry");
		assertThat(TestUtils.<Object>getPropertyValue(subProtocolHandlerRegistry,
				"defaultProtocolHandler")).isSameAs(this.stompSubProtocolHandler);
		Map<?, ?> protocolHandlers = TestUtils.getPropertyValue(subProtocolHandlerRegistry, "protocolHandlers");
		assertThat(protocolHandlers.size()).isEqualTo(3);
		//PassThruSubProtocolHandler is ignored because it doesn't provide any 'protocol' by default.
		//See warn log message.
		for (Object handler : protocolHandlers.values()) {
			assertThat(handler).isSameAs(this.stompSubProtocolHandler);
		}

		assertThat(TestUtils.<Boolean>getPropertyValue(this.customInboundAdapter, "mergeWithDefaultConverters"))
				.isTrue();
		CompositeMessageConverter compositeMessageConverter =
				TestUtils.getPropertyValue(this.customInboundAdapter, "messageConverter");
		List<MessageConverter> converters = compositeMessageConverter.getConverters();
		assertThat(converters.size()).isEqualTo(5);
		assertThat(converters.get(0)).isSameAs(this.simpleMessageConverter);
		assertThat(converters.get(1)).isSameAs(this.mapMessageConverter);
		assertThat(converters.get(2)).isInstanceOf(StringMessageConverter.class);

		//Test ClientWebSocketContainer parser
		assertThat(TestUtils.<WebSocketInboundChannelAdapter>getPropertyValue(this.clientWebSocketContainer,
				"messageListener")).isSameAs(this.customInboundAdapter);
		assertThat(TestUtils.<Integer>getPropertyValue(this.clientWebSocketContainer, "sendTimeLimit")).isEqualTo(100);
		assertThat(TestUtils.<Integer>getPropertyValue(this.clientWebSocketContainer, "sendBufferSizeLimit"))
				.isEqualTo(1000);
		assertThat(TestUtils.<ConcurrentWebSocketSessionDecorator.OverflowStrategy>getPropertyValue(
				this.clientWebSocketContainer, "sendBufferOverflowStrategy"))
				.isEqualTo(ConcurrentWebSocketSessionDecorator.OverflowStrategy.DROP);
		assertThat(TestUtils.<URI>getPropertyValue(this.clientWebSocketContainer, "connectionManager.uri"))
				.isEqualTo(new URI("ws://foo.bar/ws?service=user"));
		assertThat(TestUtils.<Object>getPropertyValue(this.clientWebSocketContainer, "connectionManager.client"))
				.isSameAs(this.webSocketClient);
		assertThat(TestUtils.<Integer>getPropertyValue(this.clientWebSocketContainer, "connectionManager.phase"))
				.isEqualTo(100);
		WebSocketHttpHeaders headers = TestUtils.getPropertyValue(this.clientWebSocketContainer, "headers");
		assertThat(headers.getOrigin()).isEqualTo("FOO");
		assertThat(headers.get("FOO")).isEqualTo(Arrays.asList("BAR", "baz"));

		assertThat(TestUtils.<Integer>getPropertyValue(this.simpleClientWebSocketContainer, "sendTimeLimit"))
				.isEqualTo(10 * 1000);
		assertThat(TestUtils.<Integer>getPropertyValue(this.simpleClientWebSocketContainer, "sendBufferSizeLimit"))
				.isEqualTo(512 * 1024);
		assertThat(TestUtils.<Object>getPropertyValue(this.simpleClientWebSocketContainer,
				"sendBufferOverflowStrategy")).isNull();
		assertThat(TestUtils.<URI>getPropertyValue(this.simpleClientWebSocketContainer, "connectionManager.uri"))
				.isEqualTo(new URI("ws://foo.bar"));
		assertThat(TestUtils.<Object>getPropertyValue(this.simpleClientWebSocketContainer, "connectionManager.client"))
				.isSameAs(this.webSocketClient);
		assertThat(TestUtils.<Integer>getPropertyValue(this.simpleClientWebSocketContainer, "connectionManager.phase"))
				.isEqualTo(Integer.MAX_VALUE);
		assertThat(TestUtils.<Boolean>getPropertyValue(this.simpleClientWebSocketContainer, "connectionManager.autoStartup"))
				.isFalse();
		assertThat(TestUtils.<WebSocketHttpHeaders>getPropertyValue(this.simpleClientWebSocketContainer, "headers").isEmpty()).isTrue();
	}

	@Test
	public void testDefaultOutboundChannelAdapter() {
		assertThat(TestUtils.<Object>getPropertyValue(this.defaultOutboundAdapter, "webSocketContainer"))
				.isSameAs(this.serverWebSocketContainer);
		assertThat(TestUtils.<Object>getPropertyValue(this.defaultOutboundAdapter, "messageConverters")).isNull();
		assertThat(TestUtils.<Object>getPropertyValue(this.defaultOutboundAdapter, "defaultConverters"))
				.isEqualTo(TestUtils.getPropertyValue(this.defaultOutboundAdapter, "messageConverter.converters"));
		SubProtocolHandlerRegistry subProtocolHandlerRegistry =
				TestUtils.getPropertyValue(this.defaultOutboundAdapter, "subProtocolHandlerRegistry");
		assertThat(TestUtils.<PassThruSubProtocolHandler>getPropertyValue(subProtocolHandlerRegistry, "defaultProtocolHandler"))
				.isInstanceOf(PassThruSubProtocolHandler.class);
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(subProtocolHandlerRegistry, "protocolHandlers").isEmpty())
				.isTrue();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.defaultOutboundAdapter, "client")).isFalse();
	}

	@Test
	public void testCustomOutboundChannelAdapter() throws URISyntaxException {
		assertThat(TestUtils.<IntegrationWebSocketContainer>getPropertyValue(this.customOutboundAdapter,
				"webSocketContainer")).isSameAs(this.clientWebSocketContainer);

		SubProtocolHandlerRegistry subProtocolHandlerRegistry = TestUtils.getPropertyValue(this.customOutboundAdapter,
				"subProtocolHandlerRegistry");
		assertThat(TestUtils.<StompSubProtocolHandler>getPropertyValue(subProtocolHandlerRegistry,
				"defaultProtocolHandler")).isSameAs(this.stompSubProtocolHandler);
		Map<?, ?> protocolHandlers = TestUtils.getPropertyValue(subProtocolHandlerRegistry, "protocolHandlers");
		assertThat(protocolHandlers.size()).isEqualTo(3);
		//PassThruSubProtocolHandler is ignored because it doesn't provide any 'protocol' by default.
		//See warn log message.
		for (Object handler : protocolHandlers.values()) {
			assertThat(handler).isSameAs(this.stompSubProtocolHandler);
		}

		assertThat(TestUtils.<Boolean>getPropertyValue(this.customOutboundAdapter, "mergeWithDefaultConverters"))
				.isTrue();
		CompositeMessageConverter compositeMessageConverter =
				TestUtils.getPropertyValue(this.customOutboundAdapter, "messageConverter");
		List<MessageConverter> converters = compositeMessageConverter.getConverters();
		assertThat(converters.size()).isEqualTo(5);
		assertThat(converters.get(0)).isSameAs(this.simpleMessageConverter);
		assertThat(converters.get(1)).isSameAs(this.mapMessageConverter);
		assertThat(converters.get(2)).isInstanceOf(StringMessageConverter.class);
		assertThat(TestUtils.<Boolean>getPropertyValue(this.customOutboundAdapter, "client")).isTrue();
	}

	private static class TestWebSocketHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {

		@Override
		public WebSocketHandler decorate(WebSocketHandler handler) {
			return handler;
		}

	}

}

/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.websocket.config;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * @author Artem Bilan
 * @since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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

	@Test
	public void testDefaultInboundChannelAdapterAndServerContainer() {
		Map<?, ?> urlMap = TestUtils.getPropertyValue(this.handlerMapping, "urlMap", Map.class);
		assertEquals(1, urlMap.size());
		assertTrue(urlMap.containsKey("/ws/**"));
		Object mappedHandler = urlMap.get("/ws/**");
		//WebSocketHttpRequestHandler -> ExceptionWebSocketHandlerDecorator - > LoggingWebSocketHandlerDecorator
		// -> IntegrationWebSocketContainer$IntegrationWebSocketHandler
		assertSame(TestUtils.getPropertyValue(this.serverWebSocketContainer, "webSocketHandler"),
				TestUtils.getPropertyValue(mappedHandler, "webSocketHandler.delegate.delegate"));
		assertSame(this.handshakeHandler,
				TestUtils.getPropertyValue(this.serverWebSocketContainer, "handshakeHandler"));
		HandshakeInterceptor[] interceptors =
				TestUtils.getPropertyValue(this.serverWebSocketContainer, "interceptors", HandshakeInterceptor[].class);
		assertEquals(1, interceptors.length);
		assertSame(this.handshakeInterceptor, interceptors[0]);
		assertEquals(100, TestUtils.getPropertyValue(this.serverWebSocketContainer, "sendTimeLimit"));
		assertEquals(100000, TestUtils.getPropertyValue(this.serverWebSocketContainer, "sendBufferSizeLimit"));

		TransportHandlingSockJsService sockJsService =
				TestUtils.getPropertyValue(mappedHandler, "sockJsService", TransportHandlingSockJsService.class);
		assertSame(this.taskScheduler, sockJsService.getTaskScheduler());
		assertSame(this.sockJsMessageCodec, sockJsService.getMessageCodec());
		Map<TransportType, TransportHandler> transportHandlers = sockJsService.getTransportHandlers();

		//If "handshake-handler" is provided, "transport-handlers" isn't allowed
		assertEquals(8, transportHandlers.size());
		assertSame(this.handshakeHandler,
				TestUtils.getPropertyValue(transportHandlers.get(TransportType.WEBSOCKET), "handshakeHandler"));
		assertEquals(4000L, sockJsService.getDisconnectDelay());
		assertEquals(30000L, sockJsService.getHeartbeatTime());
		assertEquals(10000, sockJsService.getHttpMessageCacheSize());
		assertEquals(2000, sockJsService.getStreamBytesLimit());
		assertEquals("https://foo.sock.js", sockJsService.getSockJsClientLibraryUrl());
		assertFalse(sockJsService.isSessionCookieNeeded());
		assertFalse(sockJsService.isWebSocketEnabled());

		assertSame(this.serverWebSocketContainer,
				TestUtils.getPropertyValue(this.defaultInboundAdapter, "webSocketContainer"));
		assertNull(TestUtils.getPropertyValue(this.defaultInboundAdapter, "messageConverters"));
		assertEquals(TestUtils.getPropertyValue(this.defaultInboundAdapter, "messageConverter.converters"),
				TestUtils.getPropertyValue(this.defaultInboundAdapter, "defaultConverters"));
		assertEquals(String.class,
				TestUtils.getPropertyValue(this.defaultInboundAdapter, "payloadType", AtomicReference.class).get());
		assertTrue(TestUtils.getPropertyValue(this.defaultInboundAdapter, "useBroker", Boolean.class));
		assertSame(this.brokerHandler, TestUtils.getPropertyValue(this.defaultInboundAdapter, "brokerHandler"));

		SubProtocolHandlerRegistry subProtocolHandlerRegistry = TestUtils.getPropertyValue(this.defaultInboundAdapter,
				"subProtocolHandlerRegistry", SubProtocolHandlerRegistry.class);
		assertThat(TestUtils.getPropertyValue(subProtocolHandlerRegistry, "defaultProtocolHandler"),
				instanceOf(PassThruSubProtocolHandler.class));
		assertTrue(TestUtils.getPropertyValue(subProtocolHandlerRegistry, "protocolHandlers", Map.class).isEmpty());
	}

	@Test
	public void testCustomInboundChannelAdapterAndClientContainer() throws URISyntaxException {
		assertSame(this.clientInboundChannel, TestUtils.getPropertyValue(this.customInboundAdapter, "outputChannel"));
		assertSame(this.errorChannel, TestUtils.getPropertyValue(this.customInboundAdapter, "errorChannel"));
		assertSame(this.clientWebSocketContainer,
				TestUtils.getPropertyValue(this.customInboundAdapter, "webSocketContainer"));
		assertEquals(2000L, TestUtils.getPropertyValue(this.customInboundAdapter, "messagingTemplate.sendTimeout"));
		assertEquals(200, TestUtils.getPropertyValue(this.customInboundAdapter, "phase"));
		assertFalse(TestUtils.getPropertyValue(this.customInboundAdapter, "autoStartup", Boolean.class));
		assertEquals(Integer.class,
				TestUtils.getPropertyValue(this.customInboundAdapter, "payloadType", AtomicReference.class).get());
		SubProtocolHandlerRegistry subProtocolHandlerRegistry = TestUtils.getPropertyValue(this.customInboundAdapter,
				"subProtocolHandlerRegistry", SubProtocolHandlerRegistry.class);
		assertSame(this.stompSubProtocolHandler, TestUtils.getPropertyValue(subProtocolHandlerRegistry,
				"defaultProtocolHandler"));
		Map<?, ?> protocolHandlers =
				TestUtils.getPropertyValue(subProtocolHandlerRegistry, "protocolHandlers", Map.class);
		assertEquals(3, protocolHandlers.size());
		//PassThruSubProtocolHandler is ignored because it doesn't provide any 'protocol' by default.
		//See warn log message.
		for (Object handler : protocolHandlers.values()) {
			assertSame(this.stompSubProtocolHandler, handler);
		}

		assertTrue(TestUtils.getPropertyValue(this.customInboundAdapter, "mergeWithDefaultConverters", Boolean.class));
		CompositeMessageConverter compositeMessageConverter = TestUtils.getPropertyValue(this.customInboundAdapter,
				"messageConverter", CompositeMessageConverter.class);
		List<MessageConverter> converters = compositeMessageConverter.getConverters();
		assertEquals(5, converters.size());
		assertSame(this.simpleMessageConverter, converters.get(0));
		assertSame(this.mapMessageConverter, converters.get(1));
		assertThat(converters.get(2), instanceOf(StringMessageConverter.class));

		//Test ClientWebSocketContainer parser
		assertSame(this.customInboundAdapter,
				TestUtils.getPropertyValue(this.clientWebSocketContainer, "messageListener"));
		assertEquals(100, TestUtils.getPropertyValue(this.clientWebSocketContainer, "sendTimeLimit"));
		assertEquals(1000, TestUtils.getPropertyValue(this.clientWebSocketContainer, "sendBufferSizeLimit"));
		assertEquals(new URI("ws://foo.bar/ws?service=user"),
				TestUtils.getPropertyValue(this.clientWebSocketContainer, "connectionManager.uri", URI.class));
		assertSame(this.webSocketClient,
				TestUtils.getPropertyValue(this.clientWebSocketContainer, "connectionManager.client"));
		assertEquals(100, TestUtils.getPropertyValue(this.clientWebSocketContainer, "connectionManager.phase"));
		WebSocketHttpHeaders headers = TestUtils.getPropertyValue(this.clientWebSocketContainer, "headers",
				WebSocketHttpHeaders.class);
		assertEquals("FOO", headers.getOrigin());
		assertEquals(Arrays.asList("BAR", "baz"), headers.get("FOO"));
	}

	@Test
	public void testDefaultOutboundChannelAdapter() {
		assertSame(this.serverWebSocketContainer,
				TestUtils.getPropertyValue(this.defaultOutboundAdapter, "webSocketContainer"));
		assertNull(TestUtils.getPropertyValue(this.defaultOutboundAdapter, "messageConverters"));
		assertEquals(TestUtils.getPropertyValue(this.defaultOutboundAdapter, "messageConverter.converters"),
				TestUtils.getPropertyValue(this.defaultOutboundAdapter, "defaultConverters"));
		SubProtocolHandlerRegistry subProtocolHandlerRegistry = TestUtils.getPropertyValue(this.defaultOutboundAdapter,
				"subProtocolHandlerRegistry", SubProtocolHandlerRegistry.class);
		assertThat(TestUtils.getPropertyValue(subProtocolHandlerRegistry, "defaultProtocolHandler"),
				instanceOf(PassThruSubProtocolHandler.class));
		assertTrue(TestUtils.getPropertyValue(subProtocolHandlerRegistry, "protocolHandlers", Map.class).isEmpty());
		assertFalse(TestUtils.getPropertyValue(this.defaultOutboundAdapter, "client", Boolean.class));
	}

	@Test
	public void testCustomOutboundChannelAdapter() throws URISyntaxException {
		assertSame(this.clientWebSocketContainer,
				TestUtils.getPropertyValue(this.customOutboundAdapter, "webSocketContainer"));

		SubProtocolHandlerRegistry subProtocolHandlerRegistry = TestUtils.getPropertyValue(this.customOutboundAdapter,
				"subProtocolHandlerRegistry", SubProtocolHandlerRegistry.class);
		assertSame(this.stompSubProtocolHandler, TestUtils.getPropertyValue(subProtocolHandlerRegistry,
				"defaultProtocolHandler"));
		Map<?, ?> protocolHandlers =
				TestUtils.getPropertyValue(subProtocolHandlerRegistry, "protocolHandlers", Map.class);
		assertEquals(3, protocolHandlers.size());
		//PassThruSubProtocolHandler is ignored because it doesn't provide any 'protocol' by default.
		//See warn log message.
		for (Object handler : protocolHandlers.values()) {
			assertSame(this.stompSubProtocolHandler, handler);
		}

		assertTrue(TestUtils.getPropertyValue(this.customOutboundAdapter, "mergeWithDefaultConverters", Boolean.class));
		CompositeMessageConverter compositeMessageConverter = TestUtils.getPropertyValue(this.customOutboundAdapter,
				"messageConverter", CompositeMessageConverter.class);
		List<MessageConverter> converters = compositeMessageConverter.getConverters();
		assertEquals(5, converters.size());
		assertSame(this.simpleMessageConverter, converters.get(0));
		assertSame(this.mapMessageConverter, converters.get(1));
		assertThat(converters.get(2), instanceOf(StringMessageConverter.class));
		assertTrue(TestUtils.getPropertyValue(this.customOutboundAdapter, "client", Boolean.class));
	}

}

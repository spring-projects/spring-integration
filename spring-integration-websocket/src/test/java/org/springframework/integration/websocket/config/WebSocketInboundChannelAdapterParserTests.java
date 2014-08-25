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
import org.springframework.integration.websocket.support.PassThruSubProtocolHandler;
import org.springframework.integration.websocket.support.SubProtocolHandlerRegistry;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;

/**
 * @author Artem Bilan
 * @since 4.1
 */

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class WebSocketInboundChannelAdapterParserTests {

	@Autowired
	@Qualifier("serverWebSocketContainer")
	private IntegrationWebSocketContainer serverWebSocketContainer;

	@Autowired
	private HandlerMapping handlerMapping;

	@Autowired
	@Qualifier("defaultAdapter.adapter")
	private WebSocketInboundChannelAdapter defaultAdapter;

	@Autowired
	@Qualifier("clientWebSocketContainer")
	private IntegrationWebSocketContainer clientWebSocketContainer;

	@Autowired
	@Qualifier("customAdapter")
	private WebSocketInboundChannelAdapter customAdapter;

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

	@Test
	public void testDefaultAdapter() {
		Map<?, ?> urlMap = TestUtils.getPropertyValue(this.handlerMapping, "urlMap", Map.class);
		assertEquals(1, urlMap.size());
		assertTrue(urlMap.containsKey("/ws"));
		Object mappedHandler = urlMap.get("/ws");
		//WebSocketHttpRequestHandler -> ExceptionWebSocketHandlerDecorator - > LoggingWebSocketHandlerDecorator
		// -> IntegrationWebSocketContainer$IntegrationWebSocketHandler
		assertSame(TestUtils.getPropertyValue(this.serverWebSocketContainer, "webSocketHandler"),
				TestUtils.getPropertyValue(mappedHandler, "wsHandler.delegate.delegate"));
		assertSame(this.serverWebSocketContainer,
				TestUtils.getPropertyValue(this.defaultAdapter, "webSocketContainer"));
		assertNull(TestUtils.getPropertyValue(this.defaultAdapter, "messageConverters"));
		assertEquals(TestUtils.getPropertyValue(this.defaultAdapter, "messageConverter.converters"),
				TestUtils.getPropertyValue(this.defaultAdapter, "defaultConverters"));
		assertEquals(String.class,
				TestUtils.getPropertyValue(this.defaultAdapter, "payloadType", AtomicReference.class).get());
		SubProtocolHandlerRegistry subProtocolHandlerRegistry = TestUtils.getPropertyValue(this.defaultAdapter,
				"subProtocolHandlerRegistry", SubProtocolHandlerRegistry.class);
		assertThat(TestUtils.getPropertyValue(subProtocolHandlerRegistry, "defaultProtocolHandler"),
				instanceOf(PassThruSubProtocolHandler.class));
		assertTrue(TestUtils.getPropertyValue(subProtocolHandlerRegistry, "protocolHandlers", Map.class).isEmpty());
	}

	@Test
	public void testCustomAdapter() throws URISyntaxException {
		assertSame(this.clientInboundChannel, TestUtils.getPropertyValue(this.customAdapter, "outputChannel"));
		assertSame(this.errorChannel, TestUtils.getPropertyValue(this.customAdapter, "errorChannel"));
		assertSame(this.clientWebSocketContainer, TestUtils.getPropertyValue(this.customAdapter, "webSocketContainer"));
		assertEquals(2000L, TestUtils.getPropertyValue(this.customAdapter, "messagingTemplate.sendTimeout"));
		assertEquals(200, TestUtils.getPropertyValue(this.customAdapter, "phase"));
		assertFalse(TestUtils.getPropertyValue(this.customAdapter, "autoStartup", Boolean.class));
		assertEquals(Integer.class,
				TestUtils.getPropertyValue(this.customAdapter, "payloadType", AtomicReference.class).get());
		SubProtocolHandlerRegistry subProtocolHandlerRegistry = TestUtils.getPropertyValue(this.customAdapter,
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

		assertTrue(TestUtils.getPropertyValue(this.customAdapter, "mergeWithDefaultConverters", Boolean.class));
		CompositeMessageConverter compositeMessageConverter =
				TestUtils.getPropertyValue(this.customAdapter, "messageConverter", CompositeMessageConverter.class);
		List<MessageConverter> converters = compositeMessageConverter.getConverters();
		assertEquals(5, converters.size());
		assertSame(this.simpleMessageConverter, converters.get(0));
		assertSame(this.mapMessageConverter, converters.get(1));
		assertThat(converters.get(2), instanceOf(StringMessageConverter.class));

		//Test ClientWebSocketContainer parser
		assertSame(this.customAdapter, TestUtils.getPropertyValue(this.clientWebSocketContainer, "messageListener"));
		assertEquals(100, TestUtils.getPropertyValue(this.clientWebSocketContainer, "sendTimeLimit"));
		assertEquals(1000, TestUtils.getPropertyValue(this.clientWebSocketContainer, "sendBufferSizeLimit"));
		assertEquals(new URI("ws://foo.bar/ws"),
				TestUtils.getPropertyValue(this.clientWebSocketContainer, "connectionManager.uri", URI.class));
		assertSame(this.webSocketClient,
				TestUtils.getPropertyValue(this.clientWebSocketContainer, "connectionManager.client"));
		assertEquals(100, TestUtils.getPropertyValue(this.clientWebSocketContainer, "connectionManager.phase"));
		WebSocketHttpHeaders headers = TestUtils.getPropertyValue(this.clientWebSocketContainer, "headers",
				WebSocketHttpHeaders.class);
		assertEquals("FOO", headers.getOrigin());
		assertEquals(Arrays.asList("BAR", "baz"), headers.get("FOO"));
	}

}

/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.ws.config;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.mapping.AbstractHeaderMapper;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.ws.SoapHeaderMapper;
import org.springframework.integration.ws.inbound.MarshallingWebServiceInboundGateway;
import org.springframework.integration.ws.inbound.SimpleWebServiceInboundGateway;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.oxm.Unmarshaller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.ws.context.DefaultMessageContext;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Stephane Nicoll
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class WebServiceInboundGatewayParserTests {

	@Autowired
	@Qualifier("requestsMarshalling")
	PollableChannel requestsMarshalling;

	@Autowired
	@Qualifier("requestsSimple")
	PollableChannel requestsSimple;

	@Autowired
	@Qualifier("customErrorChannel")
	MessageChannel customErrorChannel;

	@Autowired
	@Qualifier("requestsVerySimple")
	MessageChannel requestsVerySimple;

	//Simple
	@Autowired
	@Qualifier("simple")
	SimpleWebServiceInboundGateway simpleGateway;

	//marshalling
	@Autowired
	@Qualifier("marshalling")
	MarshallingWebServiceInboundGateway marshallingGateway;

	@Autowired
	Unmarshaller marshaller;

	@Test
	public void simpleGatewayProperties() {
		assertThat(this.simpleGateway.getRequestChannel()).isSameAs(this.requestsVerySimple);
		assertThat(this.simpleGateway.getErrorChannel()).isSameAs(this.customErrorChannel);
		assertThat(this.simpleGateway.isAutoStartup()).isFalse();
		assertThat(this.simpleGateway.getPhase()).isEqualTo(101);
	}

	//extractPayload = false
	@Autowired
	@Qualifier("extractsPayload")
	SimpleWebServiceInboundGateway payloadExtractingGateway;

	@Test
	public void extractPayloadSet() {
		assertThat(TestUtils.<Boolean>getPropertyValue(this.payloadExtractingGateway, "extractPayload")).isFalse();
	}

	@Test
	public void marshallersSet() {
		DirectFieldAccessor accessor = new DirectFieldAccessor(marshallingGateway);

		assertThat(accessor.getPropertyValue("marshaller")).isEqualTo(marshaller);
		assertThat(accessor.getPropertyValue("unmarshaller")).isEqualTo(marshaller);

		assertThat(this.marshallingGateway.isRunning()).as("messaging gateway is not running").isTrue();

		assertThat(this.marshallingGateway.getErrorChannel()).isSameAs(this.customErrorChannel);

		AbstractHeaderMapper.HeaderMatcher requestHeaderMatcher = TestUtils.getPropertyValue(marshallingGateway, "headerMapper.requestHeaderMatcher");
		assertThat(requestHeaderMatcher.matchHeader("testRequest")).isTrue();
		assertThat(requestHeaderMatcher.matchHeader("testReply")).isFalse();

		AbstractHeaderMapper.HeaderMatcher replyHeaderMatcher = TestUtils.getPropertyValue(marshallingGateway, "headerMapper.replyHeaderMatcher");
		assertThat(replyHeaderMatcher.matchHeader("testRequest")).isFalse();
		assertThat(replyHeaderMatcher.matchHeader("testReply")).isTrue();
	}

	@Test
	public void testMessageHistoryWithMarshallingGateway() throws Exception {
		MessageContext context = new DefaultMessageContext(new StubMessageFactory());
		Unmarshaller unmarshaller = mock();
		when(unmarshaller.unmarshal(Mockito.any())).thenReturn("hello");
		marshallingGateway.setUnmarshaller(unmarshaller);
		marshallingGateway.invoke(context);
		Message<?> message = requestsMarshalling.receive(100);
		MessageHistory history = MessageHistory.read(message);
		assertThat(history).isNotNull();
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "marshalling", 0);
		assertThat(componentHistoryRecord).isNotNull();
		assertThat(componentHistoryRecord.get("type")).isEqualTo("ws:inbound-gateway");
	}

	@Test
	public void testMessageHistoryWithSimpleGateway() throws Exception {
		MessageContext context = new DefaultMessageContext(new StubMessageFactory());
		payloadExtractingGateway.invoke(context);
		Message<?> message = requestsSimple.receive(100);
		MessageHistory history = MessageHistory.read(message);
		assertThat(history).isNotNull();
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "extractsPayload", 0);
		assertThat(componentHistoryRecord).isNotNull();
		assertThat(componentHistoryRecord.get("type")).isEqualTo("ws:inbound-gateway");
	}

	@Autowired
	private SimpleWebServiceInboundGateway headerMappingGateway;

	@Autowired
	private SoapHeaderMapper testHeaderMapper;

	@Test
	public void testHeaderMapperReference() {
		assertThat(TestUtils.<Object>getPropertyValue(this.headerMappingGateway, "headerMapper"))
				.isSameAs(testHeaderMapper);
	}

	@Autowired
	@Qualifier("replyTimeoutGateway")
	private SimpleWebServiceInboundGateway replyTimeoutGateway;

	@Test
	public void testReplyTimeout() {
		assertThat(TestUtils.<Long>getPropertyValue(replyTimeoutGateway, "messagingTemplate.receiveTimeout"))
				.isEqualTo(1234L);
	}

	@SuppressWarnings("unused")
	private static class TestHeaderMapper implements SoapHeaderMapper {

		@Override
		public void fromHeadersToRequest(MessageHeaders headers, SoapMessage target) {
		}

		@Override
		public void fromHeadersToReply(MessageHeaders headers, SoapMessage target) {
		}

		@Override
		public Map<String, Object> toHeadersFromRequest(SoapMessage source) {
			return Collections.emptyMap();
		}

		@Override
		public Map<String, Object> toHeadersFromReply(SoapMessage source) {
			return Collections.emptyMap();
		}

	}

}

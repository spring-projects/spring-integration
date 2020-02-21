/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.ws.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.ws.MarshallingWebServiceInboundGateway;
import org.springframework.integration.ws.MarshallingWebServiceOutboundGateway;
import org.springframework.integration.ws.SimpleWebServiceInboundGateway;
import org.springframework.integration.ws.SimpleWebServiceOutboundGateway;
import org.springframework.integration.ws.SoapHeaderMapper;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.WebServiceMessageSender;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.3
 *
 */
public class WsDslTests {

	@Test
	void marshallingInbound() {
		Marshaller marshaller = mock(Marshaller.class);
		Unmarshaller unmarshaller = mock(Unmarshaller.class);
		MarshallingWebServiceInboundGateway gateway = Ws.marshallingInboundGateway(marshaller)
				.unmarshaller(unmarshaller)
				.get();
		assertThat(TestUtils.getPropertyValue(gateway, "marshaller")).isSameAs(marshaller);
		assertThat(TestUtils.getPropertyValue(gateway, "unmarshaller")).isSameAs(unmarshaller);

		marshaller = mock(Both.class);
		gateway = Ws.marshallingInboundGateway(marshaller).get();
		assertThat(TestUtils.getPropertyValue(gateway, "marshaller")).isSameAs(marshaller);
		assertThat(TestUtils.getPropertyValue(gateway, "unmarshaller")).isSameAs(marshaller);
	}

	@Test
	void simpleInbound() {
		SimpleWebServiceInboundGateway gateway = Ws.simpleInboundGateway()
				.extractPayload(false)
				.get();
		assertThat(TestUtils.getPropertyValue(gateway, "extractPayload", Boolean.class)).isFalse();
	}

	@Test
	void marshallingOutbound() {
		DestinationProvider destinationProvider = mock(DestinationProvider.class);
		Marshaller marshaller = mock(Marshaller.class);
		Unmarshaller unmarshaller = mock(Unmarshaller.class);
		WebServiceMessageFactory messageFactory = mock(WebServiceMessageFactory.class);
		FaultMessageResolver faultMessageResolver = mock(FaultMessageResolver.class);
		SoapHeaderMapper headerMapper = mock(SoapHeaderMapper.class);
		ClientInterceptor interceptor = mock(ClientInterceptor.class);
		WebServiceMessageSender messageSender = mock(WebServiceMessageSender.class);
		WebServiceMessageCallback requestCallback = msg -> { };
		Map<String, Expression> uriVariableExpressions = new HashMap<>();
		uriVariableExpressions.put("foo", new LiteralExpression("bar"));
		MarshallingWebServiceOutboundGateway gateway =
				Ws.marshallingOutboundGateway()
						.destinationProvider(destinationProvider)
						.marshaller(marshaller)
						.unmarshaller(unmarshaller)
						.messageFactory(messageFactory)
						.encodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY)
						.faultMessageResolver(faultMessageResolver)
						.headerMapper(headerMapper)
						.ignoreEmptyResponses(true)
						.interceptors(interceptor)
						.messageSenders(messageSender)
						.requestCallback(requestCallback)
						.uriVariableExpressions(uriVariableExpressions)
						.get();
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.marshaller")).isSameAs(marshaller);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.unmarshaller")).isSameAs(unmarshaller);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.messageFactory")).isSameAs(messageFactory);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.faultMessageResolver"))
				.isSameAs(faultMessageResolver);
		assertThat(TestUtils.getPropertyValue(gateway, "headerMapper")).isSameAs(headerMapper);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.interceptors", ClientInterceptor[].class)[0])
				.isSameAs(interceptor);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.messageSenders",
				WebServiceMessageSender[].class)[0])
				.isSameAs(messageSender);
		assertThat(TestUtils.getPropertyValue(gateway, "requestCallback")).isSameAs(requestCallback);
		assertThat(TestUtils.getPropertyValue(gateway, "uriVariableExpressions")).isEqualTo(uriVariableExpressions);
	}

	@Test
	void simpleOutbound() {
		DestinationProvider destinationProvider = mock(DestinationProvider.class);
		WebServiceMessageFactory messageFactory = mock(WebServiceMessageFactory.class);
		FaultMessageResolver faultMessageResolver = mock(FaultMessageResolver.class);
		SoapHeaderMapper headerMapper = mock(SoapHeaderMapper.class);
		ClientInterceptor interceptor = mock(ClientInterceptor.class);
		WebServiceMessageSender messageSender = mock(WebServiceMessageSender.class);
		WebServiceMessageCallback requestCallback = msg -> { };
		Map<String, Expression> uriVariableExpressions = new HashMap<>();
		uriVariableExpressions.put("foo", new LiteralExpression("bar"));
		SourceExtractor<?> sourceExtractor = mock(SourceExtractor.class);
		SimpleWebServiceOutboundGateway gateway =
				Ws.simpleOutboundGateway()
						.destinationProvider(destinationProvider)
						.sourceExtractor(sourceExtractor)
						.messageFactory(messageFactory)
						.encodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY)
						.faultMessageResolver(faultMessageResolver)
						.headerMapper(headerMapper)
						.ignoreEmptyResponses(true)
						.interceptors(interceptor)
						.messageSenders(messageSender)
						.requestCallback(requestCallback)
						.uriVariableExpressions(uriVariableExpressions)
						.extractPayload(false)
						.get();
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.messageFactory")).isSameAs(messageFactory);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.faultMessageResolver"))
				.isSameAs(faultMessageResolver);
		assertThat(TestUtils.getPropertyValue(gateway, "headerMapper")).isSameAs(headerMapper);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.interceptors", ClientInterceptor[].class)[0])
				.isSameAs(interceptor);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.messageSenders",
				WebServiceMessageSender[].class)[0])
				.isSameAs(messageSender);
		assertThat(TestUtils.getPropertyValue(gateway, "requestCallback")).isSameAs(requestCallback);
		assertThat(TestUtils.getPropertyValue(gateway, "uriVariableExpressions")).isEqualTo(uriVariableExpressions);
		assertThat(TestUtils.getPropertyValue(gateway, "extractPayload", Boolean.class)).isFalse();
	}

	@Test
	void marshallingOutboundTemplate() {
		SoapHeaderMapper headerMapper = mock(SoapHeaderMapper.class);
		WebServiceMessageCallback requestCallback = msg -> { };
		Map<String, Expression> uriVariableExpressions = new HashMap<>();
		uriVariableExpressions.put("foo", new LiteralExpression("bar"));
		WebServiceTemplate template = mock(WebServiceTemplate.class);
		String uri = "foo";
		MarshallingWebServiceOutboundGateway gateway =
				Ws.marshallingOutboundGateway(template)
						.uri(uri)
						.encodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY)
						.headerMapper(headerMapper)
						.ignoreEmptyResponses(true)
						.requestCallback(requestCallback)
						.uriVariableExpressions(uriVariableExpressions)
						.get();
		assertThat(TestUtils.getPropertyValue(gateway, "uri")).isSameAs(uri);
		assertThat(TestUtils.getPropertyValue(gateway, "headerMapper")).isSameAs(headerMapper);
		assertThat(TestUtils.getPropertyValue(gateway, "requestCallback")).isSameAs(requestCallback);
		assertThat(TestUtils.getPropertyValue(gateway, "uriVariableExpressions")).isEqualTo(uriVariableExpressions);
	}

	@Test
	void simpleOutboundTemplate() {
		SoapHeaderMapper headerMapper = mock(SoapHeaderMapper.class);
		WebServiceMessageCallback requestCallback = msg -> { };
		Map<String, Expression> uriVariableExpressions = new HashMap<>();
		uriVariableExpressions.put("foo", new LiteralExpression("bar"));
		SourceExtractor<?> sourceExtractor = mock(SourceExtractor.class);
		WebServiceTemplate template = mock(WebServiceTemplate.class);
		String uri = "foo";
		SimpleWebServiceOutboundGateway gateway =
				Ws.simpleOutboundGateway(template)
						.uri(uri)
						.sourceExtractor(sourceExtractor)
						.encodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY)
						.headerMapper(headerMapper)
						.ignoreEmptyResponses(true)
						.requestCallback(requestCallback)
						.uriVariableExpressions(uriVariableExpressions)
						.extractPayload(false)
						.get();
		assertThat(TestUtils.getPropertyValue(gateway, "headerMapper")).isSameAs(headerMapper);
		assertThat(TestUtils.getPropertyValue(gateway, "requestCallback")).isSameAs(requestCallback);
		assertThat(TestUtils.getPropertyValue(gateway, "uriVariableExpressions")).isEqualTo(uriVariableExpressions);
		assertThat(TestUtils.getPropertyValue(gateway, "extractPayload", Boolean.class)).isFalse();
		assertThat(
				TestUtils.getPropertyValue(gateway, "uriFactory.encodingMode",
						DefaultUriBuilderFactory.EncodingMode.class))
				.isEqualTo(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
	}

	interface Both extends Marshaller, Unmarshaller {

	}

}


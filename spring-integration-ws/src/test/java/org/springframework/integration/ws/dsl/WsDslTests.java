/*
 * Copyright 2020-present the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.ws.DefaultSoapHeaderMapper;
import org.springframework.integration.ws.SoapHeaderMapper;
import org.springframework.integration.ws.inbound.MarshallingWebServiceInboundGateway;
import org.springframework.integration.ws.inbound.SimpleWebServiceInboundGateway;
import org.springframework.integration.ws.outbound.MarshallingWebServiceOutboundGateway;
import org.springframework.integration.ws.outbound.SimpleWebServiceOutboundGateway;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 5.3
 *
 */
public class WsDslTests {

	@Test
	void marshallingInbound() {
		Marshaller marshaller = mock();
		Unmarshaller unmarshaller = mock();
		MarshallingWebServiceInboundGateway gateway = Ws.marshallingInboundGateway(marshaller)
				.unmarshaller(unmarshaller)
				.getObject();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "marshaller")).isSameAs(marshaller);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "unmarshaller")).isSameAs(unmarshaller);

		marshaller = mock(Both.class);
		gateway = Ws.marshallingInboundGateway(marshaller).getObject();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "marshaller")).isSameAs(marshaller);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "unmarshaller")).isSameAs(marshaller);
	}

	@Test
	void simpleInbound() {
		DefaultSoapHeaderMapper testHeaderMapper = new DefaultSoapHeaderMapper();
		SimpleWebServiceInboundGateway gateway =
				Ws.simpleInboundGateway()
						.extractPayload(false)
						.headerMapper(testHeaderMapper)
						.errorChannel("myErrorChannel")
						.getObject();
		assertThat(TestUtils.<Boolean>getPropertyValue(gateway, "extractPayload")).isFalse();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "headerMapper")).isSameAs(testHeaderMapper);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "errorChannelName")).isEqualTo("myErrorChannel");
	}

	@Test
	void marshallingOutbound() {
		DestinationProvider destinationProvider = mock();
		Marshaller marshaller = mock();
		Unmarshaller unmarshaller = mock();
		WebServiceMessageFactory messageFactory = mock();
		FaultMessageResolver faultMessageResolver = mock();
		SoapHeaderMapper headerMapper = mock();
		ClientInterceptor interceptor = mock();
		WebServiceMessageSender messageSender = mock();
		WebServiceMessageCallback requestCallback = msg -> {
		};
		Map<String, Expression> uriVariableExpressions = new HashMap<>();
		uriVariableExpressions.put("foo", new LiteralExpression("bar"));
		MarshallingWebServiceOutboundGateway gateway =
				Ws.marshallingOutboundGateway()
						.destinationProvider(destinationProvider)
						.marshaller(marshaller)
						.unmarshaller(unmarshaller)
						.messageFactory(messageFactory)
						.faultMessageResolver(faultMessageResolver)
						.headerMapper(headerMapper)
						.ignoreEmptyResponses(true)
						.interceptors(interceptor)
						.messageSenders(messageSender)
						.requestCallback(requestCallback)
						.uriVariableExpressions(uriVariableExpressions)
						.getObject();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "webServiceTemplate.marshaller")).isSameAs(marshaller);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "webServiceTemplate.unmarshaller"))
				.isSameAs(unmarshaller);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "webServiceTemplate.messageFactory"))
				.isSameAs(messageFactory);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "webServiceTemplate.faultMessageResolver"))
				.isSameAs(faultMessageResolver);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "headerMapper")).isSameAs(headerMapper);
		assertThat(TestUtils.<ClientInterceptor[]>getPropertyValue(gateway, "webServiceTemplate.interceptors")[0])
				.isSameAs(interceptor);
		assertThat(TestUtils.<WebServiceMessageSender[]>getPropertyValue(gateway, "webServiceTemplate.messageSenders")[0])
				.isSameAs(messageSender);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "requestCallback")).isSameAs(requestCallback);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "uriVariableExpressions"))
				.isEqualTo(uriVariableExpressions);
	}

	@Test
	void simpleOutbound() {
		DestinationProvider destinationProvider = mock();
		WebServiceMessageFactory messageFactory = mock();
		FaultMessageResolver faultMessageResolver = mock();
		SoapHeaderMapper headerMapper = mock();
		ClientInterceptor interceptor = mock();
		WebServiceMessageSender messageSender = mock();
		WebServiceMessageCallback requestCallback = msg -> {
		};
		Map<String, Expression> uriVariableExpressions = new HashMap<>();
		uriVariableExpressions.put("foo", new LiteralExpression("bar"));
		SourceExtractor<?> sourceExtractor = mock();
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
						.getObject();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "webServiceTemplate.messageFactory"))
				.isSameAs(messageFactory);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "webServiceTemplate.faultMessageResolver"))
				.isSameAs(faultMessageResolver);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "headerMapper")).isSameAs(headerMapper);
		assertThat(TestUtils.<ClientInterceptor[]>getPropertyValue(gateway, "webServiceTemplate.interceptors")[0])
				.isSameAs(interceptor);
		assertThat(TestUtils.<WebServiceMessageSender[]>getPropertyValue(gateway, "webServiceTemplate.messageSenders")[0])
				.isSameAs(messageSender);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "requestCallback")).isSameAs(requestCallback);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "uriVariableExpressions"))
				.isEqualTo(uriVariableExpressions);
		assertThat(TestUtils.<Boolean>getPropertyValue(gateway, "extractPayload")).isFalse();
	}

	@Test
	void marshallingOutboundTemplate() {
		SoapHeaderMapper headerMapper = mock();
		WebServiceMessageCallback requestCallback = msg -> {
		};
		Map<String, Expression> uriVariableExpressions = new HashMap<>();
		uriVariableExpressions.put("testVariable", new LiteralExpression("testValue"));
		WebServiceTemplate template = new WebServiceTemplate();
		String uri = "testUri";
		template.setDefaultUri(uri);
		MarshallingWebServiceOutboundGateway gateway =
				Ws.marshallingOutboundGateway(template)
						.uri(uri)
						.headerMapper(headerMapper)
						.ignoreEmptyResponses(true)
						.requestCallback(requestCallback)
						.uriVariableExpressions(uriVariableExpressions)
						.getObject();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "uri")).isSameAs(uri);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "headerMapper")).isSameAs(headerMapper);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "requestCallback")).isSameAs(requestCallback);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "uriVariableExpressions"))
				.isEqualTo(uriVariableExpressions);
		assertThat(
				TestUtils.<DefaultUriBuilderFactory.EncodingMode>getPropertyValue(gateway, "uriFactory.encodingMode"))
				.isEqualTo(DefaultUriBuilderFactory.EncodingMode.TEMPLATE_AND_VALUES);
	}

	@Test
	void simpleOutboundTemplate() {
		SoapHeaderMapper headerMapper = mock();
		WebServiceMessageCallback requestCallback = msg -> {
		};
		Map<String, Expression> uriVariableExpressions = new HashMap<>();
		uriVariableExpressions.put("testVariable", new LiteralExpression("testValue"));
		SourceExtractor<?> sourceExtractor = mock();
		WebServiceTemplate template = new WebServiceTemplate();
		template.setDefaultUri("testUri");
		SimpleWebServiceOutboundGateway gateway =
				Ws.simpleOutboundGateway(template)
						.sourceExtractor(sourceExtractor)
						.encodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY)
						.headerMapper(headerMapper)
						.ignoreEmptyResponses(true)
						.requestCallback(requestCallback)
						.uriVariableExpressions(uriVariableExpressions)
						.extractPayload(false)
						.getObject();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "headerMapper")).isSameAs(headerMapper);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "requestCallback")).isSameAs(requestCallback);
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "uriVariableExpressions"))
				.isEqualTo(uriVariableExpressions);
		assertThat(TestUtils.<Boolean>getPropertyValue(gateway, "extractPayload")).isFalse();
		assertThat(
				TestUtils.<DefaultUriBuilderFactory.EncodingMode>getPropertyValue(gateway, "uriFactory.encodingMode"))
				.isEqualTo(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
	}

	interface Both extends Marshaller, Unmarshaller {

	}

}

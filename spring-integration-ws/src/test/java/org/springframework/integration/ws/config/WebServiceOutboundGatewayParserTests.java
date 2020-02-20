/*
 * Copyright 2002-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.mapping.AbstractHeaderMapper;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.ws.MarshallingWebServiceOutboundGateway;
import org.springframework.integration.ws.SimpleWebServiceOutboundGateway;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceMessageExtractor;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.WebServiceMessageSender;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
public class WebServiceOutboundGatewayParserTests {

	private static volatile int adviceCalled;

	@Autowired
	private ApplicationContext context;

	@Test
	public void simpleGatewayWithReplyChannel() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithReplyChannel", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		Object expected = this.context.getBean("outputChannel");
		assertThat(accessor.getPropertyValue("outputChannel")).isEqualTo(expected);
		assertThat(accessor.getPropertyValue("requiresReply")).isEqualTo(Boolean.FALSE);

		AbstractHeaderMapper.HeaderMatcher requestHeaderMatcher = TestUtils.getPropertyValue(endpoint,
				"handler.headerMapper.requestHeaderMatcher", AbstractHeaderMapper.HeaderMatcher.class);
		assertThat(requestHeaderMatcher.matchHeader("testRequest")).isTrue();
		assertThat(requestHeaderMatcher.matchHeader("testReply")).isFalse();

		AbstractHeaderMapper.HeaderMatcher replyHeaderMatcher = TestUtils.getPropertyValue(endpoint,
				"handler.headerMapper.replyHeaderMatcher", AbstractHeaderMapper.HeaderMatcher.class);
		assertThat(replyHeaderMatcher.matchHeader("testRequest")).isFalse();
		assertThat(replyHeaderMatcher.matchHeader("testReply")).isTrue();

		Long sendTimeout = TestUtils.getPropertyValue(gateway, "messagingTemplate.sendTimeout", Long.class);
		assertThat(sendTimeout).isEqualTo(Long.valueOf(777));

		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate"))
				.isSameAs(this.context.getBean("webServiceTemplate"));
	}

	@Test
	public void simpleGatewayWithIgnoreEmptyResponseTrueByDefault() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithReplyChannel", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertThat(accessor.getPropertyValue("ignoreEmptyResponses")).isEqualTo(Boolean.TRUE);
		assertThat(accessor.getPropertyValue("requiresReply")).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void simpleGatewayWithIgnoreEmptyResponses() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithIgnoreEmptyResponsesFalseAndRequiresReplyTrue",
				AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertThat(accessor.getPropertyValue("ignoreEmptyResponses")).isEqualTo(Boolean.FALSE);
		assertThat(accessor.getPropertyValue("requiresReply")).isEqualTo(Boolean.TRUE);
		assertThat(accessor.getPropertyValue("extractPayload")).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void simpleGatewayWithDefaultSourceExtractor() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithDefaultSourceExtractor", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertThat(accessor.getPropertyValue("sourceExtractor").getClass().getSimpleName())
				.isEqualTo("DefaultSourceExtractor");
	}

	@Test
	public void simpleGatewayWithCustomSourceExtractor() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithCustomSourceExtractor", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		SourceExtractor<?> sourceExtractor = (SourceExtractor<?>) context.getBean("sourceExtractor");
		assertThat(accessor.getPropertyValue("sourceExtractor")).isEqualTo(sourceExtractor);
	}

	@Test
	public void simpleGatewayWithCustomRequestCallback() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithCustomRequestCallback", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		WebServiceMessageCallback callback = (WebServiceMessageCallback) context.getBean("requestCallback");
		assertThat(accessor.getPropertyValue("requestCallback")).isEqualTo(callback);
	}

	@Test
	public void simpleGatewayWithCustomMessageFactory() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithCustomMessageFactory", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageFactory factory = (WebServiceMessageFactory) context.getBean("messageFactory");
		assertThat(accessor.getPropertyValue("messageFactory")).isEqualTo(factory);
	}

	@Test
	public void simpleGatewayWithCustomSourceExtractorAndMessageFactory() {
		AbstractEndpoint endpoint = context.getBean("gatewayWithCustomSourceExtractorAndMessageFactory",
				AbstractEndpoint.class);
		SourceExtractor<?> sourceExtractor = (SourceExtractor<?>) context.getBean("sourceExtractor");
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertThat(accessor.getPropertyValue("sourceExtractor")).isEqualTo(sourceExtractor);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageFactory factory = (WebServiceMessageFactory) context.getBean("messageFactory");
		assertThat(accessor.getPropertyValue("messageFactory")).isEqualTo(factory);
	}

	@Test
	public void simpleGatewayWithCustomFaultMessageResolver() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithCustomFaultMessageResolver",
				AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		FaultMessageResolver resolver = (FaultMessageResolver) context.getBean("faultMessageResolver");
		assertThat(accessor.getPropertyValue("faultMessageResolver")).isEqualTo(resolver);
	}


	@Test
	public void simpleGatewayWithCustomMessageSender() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithCustomMessageSender", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageSender messageSender = (WebServiceMessageSender) context.getBean("messageSender");
		assertThat(((WebServiceMessageSender[]) accessor.getPropertyValue("messageSenders"))[0])
				.isEqualTo(messageSender);
	}

	@Test
	public void simpleGatewayWithCustomMessageSenderList() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithCustomMessageSenderList", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageSender messageSender = (WebServiceMessageSender) context.getBean("messageSender");
		assertThat(((WebServiceMessageSender[]) accessor.getPropertyValue("messageSenders"))[0])
				.isEqualTo(messageSender);
		assertThat(((WebServiceMessageSender[]) accessor.getPropertyValue("messageSenders")).length)
				.as("Wrong number of message senders ").isEqualTo(2);
	}

	@Test
	public void simpleGatewayWithCustomInterceptor() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithCustomInterceptor", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		ClientInterceptor interceptor = context.getBean("interceptor", ClientInterceptor.class);
		assertThat(((ClientInterceptor[]) accessor.getPropertyValue("interceptors"))[0]).isEqualTo(interceptor);
	}

	@Test
	public void simpleGatewayWithCustomInterceptorList() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithCustomInterceptorList", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		ClientInterceptor interceptor = context.getBean("interceptor", ClientInterceptor.class);
		assertThat(((ClientInterceptor[]) accessor.getPropertyValue("interceptors"))[0]).isEqualTo(interceptor);
		assertThat(((ClientInterceptor[]) accessor.getPropertyValue("interceptors")).length)
				.as("Wrong number of interceptors ").isEqualTo(2);
	}

	@Test
	public void simpleGatewayWithPoller() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithPoller", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(PollingConsumer.class);
		Object triggerObject = new DirectFieldAccessor(endpoint).getPropertyValue("trigger");
		assertThat(triggerObject.getClass()).isEqualTo(PeriodicTrigger.class);
		PeriodicTrigger trigger = (PeriodicTrigger) triggerObject;
		DirectFieldAccessor accessor = new DirectFieldAccessor(trigger);
		assertThat(((Long) accessor.getPropertyValue("period")).longValue()).as("PeriodicTrigger had wrong period")
				.isEqualTo(5000);
	}

	@Test
	public void simpleGatewayWithOrder() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithOrderAndAutoStartupFalse", AbstractEndpoint.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertThat(new DirectFieldAccessor(gateway).getPropertyValue("order")).isEqualTo(99);
	}

	@Test
	public void simpleGatewayWithStartupFalse() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithOrderAndAutoStartupFalse",
				AbstractEndpoint.class);
		assertThat(new DirectFieldAccessor(endpoint).getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void marshallingGatewayWithAllInOneMarshaller() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithAllInOneMarshaller");
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = TestUtils.getPropertyValue(endpoint, "handler");
		Marshaller marshaller = context.getBean("marshallerAndUnmarshaller", Marshaller.class);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.marshaller", Marshaller.class))
				.isSameAs(marshaller);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.unmarshaller", Unmarshaller.class))
				.isSameAs(marshaller);
		context.close();
	}

	@Test
	public void marshallingGatewayWithSeparateMarshallerAndUnmarshaller() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithSeparateMarshallerAndUnmarshaller");
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = TestUtils.getPropertyValue(endpoint, "handler");
		Marshaller marshaller = context.getBean("marshaller", Marshaller.class);
		Unmarshaller unmarshaller = context.getBean("unmarshaller", Unmarshaller.class);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.marshaller", Marshaller.class))
				.isSameAs(marshaller);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.unmarshaller", Unmarshaller.class))
				.isSameAs(unmarshaller);
		context.close();
	}

	@Test
	public void marshallingGatewayWithCustomRequestCallback() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomRequestCallback");
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = TestUtils.getPropertyValue(endpoint, "handler");
		assertThat(gateway.getClass()).isEqualTo(MarshallingWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		WebServiceMessageCallback callback = (WebServiceMessageCallback) context.getBean("requestCallback");
		assertThat(accessor.getPropertyValue("requestCallback")).isEqualTo(callback);
		context.close();
	}

	@Test
	public void marshallingGatewayWithAllInOneMarshallerAndMessageFactory() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean(
				"gatewayWithAllInOneMarshallerAndMessageFactory");
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = TestUtils.getPropertyValue(endpoint, "handler");
		Marshaller marshaller = context.getBean("marshallerAndUnmarshaller", Marshaller.class);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.marshaller", Marshaller.class))
				.isSameAs(marshaller);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.unmarshaller", Unmarshaller.class))
				.isSameAs(marshaller);

		WebServiceMessageFactory messageFactory = (WebServiceMessageFactory) context.getBean("messageFactory");
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.messageFactory")).isEqualTo(messageFactory);
		context.close();
	}

	@Test
	public void marshallingGatewayWithSeparateMarshallerAndUnmarshallerAndMessageFactory() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean(
				"gatewayWithSeparateMarshallerAndUnmarshallerAndMessageFactory");
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);

		Object gateway = TestUtils.getPropertyValue(endpoint, "handler");

		Marshaller marshaller = context.getBean("marshaller", Marshaller.class);
		Unmarshaller unmarshaller = context.getBean("unmarshaller", Unmarshaller.class);

		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.marshaller", Marshaller.class))
				.isSameAs(marshaller);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.unmarshaller", Unmarshaller.class))
				.isSameAs(unmarshaller);

		WebServiceMessageFactory messageFactory = context.getBean("messageFactory", WebServiceMessageFactory.class);
		assertThat(TestUtils.getPropertyValue(gateway, "webServiceTemplate.messageFactory")).isEqualTo(messageFactory);
		context.close();
	}

	@Test
	public void simpleGatewayWithDestinationProvider() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithDestinationProvider", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		StubDestinationProvider stubProvider = (StubDestinationProvider) context.getBean("destinationProvider");
		assertThat(gateway.getClass()).isEqualTo(SimpleWebServiceOutboundGateway.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertThat(accessor.getPropertyValue("destinationProvider")).as("Wrong DestinationProvider")
				.isEqualTo(stubProvider);
		assertThat(accessor.getPropertyValue("uri")).isNull();
		Object destinationProviderObject = new DirectFieldAccessor(
				accessor.getPropertyValue("webServiceTemplate")).getPropertyValue("destinationProvider");
		assertThat(destinationProviderObject).as("Wrong DestinationProvider").isEqualTo(stubProvider);
	}

	@Test
	public void advised() {
		adviceCalled = 0;
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithAdvice", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		MessageHandler handler = TestUtils.getPropertyValue(endpoint, "handler", MessageHandler.class);
		handler.handleMessage(new GenericMessage<>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	public void testInt2718AdvisedInsideAChain() {
		adviceCalled = 0;
		MessageChannel channel = context.getBean("gatewayWithAdviceInsideAChain", MessageChannel.class);
		channel.send(new GenericMessage<>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	public void jmsUri() {
		AbstractEndpoint endpoint = this.context.getBean("gatewayWithJmsUri", AbstractEndpoint.class);
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		MessageHandler handler = TestUtils.getPropertyValue(endpoint, "handler", MessageHandler.class);
		assertThat(TestUtils.getPropertyValue(handler, "destinationProvider")).isNull();
		assertThat(
				TestUtils.getPropertyValue(handler, "uriFactory.encodingMode",
						DefaultUriBuilderFactory.EncodingMode.class))
				.isEqualTo(DefaultUriBuilderFactory.EncodingMode.NONE);

		WebServiceTemplate webServiceTemplate = TestUtils.getPropertyValue(handler, "webServiceTemplate",
				WebServiceTemplate.class);
		webServiceTemplate = spy(webServiceTemplate);

		doReturn(null).when(webServiceTemplate).sendAndReceive(anyString(),
				any(WebServiceMessageCallback.class),
				ArgumentMatchers.<WebServiceMessageExtractor<Object>>any());

		new DirectFieldAccessor(handler).setPropertyValue("webServiceTemplate", webServiceTemplate);

		handler.handleMessage(new GenericMessage<>("foo"));

		verify(webServiceTemplate).sendAndReceive(eq("jms:wsQueue"),
				any(WebServiceMessageCallback.class),
				ArgumentMatchers.<WebServiceMessageExtractor<Object>>any());
	}

	@Test
	public void invalidGatewayWithBothUriAndDestinationProvider() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("invalidGatewayWithBothUriAndDestinationProvider.xml",
								getClass()));
	}

	@Test
	public void invalidGatewayWithNeitherUriNorDestinationProvider() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("invalidGatewayWithNeitherUriNorDestinationProvider.xml",
								getClass()));
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}

/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.ws;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.soap.MessageFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.mime.Attachment;
import org.springframework.ws.mime.MimeMessage;
import org.springframework.ws.pox.PoxMessage;
import org.springframework.ws.pox.dom.DomPoxMessageFactory;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gunnar Hillert
 *
 * @since 2.0
 */
public class SimpleWebServiceOutboundGatewayTests {

	private static final String response = "<response><name>Test Name</name></response>";

	public static final String responseSoapMessage =
			"<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"> " +
					"<soap:Body> " +
					response +
					"</soap:Body> " +
					"</soap:Envelope>";

	public static final String responseEmptyBodySoapMessage =
			"<SOAP:Envelope xmlns:SOAP=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
					"<SOAP:Header/>\n" +
					"<SOAP:Body/>\n" +
					"</SOAP:Envelope>";

	@Test // INT-1051
	public void soapActionAndCustomCallback() {
		String uri = "http://www.example.org";
		SimpleWebServiceOutboundGateway gateway = new SimpleWebServiceOutboundGateway(new TestDestinationProvider(uri));
		final AtomicReference<String> soapActionFromCallback = new AtomicReference<String>();
		gateway.setRequestCallback(message -> {
			SoapMessage soapMessage = (SoapMessage) message;
			soapActionFromCallback.set(soapMessage.getSoapAction());
		});
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();
		String soapActionHeaderValue = "testAction";
		String request = "<test>foo</test>";
		try {
			gateway.handleMessage(MessageBuilder.withPayload(request)
					.setHeader(WebServiceHeaders.SOAP_ACTION, soapActionHeaderValue)
					.build());
			fail("Expected MessageHandlingException");
		}
		catch (MessageHandlingException e) {
			// expected
		}
		assertNotNull(soapActionFromCallback.get());
		assertEquals("\"" + soapActionHeaderValue + "\"", soapActionFromCallback.get());
	}


	@Test //INT-1029
	public void testWsOutboundGatewayInsideChain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"WebServiceOutboundGatewayInsideChainTests-context.xml", this.getClass());
		MessageChannel channel = context.getBean("wsOutboundGatewayInsideChain", MessageChannel.class);
		channel.send(MessageBuilder.withPayload("<test>foo</test>").build());
		PollableChannel replyChannel = context.getBean("replyChannel", PollableChannel.class);
		Message<?> replyMessage = replyChannel.receive();
		assertThat(replyMessage.getPayload().toString(), Matchers.endsWith(response));
		context.close();
	}

	@Test(expected = ReplyRequiredException.class)
	public void testInt3022EmptyResponseBody() throws Exception {
		SimpleWebServiceOutboundGateway gateway = new SimpleWebServiceOutboundGateway("http://testInt3022");
		gateway.setRequiresReply(true);
		WebServiceMessageSender messageSender = createMockMessageSender(responseEmptyBodySoapMessage);
		gateway.setMessageSender(messageSender);
		gateway.handleMessage(new GenericMessage<String>("<test>foo</test>"));
	}

	@Test
	public void testAttachments() throws Exception {
		String uri = "http://www.example.org";
		SimpleWebServiceOutboundGateway gateway = new SimpleWebServiceOutboundGateway(uri);
		gateway.setBeanFactory(mock(BeanFactory.class));

		final SettableListenableFuture<WebServiceMessage> requestFuture = new SettableListenableFuture<>();

		ClientInterceptorAdapter interceptorAdapter = new ClientInterceptorAdapter() {

			@Override
			public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
				requestFuture.set(messageContext.getRequest());
				return super.handleRequest(messageContext);
			}

		};
		gateway.setInterceptors(interceptorAdapter);

		gateway.afterPropertiesSet();

		WebServiceMessageFactory messageFactory = new SaajSoapMessageFactory(MessageFactory.newInstance());
		MimeMessage webServiceMessage = (MimeMessage) messageFactory.createWebServiceMessage();

		String request = "<test>foo</test>";

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.transform(new StringSource(request), webServiceMessage.getPayloadResult());

		webServiceMessage.addAttachment("myAttachment", new ByteArrayResource("my_data".getBytes()), "text/plain");

		try {
			gateway.handleMessage(new GenericMessage<>(webServiceMessage));
			fail("Expected MessageHandlingException");
		}
		catch (MessageHandlingException e) {
			// expected
		}

		WebServiceMessage requestMessage = requestFuture.get(10, TimeUnit.SECONDS);

		assertNotNull(requestMessage);
		assertThat(requestMessage, instanceOf(MimeMessage.class));

		transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringResult stringResult = new StringResult();
		transformer.transform(requestMessage.getPayloadSource(), stringResult);

		assertEquals(request, stringResult.toString());

		Attachment myAttachment = ((MimeMessage) requestMessage).getAttachment("myAttachment");

		assertNotNull(myAttachment);
		assertEquals("text/plain", myAttachment.getContentType());
		assertEquals("my_data", StreamUtils.copyToString(myAttachment.getInputStream(), Charset.forName("UTF-8")));
	}

	@Test
	public void testDomPoxMessageFactory() throws Exception {
		String uri = "http://www.example.org";
		SimpleWebServiceOutboundGateway gateway = new SimpleWebServiceOutboundGateway(uri);
		gateway.setBeanFactory(mock(BeanFactory.class));

		final SettableListenableFuture<WebServiceMessage> requestFuture = new SettableListenableFuture<>();

		ClientInterceptorAdapter interceptorAdapter = new ClientInterceptorAdapter() {

			@Override
			public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
				requestFuture.set(messageContext.getRequest());
				return super.handleRequest(messageContext);
			}

		};
		gateway.setInterceptors(interceptorAdapter);
		gateway.setMessageFactory(new DomPoxMessageFactory());
		gateway.afterPropertiesSet();

		String request = "<test>foo</test>";
		try {
			gateway.handleMessage(new GenericMessage<>(request));
			fail("Expected MessageHandlingException");
		}
		catch (MessageHandlingException e) {
			// expected
		}

		WebServiceMessage requestMessage = requestFuture.get(10, TimeUnit.SECONDS);

		assertNotNull(requestMessage);
		assertThat(requestMessage, instanceOf(PoxMessage.class));

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		StringResult stringResult = new StringResult();
		transformer.transform(requestMessage.getPayloadSource(), stringResult);

		assertEquals(request, stringResult.toString());
	}

	public static WebServiceMessageSender createMockMessageSender(final String mockResponseMessage) throws Exception {
		WebServiceMessageSender messageSender = Mockito.mock(WebServiceMessageSender.class);
		WebServiceConnection wsConnection = Mockito.mock(WebServiceConnection.class);
		Mockito.when(messageSender.createConnection(Mockito.any(URI.class))).thenReturn(wsConnection);
		Mockito.when(messageSender.supports(Mockito.any(URI.class))).thenReturn(true);

		Mockito.doAnswer(invocation -> {
			Object[] args = invocation.getArguments();
			WebServiceMessageFactory factory = (WebServiceMessageFactory) args[0];
			return factory.createWebServiceMessage(new ByteArrayInputStream(mockResponseMessage.getBytes()));
		}).when(wsConnection).receive(Mockito.any(WebServiceMessageFactory.class));

		return messageSender;
	}

	private static class TestDestinationProvider implements DestinationProvider {

		private final URI uri;

		TestDestinationProvider(String uri) {
			this.uri = URI.create(uri);
		}

		@Override
		public URI getDestination() {
			return this.uri;
		}

	}

}

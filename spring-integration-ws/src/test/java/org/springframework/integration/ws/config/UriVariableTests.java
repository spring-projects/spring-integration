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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doAnswer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Stanza;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceMessageExtractor;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.mail.MailSenderConnection;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Andy Wilkinson
 *
 * @since 2.1
 */
@SpringJUnitConfig
public class UriVariableTests {

	@Autowired
	@Qualifier("httpOutboundGateway.handler")
	private MessageHandler httpOutboundGateway;

	@Autowired
	private TestClientInterceptor interceptor;

	@Autowired
	private MessageChannel inputHttp;

	@Autowired
	private MessageChannel inputJms;

	@Autowired
	private WebServiceMessageSender jmsMessageSender;

	@Autowired
	private ConnectionFactory jmsConnectionFactory;

	@Autowired
	private MessageChannel inputXmpp;

	@Autowired
	private XMPPConnection xmppConnection;

	@Autowired
	private MessageChannel inputEmail;

	@Autowired
	private Int2720EmailTestClientInterceptor emailInterceptor;

	@Test
	@SuppressWarnings("unchecked")
	public void testHttpUriVariables() {
		WebServiceTemplate webServiceTemplate = TestUtils.getPropertyValue(this.httpOutboundGateway,
				"webServiceTemplate", WebServiceTemplate.class);
		webServiceTemplate = Mockito.spy(webServiceTemplate);
		final AtomicReference<String> uri = new AtomicReference<>();
		doAnswer(invocation -> {
			uri.set(invocation.getArgument(0));
			throw new WebServiceIOException("intentional");
		}).when(webServiceTemplate)
				.sendAndReceive(Mockito.anyString(),
						Mockito.any(WebServiceMessageCallback.class),
						(WebServiceMessageExtractor<Object>) Mockito.any(WebServiceMessageExtractor.class));

		new DirectFieldAccessor(this.httpOutboundGateway).setPropertyValue("webServiceTemplate", webServiceTemplate);

		Message<?> message = MessageBuilder.withPayload("<spring/>")
				.setHeader("x", "integration")
				.setHeader("param", "test1 & test2")
				.build();

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.inputHttp.send(message))
				.withCauseInstanceOf(WebServiceIOException.class); // offline
		assertThat(uri.get()).isEqualTo("http://localhost/spring-integration?param=test1%20%26%20test2");
	}

	@Test
	public void testInt2720JmsUriVariables() throws JMSException, IOException {
		final String destinationName = "SPRING.INTEGRATION.QUEUE";

		Queue queue = Mockito.mock(Queue.class);
		// Need for 'QueueSession#createQueue()'
		Mockito.when(queue.getQueueName()).thenReturn(destinationName);

		Session session = Mockito.mock(Session.class);
		Mockito.when(session.createQueue(Mockito.anyString())).thenReturn(queue);
		Mockito.when(session.createBytesMessage()).thenReturn(Mockito.mock(BytesMessage.class));
		MessageProducer producer = Mockito.mock(MessageProducer.class);
		Mockito.when(session.createProducer(queue)).thenReturn(producer);
		// For this test it's enough to not go ahead. Invoked in the 'JmsSenderConnection#onSendAfterWrite' on the
		// 'WebServiceTemplate#sendRequest' after invocation of our 'TestClientInterceptor'
		Mockito.when(session.createTemporaryQueue()).thenThrow(new WebServiceIOException("intentional"));

		Connection connection = Mockito.mock(Connection.class);
		Mockito.when(connection.createSession(Mockito.anyBoolean(), Mockito.anyInt())).thenReturn(session);
		Mockito.when(this.jmsConnectionFactory.createConnection()).thenReturn(connection);

		Message<?> message = MessageBuilder.withPayload("<spring/>")
				.setHeader("jmsQueue", destinationName)
				.setHeader("deliveryMode", "NON_PERSISTENT")
				.setHeader("jms_priority", "5")
				.build();
		try {
			this.inputJms.send(message);
		}
		catch (MessageHandlingException e) {
			// expected
			Class<?> causeType = e.getCause().getClass();
			assertThat(WebServiceIOException.class.equals(causeType)).isTrue(); // offline
		}
		URI uri = URI.create("jms:SPRING.INTEGRATION.QUEUE?deliveryMode=NON_PERSISTENT&priority=5");
		Mockito.verify(this.jmsMessageSender).createConnection(uri);

		Mockito.verify(session).createQueue(destinationName);

		assertThat(this.interceptor.getLastUri().toString()).isEqualTo("jms:" + destinationName);
		Mockito.verify(producer).setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		Mockito.verify(producer).setPriority(5);
	}

	@Test
	public void testInt2720EmailUriVariables() {
		final String testEmailTo = "user@example.com";
		final String testEmailSubject = "Test subject";
		Message<?> message = MessageBuilder.withPayload("<spring/>")
				.setHeader("to", testEmailTo)
				.setHeader("subject", testEmailSubject)
				.build();
		try {
			this.inputEmail.send(message);
		}
		catch (MessageHandlingException e) {
			// expected
			Class<?> causeType = e.getCause().getClass();
			assertThat(WebServiceIOException.class.equals(causeType)).isTrue(); // offline
		}
		WebServiceConnection webServiceConnection = this.emailInterceptor.getLastWebServiceConnection();
		assertThat(TestUtils.getPropertyValue(webServiceConnection, "to").toString()).isEqualTo(testEmailTo);
		assertThat(TestUtils.getPropertyValue(webServiceConnection, "subject")).isEqualTo(testEmailSubject);
		assertThat(this.emailInterceptor.getLastUri().toString())
				.isEqualTo("mailto:user@example.com?subject=Test%20subject");
	}

	@Test
	public void testInt2720XmppUriVariables() throws Exception {

		willThrow(new WebServiceIOException("intentional"))
				.given(this.xmppConnection).sendStanza(Mockito.any(Stanza.class));

		Message<?> message = MessageBuilder.withPayload("<spring/>").setHeader("to", "user").build();
		try {
			this.inputXmpp.send(message);
		}
		catch (MessageHandlingException e) {
			// expected
			Class<?> causeType = e.getCause().getClass();
			assertThat(WebServiceIOException.class.equals(causeType)).isTrue(); // offline
		}

		ArgumentCaptor<Stanza> argument = ArgumentCaptor.forClass(Stanza.class);
		Mockito.verify(this.xmppConnection).sendStanza(argument.capture());
		assertThat(argument.getValue().getTo().toString()).isEqualTo("user@jabber.org");

		assertThat(this.interceptor.getLastUri().toString()).isEqualTo("xmpp:user@jabber.org");
	}

	private static class TestClientInterceptor implements ClientInterceptor {

		private volatile URI lastUri;

		TestClientInterceptor() {
			super();
		}

		public URI getLastUri() {
			return this.lastUri;
		}

		@Override
		public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
			TransportContext tc = TransportContextHolder.getTransportContext();
			if (tc != null) {
				try {
					this.lastUri = tc.getConnection().getUri();
				}
				catch (URISyntaxException e) {
					throw new IllegalStateException(e);
				}
			}
			else {
				throw new IllegalStateException("expected WebServiceConnection in the TransportContext");
			}
			return true;
		}

		@Override
		public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
			return false;
		}

		@Override
		public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
			return false;
		}

		@Override
		public void afterCompletion(MessageContext messageContext, Exception ex) throws WebServiceClientException {

		}

	}

	private static class Int2720EmailTestClientInterceptor extends TestClientInterceptor {

		private volatile WebServiceConnection webServiceConnection;

		Int2720EmailTestClientInterceptor() {
			super();
		}

		public WebServiceConnection getLastWebServiceConnection() {
			return webServiceConnection;
		}

		@Override
		public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
			TransportContext tc = TransportContextHolder.getTransportContext();
			WebServiceConnection webServiceConnection = tc.getConnection();
			if (webServiceConnection instanceof MailSenderConnection) {
				this.webServiceConnection = webServiceConnection;
			}
			else {
				throw new IllegalStateException("expected MailSenderConnection in the TransportContext");
			}

			super.handleRequest(messageContext);
			throw new WebServiceIOException("intentional");
		}

	}

}

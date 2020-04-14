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

package org.springframework.integration.rmi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.rmi.RemoteException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.rmi.RmiServiceExporter;
import org.springframework.util.SocketUtils;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class RmiOutboundGatewayTests {

	private static final QueueChannel OUTPUT = new QueueChannel(1);

	private static int RMI_PORT;

	private static RmiServiceExporter EXPORTER;

	private static RmiOutboundGateway GATEWAY;

	@BeforeAll
	static void setup() throws RemoteException {
		RMI_PORT = SocketUtils.findAvailableTcpPort();

		EXPORTER = new RmiServiceExporter();
		EXPORTER.setService(new TestExchanger());
		EXPORTER.setServiceInterface(RequestReplyExchanger.class);
		EXPORTER.setServiceName("testRemoteHandler");
		EXPORTER.setRegistryPort(RMI_PORT);
		EXPORTER.afterPropertiesSet();

		GATEWAY = new RmiOutboundGateway("rmi://localhost:" + RMI_PORT + "/testRemoteHandler");
		GATEWAY.setOutputChannel(OUTPUT);
	}

	@AfterAll
	static void tearDown() throws RemoteException {
		EXPORTER.destroy();
	}

	@Test
	void serializablePayload() {
		GATEWAY.handleMessage(new GenericMessage<>("test"));
		Message<?> replyMessage = OUTPUT.receive(0);
		assertThat(replyMessage).isNotNull();
		assertThat(replyMessage.getPayload()).isEqualTo("TEST");
	}

	@Test
	void failedMessage() {
		GenericMessage<String> message = new GenericMessage<>("fail");

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> GATEWAY.handleMessage(message))
				.satisfies((ex) -> {
					assertThat(ex.getFailedMessage()).isSameAs(message);
					assertThat(((MessagingException) ex.getCause()).getFailedMessage().getPayload()).isEqualTo("bar");
				});
	}

	@Test
	void serializableAttribute() {
		Message<String> requestMessage = MessageBuilder.withPayload("test")
				.setHeader("testAttribute", "foo").build();
		GATEWAY.handleMessage(requestMessage);
		Message<?> replyMessage = OUTPUT.receive(0);
		assertThat(replyMessage).isNotNull();
		assertThat(replyMessage.getHeaders().get("testAttribute")).isEqualTo("foo");
	}

	@Test
	void nonSerializablePayload() {
		NonSerializableTestObject payload = new NonSerializableTestObject();
		Message<?> requestMessage = new GenericMessage<>(payload);
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> GATEWAY.handleMessage(requestMessage));
	}

	@Test
	void nonSerializableAttribute() {
		Message<String> requestMessage = MessageBuilder.withPayload("test")
				.setHeader("testAttribute", new NonSerializableTestObject()).build();
		GATEWAY.handleMessage(requestMessage);
		Message<?> reply = OUTPUT.receive(0);
		assertThat(requestMessage.getHeaders().get("testAttribute")).isNotNull();
		assertThat(reply.getHeaders().get("testAttribute")).isNotNull();
	}

	@Test
	void invalidServiceName() {
		RmiOutboundGateway gateway = new RmiOutboundGateway("rmi://localhost:" + RMI_PORT + "/noSuchService");
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> gateway.handleMessage(new GenericMessage<>("test")))
				.withCauseInstanceOf(RemoteLookupFailureException.class);
	}

	@Test
	void invalidHost() {
		RmiOutboundGateway gateway = new RmiOutboundGateway("rmi://noSuchHost:1099/testRemoteHandler");
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> gateway.handleMessage(new GenericMessage<>("test")))
				.withCauseInstanceOf(RemoteLookupFailureException.class);
	}

	@Test
	void invalidUrl() {
		RmiOutboundGateway gateway = new RmiOutboundGateway("https://sample.com/");
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> gateway.handleMessage(new GenericMessage<>("test")))
				.withCauseInstanceOf(RemoteLookupFailureException.class);
	}


	private static class TestExchanger implements RequestReplyExchanger {

		TestExchanger() {
			super();
		}

		@Override
		public Message<?> exchange(Message<?> message) {
			if (message.getPayload().equals("fail")) {
				new AbstractReplyProducingMessageHandler() {

					@Override
					protected Object handleRequestMessage(Message<?> requestMessage) {
						throw new RuntimeException("foo");
					}

				}.handleMessage(new GenericMessage<>("bar"));
			}
			return new GenericMessage<>(message.getPayload().toString().toUpperCase(), message.getHeaders());
		}

	}


	private static class NonSerializableTestObject {

		NonSerializableTestObject() {
			super();
		}

	}

}

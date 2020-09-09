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

package org.springframework.integration.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.sql.CallableStatement;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.jdbc.config.JdbcTypesEnum;
import org.springframework.integration.jdbc.storedproc.User;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.Jackson2JsonMessageParser;
import org.springframework.integration.support.json.JsonInboundMessageMapper;
import org.springframework.integration.support.json.JsonOutboundMessageMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlReturnType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class StoredProcOutboundGatewayWithSpelIntegrationTests {

	@Autowired
	private AbstractApplicationContext context;

	@Autowired
	@Qualifier("startChannel")
	MessageChannel channel;

	@Autowired
	PollableChannel outputChannel;

	@Autowired
	@Qualifier("startErrorsChannel")
	PollableChannel startErrorsChannel;

	@Autowired
	MessageChannel getMessageChannel;

	@Autowired
	PollableChannel output2Channel;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	SqlReturnType clobSqlReturnType;

	@Test
	public void executeStoredProcedureWithMessageHeader() {
		User user1 = new User("First User", "my first password", "email1");
		User user2 = new User("Second User", "my second password", "email2");

		Message<User> user1Message = MessageBuilder.withPayload(user1)
				.setHeader("my_stored_procedure", "CREATE_USER")
				.build();
		Message<User> user2Message = MessageBuilder.withPayload(user2)
				.setHeader("my_stored_procedure", "CREATE_USER_RETURN_ALL")
				.build();

		channel.send(user1Message);
		channel.send(user2Message);

		@SuppressWarnings("unchecked")
		Message<Collection<User>> message = (Message<Collection<User>>) this.outputChannel.receive(10000);

		context.stop();
		assertThat(message).isNotNull();

		assertThat(message.getPayload()).isNotNull();
		assertThat(message.getPayload() instanceof Collection<?>).isNotNull();

		Collection<User> allUsers = message.getPayload();

		assertThat(allUsers.size() == 2).isTrue();

	}

	@Test
	public void testWithMissingMessageHeader() {
		User user1 = new User("First User", "my first password", "email1");

		Message<User> user1Message = MessageBuilder.withPayload(user1).build();

		this.channel.send(user1Message);

		Message<?> receive = this.startErrorsChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive).isInstanceOf(ErrorMessage.class);

		MessageHandlingException exception = (MessageHandlingException) receive.getPayload();

		String expectedMessage = "Unable to resolve Stored Procedure/Function name " +
				"for the provided Expression 'headers['my_stored_procedure']'.";
		String actualMessage = exception.getCause().getMessage();
		assertThat(actualMessage).isEqualTo(expectedMessage);
	}

	@Test
	@Transactional
	public void testInt2865SqlReturnType() throws Exception {
		Mockito.reset(this.clobSqlReturnType);
		Message<String> testMessage = MessageBuilder.withPayload("TEST").setHeader("FOO", "BAR").build();
		String messageId = testMessage.getHeaders().getId().toString();
		String jsonMessage = new JsonOutboundMessageMapper().fromMessage(testMessage);

		this.jdbcTemplate.update("INSERT INTO json_message VALUES (?,?)", messageId, jsonMessage);

		this.getMessageChannel.send(new GenericMessage<>(messageId));
		Message<?> resultMessage = this.output2Channel.receive(10000);
		assertThat(resultMessage).isNotNull();
		Object resultPayload = resultMessage.getPayload();
		assertThat(resultPayload instanceof String).isTrue();
		Message<?> message = new JsonInboundMessageMapper(String.class, new Jackson2JsonMessageParser())
				.toMessage((String) resultPayload);
		assertThat(message.getPayload()).isEqualTo(testMessage.getPayload());
		assertThat(message.getHeaders().get("FOO")).isEqualTo(testMessage.getHeaders().get("FOO"));
		Mockito.verify(clobSqlReturnType).getTypeValue(Mockito.any(CallableStatement.class),
				Mockito.eq(2), Mockito.eq(JdbcTypesEnum.CLOB.getCode()), Mockito.eq(null));
	}

	@Test
	public void testNoIllegalArgumentButRequiresReplyException() {
		assertThatExceptionOfType(ReplyRequiredException.class)
				.isThrownBy(() -> this.getMessageChannel.send(new GenericMessage<>("foo")));
	}

	static class Counter {

		private final AtomicInteger count = new AtomicInteger();

		public Integer next() {
			if (count.get() > 2) {
				//prevent message overload
				return null;
			}
			return count.incrementAndGet();
		}

	}

	/**
	 * This class is called by the Service Activator and populates {@link Consumer#messages}
	 * with the Gateway's response message and is used by the Test to verify that
	 * the Gateway executed correctly.
	 */
	static class Consumer {

		private volatile BlockingQueue<Message<Collection<User>>> messages = new LinkedBlockingQueue<>();

		@ServiceActivator
		public void receive(Message<Collection<User>> message) {
			messages.add(message);
		}

		Message<Collection<User>> poll(long timeoutInMillis) throws InterruptedException {
			return messages.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
		}

	}

}

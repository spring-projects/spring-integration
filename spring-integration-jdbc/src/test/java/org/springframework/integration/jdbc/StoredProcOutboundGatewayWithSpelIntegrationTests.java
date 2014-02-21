/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.CallableStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.jdbc.config.JdbcTypesEnum;
import org.springframework.integration.jdbc.storedproc.User;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.Jackson2JsonMessageParser;
import org.springframework.integration.support.json.JsonInboundMessageMapper;
import org.springframework.integration.support.json.JsonOutboundMessageMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlReturnType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext // close at the end after class
public class StoredProcOutboundGatewayWithSpelIntegrationTests {

	@Autowired
	private AbstractApplicationContext context;

	@Autowired
	private Consumer consumer;

	@Autowired
	@Qualifier("startChannel")
	DirectChannel channel;

	@Autowired
	DirectChannel getMessageChannel;

	@Autowired
	PollableChannel output2Channel;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	SqlReturnType clobSqlReturnType;

	@Test
	@DirtiesContext
	public void executeStoredProcedureWithMessageHeader() throws Exception {

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

		List<Message<Collection<User>>> received = new ArrayList<Message<Collection<User>>>();

		received.add(consumer.poll(2000));

		Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(received.size()));

		Message<Collection<User>> message = received.get(0);

		context.stop();
		assertNotNull(message);

		assertNotNull(message.getPayload());
		assertNotNull(message.getPayload() instanceof Collection<?>);

		Collection<User> allUsers = message.getPayload();

		assertTrue(allUsers.size() == 2);

	}

	@Test
	@DirtiesContext
	public void testWithMissingMessageHeader() throws Exception {

		User user1 = new User("First User", "my first password", "email1");

		Message<User> user1Message = MessageBuilder.withPayload(user1).build();

		try {
			channel.send(user1Message);
		} catch (MessageHandlingException e) {

			String expectedMessage = "Unable to resolve Stored Procedure/Function name " +
					"for the provided Expression 'headers['my_stored_procedure']'.";
			String actualMessage = e.getCause().getMessage();
			Assert.assertEquals(expectedMessage, actualMessage);
			return;
		}

		Assert.fail("Expected a MessageHandlingException to be thrown.");

	}

	@Test
	@Transactional
	public void testInt2865SqlReturnType() throws Exception {
		Message<String> testMessage = MessageBuilder.withPayload("TEST").setHeader("FOO", "BAR").build();
		String messageId = testMessage.getHeaders().getId().toString();
		String jsonMessage = new JsonOutboundMessageMapper().fromMessage(testMessage);

		this.jdbcTemplate.update("INSERT INTO json_message VALUES (?,?)", messageId, jsonMessage);

		this.getMessageChannel.send(new GenericMessage<String>(messageId));
		Message<?> resultMessage = this.output2Channel.receive(1000);
		assertNotNull(resultMessage);
		Object resultPayload = resultMessage.getPayload();
		assertTrue(resultPayload instanceof String);
		Message<?> message = new JsonInboundMessageMapper(String.class, new Jackson2JsonMessageParser()).toMessage((String) resultPayload);
		assertEquals(testMessage.getPayload(), message.getPayload());
		assertEquals(testMessage.getHeaders().get("FOO"), message.getHeaders().get("FOO"));
		Mockito.verify(clobSqlReturnType).getTypeValue(Mockito.any(CallableStatement.class),
				Mockito.eq(2), Mockito.eq(JdbcTypesEnum.CLOB.getCode()), Mockito.eq((String) null));
	}

	static class Counter {

		private final AtomicInteger count = new AtomicInteger();

		public Integer next() throws InterruptedException {
			if (count.get()>2){
				//prevent message overload
				return null;
			}
			return Integer.valueOf(count.incrementAndGet());
		}
	}

	/**
	 * This class is called by the Service Activator and populates {@link Consumer#messages}
	 * with the Gateway's response message and is used by the Test to verify that
	 * the Gateway executed correctly.
	 */
	static class Consumer {

		private volatile BlockingQueue<Message<Collection<User>>> messages = new LinkedBlockingQueue<Message<Collection<User>>>();

		@ServiceActivator
		public void receive(Message<Collection<User>> message) {
			messages.add(message);
		}

		Message<Collection<User>> poll(long timeoutInMillis) throws InterruptedException {
			return messages.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
		}

	}
}

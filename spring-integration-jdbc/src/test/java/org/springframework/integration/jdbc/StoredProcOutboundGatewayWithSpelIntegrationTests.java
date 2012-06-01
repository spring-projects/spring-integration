/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.jdbc.storedproc.User;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gunnar Hillert
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StoredProcOutboundGatewayWithSpelIntegrationTests {

	@Autowired
	private AbstractApplicationContext context;

	@Autowired
	private Consumer consumer;

	@Autowired
	@Qualifier("startChannel")
	DirectChannel channel;

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

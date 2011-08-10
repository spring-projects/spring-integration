/*
 * Copyright 2007-2011 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.redis.store;

import org.junit.Test;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.store.MessageGroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Oleg Zhurakousky
 *
 */
public class RedisMessageGroupStoreTests extends RedisAvailableTests {

	@Test
	@RedisAvailable
	public void testNonExistingEmptyMessageGroup(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		
		MessageGroup messageGroup = store.getMessageGroup(1);
		assertNotNull(messageGroup);
		assertTrue(messageGroup instanceof RedisMessageGroup);
		assertEquals(0, messageGroup.size());
	}
	
	@Test
	@RedisAvailable
	public void testMessageGroupWithAddedMessageViaMessageGroup(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		((RedisMessageGroup)messageGroup).add(message);
		assertEquals(1, messageGroup.size());
	}
	
	@Test
	@RedisAvailable
	public void testMessageGroupWithAddedMessageViaMessageGroupStore(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		store.addMessageToGroup(1, message);
		assertEquals(1, messageGroup.size());
	}
	
	@Test
	@RedisAvailable
	public void testRemoveMessageGroup(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		((RedisMessageGroup)messageGroup).add(message);
		assertEquals(1, messageGroup.size());
		
		store.removeMessageGroup(1);
		messageGroup = store.getMessageGroup(1);
		assertEquals(0, messageGroup.size());
	}
	
	@Test
	@RedisAvailable
	public void testMarkAllMessagesInMessageGroup(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(1);
		((RedisMessageGroup)messageGroup).add(new GenericMessage<String>("1"));
		((RedisMessageGroup)messageGroup).add(new GenericMessage<String>("2"));
		((RedisMessageGroup)messageGroup).add(new GenericMessage<String>("3"));
		((RedisMessageGroup)messageGroup).markAll();
		// No need to assert for anything here.
		// Assertion is done within the method itself and the exception would be thrown if move did not succeed
	}
	
	@Test
	@RedisAvailable
	public void testGetOneFromMessageGroup(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(1);
		((RedisMessageGroup)messageGroup).add(new GenericMessage<String>("1"));
		((RedisMessageGroup)messageGroup).add(new GenericMessage<String>("2"));
		((RedisMessageGroup)messageGroup).add(new GenericMessage<String>("3"));
		Message<?> message =  messageGroup.getOne(); 
		assertEquals("1", message.getPayload());
	}
	
}

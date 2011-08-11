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
import org.springframework.integration.support.MessageBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
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
	public void testRemoveMessageFromTheGroup(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("2");
		((RedisMessageGroup)messageGroup).add(new GenericMessage<String>("1"));
		((RedisMessageGroup)messageGroup).add(message);
		((RedisMessageGroup)messageGroup).add(new GenericMessage<String>("3"));
		assertEquals(3, messageGroup.size());
		
		store.removeMessageFromGroup(1, message);
		assertEquals(2, messageGroup.size());
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
		assertEquals(3, messageGroup.getUnmarked().size());
		assertEquals(0, messageGroup.getMarked().size());
		((RedisMessageGroup)messageGroup).markAll();
		assertEquals(0, messageGroup.getUnmarked().size());
		assertEquals(3, messageGroup.getMarked().size());
	}
	
	@Test
	@RedisAvailable
	public void testMarkMessageInMessageGroup(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> messageToMark = new GenericMessage<String>("1");
		((RedisMessageGroup)messageGroup).add(messageToMark);
		((RedisMessageGroup)messageGroup).add(new GenericMessage<String>("2"));
		((RedisMessageGroup)messageGroup).add(new GenericMessage<String>("3"));
		assertEquals(3, messageGroup.getUnmarked().size());
		assertEquals(0, messageGroup.getMarked().size());
		store.markMessageFromGroup(1, messageToMark);
		assertEquals(2, messageGroup.getUnmarked().size());
		assertEquals(1, messageGroup.getMarked().size());
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
	
	@Test
	@RedisAvailable
	public void testAddToMessageGroup(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		MessageGroup messageGroup = store.getMessageGroup(1);
		
		store.addMessageToGroup(1, new GenericMessage<String>("1"));
		assertEquals(1, messageGroup.size());
		store.addMessageToGroup(1, new GenericMessage<String>("2"));
		assertEquals(2, messageGroup.size());
		store.addMessageToGroup(1, new GenericMessage<String>("3"));
		assertEquals(3, messageGroup.size());
	}
	
	@Test
	@RedisAvailable
	public void testCanAddToMessageGroup(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		
		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> m1 = MessageBuilder.withPayload("1").setSequenceNumber(1).setSequenceSize(3).setCorrelationId(1).build();
		Message<?> m2 = MessageBuilder.withPayload("2").setSequenceNumber(2).setSequenceSize(3).setCorrelationId(1).build();
		Message<?> m3 = MessageBuilder.withPayload("3").setSequenceNumber(3).setSequenceSize(3).setCorrelationId(1).build();
		
		if (messageGroup.canAdd(m1)){
			store.addMessageToGroup(1, m1);
		}
		assertEquals(1, messageGroup.size());
		
		if (messageGroup.canAdd(m2)){
			store.addMessageToGroup(1, m2);
		}
		assertEquals(2, messageGroup.size());
		
		if (messageGroup.canAdd(m3)){
			store.addMessageToGroup(1, m3);
		}
		assertEquals(3, messageGroup.size());
		
		if (messageGroup.canAdd(m3)){
			store.addMessageToGroup(1, m3);
		}
		assertEquals(3, messageGroup.size());
	}
	
	@Test
	@RedisAvailable
	public void testSameInstance(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup mg1 = store.getMessageGroup(1);
		MessageGroup mg2 = store.getMessageGroup(1);
		
		assertEquals(mg1, mg2);
		
		store.removeMessageGroup(1);
		
		mg2 = store.getMessageGroup(1);
		assertNotSame(mg1, mg2);
	}
	
}

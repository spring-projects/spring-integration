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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Oleg Zhurakousky
 *
 */
public class RedisMessageGroupStoreTests extends RedisAvailableTests {

	@Test
	@RedisAvailable
	public void testNonExistingEmptyMessageGroup() throws Exception{	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		
		MessageGroup messageGroup = store.getMessageGroup(1);
		assertNotNull(messageGroup);
		assertTrue(messageGroup instanceof SimpleMessageGroup);
		assertEquals(0, messageGroup.size());
	}
	
	@Test
	@RedisAvailable
	public void testMessageGroupWithAddedMessage() throws Exception{	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		messageGroup = store.addMessageToGroup(1, message);
		assertEquals(1, messageGroup.size());
		
		// make sure the store is properly rebuild from Redis
		store = new RedisMessageStore(jcf);

		messageGroup = store.getMessageGroup(1);
		assertEquals(1, messageGroup.size());
	}
	
	@Test
	@RedisAvailable
	public void testRemoveMessageGroup() throws Exception{	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		messageGroup = store.addMessageToGroup(messageGroup.getGroupId(), message);
		assertEquals(1, messageGroup.size());
		
		store.removeMessageGroup(1);
		MessageGroup messageGroupA = store.getMessageGroup(1);
		assertNotSame(messageGroup, messageGroupA);
//		assertEquals(0, messageGroupA.getMarked().size());
		assertEquals(0, messageGroupA.getMessages().size());
		assertEquals(0, messageGroupA.size());
		
		// make sure the store is properly rebuild from Redis
		store = new RedisMessageStore(jcf);

		messageGroup = store.getMessageGroup(1);
		
		assertEquals(0, messageGroup.getMessages().size());
		assertEquals(0, messageGroup.size());
	}
	
	@Test
	@RedisAvailable
	public void testCompleteMessageGroup() throws Exception{	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		messageGroup = store.addMessageToGroup(messageGroup.getGroupId(), message);
		store.completeGroup(messageGroup.getGroupId());
		messageGroup = store.getMessageGroup(1);
		assertTrue(messageGroup.isComplete());
	}
	
	@Test
	@RedisAvailable
	public void testLastReleasedSequenceNumber() throws Exception{	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		messageGroup = store.addMessageToGroup(messageGroup.getGroupId(), message);
		store.setLastReleasedSequenceNumberForGroup(messageGroup.getGroupId(), 5);
		messageGroup = store.getMessageGroup(1);
		assertEquals(5, messageGroup.getLastReleasedMessageSequenceNumber());
	}
	
	@Test
	@RedisAvailable
	public void testRemoveMessageFromTheGroup() throws Exception{	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("2");
		store.addMessageToGroup(messageGroup.getGroupId(), new GenericMessage<String>("1"));
		store.addMessageToGroup(messageGroup.getGroupId(), message);
		messageGroup = store.addMessageToGroup(messageGroup.getGroupId(), new GenericMessage<String>("3"));
		assertEquals(3, messageGroup.size());
		
		messageGroup = store.removeMessageFromGroup(1, message);
		assertEquals(2, messageGroup.size());
		
		// make sure the store is properly rebuild from Redis
		store = new RedisMessageStore(jcf);

		messageGroup = store.getMessageGroup(1);
		assertEquals(2, messageGroup.size());
	}
	@Test
	@RedisAvailable
	public void testRemoveNonExistingMessageFromTheGroup() throws Exception{	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		MessageGroup messageGroup = store.getMessageGroup(1);
		store.addMessageToGroup(messageGroup.getGroupId(), new GenericMessage<String>("1"));
		store.removeMessageFromGroup(1, new GenericMessage<String>("2"));
	}
	
	@Test
	@RedisAvailable
	public void testRemoveNonExistingMessageFromNonExistingTheGroup() throws Exception{	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		store.removeMessageFromGroup(1, new GenericMessage<String>("2"));
	}
	
	
	
	@Test
	@RedisAvailable
	public void testMultipleInstancesOfGroupStore() throws Exception{	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store1 = new RedisMessageStore(jcf);
		
		RedisMessageStore store2 = new RedisMessageStore(jcf);
		
		Message<?> message = new GenericMessage<String>("1");
		store1.addMessageToGroup(1, message);
		MessageGroup messageGroup = store2.addMessageToGroup(1, new GenericMessage<String>("2"));
		
		assertEquals(2, messageGroup.getMessages().size());
		
		RedisMessageStore store3 = new RedisMessageStore(jcf);
		
		messageGroup = store3.removeMessageFromGroup(1, message);
		
		assertEquals(1, messageGroup.getMessages().size());
	}
	
	@Test
	@RedisAvailable
	public void testIteratorOfMessageGroups() throws Exception{	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store1 = new RedisMessageStore(jcf);
		RedisMessageStore store2 = new RedisMessageStore(jcf);
		
		
		store1.addMessageToGroup(1, new GenericMessage<String>("1"));
		store2.addMessageToGroup(2, new GenericMessage<String>("2"));
		store1.addMessageToGroup(3, new GenericMessage<String>("3"));
		store2.addMessageToGroup(3, new GenericMessage<String>("3A"));
		
		Iterator<MessageGroup> messageGroups = store1.iterator();
		int counter = 0;
		while (messageGroups.hasNext()) {
			messageGroups.next();
			counter++;
		}
		assertEquals(3, counter);
		
		store2.removeMessageGroup(3);
		
		messageGroups = store1.iterator();
		counter = 0;
		while (messageGroups.hasNext()) {
			messageGroups.next();
			counter++;
		}
		assertEquals(2, counter);
	}
	
	@Test
	@RedisAvailable @Ignore
	public void testConcurrentModifications() throws Exception{	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		final RedisMessageStore store1 = new RedisMessageStore(jcf);
		final RedisMessageStore store2 = new RedisMessageStore(jcf);

		final Message<?> message = new GenericMessage<String>("1"); 

		ExecutorService executor = null;
		
		final List<Object> failures = new ArrayList<Object>();
		
		for (int i = 0; i < 100; i++) {
			executor = Executors.newCachedThreadPool();
			
			executor.execute(new Runnable() {	
				public void run() {			
					MessageGroup group = store1.addMessageToGroup(1, message);
					if (group.getMessages().size() != 1){
						failures.add("ADD");
						throw new AssertionFailedError("Failed on ADD");
					}	
				}
			});
			executor.execute(new Runnable() {	
				public void run() {
					MessageGroup group = store2.removeMessageFromGroup(1, message);
					if (group.getMessages().size() != 0){
						failures.add("REMOVE");
						throw new AssertionFailedError("Failed on Remove");
					}	
				}
			});
			
			executor.shutdown();
			executor.awaitTermination(10, TimeUnit.SECONDS);
			store2.removeMessageFromGroup(1, message); // ensures that if ADD thread executed after REMOVE, the store is empty for the next cycle
		}
		assertTrue(failures.size() == 0);
	}
	
	@Test
	@RedisAvailable
	public void testWithAggregatorWithShutdown(){	
		this.getConnectionFactoryForTest(); // for this test it only ensures that DB was flushed before test
		
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("redis-aggregator-config.xml", this.getClass());
		MessageChannel input = context.getBean("inputChannel", MessageChannel.class);
		QueueChannel output = context.getBean("outputChannel", QueueChannel.class);
		
		Message<?> m1 = MessageBuilder.withPayload("1").setSequenceNumber(1).setSequenceSize(3).setCorrelationId(1).build();
		Message<?> m2 = MessageBuilder.withPayload("2").setSequenceNumber(2).setSequenceSize(3).setCorrelationId(1).build();
		input.send(m1);
		assertNull(output.receive(1000));
		input.send(m2);
		assertNull(output.receive(1000));
		context.close();
		
		context = new ClassPathXmlApplicationContext("redis-aggregator-config.xml", this.getClass());
		input = context.getBean("inputChannel", MessageChannel.class);
		output = context.getBean("outputChannel", QueueChannel.class);
		
		Message<?> m3 = MessageBuilder.withPayload("3").setSequenceNumber(3).setSequenceSize(3).setCorrelationId(1).build();
		input.send(m3);
		assertNotNull(output.receive(1000));
	}
	
}

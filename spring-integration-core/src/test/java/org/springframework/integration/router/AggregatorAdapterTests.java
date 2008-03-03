/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;

/**
 * @author Marius Bogoevici
 */
public class AggregatorAdapterTests {

	private SimpleAggregator simpleAggregator;

	@Before
	public void setUp() {
		simpleAggregator = new SimpleAggregator();
	}

	@Test
	public void testAdapterWithMessageCollectionBasedMethod() {
		Aggregator aggregator = new AggregatorAdapter(simpleAggregator, "doAggregationOnCollectionOfMessages");
		Collection<Message<?>> messages = createCollectionOfMessages();	
		Message<?> returnedMessge = aggregator.aggregate(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals("123456789", returnedMessge.getPayload());
	}

	@Test
	public void testAdapterWithWildcardParametrizedMessageBasedMethod() {
		Aggregator aggregator = new AggregatorAdapter(simpleAggregator, "doAggregationOnCollectionOfMessagesParametrizedWithWildcard");
		Collection<Message<?>> messages = createCollectionOfMessages();	
		Message<?> returnedMessge = aggregator.aggregate(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals("123456789", returnedMessge.getPayload());
	}

	@Test
	public void testAdapterWithTypeParametrizedMessageBasedMethod() {
		Aggregator aggregator = new AggregatorAdapter(simpleAggregator, "doAggregationOnCollectionOfMessagesParametrizedWithString");
		Collection<Message<?>> messages = createCollectionOfMessages();	
		Message<?> returnedMessge = aggregator.aggregate(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals("123456789", returnedMessge.getPayload());
	}
	
	@Test
	public void testAdapterWithPojoBasedMethod() {
		Aggregator aggregator = new AggregatorAdapter(simpleAggregator, "doAggregationOnCollectionOfStrings");
		Collection<Message<?>> messages = createCollectionOfMessages();	
		Message<?> returnedMessge = aggregator.aggregate(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals("123456789", returnedMessge.getPayload());
	}

	@Test
	public void testAdapterWithPojoBasedMethodReturningObject() {
		Aggregator aggregator = new AggregatorAdapter(simpleAggregator, "doAggregationOnCollectionOfStringsReturningLong");
		Collection<Message<?>> messages = createCollectionOfMessages();	
		Message<?> returnedMessge = aggregator.aggregate(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals(123456789l, returnedMessge.getPayload());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAdapterWithWrongMethodName() {
		Aggregator aggregator = new AggregatorAdapter(simpleAggregator, "methodThatDoesNotExist");
	}
	
	private static Collection<Message<?>> createCollectionOfMessages() {
		Collection<Message<?>> messages =  new ArrayList<Message<?>>();
		messages.add(new GenericMessage<String>("123"));
		messages.add(new GenericMessage<String>("456"));  
		messages.add(new GenericMessage<String>("789"));
		return messages;
	}

	private class SimpleAggregator {

		private volatile boolean aggregationPerformed;


		public SimpleAggregator() {
			aggregationPerformed = false;
		}

		public Message<?> doAggregationOnCollectionOfMessages(Collection<Message> messages) {
			aggregationPerformed = true;
			StringBuffer buffer = new StringBuffer();
			for (Message<?> message : messages) {
				buffer.append(message.getPayload());
			}
			return new GenericMessage<String>(buffer.toString());
		}

		public Message<?> doAggregationOnCollectionOfMessagesParametrizedWithWildcard(Collection<Message<?>> messages) {
			aggregationPerformed = true;
			StringBuffer buffer = new StringBuffer();
			for (Message<?> message : messages) {
				buffer.append(message.getPayload());
			}
			return new GenericMessage<String>(buffer.toString());
		}
		
		public Message<?> doAggregationOnCollectionOfMessagesParametrizedWithString(Collection<Message<String>> messages) {
			aggregationPerformed = true;
			StringBuffer buffer = new StringBuffer();
			for (Message<String> message : messages) {
				buffer.append(message.getPayload());
			}
			return new GenericMessage<String>(buffer.toString());
		}

		public Message<?> doAggregationOnCollectionOfStrings(Collection<String> messages) {
			aggregationPerformed = true;
			StringBuffer buffer = new StringBuffer();
			for (String payload : messages) {
				buffer.append(payload);
			}
			return new GenericMessage<String>(buffer.toString());
		}

		public Long doAggregationOnCollectionOfStringsReturningLong(Collection<String> messages) {
			aggregationPerformed = true;
			StringBuffer buffer = new StringBuffer();
			for (String payload : messages) {
				buffer.append(payload);
			}
			return Long.parseLong(buffer.toString());
		}

		public boolean isAggregationPerformed() {
			return aggregationPerformed;
		}
		
	}
	
}

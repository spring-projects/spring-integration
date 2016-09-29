/* Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.dispatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.SerialExecutorChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.support.MessageBuilder;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Oleg Zhurakousky
 *
 */
public class SerialUnicastingDispatcherTests {
	
	@Test
	public void outOfSequenceTest() throws Exception{
		final List<String> sequenceFailed = new ArrayList<String>();
		
		ExecutorService executor = Executors.newFixedThreadPool(5);
		SerialExecutorChannel channel = new SerialExecutorChannel(executor);
		QueueChannel errorChannel = new QueueChannel();
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("errorChannel", errorChannel);
		channel.setBeanFactory(bf);
		channel.afterPropertiesSet();
		
		channel.subscribe(new MessageHandler() {
			
			private volatile Map<String, Integer> messageSequence = new HashMap<String, Integer>();
			
			public void handleMessage(Message<?> message) throws MessagingException {	
				Integer sequenceNumber = (Integer) message.getHeaders().getSequenceNumber();
				String serialId = (String) message.getHeaders().get(MessageHeaders.CORRELATION_ID);
				synchronized (serialId) {
					if (messageSequence.containsKey(serialId)){
						Integer currentSequence = messageSequence.get(serialId)+1;
						if (!sequenceNumber.equals(currentSequence)){
							sequenceFailed.add("Sequence failed for " + serialId + ". " + sequenceNumber + "-" + currentSequence);
						}
						messageSequence.put(serialId, sequenceNumber);
					}
					else {
						if (!sequenceNumber.equals(0)){
							sequenceFailed.add("Sequence failed for " + serialId + ". " + sequenceNumber + "-" + 0);
						}
						messageSequence.put(serialId, sequenceNumber);
					}
				}
				
				System.out.println("Handling Message: " + message);
			}
		});
		
		final Message<?> messageA = MessageBuilder.withPayload("hello").setHeader(MessageHeaders.CORRELATION_ID, "A").setHeader(MessageHeaders.SEQUENCE_NUMBER, 0).build();
		final Message<?> messageA1 = MessageBuilder.withPayload("hello").setHeader(MessageHeaders.CORRELATION_ID, "A").setHeader(MessageHeaders.SEQUENCE_NUMBER, 1).build();
		final Message<?> messageA2 = MessageBuilder.withPayload("hello").setHeader(MessageHeaders.CORRELATION_ID, "A").setHeader(MessageHeaders.SEQUENCE_NUMBER, 2).build();
		final Message<?> messageA3 = MessageBuilder.withPayload("hello").setHeader(MessageHeaders.CORRELATION_ID, "A").setHeader(MessageHeaders.SEQUENCE_NUMBER, 3).build();
		
		channel.send(messageA2);
		
		Thread.sleep(100);
		
		channel.send(messageA1);
		
		Thread.sleep(100);
		
		channel.send(messageA3);
		
		Thread.sleep(100);
		
		channel.send(messageA);
		
		executor.shutdown();
		executor.awaitTermination(5000, TimeUnit.SECONDS);
		
		assertTrue(sequenceFailed.isEmpty());
		assertNull(errorChannel.receive(1000));
	}
	
	@Test
	public void demoWithConfigNoNamespace(){
		ApplicationContext context = new ClassPathXmlApplicationContext("serial-dispatcher-config.xml", this.getClass());
		MessageChannel serialChannel = context.getBean("serialChannel", MessageChannel.class);
		
		Message<?> messageA = MessageBuilder.withPayload("A").setHeader(MessageHeaders.CORRELATION_ID, "A").setHeader(MessageHeaders.SEQUENCE_NUMBER, 3).build();
		Message<?> messageA1 = MessageBuilder.withPayload("A").setHeader(MessageHeaders.CORRELATION_ID, "A").setHeader(MessageHeaders.SEQUENCE_NUMBER, 1).build();
		Message<?> messageA2 = MessageBuilder.withPayload("A").setHeader(MessageHeaders.CORRELATION_ID, "A").setHeader(MessageHeaders.SEQUENCE_NUMBER, 2).build();
		Message<?> messageA3 = MessageBuilder.withPayload("A").setHeader(MessageHeaders.CORRELATION_ID, "A").setHeader(MessageHeaders.SEQUENCE_NUMBER, 0).build();
		
		Message<?> messageB = MessageBuilder.withPayload("B").setHeader(MessageHeaders.CORRELATION_ID, "B").setHeader(MessageHeaders.SEQUENCE_NUMBER, 2).build();
		Message<?> messageB1 = MessageBuilder.withPayload("B").setHeader(MessageHeaders.CORRELATION_ID, "B").setHeader(MessageHeaders.SEQUENCE_NUMBER, 1).build();
		Message<?> messageB2 = MessageBuilder.withPayload("B").setHeader(MessageHeaders.CORRELATION_ID, "B").setHeader(MessageHeaders.SEQUENCE_NUMBER, 0).build();
		
		serialChannel.send(messageB2);
		serialChannel.send(messageA1);
		serialChannel.send(messageA3);
		serialChannel.send(messageB1);
		serialChannel.send(messageA);
		serialChannel.send(messageA2);
		serialChannel.send(messageB);
		
	}

	@Test
	public void concurrencyWithLoadTest() throws Exception{
		ExecutorService executor = Executors.newCachedThreadPool();
		
		SerialExecutorChannel channel = new SerialExecutorChannel(executor);
		QueueChannel errorChannel = new QueueChannel();
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("errorChannel", errorChannel);
		channel.setBeanFactory(bf);
		channel.afterPropertiesSet();
		
		final List<String> sequenceFailed = new ArrayList<String>();
		
		channel.subscribe(new MessageHandler() {
			
			private volatile Map<String, Integer> messageSequence = new HashMap<String, Integer>();
			
			public void handleMessage(Message<?> message) throws MessagingException {	
				Integer sequenceNumber = (Integer) message.getHeaders().getSequenceNumber();
				String serialId = (String) message.getHeaders().get(MessageHeaders.CORRELATION_ID);
				synchronized (serialId) {
					if (messageSequence.containsKey(serialId)){
						Integer currentSequence = messageSequence.get(serialId)+1;
						if (!sequenceNumber.equals(currentSequence)){
							sequenceFailed.add("Sequence failed for " + serialId + ". " + sequenceNumber + "-" + currentSequence);
						}
						messageSequence.put(serialId, sequenceNumber);
					}
					else {
						if (!sequenceNumber.equals(0)){
							sequenceFailed.add("Sequence failed for " + serialId + ". " + sequenceNumber + "-" + 0);
						}
						messageSequence.put(serialId, sequenceNumber);
					}
				}
				
				System.out.println("Handling Message: " + message);
			}
		});
		String serialIdA = "A";
		String serialIdB = "B";
		String serialIdC = "C";
		for (int i = 0; i < 50; i++) {
			Message<?> messageA = MessageBuilder.withPayload("A"+i).setHeader(MessageHeaders.CORRELATION_ID, serialIdA).setHeader(MessageHeaders.SEQUENCE_NUMBER, i).build();
			Message<?> messageB = MessageBuilder.withPayload("B"+i).setHeader(MessageHeaders.CORRELATION_ID, serialIdB).setHeader(MessageHeaders.SEQUENCE_NUMBER, i).build();
			Message<?> messageC = MessageBuilder.withPayload("C"+i).setHeader(MessageHeaders.CORRELATION_ID, serialIdC).setHeader(MessageHeaders.SEQUENCE_NUMBER, i).build();
			channel.send(messageA);
			channel.send(messageB);
			channel.send(messageC);
		}
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		assertTrue(sequenceFailed.isEmpty());
		assertNull(errorChannel.receive(1000));
	}
}

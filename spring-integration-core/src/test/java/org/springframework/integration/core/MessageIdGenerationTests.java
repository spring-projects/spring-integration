/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.core;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessageHeaders.MessageIdGenerationStrategy;
import org.springframework.integration.message.GenericMessage;
import org.springframework.util.StopWatch;

/**
 * @author Oleg Zhurakousky
 *
 */
public class MessageIdGenerationTests {

	@Test
	public void testCustomIdGeneration(){
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("MessageIdGenerationTests-context.xml", this.getClass());
		MessageIdGenerationStrategy idGenerator = context.getBean("idGenerator", MessageIdGenerationStrategy.class);
		MessageChannel inputChannel = context.getBean("input", MessageChannel.class);
		inputChannel.send(new GenericMessage<Integer>(0));
		verify(idGenerator, times(4)).generateId();
		reset(idGenerator);
		context.destroy();
		new GenericMessage<Integer>(0);
		verify(idGenerator, times(0)).generateId();
	}
	
	@Test
	@Ignore
	public void performanceTest(){
		int times = 1000000;
		StopWatch watch = new StopWatch();
		watch.start();
		for (int i = 0; i < times; i++) {
			new GenericMessage<Integer>(0);
		}
		watch.stop();
		double defaultGeneratorElapsedTime = watch.getTotalTimeSeconds();
		
		MessageHeaders.setMessageIdGenerationStrategy(new MessageIdGenerationStrategy() {
			public UUID generateId() {
				return TimeBasedUUIDGenerator.generateId();
			}
		});
		watch = new StopWatch();
		watch.start();
		for (int i = 0; i < times; i++) {
			new GenericMessage<Integer>(0);
		}
		watch.stop();
		double timebasedGeneratorElapsedTime = watch.getTotalTimeSeconds();
		
		System.out.println("Generated " + times + " messages using default UUID generator " +
				"in " + defaultGeneratorElapsedTime + " seconds");
		System.out.println("Generated " + times + " messages using Timebased UUID generator " +
				"in " + timebasedGeneratorElapsedTime + " seconds");
		
		System.out.println(defaultGeneratorElapsedTime/timebasedGeneratorElapsedTime);
	
	}
	

	
	public static class SampleIdGenerator implements MessageIdGenerationStrategy {

		public UUID generateId() {
			return UUID.nameUUIDFromBytes(((System.currentTimeMillis() - System.nanoTime()) + "").getBytes());
		}
		
	}
	
	
}

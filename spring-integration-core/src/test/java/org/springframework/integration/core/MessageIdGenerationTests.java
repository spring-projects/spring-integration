/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.IdGenerator;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StopWatch;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 */
public class MessageIdGenerationTests {

	@Test
	public void testCustomIdGenerationWithParentRegistrar() throws Exception {
		ClassPathXmlApplicationContext parent = new ClassPathXmlApplicationContext("MessageIdGenerationTests-context-withGenerator.xml", this.getClass());
		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(new String[]{"MessageIdGenerationTests-context.xml"}, this.getClass(), parent);

		IdGenerator idGenerator = child.getBean("idGenerator", IdGenerator.class);
		MessageChannel inputChannel = child.getBean("input", MessageChannel.class);
		inputChannel.send(new GenericMessage<Integer>(0));
		verify(idGenerator, atLeastOnce()).generateId();
		child.close();
		parent.close();
		this.assertDestroy();
	}

	@Test
	public void testCustomIdGenerationWithParentChildIndependentCreation() throws Exception {
		ClassPathXmlApplicationContext parent = new ClassPathXmlApplicationContext("MessageIdGenerationTests-context-withGenerator.xml", this.getClass());
		GenericXmlApplicationContext child = new GenericXmlApplicationContext();
		child.load("classpath:/org/springframework/integration/core/MessageIdGenerationTests-context.xml");
		child.setParent(parent);
		child.refresh();

		IdGenerator idGenerator = child.getBean("idGenerator", IdGenerator.class);
		MessageChannel inputChannel = child.getBean("input", MessageChannel.class);
		inputChannel.send(new GenericMessage<Integer>(0));
		verify(idGenerator, atLeastOnce()).generateId();
		child.close();
		parent.close();
		this.assertDestroy();
	}

	@Test
	public void testCustomIdGenerationWithParentRegistrarClosed() throws Exception {
		ClassPathXmlApplicationContext parent = new ClassPathXmlApplicationContext("MessageIdGenerationTests-context-withGenerator.xml", this.getClass());
		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(new String[]{"MessageIdGenerationTests-context.xml"}, this.getClass(), parent);

		IdGenerator idGenerator = child.getBean("idGenerator", IdGenerator.class);
		MessageChannel inputChannel = child.getBean("input", MessageChannel.class);
		inputChannel.send(new GenericMessage<Integer>(0));
		verify(idGenerator, atLeastOnce()).generateId();
		parent.close();
		child.close();
		this.assertDestroy();
	}

	@Test
	public void testCustomIdGenerationWithChildRegistrar() throws Exception {
		ClassPathXmlApplicationContext parent = new ClassPathXmlApplicationContext("MessageIdGenerationTests-context.xml", this.getClass());
		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(new String[]{"MessageIdGenerationTests-context-withGenerator.xml"}, this.getClass(), parent);

		IdGenerator idGenerator = child.getBean("idGenerator", IdGenerator.class);
		Mockito.reset(idGenerator);
		MessageChannel inputChannel = child.getBean("input", MessageChannel.class);
		inputChannel.send(new GenericMessage<Integer>(0));
		verify(idGenerator, atLeastOnce()).generateId();
		child.close();
		parent.close();
		this.assertDestroy();
	}

	@Test
	public void testCustomIdGenerationWithChildRegistrarClosed() throws Exception {
		ClassPathXmlApplicationContext parent = new ClassPathXmlApplicationContext("MessageIdGenerationTests-context.xml", this.getClass());
		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(new String[]{"MessageIdGenerationTests-context-withGenerator.xml"}, this.getClass(), parent);

		IdGenerator idGenerator = child.getBean("idGenerator", IdGenerator.class);
		Mockito.reset(idGenerator);
		MessageChannel inputChannel = child.getBean("input", MessageChannel.class);
		inputChannel.send(new GenericMessage<Integer>(0));
		verify(idGenerator, atLeastOnce()).generateId();
		child.close();
		parent.close();
		this.assertDestroy();
	}

	// similar to the last test, but should not fail because child AC is closed before second child AC is started
	@Test
	public void testCustomIdGenerationWithParentChildIndependentCreationChildrenRegistrarsOneAtTheTime() throws Exception {
		ClassPathXmlApplicationContext parent = new ClassPathXmlApplicationContext("MessageIdGenerationTests-context.xml", this.getClass());

		GenericXmlApplicationContext childA = new GenericXmlApplicationContext();
		childA.load("classpath:/org/springframework/integration/core/MessageIdGenerationTests-context-withGenerator.xml");
		childA.setParent(parent);
		childA.refresh();

		childA.close();

		GenericXmlApplicationContext childB = new GenericXmlApplicationContext();
		childB.load("classpath:/org/springframework/integration/core/MessageIdGenerationTests-context-withGenerator.xml");
		childB.setParent(parent);
		childB.refresh();

		parent.close();
		childB.close();
		this.assertDestroy();
	}

	@Test
	@Ignore
	public void performanceTest() {
		int times = 1000000;
		StopWatch watch = new StopWatch();
		watch.start();
		for (int i = 0; i < times; i++) {
			new GenericMessage<Integer>(0);
		}
		watch.stop();
		double defaultGeneratorElapsedTime = watch.getTotalTimeSeconds();

		Field idGeneratorField = ReflectionUtils.findField(MessageHeaders.class, "idGenerator");
		ReflectionUtils.makeAccessible(idGeneratorField);
		ReflectionUtils.setField(idGeneratorField, null, new IdGenerator() {
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

		System.out.println("Time-based ID generator is " + defaultGeneratorElapsedTime/timebasedGeneratorElapsedTime + " times faster");
	}

	private void assertDestroy() throws Exception {
		Field idGenField = ReflectionUtils.findField(MessageHeaders.class, "idGenerator");
		ReflectionUtils.makeAccessible(idGenField);
		assertNull("the idGenerator field has not been properly reset to null", idGenField.get(null));
	}


	public static class SampleIdGenerator implements IdGenerator {
		public UUID generateId() {
			return UUID.nameUUIDFromBytes(((System.currentTimeMillis() - System.nanoTime()) + "").getBytes());
		}
	}
}

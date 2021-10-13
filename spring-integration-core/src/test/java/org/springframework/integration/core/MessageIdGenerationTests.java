/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.integration.test.util.TestUtils;
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
 * @author Artem Bilan
 */
public class MessageIdGenerationTests {

	private final Log logger = LogFactory.getLog(getClass());

	@Test
	public void testCustomIdGenerationWithParentRegistrar() {
		try (ClassPathXmlApplicationContext parent =
				new ClassPathXmlApplicationContext("MessageIdGenerationTests-context-withGenerator.xml", getClass());
				ClassPathXmlApplicationContext child =
						new ClassPathXmlApplicationContext(new String[]{ "MessageIdGenerationTests-context.xml" },
								getClass(), parent)) {
			IdGenerator idGenerator = child.getBean("idGenerator", IdGenerator.class);
			MessageChannel inputChannel = child.getBean("input", MessageChannel.class);
			inputChannel.send(new GenericMessage<>(0));
			verify(idGenerator, atLeastOnce()).generateId();
		}
		assertDestroy();
	}

	@Test
	public void testCustomIdGenerationWithParentChildIndependentCreation() {
		try (ClassPathXmlApplicationContext parent =
				new ClassPathXmlApplicationContext("MessageIdGenerationTests-context-withGenerator.xml", getClass());
				GenericXmlApplicationContext child = new GenericXmlApplicationContext()) {

			child.load("classpath:/org/springframework/integration/core/MessageIdGenerationTests-context.xml");
			child.setParent(parent);
			child.refresh();

			IdGenerator idGenerator = child.getBean("idGenerator", IdGenerator.class);
			MessageChannel inputChannel = child.getBean("input", MessageChannel.class);
			inputChannel.send(new GenericMessage<>(0));
			verify(idGenerator, atLeastOnce()).generateId();
		}
		assertDestroy();
	}

	@Test
	public void testCustomIdGenerationWithChildRegistrar() {
		try (ClassPathXmlApplicationContext parent =
				new ClassPathXmlApplicationContext("MessageIdGenerationTests-context.xml", getClass());
				ClassPathXmlApplicationContext child =
						new ClassPathXmlApplicationContext(
								new String[]{ "MessageIdGenerationTests-context-withGenerator.xml" },
								getClass(), parent)) {

			IdGenerator idGenerator = child.getBean("idGenerator", IdGenerator.class);
			Mockito.reset(idGenerator);
			MessageChannel inputChannel = child.getBean("input", MessageChannel.class);
			inputChannel.send(new GenericMessage<>(0));
			verify(idGenerator, atLeastOnce()).generateId();
		}
		assertDestroy();
	}

	@Test
	@Disabled
	public void performanceTest() {
		int times = 1000000;
		StopWatch watch = new StopWatch();
		watch.start();
		for (int i = 0; i < times; i++) {
			new GenericMessage<>(0);
		}
		watch.stop();
		double defaultGeneratorElapsedTime = watch.getTotalTimeSeconds();

		Field idGeneratorField = ReflectionUtils.findField(MessageHeaders.class, "idGenerator");
		ReflectionUtils.makeAccessible(idGeneratorField);
		ReflectionUtils.setField(idGeneratorField, null, (IdGenerator) TimeBasedUUIDGenerator::generateId);
		watch = new StopWatch();
		watch.start();
		for (int i = 0; i < times; i++) {
			new GenericMessage<>(0);
		}
		watch.stop();
		double timeBasedGeneratorElapsedTime = watch.getTotalTimeSeconds();

		logger.info("Generated " + times + " messages using default UUID generator " +
				"in " + defaultGeneratorElapsedTime + " seconds");
		logger.info("Generated " + times + " messages using time-based UUID generator " +
				"in " + timeBasedGeneratorElapsedTime + " seconds");

		logger.info("Time-based ID generator is " + defaultGeneratorElapsedTime / timeBasedGeneratorElapsedTime +
				" times faster");
	}

	private void assertDestroy() {
		assertThat(TestUtils.getPropertyValue(new MessageHeaders(null), "idGenerator")).isNull();
	}


	public static class SampleIdGenerator implements IdGenerator {

		@Override
		public UUID generateId() {
			return UUID.nameUUIDFromBytes(((System.currentTimeMillis() - System.nanoTime()) + "").getBytes());
		}

	}

}

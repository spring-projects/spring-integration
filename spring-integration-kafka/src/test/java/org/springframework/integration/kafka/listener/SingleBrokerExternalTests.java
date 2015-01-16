/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.integration.kafka.listener;

import java.util.UUID;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.kafka.rule.KafkaRule;
import org.springframework.integration.kafka.rule.KafkaRunning;

/**
 * @author Marius Bogoevici
 */
@Ignore
public class SingleBrokerExternalTests extends AbstractMessageListenerContainerTests {

	@ClassRule
	public static final KafkaRunning kafkaRunningRule = KafkaRunning.isRunning();

	@Override
	public KafkaRule getKafkaRule() {
		return kafkaRunningRule;
	}

	@Override
	@After
	public void cleanUp() {
		// do nothing, each topic must be created individually
	}

	private String generateTopicName() {
		return "test" + UUID.randomUUID().toString().replace("-","");
	}

	@Test
	public void testLowVolumeLowConcurrency() throws Exception {
		String topicName = generateTopicName();
		createTopic(topicName, 5, 1, 1);
		runMessageListenerTest(100, 2, 5, 100, 1, 0, topicName);
		deleteTopic(topicName);
	}

	@Test
	public void testMediumVolumeLowConcurrency() throws Exception {
		String topicName = generateTopicName();
		createTopic(topicName, 5, 1, 1);
		runMessageListenerTest(100, 2, 5, 1000, 1, 0, topicName);
		deleteTopic(topicName);
	}

	@Test
	@Ignore
	public void testHighVolumeLowConcurrency() throws Exception {
		String topicName = generateTopicName();
		createTopic(topicName, 5, 1, 1);
		runMessageListenerTest(100, 2, 5, 10000, 1, 0, topicName);
		deleteTopic(topicName);
	}

	@Test
	public void testLowVolumeMediumConcurrency() throws Exception {
		String topicName = generateTopicName();
		createTopic(topicName, 5, 1, 1);
		runMessageListenerTest(100, 5, 5, 100, 1, 0, topicName);
		deleteTopic(topicName);
	}

	@Test
	public void testMediumVolumeMediumConcurrency() throws Exception {
		String topicName = generateTopicName();
		createTopic(topicName, 5, 1, 1);
		runMessageListenerTest(100, 5, 5, 1000, 1, 0, topicName);
		deleteTopic(topicName);
	}

	@Test
	@Ignore
	public void testHighVolumeMediumConcurrency() throws Exception {
		String topicName = generateTopicName();
		createTopic(topicName, 5, 1, 1);
		runMessageListenerTest(100, 5, 5, 100000, 1, 0, topicName);
		deleteTopic(topicName);
	}

	@Test
	@Ignore
	public void testLowVolumeHighConcurrency() throws Exception {
		String topicName = generateTopicName();
		createTopic(topicName, 100, 1, 1);
		runMessageListenerTest(100, 20, 100, 1000, 1, 0, topicName);
		deleteTopic(topicName);
	}

	@Test
	@Ignore
	public void testMediumVolumeHighConcurrency() throws Exception {
		String topicName = generateTopicName();
		createTopic(topicName, 100, 1, 1);
		runMessageListenerTest(100, 20, 100, 10000, 1, 0, topicName);
		deleteTopic(topicName);
	}

	@Test
	@Ignore
	public void testHighVolumeHighConcurrency() throws Exception {
		String topicName = generateTopicName();
		createTopic(topicName, 100, 1, 1);
		runMessageListenerTest(100, 20, 100, 100000, 1, 0, topicName);
		deleteTopic(topicName);
	}

}

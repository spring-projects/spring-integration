/*
 * Copyright 2015 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.config.xml;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.concurrent.Executor;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.listener.ErrorHandler;
import org.springframework.integration.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.integration.kafka.listener.OffsetManager;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import kafka.serializer.Decoder;

/**
 * @author Artem Bilan.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class KafkaMessageDrivenChannelAdapterParserTests {

	@Autowired
	private ConnectionFactory connectionFactory;

	@Autowired
	private OffsetManager offsetManager;

	@Autowired
	private ErrorHandler errorHandler;

	@Autowired
	private Executor executor;

	@Autowired
	private Decoder<?> keyDecoder;

	@Autowired
	private Decoder<?> payloadDecoder;

	@Autowired
	private NullChannel nullChannel;

	@Autowired
	private PublishSubscribeChannel errorChannel;

	@Autowired
	private KafkaMessageDrivenChannelAdapter kafkaListener;

	@Test
	public void testKafkaMessageDrivenChannelAdapterParser() {
		assertFalse(this.kafkaListener.isAutoStartup());
		assertFalse(this.kafkaListener.isRunning());
		assertEquals(100, this.kafkaListener.getPhase());
		assertSame(this.nullChannel, TestUtils.getPropertyValue(this.kafkaListener, "outputChannel"));
		assertSame(this.errorChannel, TestUtils.getPropertyValue(this.kafkaListener, "errorChannel"));
		assertSame(this.keyDecoder, TestUtils.getPropertyValue(this.kafkaListener, "keyDecoder"));
		assertSame(this.payloadDecoder, TestUtils.getPropertyValue(this.kafkaListener, "payloadDecoder"));
		KafkaMessageListenerContainer container =
				TestUtils.getPropertyValue(this.kafkaListener, "messageListenerContainer",
						KafkaMessageListenerContainer.class);
		assertSame(this.connectionFactory, TestUtils.getPropertyValue(container, "kafkaTemplate.connectionFactory"));
		assertSame(this.offsetManager, container.getOffsetManager());
		assertSame(this.errorHandler, container.getErrorHandler());
		assertSame(this.executor, container.getFetchTaskExecutor());
		assertEquals(10, container.getConcurrency());
		assertEquals(1000, container.getMaxFetch());
		assertEquals(1024, container.getQueueSize());
		assertEquals(1024, container.getQueueSize());
		assertArrayEquals(new String[] {"foo", "bar"}, TestUtils.getPropertyValue(container, "topics", String[].class));
	}

}

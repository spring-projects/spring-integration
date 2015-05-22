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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.KafkaMessageMetadata;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.listener.Acknowledgment;
import org.springframework.integration.kafka.listener.ErrorHandler;
import org.springframework.integration.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.integration.kafka.listener.OffsetManager;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;

import kafka.serializer.Decoder;

/**
 * @author Artem Bilan.
 * @author Gary Russell
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

	@Autowired
	private KafkaMessageDrivenChannelAdapter withMBFactoryOverrideAndId;

	@Autowired
	private KafkaMessageDrivenChannelAdapter withMBFactoryOverrideAndTS;

	@Autowired
	private KafkaMessageDrivenChannelAdapter withOverrideIdTS;

	@Test
	public void testKafkaMessageDrivenChannelAdapterParser() throws Exception {
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
		assertEquals(5000, container.getStopTimeout());
		assertArrayEquals(new String[] {"foo", "bar"}, TestUtils.getPropertyValue(container, "topics", String[].class));
		assertOverrides(this.kafkaListener, false, false, false, true);
		assertOverrides(this.withMBFactoryOverrideAndId, true, true, false, false);
		assertOverrides(this.withMBFactoryOverrideAndTS, true, false, true, true);
		assertOverrides(this.withOverrideIdTS, false, true, true, true);

		final AtomicReference<Method> toMessage = new AtomicReference<Method>();
		ReflectionUtils.doWithMethods(KafkaMessageDrivenChannelAdapter.class, new MethodCallback() {

			@Override
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				method.setAccessible(true);
				toMessage.set(method);
;			}
		},
		new MethodFilter() {

			@Override
			public boolean matches(Method method) {
				return method.getName().equals("toMessage");
			}
		});

		Message<?> m = getAMessageFrom(this.kafkaListener, toMessage.get());
		assertNull(m.getHeaders().getId());
		assertNull(m.getHeaders().getTimestamp());
		assertNull(m.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT));
		assertRest(m);

		m = getAMessageFrom(this.withMBFactoryOverrideAndId, toMessage.get());
		assertThat(m, Matchers.instanceOf(GenericMessage.class));
		assertNotNull(m.getHeaders().getId());
		assertNotNull(m.getHeaders().getTimestamp());
		assertNotNull(m.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT));
		assertRest(m);

		m = getAMessageFrom(this.withMBFactoryOverrideAndTS, toMessage.get());
		assertThat(m, Matchers.instanceOf(GenericMessage.class));
		assertNotNull(m.getHeaders().getId());
		assertNotNull(m.getHeaders().getTimestamp());
		assertNull(m.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT));
		assertRest(m);

		m = getAMessageFrom(this.withOverrideIdTS, toMessage.get());
		assertNotNull(m.getHeaders().getId());
//TODO org.springframework.messaging.support.MessageBuilder doesn't support simple way
// to provide TIMESTAMP generation option.
//		assertNotNull(m.getHeaders().getTimestamp());
		assertNull(m.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT));
		assertRest(m);
	}

	private void assertOverrides(KafkaMessageDrivenChannelAdapter kafkaListener, boolean mbf, boolean id, boolean ts,
			boolean ac) {
		assertThat(TestUtils.getPropertyValue(kafkaListener, "autoCommitOffset", Boolean.class), equalTo(ac));
		assertThat(TestUtils.getPropertyValue(kafkaListener, "useMessageBuilderFactory", Boolean.class), equalTo(mbf));
		assertThat(TestUtils.getPropertyValue(kafkaListener, "generateMessageId", Boolean.class), equalTo(id));
		assertThat(TestUtils.getPropertyValue(kafkaListener, "generateTimestamp", Boolean.class), equalTo(ts));
	}

	private void assertRest(Message<?> m) {
		assertEquals("bar",  m.getPayload());
		assertEquals("foo", m.getHeaders().get(KafkaHeaders.MESSAGE_KEY));
		assertEquals("topic", m.getHeaders().get(KafkaHeaders.TOPIC));
		assertEquals(42, m.getHeaders().get(KafkaHeaders.PARTITION_ID));
		assertEquals(1L, m.getHeaders().get(KafkaHeaders.OFFSET));
		assertEquals(2L, m.getHeaders().get(KafkaHeaders.NEXT_OFFSET));
	}

	private Message<?> getAMessageFrom(KafkaMessageDrivenChannelAdapter adapter, Method toMessage) throws Exception {
		KafkaMessageMetadata meta = mock(KafkaMessageMetadata.class);
		Partition partition = mock(Partition.class);
		when(partition.getTopic()).thenReturn("topic");
		when(partition.getId()).thenReturn(42);
		when(meta.getPartition()).thenReturn(partition);
		when(meta.getOffset()).thenReturn(1L);
		when(meta.getNextOffset()).thenReturn(2L);
		Acknowledgment ack = mock(Acknowledgment.class);
		return (Message<?>) toMessage.invoke(adapter, "foo", "bar", meta, ack);
	}

}

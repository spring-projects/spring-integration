/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.stream;

import java.io.StringReader;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class CharacterStreamSourceTests {

	@Test
	public void testEndOfStream() {
		StringReader reader = new StringReader("test");
		CharacterStreamReadingMessageSource source = new CharacterStreamReadingMessageSource(reader);
		source.setBeanFactory(mock(BeanFactory.class));
		Message<?> message1 = source.receive();
		assertThat(message1.getPayload()).isEqualTo("test");
		Message<?> message2 = source.receive();
		assertThat(message2).isNull();
	}

	@Test
	public void testEOF() {
		StringReader reader = new StringReader("test");
		CharacterStreamReadingMessageSource source = new CharacterStreamReadingMessageSource(reader, -1, true);
		ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
		source.setApplicationEventPublisher(publisher);
		source.setBeanFactory(mock(BeanFactory.class));
		Message<?> message1 = source.receive();
		assertThat(message1.getPayload()).isEqualTo("test");
		Message<?> message2 = source.receive();
		assertThat(message2).isNull();
		verify(publisher).publishEvent(any(StreamClosedEvent.class));
	}

	@Test
	public void testEOFIntegrationTest() throws Exception {
		StringReader reader = new StringReader("test");
		CharacterStreamReadingMessageSource source = new CharacterStreamReadingMessageSource(reader, -1, true);
		source.setBeanFactory(mock(BeanFactory.class));
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		CountDownLatch latch = new CountDownLatch(2);
		source.setApplicationEventPublisher(e -> {
			if (e instanceof StreamClosedEvent) {
				if (latch.getCount() == 1) {
					adapter.stop();
				}
				latch.countDown();
			}
		});
		adapter.setSource(source);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();
		adapter.setTaskScheduler(scheduler);
		adapter.setTrigger(new PeriodicTrigger(Duration.ofMillis(100)));
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();
		Message<?> received = out.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("test");
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(adapter.isRunning()).isFalse();
		scheduler.shutdown();
	}

}

/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.StringReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class CharacterStreamSourceTests {

	@Test
	public void testEndOfStream() {
		StringReader reader = new StringReader("test");
		CharacterStreamReadingMessageSource source = new CharacterStreamReadingMessageSource(reader);
		Message<?> message1 = source.receive();
		assertEquals("test", message1.getPayload());
		Message<?> message2 = source.receive();
		assertNull(message2);
	}

	@Test
	public void testEOF() {
		StringReader reader = new StringReader("test");
		CharacterStreamReadingMessageSource source = new CharacterStreamReadingMessageSource(reader, -1, true);
		ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
		source.setApplicationEventPublisher(publisher);
		Message<?> message1 = source.receive();
		assertEquals("test", message1.getPayload());
		Message<?> message2 = source.receive();
		assertNull(message2);
		verify(publisher).publishEvent(any(StreamClosedEvent.class));
	}

	@Test
	public void testEOFIntegrationTest() throws Exception {
		StringReader reader = new StringReader("test");
		CharacterStreamReadingMessageSource source = new CharacterStreamReadingMessageSource(reader, -1, true);
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
		adapter.setTrigger(new PeriodicTrigger(100));
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();
		Message<?> received = out.receive(10000);
		assertNotNull(received);
		assertEquals("test", received.getPayload());
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertFalse(adapter.isRunning());
		scheduler.shutdown();
	}

}

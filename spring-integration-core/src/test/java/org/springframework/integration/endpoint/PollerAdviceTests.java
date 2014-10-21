/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aopalliance.aop.Advice;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.scheduling.PollSkipAdvice;
import org.springframework.integration.scheduling.PollSkipStrategy;
import org.springframework.messaging.Message;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 * @since 4.1
 *
 */
public class PollerAdviceTests {

	@Test
	public void testDefaultDontSkip() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		final CountDownLatch latch = new CountDownLatch(1);
		adapter.setSource(new MessageSource<Object>() {

			@Override
			public Message<Object> receive() {
				latch.countDown();
				return null;
			}
		});
		adapter.setTrigger(new Trigger() {

			@Override
			public Date nextExecutionTime(TriggerContext triggerContext) {
				return new Date(System.currentTimeMillis() + 10);
			}
		});
		configure(adapter);
		List<Advice> adviceChain = new ArrayList<Advice>();
		PollSkipAdvice advice = new PollSkipAdvice();
		adviceChain.add(advice);
		adapter.setAdviceChain(adviceChain);
		adapter.afterPropertiesSet();
		adapter.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		adapter.stop();
	}

	@Test
	public void testSkipAll() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		final CountDownLatch latch = new CountDownLatch(1);
		adapter.setSource(new MessageSource<Object>() {

			@Override
			public Message<Object> receive() {
				latch.countDown();
				return null;
			}
		});
		adapter.setTrigger(new Trigger() {

			@Override
			public Date nextExecutionTime(TriggerContext triggerContext) {
				return new Date(System.currentTimeMillis() + 10);
			}
		});
		configure(adapter);
		List<Advice> adviceChain = new ArrayList<Advice>();
		PollSkipAdvice advice = new PollSkipAdvice(new PollSkipStrategy() {

			@Override
			public boolean skipPoll() {
				return true;
			}

		});
		adviceChain.add(advice);
		adapter.setAdviceChain(adviceChain);
		adapter.afterPropertiesSet();
		adapter.start();
		assertFalse(latch.await(1, TimeUnit.SECONDS));
		adapter.stop();
	}

	private void configure(SourcePollingChannelAdapter adapter) {
		adapter.setOutputChannel(new NullChannel());
		adapter.setBeanFactory(mock(BeanFactory.class));
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();
		adapter.setTaskScheduler(scheduler);
	}

}

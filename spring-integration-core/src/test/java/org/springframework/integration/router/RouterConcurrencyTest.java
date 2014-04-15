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
package org.springframework.integration.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class RouterConcurrencyTest {

	@Test
	public void test() throws Exception {
		final AtomicInteger count = new AtomicInteger();
		final Semaphore semaphore = new Semaphore(1);
		final AbstractMessageRouter router = new AbstractMessageRouter() {
			@Override
			protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
				return null;
			}

			@Override
			protected void setConversionService(ConversionService conversionService) {
				try {
					if (count.incrementAndGet() > 1) {
						Thread.sleep(2000);
					}
					super.setConversionService(conversionService);
					semaphore.release();
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

		};

		final AtomicInteger beanCounter = new AtomicInteger();
		BeanFactory beanFactory = mock(BeanFactory.class);
		doAnswer(new Answer<Boolean>() {

			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				if (beanCounter.getAndIncrement() < 2) {
					semaphore.tryAcquire(4, TimeUnit.SECONDS);
				}
				return false;
			}
		}).when(beanFactory).containsBean(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME);
		router.setBeanFactory(beanFactory);

		ExecutorService exec = Executors.newFixedThreadPool(2);
		final List<ConversionService> returns = Collections.synchronizedList(
				new ArrayList<ConversionService>());
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				ConversionService requiredConversionService = router.getRequiredConversionService();
				returns.add(requiredConversionService);
			}
		};
		exec.execute(runnable);
		exec.execute(runnable);
		exec.shutdown();
		exec.awaitTermination(10, TimeUnit.SECONDS);
		assertEquals(2, returns.size());
		assertNotNull(returns.get(0));
		assertNotNull(returns.get(1));
	}

}

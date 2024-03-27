/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class RouterConcurrencyTests {

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
			public void setConversionService(ConversionService conversionService) {
				try {
					if (count.incrementAndGet() > 1) {
						Thread.sleep(20);
					}
					super.setConversionService(conversionService);
					semaphore.release();
					Thread.sleep(10);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

		};

		final AtomicInteger beanCounter = new AtomicInteger();
		BeanFactory beanFactory = mock(BeanFactory.class);
		doAnswer(invocation -> {
			if (beanCounter.getAndIncrement() < 2) {
				semaphore.tryAcquire(4, TimeUnit.SECONDS);
			}
			return false;
		}).when(beanFactory).containsBean(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME);
		router.setBeanFactory(beanFactory);

		ExecutorService exec = Executors.newFixedThreadPool(2);
		final List<ConversionService> returns = Collections.synchronizedList(new ArrayList<>());
		Runnable runnable = () -> {
			ConversionService requiredConversionService = router.getRequiredConversionService();
			returns.add(requiredConversionService);
		};
		exec.execute(runnable);
		exec.execute(runnable);
		exec.shutdown();
		assertThat(exec.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
		assertThat(returns.size()).isEqualTo(2);
		assertThat(returns.get(0)).isNotNull();
		assertThat(returns.get(1)).isNotNull();
	}

}

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

package org.springframework.integration.jmx;

import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;

import org.junit.Test;

import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class AttributePollingMessageSourceTests {

	@Test
	public void basicPolling() throws Exception {
		MBeanServerFactoryBean factoryBean = new MBeanServerFactoryBean();
		factoryBean.afterPropertiesSet();
		MBeanServer server = factoryBean.getObject();
		TestCounter counter = new TestCounter();
		server.registerMBean(counter, ObjectNameManager.getInstance("test:name=counter"));
		AttributePollingMessageSource source = new AttributePollingMessageSource();
		source.setAttributeName("Count");
		source.setObjectName("test:name=counter");
		source.setServer(server);
		Message<?> message1 = source.receive();
		assertThat(message1).isNotNull();
		assertThat(message1.getPayload()).isEqualTo(0);
		counter.increment();
		Message<?> message2 = source.receive();
		assertThat(message2).isNotNull();
		assertThat(message2.getPayload()).isEqualTo(1);

		factoryBean.destroy();
	}

	public interface TestCounterMBean {

		int getCount();

	}

	public static class TestCounter implements TestCounterMBean {

		private final AtomicInteger counter = new AtomicInteger();

		public int getCount() {
			return this.counter.get();
		}

		public void increment() {
			this.counter.incrementAndGet();
		}

	}

}

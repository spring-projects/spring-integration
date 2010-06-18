/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.aop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class PublisherAnnotationAdvisorTests {

	private final StaticApplicationContext context = new StaticApplicationContext();


	@Before
	public void setup() {
		context.registerSingleton("testChannel", QueueChannel.class);		
	}

	@Test
	public void returnValue() {
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(context);
		QueueChannel testChannel = context.getBean("testChannel", QueueChannel.class);
		advisor.setDefaultChannel(testChannel);
		ProxyFactory pf = new ProxyFactory(new TestBeanImpl());
		pf.addAdvisor(advisor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test();
		Message<?> message = testChannel.receive(0);
		assertNotNull(message);
		assertEquals("foo", message.getPayload());
	}


	static interface TestBean {

		String test();

	}


	static class TestBeanImpl implements TestBean {

		@Publisher(payload="#return")
		public String test() {
			return "foo";
		}

	}

}

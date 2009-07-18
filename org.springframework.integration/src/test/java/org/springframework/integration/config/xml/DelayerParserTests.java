/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 1.0.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DelayerParserTests {

	@Autowired
	private ApplicationContext context;


	@Test
	public void checkConfiguration() {
		Object endpoint = context.getBean("delayer");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object handler = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(DelayHandler.class, handler.getClass());
		DelayHandler delayHandler = (DelayHandler) handler;
		assertEquals(99, delayHandler.getOrder());
		DirectFieldAccessor accessor = new DirectFieldAccessor(delayHandler);
		assertEquals(context.getBean("output"), accessor.getPropertyValue("outputChannel"));
		assertEquals(new Long(1234), accessor.getPropertyValue("defaultDelay"));
		assertEquals("foo", accessor.getPropertyValue("delayHeaderName"));
		assertEquals(new Long(987), new DirectFieldAccessor(
				accessor.getPropertyValue("channelTemplate")).getPropertyValue("sendTimeout"));
		assertEquals(Boolean.TRUE, accessor.getPropertyValue("waitForTasksToCompleteOnShutdown"));
	}

}

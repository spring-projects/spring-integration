/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.router.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.RouterFactoryBean;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @since 4.2.5
 *
 */
public class RouterFactoryBeanTests {

	private boolean routeAttempted;

	@Test
	public void testOutputChannelName() throws Exception {
		RouterFactoryBean fb = new RouterFactoryBean();
		fb.setTargetObject(this);
		fb.setTargetMethodName("foo");
		fb.setDefaultOutputChannelName("bar");
		BeanFactory beanFactory = mock(BeanFactory.class);
		QueueChannel bar = new QueueChannel();
		doReturn(bar).when(beanFactory).getBean("bar", MessageChannel.class);
		fb.setBeanFactory(beanFactory);
		MessageHandler handler = fb.getObject();
		this.routeAttempted = false;
		handler.handleMessage(new GenericMessage<>("foo"));
		assertNotNull(bar.receive(10000));
		assertTrue(this.routeAttempted);
	}

	public String foo() {
		routeAttempted = true;
		return null;
	}

}

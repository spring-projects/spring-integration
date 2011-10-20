/*
 * Copyright 2002-2011 the original author or authors.
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
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RouterWithMappingTests {

	@Autowired
	private MessageChannel expressionRouter;
	
	@Autowired
	@Qualifier("spelRouter")
	private ConsumerEndpointFactoryBean spelRouter;

	@Autowired
	private MessageChannel pojoRouter;

	@Autowired
	private PollableChannel fooChannelForExpression;

	@Autowired
	private PollableChannel barChannelForExpression;

	@Autowired
	private PollableChannel defaultChannelForExpression;

	@Autowired
	private PollableChannel fooChannelForPojo;

	@Autowired
	private PollableChannel barChannelForPojo;

	@Autowired
	private PollableChannel defaultChannelForPojo;

	@Test
	public void expressionRouter() {
		Message<?> message1 = MessageBuilder.withPayload(new TestBean("foo")).build();
		Message<?> message2 = MessageBuilder.withPayload(new TestBean("bar")).build();
		Message<?> message3 = MessageBuilder.withPayload(new TestBean("baz")).build();
		expressionRouter.send(message1);
		assertNotNull(fooChannelForExpression.receive(0));
		assertNull(barChannelForExpression.receive(0));
		assertNull(defaultChannelForExpression.receive(0));
		expressionRouter.send(message2);
		assertNotNull(barChannelForExpression.receive(0));
		assertNull(fooChannelForExpression.receive(0));
		assertNull(defaultChannelForExpression.receive(0));
		expressionRouter.send(message3);
		assertNotNull(defaultChannelForExpression.receive(0));
		assertNull(fooChannelForExpression.receive(0));
		assertNull(barChannelForExpression.receive(0));
		// validate dynamics
		AbstractMappingMessageRouter router = (AbstractMappingMessageRouter) TestUtils.getPropertyValue(spelRouter, "handler");
		router.setChannelMapping("baz", "fooChannelForExpression");
		expressionRouter.send(message3);
		assertNull(defaultChannelForExpression.receive(10));
		assertNotNull(fooChannelForExpression.receive(10));
		assertNull(barChannelForExpression.receive(0));
	}

	@Test
	public void pojoRouter() {
		Message<?> message1 = MessageBuilder.withPayload(new TestBean("foo")).build();
		Message<?> message2 = MessageBuilder.withPayload(new TestBean("bar")).build();
		Message<?> message3 = MessageBuilder.withPayload(new TestBean("baz")).build();
		pojoRouter.send(message1);
		assertNotNull(fooChannelForPojo.receive(0));
		assertNull(barChannelForPojo.receive(0));
		assertNull(defaultChannelForPojo.receive(0));
		pojoRouter.send(message2);
		assertNotNull(barChannelForPojo.receive(0));
		assertNull(fooChannelForPojo.receive(0));
		assertNull(defaultChannelForPojo.receive(0));
		pojoRouter.send(message3);
		assertNotNull(defaultChannelForPojo.receive(0));
		assertNull(fooChannelForPojo.receive(0));
		assertNull(barChannelForPojo.receive(0));
	}

	private static class TestBean {

		private final String name;

		public TestBean(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}


	@SuppressWarnings("unused")
	private static class TestRouter {

		public String route(TestBean bean) {
			return bean.getName();
		}
	}

}

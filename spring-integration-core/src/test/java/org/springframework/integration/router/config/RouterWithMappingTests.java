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

package org.springframework.integration.router.config;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.Lifecycle;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RouterWithMappingTests {

	@Autowired
	private MessageChannel expressionRouter;

	@Autowired
	@Qualifier("spelRouter.handler")
	private AbstractMappingMessageRouter spelRouterHandler;

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

	@Autowired
	private AbstractEndpoint pojoRouterEndpoint;

	@Autowired
	private TestRouter testBean;

	@Test
	public void expressionRouter() {
		Message<?> message1 = MessageBuilder.withPayload(new TestBean("foo")).build();
		Message<?> message2 = MessageBuilder.withPayload(new TestBean("bar")).build();
		Message<?> message3 = MessageBuilder.withPayload(new TestBean("baz")).build();
		expressionRouter.send(message1);
		assertThat(fooChannelForExpression.receive(0)).isNotNull();
		assertThat(barChannelForExpression.receive(0)).isNull();
		assertThat(defaultChannelForExpression.receive(0)).isNull();
		expressionRouter.send(message2);
		assertThat(barChannelForExpression.receive(0)).isNotNull();
		assertThat(fooChannelForExpression.receive(0)).isNull();
		assertThat(defaultChannelForExpression.receive(0)).isNull();
		expressionRouter.send(message3);
		assertThat(defaultChannelForExpression.receive(0)).isNotNull();
		assertThat(fooChannelForExpression.receive(0)).isNull();
		assertThat(barChannelForExpression.receive(0)).isNull();
		// validate dynamics
		spelRouterHandler.setChannelMapping("baz", "fooChannelForExpression");
		expressionRouter.send(message3);
		assertThat(defaultChannelForExpression.receive(10)).isNull();
		assertThat(fooChannelForExpression.receive(10)).isNotNull();
		assertThat(barChannelForExpression.receive(0)).isNull();
	}

	@Test
	public void pojoRouter() {
		Message<?> message1 = MessageBuilder.withPayload(new TestBean("foo")).build();
		Message<?> message2 = MessageBuilder.withPayload(new TestBean("bar")).build();
		Message<?> message3 = MessageBuilder.withPayload(new TestBean("baz")).build();
		pojoRouter.send(message1);
		assertThat(fooChannelForPojo.receive(0)).isNotNull();
		assertThat(barChannelForPojo.receive(0)).isNull();
		assertThat(defaultChannelForPojo.receive(0)).isNull();
		pojoRouter.send(message2);
		assertThat(barChannelForPojo.receive(0)).isNotNull();
		assertThat(fooChannelForPojo.receive(0)).isNull();
		assertThat(defaultChannelForPojo.receive(0)).isNull();
		pojoRouter.send(message3);
		assertThat(defaultChannelForPojo.receive(0)).isNotNull();
		assertThat(fooChannelForPojo.receive(0)).isNull();
		assertThat(barChannelForPojo.receive(0)).isNull();

		assertThat(this.testBean.isRunning()).isTrue();
		this.pojoRouterEndpoint.stop();
		assertThat(this.testBean.isRunning()).isFalse();
		this.pojoRouterEndpoint.start();
		assertThat(this.testBean.isRunning()).isTrue();
	}

	private static class TestBean {

		private final String name;

		TestBean(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

	private static class TestRouter implements Lifecycle {

		private boolean running;

		@SuppressWarnings("unused")
		public String route(TestBean bean) {
			return bean.getName();
		}

		@Override
		public void start() {
			this.running = true;
		}

		@Override
		public void stop() {
			this.running = false;
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

	}

}

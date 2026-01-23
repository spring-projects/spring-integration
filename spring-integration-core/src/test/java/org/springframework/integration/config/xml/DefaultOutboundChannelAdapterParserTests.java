/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.TestConsumer;
import org.springframework.integration.handler.MethodInvokingMessageHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class DefaultOutboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@SuppressWarnings("unused") // testing auto wiring only
	@Autowired
	@Qualifier("org.springframework.integration.handler.MethodInvokingMessageHandler#0")
	private MethodInvokingMessageHandler adapterByGeneratedName;

	@SuppressWarnings("unused") // testing auto wiring only
	@Autowired
	@Qualifier("adapter.handler")
	private MethodInvokingMessageHandler adapterByAlias;

	@Test
	public void checkConfig() {
		Object adapter = context.getBean("adapter");
		assertThat(TestUtils.<Boolean>getPropertyValue(adapter, "autoStartup")).isEqualTo(Boolean.FALSE);
		Object handler = TestUtils.getPropertyValue(adapter, "handler");
		assertThat(handler.getClass()).isEqualTo(MethodInvokingMessageHandler.class);
		assertThat(TestUtils.<Integer>getPropertyValue(handler, "order")).isEqualTo(99);
	}

	@Test
	public void checkConfigWithInnerBeanAndPoller() {
		Object adapter = context.getBean("adapterB");
		assertThat(TestUtils.<Boolean>getPropertyValue(adapter, "autoStartup")).isEqualTo(Boolean.FALSE);
		MessageHandler handler = TestUtils.getPropertyValue(adapter, "handler");
		assertThat(AopUtils.isAopProxy(handler)).isTrue();
		assertThat(((Advised) handler).getAdvisors()[0].getAdvice()).isInstanceOf(RequestHandlerRetryAdvice.class);

		handler.handleMessage(new GenericMessage<>("foo"));
		QueueChannel recovery = context.getBean("recovery", QueueChannel.class);
		Message<?> received = recovery.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received).isInstanceOf(ErrorMessage.class);
		assertThat(received.getPayload()).isInstanceOf(MessagingException.class);
		assertThat(((MessagingException) received.getPayload()).getFailedMessage().getPayload()).isEqualTo("foo");
	}

	@Test
	public void checkConfigWithInnerMessageHandler() {
		Object adapter = context.getBean("adapterC");
		Object handler = TestUtils.getPropertyValue(adapter, "handler");
		assertThat(handler.getClass()).isEqualTo(MethodInvokingMessageHandler.class);
		assertThat(TestUtils.<Integer>getPropertyValue(handler, "order")).isEqualTo(99);
		Object targetObject = TestUtils.getPropertyValue(handler, "processor.delegate.targetObject");
		assertThat(targetObject.getClass()).isEqualTo(TestConsumer.class);
	}

	static class TestBean {

		public void out(Object o) {
			throw new RuntimeException("ex");
		}

	}

}

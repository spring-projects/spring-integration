/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.rmi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.rmi.RmiInboundGateway;
import org.springframework.integration.rmi.RmiOutboundGateway;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.SocketUtils;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class RmiOutboundGatewayParserTests {

	public static final int port = SocketUtils.findAvailableTcpPort();

	private static final QueueChannel testChannel = new QueueChannel();

	private static final RmiInboundGateway rmiInboundGateway = new RmiInboundGateway();

	@Autowired
	public FooAdvice advice;

	@Autowired
	private MessageChannel advisedChannel;

	@Autowired
	private MessageChannel rmiOutboundGatewayInsideChain;

	@Autowired
	private MessageChannel requestReplyRmiWithChainChannel;

	@Autowired
	private PollableChannel replyChannel;

	@Autowired
	private RmiOutboundGateway.RmiProxyFactoryBeanConfigurer configurer;

	@Autowired
	@Qualifier("gateway.handler")
	RmiOutboundGateway gateway;

	@Autowired
	@Qualifier("advised.handler")
	RmiOutboundGateway advised;

	@BeforeAll
	public static void setupTestInboundGateway() {
		testChannel.setBeanName("testChannel");
		rmiInboundGateway.setRequestChannel(testChannel);
		rmiInboundGateway.setRegistryPort(port);
		rmiInboundGateway.setExpectReply(false);
		rmiInboundGateway.setBeanFactory(mock(BeanFactory.class));
		rmiInboundGateway.afterPropertiesSet();
	}

	@AfterAll
	public static void destroyInboundGateway() {
		rmiInboundGateway.destroy();
	}

	@Test
	public void testProperties() {
		assertThat(TestUtils.getPropertyValue(gateway, "order")).isEqualTo(23);
		assertThat(TestUtils.getPropertyValue(gateway, "requiresReply", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.gateway, "configurer")).isSameAs(this.configurer);
		verify(this.configurer).configure(any(RmiProxyFactoryBean.class));
	}

	@Test
	public void directInvocation() {
		assertThat(TestUtils.getPropertyValue(advised, "requiresReply", Boolean.class)).isFalse();

		advisedChannel.send(new GenericMessage<>("test"));
		Message<?> result = testChannel.receive(1000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test");
		assertThat(advice.adviceCalled).isEqualTo(1);
	}

	@Test //INT-1029
	public void testRmiOutboundGatewayInsideChain() {
		rmiOutboundGatewayInsideChain.send(MessageBuilder.withPayload("test").build());
		Message<?> result = testChannel.receive(1000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test");
	}

	@Test //INT-1029
	public void testRmiRequestReplyWithinChain() {
		requestReplyRmiWithChainChannel.send(MessageBuilder.withPayload("test").build());
		Message<?> result = replyChannel.receive(1000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		int adviceCalled;

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

}

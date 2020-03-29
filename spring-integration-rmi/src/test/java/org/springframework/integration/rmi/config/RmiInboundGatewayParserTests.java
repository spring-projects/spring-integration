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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.rmi.RmiInboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
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
public class RmiInboundGatewayParserTests {

	public static final int PORT = SocketUtils.findAvailableTcpPort();

	@Autowired
	@Qualifier("testChannel")
	private MessageChannel channel;

	@Autowired
	private ApplicationContext context;

	@Test
	public void gatewayWithDefaultsAndHistory() {
		RmiInboundGateway gateway = (RmiInboundGateway) this.context.getBean("gatewayWithDefaults");

		assertThat(gateway.getComponentName()).isEqualTo("gatewayWithDefaults");
		assertThat(gateway.getComponentType()).isEqualTo("rmi:inbound-gateway");
		assertThat(TestUtils.getPropertyValue(gateway, "expectReply", Boolean.class)).isTrue();
		assertThat(gateway.getRequestChannel()).isSameAs(this.channel);
		assertThat(TestUtils.getPropertyValue(gateway, "messagingTemplate.sendTimeout")).isEqualTo(1000L);
		assertThat(TestUtils.getPropertyValue(gateway, "messagingTemplate.receiveTimeout")).isEqualTo(1000L);
	}

	@Test
	public void gatewayWithCustomProperties() {
		RmiInboundGateway gateway = (RmiInboundGateway) context.getBean("gatewayWithCustomProperties");

		assertThat(TestUtils.getPropertyValue(gateway, "expectReply", Boolean.class)).isFalse();
		assertThat(gateway.getRequestChannel()).isSameAs(this.channel);
		assertThat(TestUtils.getPropertyValue(gateway, "messagingTemplate.sendTimeout")).isEqualTo(123L);
		assertThat(TestUtils.getPropertyValue(gateway, "messagingTemplate.receiveTimeout")).isEqualTo(456L);
	}

	@Test
	public void gatewayWithHost() {
		RmiInboundGateway gateway = (RmiInboundGateway) context.getBean("gatewayWithHostAndErrorChannel");
		assertThat(TestUtils.getPropertyValue(gateway, "registryHost")).isEqualTo("localhost");
		assertThat(gateway.getErrorChannel()).isSameAs(context.getBean("testErrorChannel"));
	}

	@Test
	public void gatewayWithPort() {
		RmiInboundGateway gateway = (RmiInboundGateway) context.getBean("gatewayWithPort");
		assertThat(TestUtils.getPropertyValue(gateway, "registryPort")).isEqualTo(PORT);
	}

	@Test
	public void gatewayWithRemoteInvocationExecutorReference() {
		RmiInboundGateway gateway = (RmiInboundGateway) context.getBean("gatewayWithExecutorRef");
		Object remoteInvocationExecutor = TestUtils.getPropertyValue(gateway, "remoteInvocationExecutor");
		assertThat(remoteInvocationExecutor).isNotNull();
		assertThat(remoteInvocationExecutor).isInstanceOf(StubRemoteInvocationExecutor.class);
	}

}

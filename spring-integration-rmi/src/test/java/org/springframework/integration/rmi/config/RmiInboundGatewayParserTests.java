/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.rmi.config;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.rmi.RmiInboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class RmiInboundGatewayParserTests {

	@Autowired
	@Qualifier("testChannel")
	private MessageChannel channel;

	@Autowired
	private ApplicationContext context;

	@Test
	public void gatewayWithDefaultsAndHistory() {
		RmiInboundGateway gateway = (RmiInboundGateway) this.context.getBean("gatewayWithDefaults");

		assertEquals("gatewayWithDefaults", gateway.getComponentName());
		assertEquals("rmi:inbound-gateway", gateway.getComponentType());
		assertTrue(TestUtils.getPropertyValue(gateway, "expectReply", Boolean.class));
		assertSame(this.channel, gateway.getRequestChannel());
		assertEquals(1000L, TestUtils.getPropertyValue(gateway, "messagingTemplate.sendTimeout"));
		assertEquals(1000L, TestUtils.getPropertyValue(gateway, "messagingTemplate.receiveTimeout"));
	}

	@Test
	public void gatewayWithCustomProperties() {
		RmiInboundGateway gateway = (RmiInboundGateway) context.getBean("gatewayWithCustomProperties");

		assertFalse(TestUtils.getPropertyValue(gateway, "expectReply", Boolean.class));
		assertSame(this.channel, gateway.getRequestChannel());
		assertEquals(123L, TestUtils.getPropertyValue(gateway, "messagingTemplate.sendTimeout"));
		assertEquals(456L, TestUtils.getPropertyValue(gateway, "messagingTemplate.receiveTimeout"));
	}

	@Test
	public void gatewayWithHost() {
		RmiInboundGateway gateway = (RmiInboundGateway) context.getBean("gatewayWithHostAndErrorChannel");
		assertEquals("localhost", TestUtils.getPropertyValue(gateway, "registryHost"));
		assertSame(context.getBean("testErrorChannel"), gateway.getErrorChannel());
	}

	@Test
	public void gatewayWithPort() {
		RmiInboundGateway gateway = (RmiInboundGateway) context.getBean("gatewayWithPort");
		assertEquals(1234, TestUtils.getPropertyValue(gateway, "registryPort"));
	}

	@Test
	public void gatewayWithRemoteInvocationExecutorReference() {
		RmiInboundGateway gateway = (RmiInboundGateway) context.getBean("gatewayWithExecutorRef");
		Object remoteInvocationExecutor = TestUtils.getPropertyValue(gateway, "remoteInvocationExecutor");
		assertNotNull(remoteInvocationExecutor);
		assertThat(remoteInvocationExecutor, instanceOf(StubRemoteInvocationExecutor.class));
	}

}

/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.sftp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Command;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Option;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.1
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SftpOutboundGatewayParserTests {

	@Autowired
	AbstractEndpoint gateway1;

	@Autowired
	AbstractEndpoint gateway2;

	@Autowired
	AbstractEndpoint gateway3;

	@Autowired
	AbstractEndpoint advised;

	private static volatile int adviceCalled;

	@Test
	public void testGateway1() {
		SftpOutboundGateway gateway = TestUtils.getPropertyValue(gateway1,
				"handler", SftpOutboundGateway.class);
		assertEquals("X", TestUtils.getPropertyValue(gateway, "remoteFileSeparator"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "sessionFactory"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "outputChannel"));
		assertEquals(new File("local-test-dir"), TestUtils.getPropertyValue(gateway, "localDirectory"));
		assertFalse((Boolean) TestUtils.getPropertyValue(gateway, "autoCreateLocalDirectory"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "filter"));
		assertEquals(Command.LS, TestUtils.getPropertyValue(gateway, "command"));
		@SuppressWarnings("unchecked")
		Set<String> options = TestUtils.getPropertyValue(gateway, "options", Set.class);
		assertTrue(options.contains(Option.NAME_ONLY));
		assertTrue(options.contains(Option.NOSORT));

		Long sendTimeout = TestUtils.getPropertyValue(gateway, "messagingTemplate.sendTimeout", Long.class);
		assertEquals(Long.valueOf(777), sendTimeout);
	}

	@Test
	public void testGateway2() {
		SftpOutboundGateway gateway = TestUtils.getPropertyValue(gateway2,
				"handler", SftpOutboundGateway.class);
		assertEquals("X", TestUtils.getPropertyValue(gateway, "remoteFileSeparator"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "sessionFactory"));
		assertTrue(TestUtils.getPropertyValue(gateway, "sessionFactory") instanceof CachingSessionFactory);
		assertNotNull(TestUtils.getPropertyValue(gateway, "outputChannel"));
		assertEquals(new File("local-test-dir"), TestUtils.getPropertyValue(gateway, "localDirectory"));
		assertFalse((Boolean) TestUtils.getPropertyValue(gateway, "autoCreateLocalDirectory"));
		assertEquals(Command.GET, TestUtils.getPropertyValue(gateway, "command"));
		@SuppressWarnings("unchecked")
		Set<String> options = TestUtils.getPropertyValue(gateway, "options", Set.class);
		assertTrue(options.contains(Option.PRESERVE_TIMESTAMP));
	}

	@Test
	public void testGatewayMv() {
		SftpOutboundGateway gateway = TestUtils.getPropertyValue(gateway3,
				"handler", SftpOutboundGateway.class);
		assertNotNull(TestUtils.getPropertyValue(gateway, "sessionFactory"));
		assertNotNull(TestUtils.getPropertyValue(gateway, "outputChannel"));
		assertEquals(Command.MV, TestUtils.getPropertyValue(gateway, "command"));
		assertEquals("'foo'", TestUtils.getPropertyValue(gateway, "renameProcessor.expression.expression"));
	}

	@Test
	public void advised() {
		SftpOutboundGateway gateway = TestUtils.getPropertyValue(advised,
				"handler", SftpOutboundGateway.class);
		gateway.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}

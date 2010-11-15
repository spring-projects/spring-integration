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
package org.springframework.integration.ftp.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.ftp.client.FtpClientFactory;
import org.springframework.integration.ftp.client.FtpClientPool;
import org.springframework.integration.ftp.outbound.FtpSendingMessageHandler;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 *
 */
public class FtpOutboundChannelAdapterParserTests {

	@Test
	public void testFtpOutboundChannelAdapterComplete() throws Exception{
		ApplicationContext ac = 
			new ClassPathXmlApplicationContext("FtpOutboundChannelAdapterParserTests-context.xml", this.getClass());
		Object consumer = ac.getBean("ftpOutbound");
		assertTrue(consumer instanceof EventDrivenConsumer);
		assertEquals(ac.getBean("ftpChannel"), TestUtils.getPropertyValue(consumer, "inputChannel"));
		assertEquals("ftpOutbound", ((EventDrivenConsumer)consumer).getComponentName());
		FtpSendingMessageHandler handler = (FtpSendingMessageHandler) TestUtils.getPropertyValue(consumer, "handler");
		assertEquals(ac.getBean("fileNameGenerator"), TestUtils.getPropertyValue(handler, "fileNameGenerator"));
		assertEquals("UTF-8", TestUtils.getPropertyValue(handler, "charset"));
		assertNotNull(TestUtils.getPropertyValue(handler, "temporaryBufferFolder"));
		assertNotNull(TestUtils.getPropertyValue(handler, "temporaryBufferFolderFile"));
		FtpClientPool clientPoll = (FtpClientPool) TestUtils.getPropertyValue(handler, "ftpClientPool");
		FtpClientFactory<?> clientFactory = (FtpClientFactory<?>) TestUtils.getPropertyValue(clientPoll, "factory");
		assertEquals("localhost", TestUtils.getPropertyValue(clientFactory, "host"));
		assertEquals(22, TestUtils.getPropertyValue(clientFactory, "port"));
		assertEquals("user", TestUtils.getPropertyValue(clientFactory, "username"));
		assertEquals("password", TestUtils.getPropertyValue(clientFactory, "password"));
		assertEquals("foo/bar", TestUtils.getPropertyValue(clientFactory, "remoteWorkingDirectory"));
	}
}

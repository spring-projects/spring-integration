/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.ftp.session.DefaultFtpsSessionFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class FtpsOutboundChannelAdapterParserTests {

	@Autowired
	private EventDrivenConsumer ftpOutbound;

	@Autowired
	private MessageChannel ftpChannel;

	@Autowired
	private FileNameGenerator fileNameGenerator;

	@Test
	public void testFtpsOutboundChannelAdapterComplete() throws Exception{
		assertTrue(ftpOutbound instanceof EventDrivenConsumer);
		assertEquals(this.ftpChannel, TestUtils.getPropertyValue(ftpOutbound, "inputChannel"));
		assertEquals("ftpOutbound", ftpOutbound.getComponentName());
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(ftpOutbound, "handler", FileTransferringMessageHandler.class);
		assertEquals(this.fileNameGenerator, TestUtils.getPropertyValue(handler, "remoteFileTemplate.fileNameGenerator"));
		assertEquals("UTF-8", TestUtils.getPropertyValue(handler, "remoteFileTemplate.charset"));
		DefaultFtpsSessionFactory sf = TestUtils.getPropertyValue(handler, "remoteFileTemplate.sessionFactory", DefaultFtpsSessionFactory.class);
		assertEquals("localhost", TestUtils.getPropertyValue(sf, "host"));
		assertEquals(22, TestUtils.getPropertyValue(sf, "port"));
	}

}

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

package org.springframework.integration.ftp.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.ftp.session.DefaultFtpsSessionFactory;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class FtpsOutboundChannelAdapterParserTests {

	@Test
	public void testFtpsOutboundChannelAdapterComplete() throws Exception{
		ApplicationContext ac = new ClassPathXmlApplicationContext("FtpsOutboundChannelAdapterParserTests-context.xml", this.getClass());
		Object consumer = ac.getBean("ftpOutbound");
		assertTrue(consumer instanceof EventDrivenConsumer);
		assertEquals(ac.getBean("ftpChannel"), TestUtils.getPropertyValue(consumer, "inputChannel"));
		assertEquals("ftpOutbound", ((EventDrivenConsumer)consumer).getComponentName());
		FileTransferringMessageHandler handler = (FileTransferringMessageHandler) TestUtils.getPropertyValue(consumer, "handler");
		assertEquals(ac.getBean("fileNameGenerator"), TestUtils.getPropertyValue(handler, "fileNameGenerator"));
		assertEquals("UTF-8", TestUtils.getPropertyValue(handler, "charset"));
		assertNotNull(TestUtils.getPropertyValue(handler, "temporaryDirectory"));
		CachingSessionFactory<?> cacheSf = TestUtils.getPropertyValue(handler, "sessionFactory", CachingSessionFactory.class);
		DefaultFtpsSessionFactory sf = TestUtils.getPropertyValue(cacheSf, "sessionFactory", DefaultFtpsSessionFactory.class);
		assertEquals("localhost", TestUtils.getPropertyValue(sf, "host"));
		assertEquals(22, TestUtils.getPropertyValue(sf, "port"));
	}

}

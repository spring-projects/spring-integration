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

package org.springframework.integration.ftp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizer;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizingMessageSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class FtpsInboundChannelAdapterParserTests {

	@Autowired
	private SourcePollingChannelAdapter ftpInbound;

	@Autowired
	private MessageChannel ftpChannel;

	@Test
	public void testFtpsInboundChannelAdapterComplete() throws Exception{
		assertEquals("ftpInbound", ftpInbound.getComponentName());
		assertEquals("ftp:inbound-channel-adapter", ftpInbound.getComponentType());
		assertNotNull(TestUtils.getPropertyValue(ftpInbound, "poller"));
		assertEquals(this.ftpChannel, TestUtils.getPropertyValue(ftpInbound, "outputChannel"));
		FtpInboundFileSynchronizingMessageSource inbound =
			(FtpInboundFileSynchronizingMessageSource) TestUtils.getPropertyValue(ftpInbound, "source");

		FtpInboundFileSynchronizer fisync =
			(FtpInboundFileSynchronizer) TestUtils.getPropertyValue(inbound, "synchronizer");
		assertNotNull(TestUtils.getPropertyValue(fisync, "filter"));

	}

}

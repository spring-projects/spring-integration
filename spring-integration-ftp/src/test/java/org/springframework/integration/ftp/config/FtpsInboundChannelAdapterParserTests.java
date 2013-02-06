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

import java.util.Map;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizer;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizingMessageSource;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 */
public class FtpsInboundChannelAdapterParserTests {

	@Test
	public void testFtpsInboundChannelAdapterComplete() throws Exception{

		ApplicationContext ac =
			new ClassPathXmlApplicationContext("FtpsInboundChannelAdapterParserTests-context.xml", this.getClass());
		SourcePollingChannelAdapter adapter = ac.getBean("ftpInbound", SourcePollingChannelAdapter.class);
		assertEquals("ftpInbound", adapter.getComponentName());
		assertEquals("ftp:inbound-channel-adapter", adapter.getComponentType());
		assertNotNull(TestUtils.getPropertyValue(adapter, "poller"));
		assertEquals(ac.getBean("ftpChannel"), TestUtils.getPropertyValue(adapter, "outputChannel"));
		FtpInboundFileSynchronizingMessageSource inbound =
			(FtpInboundFileSynchronizingMessageSource) TestUtils.getPropertyValue(adapter, "source");

		FtpInboundFileSynchronizer fisync =
			(FtpInboundFileSynchronizer) TestUtils.getPropertyValue(inbound, "synchronizer");
		assertNotNull(TestUtils.getPropertyValue(fisync, "filter"));

	}

	@Test
	public void testFtpsInboundChannelAdapterCompleteNoId() throws Exception{

		ApplicationContext ac =
			new ClassPathXmlApplicationContext("FtpsInboundChannelAdapterParserTests-context.xml", this.getClass());
		Map<String, SourcePollingChannelAdapter> spcas = ac.getBeansOfType(SourcePollingChannelAdapter.class);
		SourcePollingChannelAdapter adapter = null;
		for (String key : spcas.keySet()) {
			if (!key.equals("ftpInbound")){
				adapter = spcas.get(key);
			}
		}
		assertNotNull(adapter);
	}

}

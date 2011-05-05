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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.ftp.filters.FtpSimplePatternFileListFilter;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizer;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizingMessageSource;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 */
public class FtpInboundChannelAdapterParserTests {

	@Test
	public void testFtpInboundChannelAdapterComplete() throws Exception{
		ApplicationContext ac = 
			new ClassPathXmlApplicationContext("FtpInboundChannelAdapterParserTests-context.xml", this.getClass());
		SourcePollingChannelAdapter adapter = ac.getBean("ftpInbound", SourcePollingChannelAdapter.class);
		assertEquals("ftpInbound", adapter.getComponentName());
		assertEquals("ftp:inbound-channel-adapter", adapter.getComponentType());
		assertNotNull(TestUtils.getPropertyValue(adapter, "poller"));
		assertEquals(ac.getBean("ftpChannel"), TestUtils.getPropertyValue(adapter, "outputChannel"));
		FtpInboundFileSynchronizingMessageSource inbound = 
			(FtpInboundFileSynchronizingMessageSource) TestUtils.getPropertyValue(adapter, "source");
		
		FtpInboundFileSynchronizer fisync = 
			(FtpInboundFileSynchronizer) TestUtils.getPropertyValue(inbound, "synchronizer");
		assertEquals(".foo", fisync.getTemporaryFileSuffix());
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(fisync, "remoteFileSeparator");
		assertNotNull(remoteFileSeparator);
		assertEquals("", remoteFileSeparator);
		FtpSimplePatternFileListFilter filter = (FtpSimplePatternFileListFilter) TestUtils.getPropertyValue(fisync, "filter");
		assertNotNull(filter);
		Object sessionFactory = TestUtils.getPropertyValue(fisync, "sessionFactory");
		assertTrue(DefaultFtpSessionFactory.class.isAssignableFrom(sessionFactory.getClass()));
	}

	@Test
	public void cachingSessionFactoryByDefault() throws Exception{
		ApplicationContext ac = new ClassPathXmlApplicationContext(
				"FtpInboundChannelAdapterParserTests-context.xml", this.getClass());
		SourcePollingChannelAdapter adapter = ac.getBean("simpleAdapter", SourcePollingChannelAdapter.class);
		Object sessionFactory = TestUtils.getPropertyValue(adapter, "source.synchronizer.sessionFactory");
		assertEquals(CachingSessionFactory.class, sessionFactory.getClass());
	}

	@Test
	public void testFtpInboundChannelAdapterCompleteNoId() throws Exception{
		ApplicationContext ac = 
			new ClassPathXmlApplicationContext("FtpInboundChannelAdapterParserTests-context.xml", this.getClass());
		Map<String, SourcePollingChannelAdapter> spcas = ac.getBeansOfType(SourcePollingChannelAdapter.class);
		SourcePollingChannelAdapter adapter = null;
		for (String key : spcas.keySet()) {
			if (!key.equals("ftpInbound")){
				adapter = spcas.get(key);
			}
		}
		assertNotNull(adapter);
	}


	public static class TestSessionFactoryBean implements FactoryBean<DefaultFtpSessionFactory> {

		public DefaultFtpSessionFactory getObject() throws Exception {
			DefaultFtpSessionFactory factory = mock(DefaultFtpSessionFactory.class);
			Session session = mock(Session.class);
			when(factory.getSession()).thenReturn(session);
			return factory;
		}

		public Class<?> getObjectType() {
			return DefaultFtpSessionFactory.class;
		}

		public boolean isSingleton() {
			return true;
		}
	}

}

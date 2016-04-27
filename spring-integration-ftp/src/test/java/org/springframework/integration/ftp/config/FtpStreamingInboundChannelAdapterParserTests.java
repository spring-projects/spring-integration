/*
 * Copyright 2016 the original author or authors.
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.ftp.filters.FtpSimplePatternFileListFilter;
import org.springframework.integration.ftp.inbound.FtpStreamingMessageSource;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class FtpStreamingInboundChannelAdapterParserTests {

	@Autowired
	private SourcePollingChannelAdapter ftpInbound;

	@Autowired
	private MessageChannel ftpChannel;

	@Autowired
	private CachingSessionFactory<?> csf;

	@Test
	public void testFtpInboundChannelAdapterComplete() throws Exception {
		assertFalse(TestUtils.getPropertyValue(this.ftpInbound, "autoStartup", Boolean.class));
		assertEquals("ftpInbound", this.ftpInbound.getComponentName());
		assertEquals("ftp:inbound-streaming-channel-adapter", this.ftpInbound.getComponentType());
		assertSame(this.ftpChannel, TestUtils.getPropertyValue(this.ftpInbound, "outputChannel"));
		FtpStreamingMessageSource source = TestUtils.getPropertyValue(ftpInbound, "source",
				FtpStreamingMessageSource.class);

		assertNotNull(TestUtils.getPropertyValue(source, "comparator"));
		assertThat(TestUtils.getPropertyValue(source, "remoteFileSeparator", String.class), equalTo("X"));
		assertThat(TestUtils.getPropertyValue(source, "filter"), instanceOf(FtpSimplePatternFileListFilter.class));
		assertSame(this.csf, TestUtils.getPropertyValue(source, "remoteFileTemplate.sessionFactory"));
	}

	public static class TestSessionFactoryBean implements FactoryBean<DefaultFtpSessionFactory> {

		@Override
		public DefaultFtpSessionFactory getObject() throws Exception {
			DefaultFtpSessionFactory factory = mock(DefaultFtpSessionFactory.class);
			FtpSession session = mock(FtpSession.class);
			when(factory.getSession()).thenReturn(session);
			return factory;
		}

		@Override
		public Class<?> getObjectType() {
			return DefaultFtpSessionFactory.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}

}

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

package org.springframework.integration.sftp.config;

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
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpStreamingMessageSource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
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
public class SftpStreamingInboundChannelAdapterParserTests {

	@Autowired
	private SourcePollingChannelAdapter sftpInbound;

	@Autowired
	private MessageChannel sftpChannel;

	@Autowired
	private CachingSessionFactory<?> csf;

	@Test
	public void testFtpInboundChannelAdapterComplete() throws Exception {
		assertFalse(TestUtils.getPropertyValue(this.sftpInbound, "autoStartup", Boolean.class));
		assertEquals("sftpInbound", this.sftpInbound.getComponentName());
		assertEquals("sftp:inbound-streaming-channel-adapter", this.sftpInbound.getComponentType());
		assertSame(this.sftpChannel, TestUtils.getPropertyValue(this.sftpInbound, "outputChannel"));
		SftpStreamingMessageSource source = TestUtils.getPropertyValue(sftpInbound, "source",
				SftpStreamingMessageSource.class);

		assertNotNull(TestUtils.getPropertyValue(source, "comparator"));
		assertThat(TestUtils.getPropertyValue(source, "remoteFileSeparator", String.class), equalTo("X"));
		assertThat(TestUtils.getPropertyValue(source, "filter"), instanceOf(SftpSimplePatternFileListFilter.class));
		assertSame(this.csf, TestUtils.getPropertyValue(source, "remoteFileTemplate.sessionFactory"));
	}

	public static class TestSessionFactoryBean implements FactoryBean<DefaultSftpSessionFactory> {

		@Override
		public DefaultSftpSessionFactory getObject() throws Exception {
			DefaultSftpSessionFactory factory = mock(DefaultSftpSessionFactory.class);
			SftpSession session = mock(SftpSession.class);
			when(factory.getSession()).thenReturn(session);
			return factory;
		}

		@Override
		public Class<?> getObjectType() {
			return DefaultSftpSessionFactory.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}

}

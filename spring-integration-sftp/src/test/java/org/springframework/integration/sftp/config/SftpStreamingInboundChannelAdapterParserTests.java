/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.sftp.config;

import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.ExpressionFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpStreamingMessageSource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class SftpStreamingInboundChannelAdapterParserTests {

	@Autowired
	private SourcePollingChannelAdapter sftpInbound;

	@Autowired
	private SourcePollingChannelAdapter contextLoadsWithNoComparator;

	@Autowired
	private MessageChannel sftpChannel;

	@Autowired
	private CachingSessionFactory<?> csf;

	@Test
	public void testFtpInboundChannelAdapterComplete() throws Exception {
		assertThat(TestUtils.<Boolean>getPropertyValue(this.sftpInbound, "autoStartup")).isFalse();
		assertThat(this.sftpInbound.getComponentName()).isEqualTo("sftpInbound");
		assertThat(this.sftpInbound.getComponentType()).isEqualTo("sftp:inbound-streaming-channel-adapter");
		assertThat(TestUtils.<Object>getPropertyValue(this.sftpInbound, "outputChannel")).isSameAs(this.sftpChannel);
		SftpStreamingMessageSource source = TestUtils.getPropertyValue(sftpInbound, "source");

		assertThat(TestUtils.<Object>getPropertyValue(source, "comparator")).isNotNull();
		assertThat(TestUtils.<String>getPropertyValue(source, "remoteFileSeparator")).isEqualTo("X");

		FileListFilter<?> filter = TestUtils.getPropertyValue(source, "filter");
		assertThat(filter).isNotNull();
		assertThat(filter).isInstanceOf(CompositeFileListFilter.class);
		Set<?> fileFilters = TestUtils.getPropertyValue(filter, "fileFilters");

		Iterator<?> filtersIterator = fileFilters.iterator();
		assertThat(filtersIterator.next()).isInstanceOf(SftpSimplePatternFileListFilter.class);
		assertThat(filtersIterator.next()).isInstanceOf(SftpPersistentAcceptOnceFileListFilter.class);

		assertThat(TestUtils.<Object>getPropertyValue(source, "remoteFileTemplate.sessionFactory")).isSameAs(this.csf);
		assertThat(TestUtils.<Integer>getPropertyValue(source, "maxFetchSize")).isEqualTo(31);

		source = TestUtils.getPropertyValue(this.contextLoadsWithNoComparator, "source");
		assertThat(TestUtils.<Object>getPropertyValue(source, "filter")).isInstanceOf(ExpressionFileListFilter.class);
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

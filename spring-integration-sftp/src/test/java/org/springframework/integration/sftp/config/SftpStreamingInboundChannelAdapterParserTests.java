/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
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
		assertThat(TestUtils.getPropertyValue(this.sftpInbound, "autoStartup", Boolean.class)).isFalse();
		assertThat(this.sftpInbound.getComponentName()).isEqualTo("sftpInbound");
		assertThat(this.sftpInbound.getComponentType()).isEqualTo("sftp:inbound-streaming-channel-adapter");
		assertThat(TestUtils.getPropertyValue(this.sftpInbound, "outputChannel")).isSameAs(this.sftpChannel);
		SftpStreamingMessageSource source = TestUtils.getPropertyValue(sftpInbound, "source",
				SftpStreamingMessageSource.class);

		assertThat(TestUtils.getPropertyValue(source, "comparator")).isNotNull();
		assertThat(TestUtils.getPropertyValue(source, "remoteFileSeparator", String.class)).isEqualTo("X");

		FileListFilter<?> filter = TestUtils.getPropertyValue(source, "filter", FileListFilter.class);
		assertThat(filter).isNotNull();
		assertThat(filter).isInstanceOf(CompositeFileListFilter.class);
		Set<?> fileFilters = TestUtils.getPropertyValue(filter, "fileFilters", Set.class);

		Iterator<?> filtersIterator = fileFilters.iterator();
		assertThat(filtersIterator.next()).isInstanceOf(SftpSimplePatternFileListFilter.class);
		assertThat(filtersIterator.next()).isInstanceOf(SftpPersistentAcceptOnceFileListFilter.class);

		assertThat(TestUtils.getPropertyValue(source, "remoteFileTemplate.sessionFactory")).isSameAs(this.csf);
		assertThat(TestUtils.getPropertyValue(source, "maxFetchSize")).isEqualTo(31);

		source = TestUtils.getPropertyValue(this.contextLoadsWithNoComparator, "source",
				SftpStreamingMessageSource.class);
		assertThat(TestUtils.getPropertyValue(source, "filter")).isInstanceOf(ExpressionFileListFilter.class);
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

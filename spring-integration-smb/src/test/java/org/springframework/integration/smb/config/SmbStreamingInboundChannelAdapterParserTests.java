/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.smb.config;

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
import org.springframework.integration.smb.filters.SmbPersistentAcceptOnceFileListFilter;
import org.springframework.integration.smb.filters.SmbSimplePatternFileListFilter;
import org.springframework.integration.smb.inbound.SmbStreamingMessageSource;
import org.springframework.integration.smb.session.SmbSession;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests follow original logic for testing SFTP module,
 * adapted for SMB module.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gregory Bragg
 *
 * @since 6.0
 */
@SpringJUnitConfig
@DirtiesContext
public class SmbStreamingInboundChannelAdapterParserTests {

	@Autowired
	private SourcePollingChannelAdapter smbInbound;

	@Autowired
	private SourcePollingChannelAdapter contextLoadsWithNoComparator;

	@Autowired
	private MessageChannel smbChannel;

	@Autowired
	private CachingSessionFactory<?> csf;

	@Test
	public void testSmbInboundChannelAdapterComplete() {
		assertThat(TestUtils.getPropertyValue(this.smbInbound, "autoStartup", Boolean.class)).isFalse();
		assertThat(this.smbInbound.getComponentName()).isEqualTo("smbInbound");
		assertThat(this.smbInbound.getComponentType()).isEqualTo("smb:inbound-streaming-channel-adapter");
		assertThat(TestUtils.getPropertyValue(this.smbInbound, "outputChannel")).isSameAs(this.smbChannel);
		SmbStreamingMessageSource source = TestUtils.getPropertyValue(smbInbound, "source",
				SmbStreamingMessageSource.class);

		assertThat(TestUtils.getPropertyValue(source, "comparator")).isNotNull();
		assertThat(TestUtils.getPropertyValue(source, "remoteFileSeparator", String.class)).isEqualTo("X");

		FileListFilter<?> filter = TestUtils.getPropertyValue(source, "filter", FileListFilter.class);
		assertThat(filter).isNotNull();
		assertThat(filter).isInstanceOf(CompositeFileListFilter.class);
		Set<?> fileFilters = TestUtils.getPropertyValue(filter, "fileFilters", Set.class);

		Iterator<?> filtersIterator = fileFilters.iterator();
		assertThat(filtersIterator.next()).isInstanceOf(SmbSimplePatternFileListFilter.class);
		assertThat(filtersIterator.next()).isInstanceOf(SmbPersistentAcceptOnceFileListFilter.class);

		assertThat(TestUtils.getPropertyValue(source, "remoteFileTemplate.sessionFactory")).isSameAs(this.csf);
		assertThat(TestUtils.getPropertyValue(source, "maxFetchSize")).isEqualTo(31);

		source = TestUtils.getPropertyValue(this.contextLoadsWithNoComparator, "source",
				SmbStreamingMessageSource.class);
		assertThat(TestUtils.getPropertyValue(source, "filter")).isInstanceOf(ExpressionFileListFilter.class);
	}

	public static class TestSessionFactoryBean implements FactoryBean<SmbSessionFactory> {

		@Override
		public SmbSessionFactory getObject() {
			SmbSessionFactory factory = mock(SmbSessionFactory.class);
			SmbSession session = mock(SmbSession.class);
			when(factory.getSession()).thenReturn(session);
			return factory;
		}

		@Override
		public Class<?> getObjectType() {
			return SmbSessionFactory.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

	}

}

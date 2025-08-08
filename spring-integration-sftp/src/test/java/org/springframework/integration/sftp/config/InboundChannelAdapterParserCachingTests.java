/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.sftp.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
 */
@SpringJUnitConfig
@DirtiesContext
public class InboundChannelAdapterParserCachingTests {

	@Autowired
	private Object cachingAdapter;

	@Autowired
	private Object nonCachingAdapter;

	@Test
	public void cachingAdapter() {
		Object sessionFactory =
				TestUtils.getPropertyValue(cachingAdapter, "source.synchronizer.remoteFileTemplate.sessionFactory");
		assertThat(sessionFactory.getClass()).isEqualTo(CachingSessionFactory.class);
	}

	@Test
	public void nonCachingAdapter() {
		Object sessionFactory =
				TestUtils.getPropertyValue(nonCachingAdapter, "source.synchronizer.remoteFileTemplate.sessionFactory");
		assertThat(sessionFactory.getClass()).isEqualTo(DefaultSftpSessionFactory.class);
	}

}

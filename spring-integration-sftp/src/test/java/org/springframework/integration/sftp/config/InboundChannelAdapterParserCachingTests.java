/*
 * Copyright 2002-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
 */
@SpringJUnitConfig
@DirtiesContext
public class InboundChannelAdapterParserCachingTests {

	@Autowired private Object cachingAdapter;

	@Autowired private Object nonCachingAdapter;

	@Test
	public void cachingAdapter() {
		Object sessionFactory = TestUtils.getPropertyValue(cachingAdapter, "source.synchronizer.remoteFileTemplate.sessionFactory");
		assertThat(sessionFactory.getClass()).isEqualTo(CachingSessionFactory.class);
		Properties sessionConfig = TestUtils.getPropertyValue(sessionFactory, "sessionFactory.sessionConfig", Properties.class);
		assertThat(sessionConfig).isNotNull();
		assertThat(sessionConfig.getProperty("StrictHostKeyChecking")).isEqualTo("no");
	}

	@Test
	public void nonCachingAdapter() {
		Object sessionFactory = TestUtils.getPropertyValue(nonCachingAdapter, "source.synchronizer.remoteFileTemplate.sessionFactory");
		assertThat(sessionFactory.getClass()).isEqualTo(DefaultSftpSessionFactory.class);
	}

}

/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.ftp.config;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.file.DirectoryScanner;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpSimplePatternFileListFilter;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizer;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizingMessageSource;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Venil Noronha
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class FtpInboundChannelAdapterParserTests {

	@Autowired
	private SourcePollingChannelAdapter ftpInbound;

	@Autowired
	private SourcePollingChannelAdapter simpleAdapterWithCachedSessions;

	@Autowired
	private MessageChannel autoChannel;

	@Autowired
	@Qualifier("autoChannel.adapter")
	private SourcePollingChannelAdapter autoChannelAdapter;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private DirectoryScanner dirScanner;

	@Autowired
	private MetadataStore metadataStore;

	@Test
	public void testFtpInboundChannelAdapterComplete() throws Exception {
		assertThat(TestUtils.<Boolean>getPropertyValue(ftpInbound, "autoStartup")).isFalse();
		PriorityBlockingQueue<?> blockingQueue =
				TestUtils.getPropertyValue(ftpInbound, "source.fileSource.toBeReceived");
		Comparator<?> comparator = blockingQueue.comparator();
		assertThat(comparator).isNotNull();
		assertThat(ftpInbound.getComponentName()).isEqualTo("ftpInbound");
		assertThat(ftpInbound.getComponentType()).isEqualTo("ftp:inbound-channel-adapter");
		assertThat(TestUtils.<Object>getPropertyValue(ftpInbound, "outputChannel"))
				.isEqualTo(context.getBean("ftpChannel"));
		FtpInboundFileSynchronizingMessageSource inbound = TestUtils.getPropertyValue(ftpInbound, "source");

		assertThat(TestUtils.<Object>getPropertyValue(inbound, "fileSource.scanner")).isSameAs(dirScanner);

		FtpInboundFileSynchronizer fisync = TestUtils.getPropertyValue(inbound, "synchronizer");
		assertThat(TestUtils.<Expression>getPropertyValue(fisync, "remoteDirectoryExpression")
				.getExpressionString()).isEqualTo("'foo/bar'");
		assertThat(TestUtils.<Object>getPropertyValue(fisync, "localFilenameGeneratorExpression"))
				.isNotNull();
		assertThat(TestUtils.<Boolean>getPropertyValue(fisync, "preserveTimestamp")).isTrue();
		assertThat(TestUtils.<String>getPropertyValue(fisync, "temporaryFileSuffix")).isEqualTo(".foo");
		assertThat(TestUtils.<MetadataStore>getPropertyValue(fisync, "remoteFileMetadataStore"))
				.isSameAs(this.metadataStore);
		assertThat(TestUtils.<String>getPropertyValue(fisync, "metadataStorePrefix")).isEqualTo("testPrefix");
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(fisync, "remoteFileSeparator");
		assertThat(remoteFileSeparator).isNotNull();
		assertThat(remoteFileSeparator).isEqualTo("");

		FileListFilter<?> filter = TestUtils.getPropertyValue(fisync, "filter");
		assertThat(filter).isNotNull();
		assertThat(filter).isInstanceOf(CompositeFileListFilter.class);
		Set<?> fileFilters = TestUtils.getPropertyValue(filter, "fileFilters");

		Iterator<?> filtersIterator = fileFilters.iterator();
		assertThat(filtersIterator.next()).isInstanceOf(FtpSimplePatternFileListFilter.class);
		assertThat(filtersIterator.next()).isInstanceOf(FtpPersistentAcceptOnceFileListFilter.class);

		Object sessionFactory = TestUtils.getPropertyValue(fisync, "remoteFileTemplate.sessionFactory");
		assertThat(sessionFactory).isInstanceOf(DefaultFtpSessionFactory.class);
		FileListFilter<?> acceptAllFilter = context.getBean("acceptAllFilter", FileListFilter.class);
		assertThat(TestUtils.<Set<?>>getPropertyValue(inbound, "fileSource.scanner.filter.fileFilters")
				.contains(acceptAllFilter));
		final AtomicReference<Method> genMethod = new AtomicReference<>();
		ReflectionUtils.doWithMethods(AbstractInboundFileSynchronizer.class, method -> {
			method.setAccessible(true);
			genMethod.set(method);
		}, method -> "generateLocalFileName".equals(method.getName()));
		assertThat(genMethod.get().invoke(fisync, "foo", ExpressionUtils.createStandardEvaluationContext(this.context)))
				.isEqualTo("FOO.afoo");
		assertThat(inbound.getMaxFetchSize()).isEqualTo(42);
	}

	@Test
	public void cachingSessionFactory() {
		Object sessionFactory = TestUtils.getPropertyValue(simpleAdapterWithCachedSessions,
				"source.synchronizer.remoteFileTemplate.sessionFactory");
		assertThat(sessionFactory).isInstanceOf(CachingSessionFactory.class);
		FtpInboundFileSynchronizer fisync =
				TestUtils.getPropertyValue(simpleAdapterWithCachedSessions, "source.synchronizer");
		String remoteFileSeparator = TestUtils.getPropertyValue(fisync, "remoteFileSeparator");
		assertThat(remoteFileSeparator).isNotNull();
		assertThat(remoteFileSeparator).isEqualTo("/");
		assertThat(TestUtils.<Expression>getPropertyValue(fisync, "remoteDirectoryExpression")
				.getExpressionString()).isEqualTo("foo/bar");
		assertThat(TestUtils.<Integer>getPropertyValue(simpleAdapterWithCachedSessions,
				"source.maxFetchSize")).isEqualTo(Integer.MIN_VALUE);
	}

	@Test
	public void testAutoChannel() {
		assertThat(TestUtils.<Object>getPropertyValue(autoChannelAdapter, "outputChannel"))
				.isSameAs(autoChannel);
	}

	public static class TestSessionFactoryBean implements FactoryBean<DefaultFtpSessionFactory> {

		@Override
		public DefaultFtpSessionFactory getObject() {
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

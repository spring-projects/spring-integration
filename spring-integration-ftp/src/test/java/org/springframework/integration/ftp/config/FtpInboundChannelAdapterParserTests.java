/*
 * Copyright 2002-2017 the original author or authors.
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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
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
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Venil Noronha
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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

	@Test
	public void testFtpInboundChannelAdapterComplete() throws Exception {
		assertFalse(TestUtils.getPropertyValue(ftpInbound, "autoStartup", Boolean.class));
		PriorityBlockingQueue<?> blockingQueue =
				TestUtils.getPropertyValue(ftpInbound, "source.fileSource.toBeReceived", PriorityBlockingQueue.class);
		Comparator<?> comparator = blockingQueue.comparator();
		assertNotNull(comparator);
		assertEquals("ftpInbound", ftpInbound.getComponentName());
		assertEquals("ftp:inbound-channel-adapter", ftpInbound.getComponentType());
		assertEquals(context.getBean("ftpChannel"), TestUtils.getPropertyValue(ftpInbound, "outputChannel"));
		FtpInboundFileSynchronizingMessageSource inbound =
				(FtpInboundFileSynchronizingMessageSource) TestUtils.getPropertyValue(ftpInbound, "source");

		assertSame(dirScanner, TestUtils.getPropertyValue(inbound, "fileSource.scanner"));

		FtpInboundFileSynchronizer fisync =
				(FtpInboundFileSynchronizer) TestUtils.getPropertyValue(inbound, "synchronizer");
		assertEquals("'foo/bar'", TestUtils.getPropertyValue(fisync, "remoteDirectoryExpression", Expression.class)
				.getExpressionString());
		assertNotNull(TestUtils.getPropertyValue(fisync, "localFilenameGeneratorExpression"));
		assertTrue(TestUtils.getPropertyValue(fisync, "preserveTimestamp", Boolean.class));
		assertEquals(".foo", TestUtils.getPropertyValue(fisync, "temporaryFileSuffix", String.class));
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(fisync, "remoteFileSeparator");
		assertNotNull(remoteFileSeparator);
		assertEquals("", remoteFileSeparator);

		FileListFilter<?> filter = TestUtils.getPropertyValue(fisync, "filter", FileListFilter.class);
		assertNotNull(filter);
		assertThat(filter, instanceOf(CompositeFileListFilter.class));
		Set<?> fileFilters = TestUtils.getPropertyValue(filter, "fileFilters", Set.class);

		Iterator<?> filtersIterator = fileFilters.iterator();
		assertThat(filtersIterator.next(), instanceOf(FtpSimplePatternFileListFilter.class));
		assertThat(filtersIterator.next(), instanceOf(FtpPersistentAcceptOnceFileListFilter.class));

		Object sessionFactory = TestUtils.getPropertyValue(fisync, "remoteFileTemplate.sessionFactory");
		assertTrue(DefaultFtpSessionFactory.class.isAssignableFrom(sessionFactory.getClass()));
		FileListFilter<?> acceptAllFilter = context.getBean("acceptAllFilter", FileListFilter.class);
		assertTrue(TestUtils.getPropertyValue(inbound, "fileSource.scanner.filter.fileFilters", Collection.class)
				.contains(acceptAllFilter));
		final AtomicReference<Method> genMethod = new AtomicReference<Method>();
		ReflectionUtils.doWithMethods(AbstractInboundFileSynchronizer.class, method -> {
			method.setAccessible(true);
			genMethod.set(method);
		}, method -> "generateLocalFileName".equals(method.getName()));
		assertEquals("FOO.afoo", genMethod.get().invoke(fisync, "foo"));
		assertEquals(42, inbound.getMaxFetchSize());
	}

	@Test
	public void cachingSessionFactory() throws Exception {
		Object sessionFactory = TestUtils.getPropertyValue(simpleAdapterWithCachedSessions,
				"source.synchronizer.remoteFileTemplate.sessionFactory");
		assertEquals(CachingSessionFactory.class, sessionFactory.getClass());
		FtpInboundFileSynchronizer fisync =
				TestUtils.getPropertyValue(simpleAdapterWithCachedSessions, "source.synchronizer",
						FtpInboundFileSynchronizer.class);
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(fisync, "remoteFileSeparator");
		assertNotNull(remoteFileSeparator);
		assertEquals("/", remoteFileSeparator);
		assertEquals("foo/bar", TestUtils.getPropertyValue(fisync, "remoteDirectoryExpression", Expression.class)
				.getExpressionString());
		assertEquals(Integer.MIN_VALUE,
				TestUtils.getPropertyValue(simpleAdapterWithCachedSessions, "source.maxFetchSize"));
	}

	@Test
	public void testAutoChannel() {
		assertSame(autoChannel, TestUtils.getPropertyValue(autoChannelAdapter, "outputChannel"));
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

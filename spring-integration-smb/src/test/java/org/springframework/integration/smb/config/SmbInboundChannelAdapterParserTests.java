/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.integration.smb.config;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.smb.filters.SmbPersistentAcceptOnceFileListFilter;
import org.springframework.integration.smb.filters.SmbSimplePatternFileListFilter;
import org.springframework.integration.smb.inbound.SmbInboundFileSynchronizer;
import org.springframework.integration.smb.inbound.SmbInboundFileSynchronizingMessageSource;
import org.springframework.integration.smb.session.SmbSession;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Markus Spann
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Prafull Kumar Soni
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SmbInboundChannelAdapterParserTests {

	@Autowired
	ApplicationContext applicationContext;

	@Test(timeout = 100000)
	public void testSmbInboundChannelAdapterComplete() {

		final SourcePollingChannelAdapter adapter = this.applicationContext.getBean("smbInbound", SourcePollingChannelAdapter.class);
		final PriorityBlockingQueue<?> queue = TestUtils.getPropertyValue(adapter, "source.fileSource.toBeReceived", PriorityBlockingQueue.class);
		assertNotNull(queue.comparator());
		assertEquals("smbInbound", adapter.getComponentName());
		assertEquals("smb:inbound-channel-adapter", adapter.getComponentType());
		assertEquals(applicationContext.getBean("smbChannel"), TestUtils.getPropertyValue(adapter, "outputChannel"));
		SmbInboundFileSynchronizingMessageSource inbound =
				(SmbInboundFileSynchronizingMessageSource) TestUtils.getPropertyValue(adapter, "source");

		SmbInboundFileSynchronizer fisync =
				(SmbInboundFileSynchronizer) TestUtils.getPropertyValue(inbound, "synchronizer");
		assertEquals(".working.tmp", TestUtils.getPropertyValue(fisync, "temporaryFileSuffix", String.class));
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(fisync, "remoteFileSeparator");
		assertNotNull(remoteFileSeparator);
		assertEquals("", remoteFileSeparator);
		FileListFilter<?> filter = TestUtils.getPropertyValue(fisync, "filter", FileListFilter.class);
		assertNotNull(filter);
		assertThat(filter, instanceOf(CompositeFileListFilter.class));
		Set<?> fileFilters = TestUtils.getPropertyValue(filter, "fileFilters", Set.class);

		Iterator<?> filtersIterator = fileFilters.iterator();
		assertThat(filtersIterator.next(), instanceOf(SmbSimplePatternFileListFilter.class));
		assertThat(filtersIterator.next(), instanceOf(SmbPersistentAcceptOnceFileListFilter.class));
		Object sessionFactory = TestUtils.getPropertyValue(fisync, "remoteFileTemplate.sessionFactory");
		assertTrue(SmbSessionFactory.class.isAssignableFrom(sessionFactory.getClass()));

		FileListFilter<?> acceptAllFilter = this.applicationContext.getBean("acceptAllFilter", FileListFilter.class);
		assertTrue(TestUtils.getPropertyValue(inbound, "fileSource.scanner.filter.fileFilters", Collection.class)
				.contains(acceptAllFilter));
	}

	@Test
	public void testNoCachingSessionFactoryByDefault() {
		SourcePollingChannelAdapter adapter = applicationContext.getBean("simpleAdapter", SourcePollingChannelAdapter.class);
		Object sessionFactory = TestUtils.getPropertyValue(adapter, "source.synchronizer.remoteFileTemplate.sessionFactory");
		assertThat(sessionFactory, instanceOf(SmbSessionFactory.class));
		SmbInboundFileSynchronizer fisync =
				TestUtils.getPropertyValue(adapter, "source.synchronizer", SmbInboundFileSynchronizer.class);
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(fisync, "remoteFileSeparator");
		assertNotNull(remoteFileSeparator);
		assertEquals("/", remoteFileSeparator);
	}

	@Test(timeout = 10000)
	public void testSmbInboundChannelAdapterCompleteNoId() {

		Map<String, SourcePollingChannelAdapter> spcas = applicationContext.getBeansOfType(SourcePollingChannelAdapter.class);
		SourcePollingChannelAdapter adapter = null;
		for (String key : spcas.keySet()) {
			if (!key.equals("smbInbound") && !key.equals("simpleAdapter")) {
				adapter = spcas.get(key);
			}
		}
		assertNotNull(adapter);
	}


	public static class TestSessionFactoryBean implements FactoryBean<SmbSessionFactory> {

		public SmbSessionFactory getObject() {
			SmbSessionFactory smbFactory = mock(SmbSessionFactory.class);
			SmbSession session = mock(SmbSession.class);
			when(smbFactory.getSession()).thenReturn(session);
			return smbFactory;
		}

		public Class<?> getObjectType() {
			return SmbSessionFactory.class;
		}

		public boolean isSingleton() {
			return true;
		}
	}

}

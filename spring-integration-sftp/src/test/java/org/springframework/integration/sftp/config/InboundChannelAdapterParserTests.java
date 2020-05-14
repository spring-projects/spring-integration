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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class InboundChannelAdapterParserTests {

	@BeforeEach
	public void prepare() {
		new File("foo").delete();
	}

	@Test
	public void testAutoStartup() {
		ConfigurableApplicationContext context =
				new ClassPathXmlApplicationContext("SftpInboundAutostartup-context.xml", this.getClass());

		SourcePollingChannelAdapter adapter = context.getBean("sftpAutoStartup", SourcePollingChannelAdapter.class);
		assertThat(adapter.isRunning()).isFalse();
		context.close();
	}

	@Test
	public void testWithLocalFiles() {
		ConfigurableApplicationContext context =
				new ClassPathXmlApplicationContext("InboundChannelAdapterParserTests-context.xml", this.getClass());
		assertThat(new File("src/main/resources").exists()).isTrue();

		Object adapter = context.getBean("sftpAdapterAutoCreate");
		assertThat(adapter instanceof SourcePollingChannelAdapter).isTrue();
		SftpInboundFileSynchronizingMessageSource source =
				(SftpInboundFileSynchronizingMessageSource) TestUtils.getPropertyValue(adapter, "source");
		assertThat(source).isNotNull();

		PriorityBlockingQueue<?> blockingQueue =
				TestUtils.getPropertyValue(adapter, "source.fileSource.toBeReceived", PriorityBlockingQueue.class);
		Comparator<?> comparator = blockingQueue.comparator();

		assertThat(comparator).isNotNull();
		SftpInboundFileSynchronizer synchronizer =
				TestUtils.getPropertyValue(source, "synchronizer", SftpInboundFileSynchronizer.class);
		assertThat(TestUtils.getPropertyValue(synchronizer, "remoteDirectoryExpression", Expression.class)
				.getExpressionString()).isEqualTo("'/foo'");
		assertThat(TestUtils.getPropertyValue(synchronizer, "localFilenameGeneratorExpression")).isNotNull();
		assertThat(TestUtils.getPropertyValue(synchronizer, "preserveTimestamp", Boolean.class)).isTrue();
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(synchronizer, "remoteFileSeparator");
		assertThat(TestUtils.getPropertyValue(synchronizer, "temporaryFileSuffix", String.class)).isEqualTo(".bar");
		assertThat(TestUtils.getPropertyValue(synchronizer, "remoteFileMetadataStore"))
				.isSameAs(context.getBean(MetadataStore.class));
		assertThat(TestUtils.getPropertyValue(synchronizer, "metadataStorePrefix", String.class))
				.isEqualTo("testPrefix");
		assertThat(remoteFileSeparator).isNotNull();
		assertThat(remoteFileSeparator).isEqualTo(".");
		PollableChannel requestChannel = context.getBean("requestChannel", PollableChannel.class);
		assertThat(requestChannel.receive(10000)).isNotNull();
		FileListFilter<?> acceptAllFilter = context.getBean("acceptAllFilter", FileListFilter.class);
		@SuppressWarnings("unchecked")
		Collection<FileListFilter<?>> filters =
				TestUtils.getPropertyValue(source, "fileSource.scanner.filter.fileFilters", Collection.class);
		assertThat(filters).contains(acceptAllFilter);
		assertThat(source.getMaxFetchSize()).isEqualTo(42);
		context.close();
	}

	@Test
	public void testAutoChannel() {
		ConfigurableApplicationContext context =
				new ClassPathXmlApplicationContext("InboundChannelAdapterParserTests-context.xml", this.getClass());
		// Auto-created channel
		MessageChannel autoChannel = context.getBean("autoChannel", MessageChannel.class);
		SourcePollingChannelAdapter autoChannelAdapter = context.getBean("autoChannel.adapter",
				SourcePollingChannelAdapter.class);
		assertThat(TestUtils
				.getPropertyValue(autoChannelAdapter, "source.synchronizer.remoteDirectoryExpression",
						Expression.class)
				.getExpressionString()).isEqualTo("/foo");
		assertThat(TestUtils.getPropertyValue(autoChannelAdapter, "outputChannel")).isSameAs(autoChannel);
		assertThat(TestUtils.getPropertyValue(autoChannelAdapter, "source.maxFetchSize")).isEqualTo(Integer.MIN_VALUE);
		context.close();
	}

	@Test
	//exactly one of 'filename-pattern' or 'filter' is allowed on SFTP inbound adapter
	public void testFailWithFilePatternAndFilter() {
		assertThat(!new File("target/bar").exists()).isTrue();
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("InboundChannelAdapterParserTests-context-fail.xml",
								getClass()));
	}

	@Test
	public void testLocalDirAutoCreated() {
		assertThat(new File("foo").exists()).isFalse();
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"InboundChannelAdapterParserTests-context.xml", getClass());
		assertThat(new File("foo").exists()).isTrue();
		context.close();
	}

	@Test
	public void testLocalDirAutoCreateFailed() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"InboundChannelAdapterParserTests-context-fail-autocreate.xml",
								getClass()));
	}

	@AfterEach
	public void cleanUp() {
		new File("foo").delete();
	}

}

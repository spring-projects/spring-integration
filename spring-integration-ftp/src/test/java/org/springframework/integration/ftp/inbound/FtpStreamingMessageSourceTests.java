/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.ftp.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.AcceptAllFileListFilter;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.FtpTestSupport;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.session.FtpFileInfo;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.test.util.OnlyOnceTrigger;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.StreamTransformer;
import org.springframework.messaging.Message;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class FtpStreamingMessageSourceTests extends FtpTestSupport {

	@Autowired
	private QueueChannel data;

	@Autowired
	private FtpStreamingMessageSource source;

	@Autowired
	private SourcePollingChannelAdapter adapter;

	@Autowired
	private Config config;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ConcurrentMap<String, String> metadataMap;

	@SuppressWarnings("unchecked")
	@Test
	public void testAllContents() {
		this.adapter.start();
		Message<byte[]> received = (Message<byte[]>) this.data.receive(10000);
		assertThat(received).isNotNull();
		assertThat(new String(received.getPayload())).isEqualTo("source1");
		String fileInfo = (String) received.getHeaders().get(FileHeaders.REMOTE_FILE_INFO);
		assertThat(fileInfo).contains("remoteDirectory\":\"ftpSource");
		assertThat(fileInfo).contains("permissions\":\"-rw-------");
		assertThat(fileInfo).contains("size\":7");
		assertThat(fileInfo).contains("directory\":false");
		assertThat(fileInfo).contains("filename\":\" ftpSource1.txt");
		assertThat(fileInfo).contains("modified\":");
		assertThat(fileInfo).contains("link\":false");
		received = (Message<byte[]>) this.data.receive(10000);
		assertThat(received).isNotNull();
		assertThat(new String(received.getPayload())).isEqualTo("source2");
		fileInfo = (String) received.getHeaders().get(FileHeaders.REMOTE_FILE_INFO);
		assertThat(fileInfo).contains("remoteDirectory\":\"ftpSource");
		assertThat(fileInfo).contains("permissions\":\"-rw-------");
		assertThat(fileInfo).contains("size\":7");
		assertThat(fileInfo).contains("directory\":false");
		assertThat(fileInfo).contains("filename\":\"ftpSource2.txt");
		assertThat(fileInfo).contains("modified\":");
		assertThat(fileInfo).contains("link\":false");

		this.adapter.stop();
		this.source.setFileInfoJson(false);
		this.data.purge(null);
		this.metadataMap.clear();
		this.adapter.setTrigger(new OnlyOnceTrigger());
		this.adapter.setMaxMessagesPerPoll(1);
		this.adapter.start();
		received = (Message<byte[]>) this.data.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE_INFO)).isInstanceOf(FtpFileInfo.class);
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_HOST_PORT, String.class)).contains("localhost:");
		assertThat(TestUtils.getPropertyValue(source, "toBeReceived", BlockingQueue.class)).hasSize(1);
		assertThat(this.metadataMap).hasSize(1);
		this.adapter.stop();
		assertThat(TestUtils.getPropertyValue(source, "toBeReceived", BlockingQueue.class)).isEmpty();
	}

	@Test
	public void testMaxFetch() throws Exception {
		FtpStreamingMessageSource messageSource = buildSource();
		messageSource.setFilter(new AcceptAllFileListFilter<>());
		messageSource.afterPropertiesSet();
		messageSource.start();
		Message<InputStream> received = messageSource.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo(" ftpSource1.txt");

		Closeable closeableResource = StaticMessageHeaderAccessor.getCloseableResource(received);
		if (closeableResource != null) {
			closeableResource.close();
		}
	}

	@Test
	public void testMaxFetchNoFilter() throws Exception {
		FtpStreamingMessageSource messageSource = buildSource();
		messageSource.setFilter(null);
		messageSource.afterPropertiesSet();
		messageSource.start();
		Message<InputStream> received = messageSource.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo(" ftpSource1.txt");

		Closeable closeableResource = StaticMessageHeaderAccessor.getCloseableResource(received);
		if (closeableResource != null) {
			closeableResource.close();
		}
	}

	private FtpStreamingMessageSource buildSource() {
		FtpStreamingMessageSource messageSource = new FtpStreamingMessageSource(this.config.template(),
				Comparator.comparing(FTPFile::getName));
		messageSource.setRemoteDirectory("ftpSource/");
		messageSource.setBeanFactory(this.context);
		return messageSource;
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public QueueChannel data() {
			return new QueueChannel();
		}

		@Bean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerMetadata defaultPoller() {
			PollerMetadata pollerMetadata = new PollerMetadata();
			pollerMetadata.setTrigger(new PeriodicTrigger(500));
			pollerMetadata.setMaxMessagesPerPoll(2);
			return pollerMetadata;
		}

		@Bean
		public ConcurrentMap<String, String> metadataMap() {
			return new ConcurrentHashMap<>();
		}

		@Bean
		@InboundChannelAdapter(channel = "stream", autoStartup = "false")
		public MessageSource<InputStream> ftpMessageSource() {
			FtpStreamingMessageSource messageSource = new FtpStreamingMessageSource(template(),
					Comparator.comparing(FTPFile::getName));
			messageSource.setFilter(
					new FtpPersistentAcceptOnceFileListFilter(
							new SimpleMetadataStore(metadataMap()), "testStreaming"));
			messageSource.setRemoteDirectory("ftpSource/");
			return messageSource;
		}

		@Bean
		@Transformer(inputChannel = "stream", outputChannel = "data")
		public org.springframework.integration.transformer.Transformer transformer() {
			return new StreamTransformer();
		}

		@Bean
		public FtpRemoteFileTemplate template() {
			return new FtpRemoteFileTemplate(ftpSessionFactory());
		}

		@Bean
		public SessionFactory<FTPFile> ftpSessionFactory() {
			return FtpStreamingMessageSourceTests.sessionFactory();
		}

	}

}

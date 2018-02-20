/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.integration.ftp.inbound;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.Comparator;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.AcceptAllFileListFilter;
import org.springframework.integration.file.remote.FileInfo;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.FtpTestSupport;
import org.springframework.integration.ftp.session.FtpFileInfo;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.test.rule.Log4j2LevelAdjuster;
import org.springframework.integration.transformer.StreamTransformer;
import org.springframework.messaging.Message;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class FtpStreamingMessageSourceTests extends FtpTestSupport {

	@Autowired
	private QueueChannel data;

	@Autowired
	private FtpStreamingMessageSource source;

	@Autowired
	private SourcePollingChannelAdapter adapter;

	@Rule
	public Log4j2LevelAdjuster adjuster =
			Log4j2LevelAdjuster.debug()
					.categories(true, "org.apache.commons");

	@SuppressWarnings("unchecked")
	@Test
	public void testAllContents() {
		Message<byte[]> received = (Message<byte[]>) this.data.receive(10000);
		assertNotNull(received);
		assertThat(new String(received.getPayload()), equalTo("source1"));
		String fileInfo = (String) received.getHeaders().get(FileHeaders.REMOTE_FILE_INFO);
		assertThat(fileInfo, containsString("remoteDirectory\":\"ftpSource"));
		assertThat(fileInfo, containsString("permissions\":\"-rw-------"));
		assertThat(fileInfo, containsString("size\":7"));
		assertThat(fileInfo, containsString("directory\":false"));
		assertThat(fileInfo, containsString("filename\":\" ftpSource1.txt"));
		assertThat(fileInfo, containsString("modified\":"));
		assertThat(fileInfo, containsString("link\":false"));
		received = (Message<byte[]>) this.data.receive(10000);
		assertNotNull(received);
		assertThat(new String(received.getPayload()), equalTo("source2"));
		fileInfo = (String) received.getHeaders().get(FileHeaders.REMOTE_FILE_INFO);
		assertThat(fileInfo, containsString("remoteDirectory\":\"ftpSource"));
		assertThat(fileInfo, containsString("permissions\":\"-rw-------"));
		assertThat(fileInfo, containsString("size\":7"));
		assertThat(fileInfo, containsString("directory\":false"));
		assertThat(fileInfo, containsString("filename\":\"ftpSource2.txt"));
		assertThat(fileInfo, containsString("modified\":"));
		assertThat(fileInfo, containsString("link\":false"));

		this.adapter.stop();
		this.source.setFileInfoJson(false);
		this.data.purge(null);
		this.adapter.start();
		received = (Message<byte[]>) this.data.receive(10000);
		assertNotNull(received);
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE_INFO), instanceOf(FtpFileInfo.class));
		this.adapter.stop();
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
			pollerMetadata.setMaxMessagesPerPoll(2000);
			return pollerMetadata;
		}

		@Bean
		@InboundChannelAdapter(channel = "stream")
		public MessageSource<InputStream> ftpMessageSource() {
			FtpStreamingMessageSource messageSource = new FtpStreamingMessageSource(template(),
					Comparator.comparing(FileInfo::getFilename));
			messageSource.setFilter(new AcceptAllFileListFilter<>());
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

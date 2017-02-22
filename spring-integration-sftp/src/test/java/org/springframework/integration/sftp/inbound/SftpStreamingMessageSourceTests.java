/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.sftp.inbound;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

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
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.sftp.SftpTestSupport;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.integration.transformer.StreamTransformer;
import org.springframework.messaging.Message;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.3
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class SftpStreamingMessageSourceTests extends SftpTestSupport {

	@Autowired
	private QueueChannel data;

	@Autowired
	private SftpStreamingMessageSource source;

	@Autowired
	private SourcePollingChannelAdapter adapter;

	@SuppressWarnings("unchecked")
	@Test
	public void testAllContents() {
		Message<byte[]> received = (Message<byte[]>) this.data.receive(10000);
		assertNotNull(received);
		assertThat(new String(received.getPayload()), equalTo("source1"));
		String fileInfo = (String) received.getHeaders().get(FileHeaders.REMOTE_FILE_INFO);
		assertThat(fileInfo, containsString("remoteDirectory\":\"sftpSource"));
		assertThat(fileInfo, containsString("permissions\":"));
		assertThat(fileInfo, containsString("size\":7"));
		assertThat(fileInfo, containsString("directory\":false"));
		assertThat(fileInfo, containsString("filename\":\" sftpSource1.txt"));
		assertThat(fileInfo, containsString("modified\":"));
		assertThat(fileInfo, containsString("link\":false"));
		received = (Message<byte[]>) this.data.receive(10000);
		assertNotNull(received);
		fileInfo = (String) received.getHeaders().get(FileHeaders.REMOTE_FILE_INFO);
		assertThat(fileInfo, containsString("remoteDirectory\":\"sftpSource"));
		assertThat(fileInfo, containsString("permissions\":"));
		assertThat(fileInfo, containsString("size\":7"));
		assertThat(fileInfo, containsString("directory\":false"));
		assertThat(fileInfo, containsString("filename\":\"sftpSource2.txt"));
		assertThat(fileInfo, containsString("modified\":"));
		assertThat(fileInfo, containsString("link\":false"));
		assertThat(new String(received.getPayload()), equalTo("source2"));

		this.adapter.stop();
		this.source.setFileInfoJson(false);
		this.data.purge(null);
		this.adapter.start();
		received = (Message<byte[]>) this.data.receive(10000);
		assertNotNull(received);
		assertThat(received.getHeaders().get(FileHeaders.REMOTE_FILE_INFO), instanceOf(SftpFileInfo.class));
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
		public MessageSource<InputStream> sftpMessageSource() {
			SftpStreamingMessageSource messageSource = new SftpStreamingMessageSource(template(), null);
			messageSource.setRemoteDirectory("sftpSource/");
			return messageSource;
		}

		@Bean
		@Transformer(inputChannel = "stream", outputChannel = "data")
		public org.springframework.integration.transformer.Transformer transformer() {
			return new StreamTransformer();
		}

		@Bean
		public SftpRemoteFileTemplate template() {
			return new SftpRemoteFileTemplate(ftpSessionFactory());
		}

		@Bean
		public SessionFactory<LsEntry> ftpSessionFactory() {
			return SftpStreamingMessageSourceTests.sessionFactory();
		}

	}

}

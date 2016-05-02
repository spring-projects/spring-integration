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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.sftp.SftpTestSupport;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.integration.transformer.StreamTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
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
	public PollableChannel data;

	@SuppressWarnings("unchecked")
	@Test
	public void testAllContents() {
		Message<byte[]> received = (Message<byte[]>) this.data.receive(10000);
		assertNotNull(received);
		assertThat(new String(received.getPayload()), equalTo("source1"));
		received = (Message<byte[]>) this.data.receive(10000);
		assertNotNull(received);
		assertThat(new String(received.getPayload()), equalTo("source2"));
		assertNull(this.data.receive(10));
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
			SftpStreamingMessageSource messageSource = new SftpStreamingMessageSource(template(), null);
			messageSource.setRemoteDirectory("sftpSource/");
			messageSource.setFilter(new SftpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "streaming"));
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

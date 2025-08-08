/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.sftp.inbound;

import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.SftpTestSupport;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem bilan
 * @author Darryl Smith
 *
 * @since 5.0.7
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class SftpMessageSourceTests extends SftpTestSupport {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private SessionFactory<SftpClient.DirEntry> sessionFactory;

	@Test
	public void testMaxFetch() {
		SftpInboundFileSynchronizingMessageSource messageSource = buildSource();
		Message<?> received = messageSource.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(FileHeaders.FILENAME))
				.isIn(" sftpSource1.txt", "sftpSource2.txt");
	}

	private SftpInboundFileSynchronizingMessageSource buildSource() {
		SftpInboundFileSynchronizer sync = new SftpInboundFileSynchronizer(sessionFactory);
		sync.setRemoteDirectory("/sftpSource/");
		sync.setBeanFactory(this.context);
		SftpInboundFileSynchronizingMessageSource messageSource = new SftpInboundFileSynchronizingMessageSource(sync);
		messageSource.setLocalDirectory(getTargetLocalDirectory());
		messageSource.setMaxFetchSize(1);
		messageSource.setBeanFactory(this.context);
		messageSource.setBeanName("source");
		messageSource.afterPropertiesSet();
		return messageSource;
	}

	@Configuration
	public static class Config {

		@Bean
		public SessionFactory<SftpClient.DirEntry> ftpSessionFactory() {
			return SftpMessageSourceTests.sessionFactory();
		}

	}

}

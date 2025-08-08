/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.ftp.inbound;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.ftp.FtpTestSupport;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 5.0.7
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class FtpMessageSourceTests extends FtpTestSupport {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testMaxFetch() throws Exception {
		FtpInboundFileSynchronizingMessageSource messageSource = buildSource();
		Message<?> received = messageSource.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(FileHeaders.FILENAME)).isEqualTo(" ftpSource1.txt");
	}

	private FtpInboundFileSynchronizingMessageSource buildSource() throws Exception {
		FtpInboundFileSynchronizer sync = new FtpInboundFileSynchronizer(sessionFactory());
		sync.setRemoteDirectory("ftpSource/");
		sync.setBeanFactory(this.context);
		FtpInboundFileSynchronizingMessageSource messageSource = new FtpInboundFileSynchronizingMessageSource(sync);
		messageSource.setLocalDirectory(getTargetLocalDirectory());
		messageSource.setMaxFetchSize(1);
		messageSource.setBeanFactory(this.context);
		messageSource.setBeanName("source");
		messageSource.afterPropertiesSet();
		return messageSource;
	}

	@Configuration
	public static class Config {

	}

}

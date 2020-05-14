/*
 * Copyright 2018-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.ftp.FtpTestSupport;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

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

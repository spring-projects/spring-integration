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

package org.springframework.integration.sftp.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.sftp.SftpTestSupport;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem bilan
 *
 * @since 5.0.7
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class SftpMessageSourceTests extends SftpTestSupport {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testMaxFetch() throws Exception {
		SftpInboundFileSynchronizingMessageSource messageSource = buildSource();
		Message<?> received = messageSource.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(FileHeaders.FILENAME))
				.isIn(" sftpSource1.txt", "sftpSource2.txt");
	}

	private SftpInboundFileSynchronizingMessageSource buildSource() throws Exception {
		SftpInboundFileSynchronizer sync = new SftpInboundFileSynchronizer(sessionFactory());
		sync.setRemoteDirectory("sftpSource/");
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

	}

}

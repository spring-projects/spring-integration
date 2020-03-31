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

package org.springframework.integration.ftp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.ftp.session.DefaultFtpsSessionFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class FtpsOutboundChannelAdapterParserTests {

	@Autowired
	private EventDrivenConsumer ftpOutbound;

	@Autowired
	private MessageChannel ftpChannel;

	@Autowired
	private FileNameGenerator fileNameGenerator;

	@Test
	public void testFtpsOutboundChannelAdapterComplete() {
		assertThat(ftpOutbound).isInstanceOf(EventDrivenConsumer.class);
		assertThat(TestUtils.getPropertyValue(ftpOutbound, "inputChannel")).isEqualTo(this.ftpChannel);
		assertThat(ftpOutbound.getComponentName()).isEqualTo("ftpOutbound");
		FileTransferringMessageHandler<?> handler =
				TestUtils.getPropertyValue(ftpOutbound, "handler", FileTransferringMessageHandler.class);
		assertThat(TestUtils.getPropertyValue(handler, "remoteFileTemplate.fileNameGenerator"))
				.isEqualTo(this.fileNameGenerator);
		assertThat(TestUtils.getPropertyValue(handler, "remoteFileTemplate.charset")).isEqualTo(StandardCharsets.UTF_8);
		DefaultFtpsSessionFactory sf =
				TestUtils.getPropertyValue(handler, "remoteFileTemplate.sessionFactory",
						DefaultFtpsSessionFactory.class);
		assertThat(TestUtils.getPropertyValue(sf, "host")).isEqualTo("localhost");
		assertThat(TestUtils.getPropertyValue(sf, "port")).isEqualTo(22);
	}

}

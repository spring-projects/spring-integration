/*
 * Copyright 2002-present the original author or authors.
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

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.ftp.session.DefaultFtpsSessionFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
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
		assertThat(TestUtils.<DirectChannel>getPropertyValue(ftpOutbound, "inputChannel"))
				.isEqualTo(this.ftpChannel);
		assertThat(ftpOutbound.getComponentName()).isEqualTo("ftpOutbound");
		FileTransferringMessageHandler<?> handler =
				TestUtils.getPropertyValue(ftpOutbound, "handler");
		assertThat(TestUtils.<FileNameGenerator>getPropertyValue(handler,
				"remoteFileTemplate.fileNameGenerator"))
				.isEqualTo(this.fileNameGenerator);
		assertThat(TestUtils.<Object>getPropertyValue(handler, "remoteFileTemplate.charset"))
				.isEqualTo(StandardCharsets.UTF_8);
		DefaultFtpsSessionFactory sf =
				TestUtils.getPropertyValue(handler, "remoteFileTemplate.sessionFactory");
		assertThat(TestUtils.<String>getPropertyValue(sf, "host")).isEqualTo("localhost");
		assertThat(TestUtils.<Integer>getPropertyValue(sf, "port")).isEqualTo(22);
	}

}

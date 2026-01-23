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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizer;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizingMessageSource;
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
 */
@SpringJUnitConfig
@DirtiesContext
public class FtpsInboundChannelAdapterParserTests {

	@Autowired
	private SourcePollingChannelAdapter ftpInbound;

	@Autowired
	private MessageChannel ftpChannel;

	@Test
	public void testFtpsInboundChannelAdapterComplete() {
		assertThat(ftpInbound.getComponentName()).isEqualTo("ftpInbound");
		assertThat(ftpInbound.getComponentType()).isEqualTo("ftp:inbound-channel-adapter");
		assertThat(TestUtils.<Object>getPropertyValue(ftpInbound, "pollingTask")).isNotNull();
		assertThat(TestUtils.<DirectChannel>getPropertyValue(ftpInbound, "outputChannel"))
				.isEqualTo(this.ftpChannel);
		FtpInboundFileSynchronizingMessageSource inbound = TestUtils.getPropertyValue(ftpInbound, "source");

		FtpInboundFileSynchronizer fisync = TestUtils.getPropertyValue(inbound, "synchronizer");
		assertThat(TestUtils.<Object>getPropertyValue(fisync, "filter")).isNotNull();

	}

}

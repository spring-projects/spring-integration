/*
 * Copyright 2002-present the original author or authors.
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

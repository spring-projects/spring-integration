/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ftp.config;

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

import static org.assertj.core.api.Assertions.assertThat;

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

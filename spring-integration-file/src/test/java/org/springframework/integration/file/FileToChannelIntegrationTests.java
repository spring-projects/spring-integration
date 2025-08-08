/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class FileToChannelIntegrationTests {

	@Autowired
	File inputDirectory;

	@Autowired
	PollableChannel fileMessages;

	@Autowired
	PollableChannel resultChannel;

	@Test
	public void fileMessageToChannel() throws Exception {
		File file = File.createTempFile("test", null, inputDirectory);
		file.setLastModified(System.currentTimeMillis() - 1000);

		Message<?> received = this.fileMessages.receive(10000);
		assertThat(received).isNotNull();
		Message<?> result = this.resultChannel.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(Boolean.TRUE);
		assertThat(!file.exists()).isTrue();
	}

	@Test
	public void directoryExhaustion() throws Exception {
		File.createTempFile("test", null, inputDirectory).setLastModified(System.currentTimeMillis() - 1000);
		Message<?> received = this.fileMessages.receive(10000);
		assertThat(received).isNotNull();
		assertThat(fileMessages.receive(200)).isNull();
	}

}

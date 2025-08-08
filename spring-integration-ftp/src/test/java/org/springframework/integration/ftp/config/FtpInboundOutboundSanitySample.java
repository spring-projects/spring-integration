/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ftp.config;

import java.io.File;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 */
public class FtpInboundOutboundSanitySample {

	@Test
	@Disabled
	public void testFtpInboundChannelAdapter() throws Exception {
		File fileA = new File("local-test-dir/a.test");
		if (fileA.exists()) {
			fileA.delete();
		}
		File fileB = new File("local-test-dir/b.test");
		if (fileB.exists()) {
			fileB.delete();
		}
		fileA = new File("remote-target-dir/a.test");
		if (fileA.exists()) {
			fileA.delete();
		}
		fileB = new File("remote-target-dir/b.test");
		if (fileB.exists()) {
			fileB.delete();
		}

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"FtpInboundChannelAdapterSample-context.xml", this.getClass());
		Thread.sleep(3000);
		fileA = new File("local-test-dir/b.test");
		fileB = new File("local-test-dir/b.test");
		assertThat(fileA.exists()).isTrue();
		assertThat(fileB.exists()).isTrue();
		context.close();
	}

	@Test
	@Disabled
	public void testFtpOutboundChannelAdapter() throws Exception {
		ClassPathXmlApplicationContext ac =
				new ClassPathXmlApplicationContext("FtpOutboundChannelAdapterSample-context.xml", this.getClass());
		File fileA = new File("local-test-dir/a.test");
		File fileB = new File("local-test-dir/b.test");
		MessageChannel ftpChannel = ac.getBean("ftpChannel", MessageChannel.class);
		ftpChannel.send(new GenericMessage<File>(fileA));
		ftpChannel.send(new GenericMessage<File>(fileB));
		Thread.sleep(3000);
		fileA = new File("remote-target-dir/a.test");
		fileB = new File("remote-target-dir/b.test");
		assertThat(fileA.exists()).isTrue();
		assertThat(fileB.exists()).isTrue();
		ac.close();
	}

}

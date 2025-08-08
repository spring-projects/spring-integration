/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.sftp.inbound;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.SftpTestSupport;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.1.7
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class RollbackLocalFilterTests extends SftpTestSupport {

	@BeforeAll
	@AfterAll
	public static void clean() {
		new File("local-test-dir/rollback/sftpSource2.txt").delete();
	}

	@Autowired
	private Crash crash;

	@Test
	public void testRollback() throws Exception {
		assertThat(this.crash.getLatch().await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.crash.getFile().getName()).isEqualTo("sftpSource2.txt");
	}

	public static class Crash {

		private final CountDownLatch latch = new CountDownLatch(2);

		private final AtomicBoolean shouldCrash = new AtomicBoolean();

		private volatile File file;

		public CountDownLatch getLatch() {
			return latch;
		}

		public File getFile() {
			return file;
		}

		public void handle(File in) {
			if (this.shouldCrash.compareAndSet(false, true)) {
				latch.countDown();
				throw new RuntimeException("foo");
			}
			this.file = in;
			latch.countDown();
		}

	}

	public static class Config {

		@Bean
		public SessionFactory<SftpClient.DirEntry> sftpSessionFactory() {
			return RollbackLocalFilterTests.sessionFactory();
		}

	}

}

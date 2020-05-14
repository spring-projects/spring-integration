/*
 * Copyright 2015-2020 the original author or authors.
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

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.SftpTestSupport;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * @author Gary Russell
 * @author Artem Bilan
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
		public SessionFactory<LsEntry> sftpSessionFactory() {
			return RollbackLocalFilterTests.sessionFactory();
		}

	}

}

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

package org.springframework.integration.sftp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousy
 * @author Gunnar Hillert
 *
 */
public class SftpInboundOutboundSanitySample {


	@Test
	@Disabled
	public void testInbound() throws Exception {
		File fileA = new File("local-test-dir/a.test");
		if (fileA.exists()) {
			fileA.delete();
		}
		File fileB = new File("local-test-dir/b.test");
		if (fileB.exists()) {
			fileB.delete();
		}
		fileA = new File("remote-target-dir/a.test-foo");
		if (fileA.exists()) {
			fileA.delete();
		}
		fileB = new File("remote-target-dir/b.test-foo");
		if (fileB.exists()) {
			fileB.delete();
		}

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"SftpInboundReceiveSample-ignored.xml", this.getClass());
		Thread.sleep(5000);
		fileA = new File("local-test-dir/a.test");
		fileB = new File("local-test-dir/b.test");
		assertThat(fileA.exists()).isTrue();
		assertThat(fileB.exists()).isTrue();
		context.close();
	}

	@Test
	@Disabled
	public void testOutbound() throws Exception {
		ClassPathXmlApplicationContext ac =
			new ClassPathXmlApplicationContext("SftpOutboundTransferSample-ignored.xml", this.getClass());
		File fileA = new File("local-test-dir/a.test");
		File fileB = new File("local-test-dir/b.test");
		MessageChannel ftpChannel = ac.getBean("ftpChannel", MessageChannel.class);
		ftpChannel.send(new GenericMessage<File>(fileA));
		ftpChannel.send(new GenericMessage<File>(fileB));
		Thread.sleep(6000);
		fileA = new File("remote-target-dir/a.test-foo");
		fileB = new File("remote-target-dir/b.test-foo");
		assertThat(fileA.exists()).isTrue();
		assertThat(fileB.exists()).isTrue();
		ac.close();
	}

}

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

import java.io.File;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * @author Oleg Zhurakousky
 *
 */
public class SftpOutboundTransferSample {

	@Test
	@Disabled
	public void testOutbound() throws Exception {
		ClassPathXmlApplicationContext ac =
			new ClassPathXmlApplicationContext("SftpOutboundTransferSample-ignored.xml", SftpOutboundTransferSample.class);
		ac.start();
		File file = new File("/Users/ozhurakousky/workspace-sts-2.3.3.M2/si/spring-integration/spring-integration-sftp/local-test-dir/foo.txt");
		if (file.exists()) {
			Message<File> message = MessageBuilder.withPayload(file).build();
			MessageChannel inputChannel = ac.getBean("inputChannel", MessageChannel.class);
			inputChannel.send(message);
			Thread.sleep(2000);
		}
		ac.close();
	}

}

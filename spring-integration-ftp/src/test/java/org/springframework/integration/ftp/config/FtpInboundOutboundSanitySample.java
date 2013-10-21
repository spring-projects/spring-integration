/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ftp.config;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 */
public class FtpInboundOutboundSanitySample {


	@Test
	@Ignore
	public void testFtpInboundChannelAdapter() throws Exception{
		File fileA = new File("local-test-dir/a.test");
		if (fileA.exists()){
			fileA.delete();
		}
		File fileB = new File("local-test-dir/b.test");
		if (fileB.exists()){
			fileB.delete();
		}
		fileA = new File("remote-target-dir/a.test");
		if (fileA.exists()){
			fileA.delete();
		}
		fileB = new File("remote-target-dir/b.test");
		if (fileB.exists()){
			fileB.delete();
		}

		new ClassPathXmlApplicationContext("FtpInboundChannelAdapterSample-context.xml", this.getClass());
		Thread.sleep(3000);
		fileA = new File("local-test-dir/b.test");
		fileB = new File("local-test-dir/b.test");
		assertTrue(fileA.exists());
		assertTrue(fileB.exists());
	}

	@Test
	@Ignore
	public void testFtpOutboundChannelAdapter() throws Exception{
		ApplicationContext ac =
			new ClassPathXmlApplicationContext("FtpOutboundChannelAdapterSample-context.xml", this.getClass());
		File fileA = new File("local-test-dir/a.test");
		File fileB = new File("local-test-dir/b.test");
		MessageChannel ftpChannel = ac.getBean("ftpChannel", MessageChannel.class);
		ftpChannel.send(new GenericMessage<File>(fileA));
		ftpChannel.send(new GenericMessage<File>(fileB));
		Thread.sleep(3000);
		fileA = new File("remote-target-dir/a.test");
		fileB = new File("remote-target-dir/b.test");
		assertTrue(fileA.exists());
		assertTrue(fileB.exists());
	}

}

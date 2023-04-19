/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.integration.smb.config;

import java.io.File;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.smb.AbstractBaseTests;
import org.springframework.integration.smb.inbound.SmbInboundFileSynchronizingMessageSource;
import org.springframework.integration.smb.session.SmbSession;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * System tests that perform SMB access without any mocking.
 * These tests are annotated with '@Ignore', as they requires real SMB share configured
 * in the application context/smbClientFactory in order to succeed.
 * The test cases create directories and files autonomously and perform clean-up
 * on a best effort basis.
 *
 * @author Markus Spann
 * @author Gunnar Hillert
 * @author Gregory Bragg
 * @author Artem Bilan
 */
public class SmbInboundOutboundSample extends AbstractBaseTests {

	private static final String INBOUND_APPLICATION_CONTEXT_XML = "SmbInboundChannelAdapterSample-context.xml";

	private static final String OUTBOUND_APPLICATION_CONTEXT_XML = "SmbOutboundChannelAdapterSample-context.xml";

	@Disabled("Actual SMB share must be configured in file [" + INBOUND_APPLICATION_CONTEXT_XML + "].")
	@Test
	public void testSmbInboundChannelAdapter() throws Exception {
		String testLocalDir = "test-temp/local-4/";
		String testRemoteDir = "test-temp/remote-4/";

		ApplicationContext ac = new ClassPathXmlApplicationContext(INBOUND_APPLICATION_CONTEXT_XML, this.getClass());

		Object consumer = ac.getBean("smbInboundChannelAdapter");
		assertThat(consumer).isInstanceOf(SourcePollingChannelAdapter.class);
		Object messageSource = TestUtils.getPropertyValue(consumer, "source");
		assertThat(messageSource).isInstanceOf(SmbInboundFileSynchronizingMessageSource.class);

		// retrieve the session factory bean to place a couple of test files remotely using a new session
		SmbSessionFactory smbSessionFactory = ac.getBean("smbSessionFactory", SmbSessionFactory.class);
		SmbSession smbSession = smbSessionFactory.getSession();

		// place text files onto the share
		smbSession.mkdir(testRemoteDir);

		String[] fileNames = createTestFileNames(5);
		for (String fileName : fileNames) {
			smbSession.write(("File [" + fileName + "] written by test case [" + getMethodName() + "].").getBytes(),
					testRemoteDir + fileName);
		}

		// allow time for the files to arrive locally
		Thread.sleep(5000);

		// confirm the local presence of all test files
		for (String fileName : fileNames) {
			assertFileExists(testLocalDir + fileName).deleteOnExit();
		}

	}

	@Disabled("Actual SMB share must be configured in file [" + OUTBOUND_APPLICATION_CONTEXT_XML + "].")
	@Test
	public void testSmbOutboundChannelAdapter() throws Exception {
		String testRemoteDir = "test-temp/remote-8/";
		String testLocalDir = "test-temp/local-8/";
		new File(testLocalDir).mkdirs();

		String[] fileNames = createTestFileNames(5);
		for (String fileName : fileNames) {
			writeToFile(("File [" + fileName + "] written by test case [" + getMethodName() + "].").getBytes(),
					testLocalDir + fileName);
		}

		ApplicationContext ac = new ClassPathXmlApplicationContext(OUTBOUND_APPLICATION_CONTEXT_XML, this.getClass());

		Object consumer = ac.getBean("smbOutboundChannelAdapter");
		assertThat(consumer).isInstanceOf(EventDrivenConsumer.class);
		Object messageSource = TestUtils.getPropertyValue(consumer, "handler");
		assertThat(messageSource).isInstanceOf(FileTransferringMessageHandler.class);

		MessageChannel smbChannel = ac.getBean("smbOutboundChannel", MessageChannel.class);

		for (String fileName : fileNames) {
			smbChannel.send(new GenericMessage<>(new File(testLocalDir + fileName)));
		}

		Thread.sleep(3000);

		// retrieve the session factory bean to check the test files are present in the remote location
		SmbSessionFactory smbSessionFactory = ac.getBean("smbSessionFactory", SmbSessionFactory.class);
		SmbSession smbSession = smbSessionFactory.getSession();

		for (String fileName : fileNames) {
			String remoteFile = testRemoteDir + fileName;
			assertThat(smbSession.exists(remoteFile)).as("Remote file [" + remoteFile + "] does not exist.").isTrue();
		}

	}

	private static String[] createTestFileNames(int _nbTestFiles) {
		String[] fileNames = new String[_nbTestFiles];
		for (int i = 0; i < fileNames.length; i++) {
			fileNames[i] = "test-file-" + i + ".txt";
		}
		return fileNames;
	}

	public static void main(String[] _args) throws Exception {
		runTests(SmbInboundOutboundSample.class, "testSmbOutboundChannelAdapter", "testSmbInboundChannelAdapter");
	}

}

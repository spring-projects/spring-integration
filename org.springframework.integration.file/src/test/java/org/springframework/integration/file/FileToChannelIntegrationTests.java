package org.springframework.integration.file;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileToChannelIntegrationTests {
	private static File inputDir;

	@Autowired
	PollableChannel fileMessages;

	@BeforeClass
	public static void setupInputDir() {
		inputDir = new File(System.getProperty("java.io.tmpdir") + "/"
				+ FileToChannelIntegrationTests.class.getSimpleName());
		inputDir.mkdir();
	}

	@After
	public void cleanoutInputDir() throws Exception {
		File[] listFiles = inputDir.listFiles();
		for (int i = 0; i < listFiles.length; i++) {
			listFiles[i].delete();
		}
	}

	@AfterClass
	public static void removeInputDir() throws Exception {
		inputDir.delete();
	}

	@Test(timeout = 2000)
	public void fileMessageToChannel() throws Exception {
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		Message<File> received = receiveFileMessage();
		while (received == null) {
			Thread.sleep(50);
			received = receiveFileMessage();
		}
		assertNotNull(received.getPayload());
	}

	@SuppressWarnings("unchecked")
	private Message<File> receiveFileMessage() {
		return (Message<File>) fileMessages.receive();
	}

	@Test(timeout = 2000)
	public void directoryExhaustion() throws Exception {
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		Message<File> received = receiveFileMessage();
		while (received == null) {
			Thread.sleep(5);
			received = receiveFileMessage();
		}
		assertNotNull(received.getPayload());
		assertNull(fileMessages.receive(200));
	}
}

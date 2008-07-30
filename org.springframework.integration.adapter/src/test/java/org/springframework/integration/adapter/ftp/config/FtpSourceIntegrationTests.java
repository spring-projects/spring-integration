package org.springframework.integration.adapter.ftp.config;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.integration.adapter.ftp.FtpSource;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;

/**
 * @author Iwein Fuld
 */
/*
 * These tests assume you have a local ftp server running. The whole class
 * should be disabled and only run when you have started your ftp server and are
 * in need of experimenting.
 * 
 * To pass the test you should have an ftp server running at localhost that
 * accepts a login for ftp-user/kaas and has a remote directory ftp-test with at
 * least one file in it. Nothing is stopping you from changing the code to your
 * needs of course, this is just a starting point for local testing.
 */
// ftp server dependency. comment away Ignore if you want to run this
@Ignore
public class FtpSourceIntegrationTests {

	private FtpSource ftpSource;

	private MessageCreator<List<File>, List<File>> messageCreator = new MessageCreator<List<File>, List<File>>() {

		public Message<List<File>> createMessage(List<File> object) {
			return new GenericMessage<List<File>>(object);
		}
	};

	private static File localWorkDir;

	@BeforeClass
	public static void initializeEnvironment() {
		localWorkDir = new File(System.getProperty("java.io.tmpdir") + "/" + FtpSourceIntegrationTests.class.getName());
		localWorkDir.mkdir();
	}

	@Before
	public void initializeFtpSource() throws Exception {
		ftpSource = new FtpSource(messageCreator);
		ftpSource.setHost("localhost");
		ftpSource.setUsername("ftp-user");
		ftpSource.setPassword("kaas");
		ftpSource.setLocalWorkingDirectory(localWorkDir);
		ftpSource.setRemoteWorkingDirectory("ftp-test");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void receive() {
		Message<List<File>> received = ftpSource.receive();
		assertTrue(received.getPayload().iterator().next().exists());
	}

	
}

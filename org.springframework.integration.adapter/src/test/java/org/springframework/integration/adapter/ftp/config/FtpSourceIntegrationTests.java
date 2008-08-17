package org.springframework.integration.adapter.ftp.config;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.ftp.FtpSource;
import org.springframework.integration.adapter.ftp.QueuedFTPClientPool;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.config.MessageBusParser;
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
		QueuedFTPClientPool queuedFTPClientPool = new QueuedFTPClientPool();
		ftpSource = new FtpSource(messageCreator, queuedFTPClientPool);
		queuedFTPClientPool.setHost("localhost");
		queuedFTPClientPool.setUsername("ftp-user");
		queuedFTPClientPool.setPassword("kaas");
		ftpSource.setLocalWorkingDirectory(localWorkDir);
		queuedFTPClientPool.setRemoteWorkingDirectory("ftp-test");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void receive() {
		Message<List<File>> received = ftpSource.receive();
		assertTrue(received.getPayload().iterator().next().exists());
	}
	
	@Test public void withChannelAdapter() {
		ApplicationContext context = new ClassPathXmlApplicationContext("ftpSourceWithChannelAdapter.xml", this.getClass());
		ChannelRegistry channelRegistry = (ChannelRegistry) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		PollableChannel input = (PollableChannel) channelRegistry.lookupChannel("output");
		List<File> files = new ArrayList<File>();
		files.add((File) input.receive().getPayload());
		files.add((File) input.receive().getPayload());
		assertTrue(files.containsAll(Arrays.asList(new File("file1"), new File("file2"))));
	}

}

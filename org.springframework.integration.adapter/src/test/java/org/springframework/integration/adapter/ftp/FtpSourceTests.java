package org.springframework.integration.adapter.ftp;

import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;

@SuppressWarnings("unchecked")
public class FtpSourceTests {

	private MessageCreator<List<File>, List<File>> messageCreator = createMock(MessageCreator.class);

	private FTPClient ftpClient = createMock(FTPClient.class);

	private FTPFile ftpFile = createMock(FTPFile.class);

	private Object[] globalMocks = new Object[] { messageCreator, ftpClient, ftpFile };

	private static final String HOST = "testHost";

	private static final String USER = "testUser";

	private static final String PASS = "testPass";

	private FtpSource ftpSource;

	private Long size = 100l;

	@Before
	public void initializeFtpSource() {
		ftpSource = new FtpSource(messageCreator, ftpClient);
		ftpSource.setHost(HOST);
		ftpSource.setUsername(USER);
		ftpSource.setPassword(PASS);
	}

	@Before
	public void clearState() {
		reset(globalMocks);
	}

	@Test
	public void retrieveSingleFile() throws Exception {
		// connect client and get file
		expect(ftpClient.isConnected()).andReturn(false);
		expect(ftpClient.isConnected()).andReturn(true).anyTimes();

		ftpClient.connect(HOST, 21);
		expect(ftpClient.login(USER, PASS)).andReturn(true);
		expect(ftpClient.setFileType(anyInt())).andReturn(true);
		expect(ftpClient.printWorkingDirectory()).andReturn("/");
		expect(ftpClient.listFiles()).andReturn(mockedFTPFilesNamed("test"));
		expect(ftpClient.retrieveFile(eq("test"), isA(OutputStream.class))).andReturn(true);
		// create message
		expect(messageCreator.createMessage(isA(List.class))).andReturn(
				new GenericMessage(Arrays.asList(new File("test"))));
		ftpClient.disconnect();
		replay(globalMocks);
		ftpSource.onSend(ftpSource.receive());
		verify(globalMocks);
	}

	private FTPFile[] mockedFTPFilesNamed(String... names) {
		List<FTPFile> files = new ArrayList<FTPFile>();
		// ensure difference by increasing size
		Calendar timestamp = Calendar.getInstance();
		size++;
		for (String name : names) {
			FTPFile ftpFile = createMock(FTPFile.class);
			expect(ftpFile.getName()).andReturn(name).anyTimes();
			expect(ftpFile.getTimestamp()).andReturn(timestamp).anyTimes();
			expect(ftpFile.getSize()).andReturn(size).anyTimes();
			files.add(ftpFile);
			replay(ftpFile);
		}
		return files.toArray(new FTPFile[] {});
	}

	@Test
	public void retrieveMultipleFiles() throws Exception {
		// connect client
		expect(ftpClient.isConnected()).andReturn(false);
		expect(ftpClient.isConnected()).andReturn(true).anyTimes();

		ftpClient.connect(HOST, 21);
		expect(ftpClient.login(USER, PASS)).andReturn(true);
		expect(ftpClient.setFileType(anyInt())).andReturn(true);
		expect(ftpClient.printWorkingDirectory()).andReturn("/");

		// get files
		expect(ftpClient.listFiles()).andReturn(mockedFTPFilesNamed("test", "test2")).times(2);

		expect(ftpClient.retrieveFile(eq("test"), isA(OutputStream.class))).andReturn(true);
		expect(ftpClient.retrieveFile(eq("test2"), isA(OutputStream.class))).andReturn(true);
		// create message
		List<File> files = Arrays.asList(new File("test"), new File("test2"));
		expect(messageCreator.createMessage(isA(List.class))).andReturn(new GenericMessage(files));
		ftpClient.disconnect();

		replay(globalMocks);
		Message receivedFiles = ftpSource.receive();
		ftpSource.onSend(receivedFiles);
		Message<List<File>> secondReceived = ftpSource.receive();
		verify(globalMocks);
		assertEquals(files, receivedFiles.getPayload());
		assertNull(secondReceived);
	}

	@Test
	public void retrieveMultipleChangingFiles() throws Exception {

		// assume client already connected
		expect(ftpClient.isConnected()).andReturn(true).anyTimes();
		// first run
		FTPFile[] mockedFTPFiles = mockedFTPFilesNamed("test", "test2");
		expect(ftpClient.listFiles()).andReturn(mockedFTPFiles);
		expect(ftpClient.retrieveFile(eq("test"), isA(OutputStream.class))).andReturn(true);
		expect(ftpClient.retrieveFile(eq("test2"), isA(OutputStream.class))).andReturn(true);

		ftpClient.disconnect();
		// second run, change the date so the messages should be retrieved again
		// expect(ftpClient.isConnected()).andReturn(true);
		FTPFile[] mockedFTPFiles2 = mockedFTPFilesNamed("test", "test2");
		expect(ftpClient.listFiles()).andReturn(mockedFTPFiles2);
		expect(ftpClient.retrieveFile(eq("test"), isA(OutputStream.class))).andReturn(true);
		expect(ftpClient.retrieveFile(eq("test2"), isA(OutputStream.class))).andReturn(true);
		// create message
		List<File> files = Arrays.asList(new File("test"), new File("test2"));
		expect(messageCreator.createMessage(isA(List.class))).andReturn(new GenericMessage(files)).times(2);
		ftpClient.disconnect();

		replay(globalMocks);
		Message receivedFiles = ftpSource.receive();
		ftpSource.onSend(receivedFiles);
		ftpSource.onSend(ftpSource.receive());
		verify(globalMocks);
		assertEquals(files, receivedFiles.getPayload());
	}

	@Test
	public void retrieveMaxFilesPerPayload() throws Exception {

		this.ftpSource.setMaxMessagesPerPayload(2);
		// assume client already connected
		expect(ftpClient.isConnected()).andReturn(true).anyTimes();
		// first run
		FTPFile[] mockedFTPFiles = mockedFTPFilesNamed("test", "test2", "test3");
		expect(ftpClient.listFiles()).andReturn(mockedFTPFiles);
		expect(ftpClient.retrieveFile(eq("test"), isA(OutputStream.class))).andReturn(true);
		expect(ftpClient.retrieveFile(eq("test2"), isA(OutputStream.class))).andReturn(true);

		ftpClient.disconnect();
		// second run
		expect(ftpClient.listFiles()).andReturn(mockedFTPFiles);
		expect(ftpClient.retrieveFile(eq("test3"), isA(OutputStream.class))).andReturn(true);
		// create message
		expect(messageCreator.createMessage(isA(List.class))).andAnswer(new IAnswer<Message<List<File>>>() {
			public Message<List<File>> answer() throws Throwable {
				return new GenericMessage(getCurrentArguments()[0]);
			}
		}).times(2);

		ftpClient.disconnect();

		replay(globalMocks);
		Message<List<File>> receivedFiles1 = ftpSource.receive();
		ftpSource.onSend(receivedFiles1);
		Message<List<File>> receivedFiles2 = ftpSource.receive();
		ftpSource.onSend(receivedFiles2);
		verify(globalMocks);
		assertEquals(2, receivedFiles1.getPayload().size());
		assertEquals(1, receivedFiles2.getPayload().size());
	}

}
